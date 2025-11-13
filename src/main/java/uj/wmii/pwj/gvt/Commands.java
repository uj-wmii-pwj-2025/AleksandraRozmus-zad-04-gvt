package uj.wmii.pwj.gvt;

import java.io.File;
import java.io.IOException;
import java.util.*;

interface Command {
    void execute(String... args);
}

class InitCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    InitCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (repo.isInitialized()) {
            exit.exit(10, "Current directory is already initialized.");
            return;
        }

        try {
            repo.init();
            exit.exit(0, "Current directory initialized successfully.");
        } catch (IOException e) {
            CommandsUtil.reportError(exit, "Underlying system problem. See ERR for details.", e, -3);
        }
    }
}

class AddCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    AddCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;
        if (!CommandsUtil.checkArgsLength(args, 1, exit, 20, "Please specify file to add.")) 
            return;

        String fileName = args[0];
        String message = CommandsUtil.parseMessage(args, "File added successfully.");

        File file = new File(fileName);
        if (!file.exists()) {
            exit.exit(21, "File not found. File: " + fileName);
            return;
        }

        try {
            Version last = repo.getLastVersion();
            Set<String> files;
            int newVersionNumber;

            if (last == null) {
                files = new HashSet<>();
                newVersionNumber = 0;
            } else {
                files = new HashSet<>(last.getFiles());
                newVersionNumber = last.getNumber() + 1;
            }

            if (!files.add(fileName)) {
                exit.exit(0, "File already added. File: " + fileName);
                return;
            }

            CommandsUtil.createAndSaveVersion(repo, last, message, files);
            exit.exit(0, "File added successfully. File: " + fileName);
        } catch (IOException e) {
            CommandsUtil.reportError(exit, "File cannot be added. See ERR for details. File: " + fileName, e, 22);
        }
    }
}

class DetachCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    DetachCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;
        if (!CommandsUtil.checkArgsLength(args, 1, exit, 30, "Please specify file to detach.")) 
            return;

        String fileName = args[0];
        String message = CommandsUtil.parseMessage(args, "File detached successfully.");

        try {
            Version last = repo.getLastVersion();
            Set<String> files = new HashSet<>(last.getFiles());

            if (!files.contains(fileName)) {
                exit.exit(0, "File is not added to gvt. File: " + fileName);
                return;
            }
            files.remove(fileName);

            CommandsUtil.createAndSaveVersion(repo, last, message, files);
            exit.exit(0, "File detached successfully. File: " + fileName);
        } catch (IOException e) {
            CommandsUtil.reportError(exit, "File cannot be detached, see ERR for details. File: " + fileName, e, 31);
        }
    }
}

class CommitCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    CommitCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;
        if (!CommandsUtil.checkArgsLength(args, 1, exit, 50, "Please specify file to commit.")) 
            return;

        String fileName = args[0];
        String message = CommandsUtil.parseMessage(args, "File committed successfully.");

        File file = new File(fileName);
        if (!file.exists()) {
            exit.exit(51, "File not found. File: " + fileName);
            return;
        }
        try {
            Version last = repo.getLastVersion();
            Set<String> files = new HashSet<>(last.getFiles());

            if (!files.contains(fileName)) {
                exit.exit(0, "File is not added to gvt. File: " + fileName);
                return;
            }
            CommandsUtil.createAndSaveVersion(repo, last, message, files);
            exit.exit(0, "File committed successfully. File: " + fileName);
        } catch (IOException e) {
            CommandsUtil.reportError(exit, "File cannot be committed, see ERR for details. File: " + fileName, e, 52);
        }
    }
}

class CheckoutCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    CheckoutCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;
        if (args.length == 0) {
            exit.exit(60, "Invalid version number: null");
            return;
        }

        int num;
        try {
            num = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            exit.exit(60, "Invalid version number: " + args[0]);
            return;
        }

        try {
            if (!repo.versionExists(num)) {
                exit.exit(60, "Invalid version number: " + num);
                return;
            }
            repo.restoreVersionFiles(num);
            exit.exit(0, "Checkout successful for version: " + num);
        } catch (IOException e) {
            CommandsUtil.reportError(exit, "Cannot restore files. See ERR for details.", e, -3);
        }
    }
}

class HistoryCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    HistoryCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;

        List<Version> versions = repo.getVersions();
        int limit = versions.size();

        if (args.length == 2 && args[0].equals("-last")) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit > versions.size() || limit < 0) 
                    limit = versions.size();
            } catch (NumberFormatException e) {}
        }

        List<Version> sublist = versions.subList(versions.size() - limit, versions.size());
        StringBuilder sb = new StringBuilder();
        int end = versions.size() - limit;

        for (int i = versions.size() - 1; i >= end; i--) {
            Version v = versions.get(i);
            String firstLine = v.getMessage().split("\n")[0];
            sb.append(v.getNumber()).append(": ").append(firstLine).append("\n");
        }

        exit.exit(0, sb.toString());
    }
}

class VersionCmd implements Command {
    private final GvtRepo repo;
    private final ExitHandler exit;

    VersionCmd(GvtRepo repo, ExitHandler exit) {
        this.repo = repo;
        this.exit = exit;
    }

    @Override
    public void execute(String... args) {
        if (!CommandsUtil.checkInit(repo, exit)) 
            return;

        List<Version> versions = repo.getVersions();
        int num;

        if (args.length == 0)
            num = versions.get(versions.size() - 1).getNumber();
        else {
            try {
                num = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                exit.exit(60, "Invalid version number: " + args[0] + ".");
                return;
            }
        }

        Version foundVersion = null;
        for (Version v : versions) {
            if (v.getNumber() == num) {
                foundVersion = v;
                break;
            }
        }

        if (foundVersion == null) {
            exit.exit(60, "Invalid version number: " + num + ".");
            return;
        }
        exit.exit(0, "Version: " + foundVersion.getNumber() + "\n" + foundVersion.getMessage());
    }
}

class CommandsUtil {
    static boolean checkInit(GvtRepo repo, ExitHandler exit) {
        if (!repo.isInitialized()) {
            exit.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
            return false;
        }
        return true;
    }

    static boolean checkArgsLength(String[] args, int min, ExitHandler exit, int errorCode, String msg) {
        if (args.length < min) {
            exit.exit(errorCode, msg);
            return false;
        }
        return true;
    }

    static String parseMessage(String[] args, String defaultMsg) {
        if (args.length > 2 && args[1].equals("-m"))
            return args[2];
        return defaultMsg + " File: " + args[0];
    }

    static void createAndSaveVersion(GvtRepo repo, Version last, String message, Set<String> files) throws IOException {
        Version newVer = new Version(last.getNumber() + 1, message, files);
        repo.addVersion(newVer);
        repo.saveVersion(newVer);
    }

    static void reportError(ExitHandler exit, String message, Exception e, int exitCode) {
        System.out.println(message);
        if (e != null)
            e.printStackTrace(System.err);
        exit.exit(exitCode, "");
    }
}
