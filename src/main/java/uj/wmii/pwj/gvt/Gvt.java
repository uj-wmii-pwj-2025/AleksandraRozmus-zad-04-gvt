package uj.wmii.pwj.gvt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Gvt {

    private final ExitHandler exitHandler;
    private final GvtRepo repo;

    public Gvt(ExitHandler exitHandler) {
        this.exitHandler = exitHandler;
        this.repo = new GvtRepo();
    }

    public static void main(String... args) {
        Gvt gvt = new Gvt(new ExitHandler());
        gvt.mainInternal(args);
    }

    public void mainInternal(String... args) {
        try {
            repo.loadVersions();
        } catch (IOException e) {
            exitHandler.exit(-3, "Cannot load repository.");
            return;
        }
        if (args.length == 0) {
            exitHandler.exit(1, "Please specify command.");
            return;
        }

        String cmd = args[0];
        String[] params = Arrays.copyOfRange(args, 1, args.length);
        Command command;

        switch (cmd) {
            case "init" -> command = new InitCmd(repo, exitHandler);
            case "add" -> command = new AddCmd(repo, exitHandler);
            case "detach" -> command = new DetachCmd(repo, exitHandler);
            case "commit" -> command = new CommitCmd(repo, exitHandler);
            case "checkout" -> command = new CheckoutCmd(repo, exitHandler);
            case "history" -> command = new HistoryCmd(repo, exitHandler);
            case "version" -> command = new VersionCmd(repo, exitHandler);
            default -> command = null;
        };

        if (command == null) {
            exitHandler.exit(1, "Unknown command " + cmd + ".");
            return;
        }

        try {
            command.execute(params);
        } catch (Exception e) {
            System.out.println("Underlying system problem. See ERR for details.");
            e.printStackTrace(System.err);
            exitHandler.exit(-3, "");
        }
    }
}

class GvtRepo {
    private final Path dirGvt = Paths.get(".gvt");
    private final List<Version> versions = new ArrayList<>();

    public boolean isInitialized() {
        return Files.exists(dirGvt);
    }

    public void init() throws IOException {
        Files.createDirectory(dirGvt);
        Version v0 = new Version(0, "GVT initialized.", new HashSet<>());
        versions.add(v0);
        saveVersion(v0);
    }

    public Version getLastVersion() {
        if (versions.isEmpty()) 
            return null;
        else 
            return versions.get(versions.size() - 1);
    }

    public boolean versionExists(int num) {
        for (Version v : versions) {
            if (v.getNumber() == num) 
                return true;
        }
        return false;
    }

    public void loadVersions() throws IOException {
        if (!isInitialized()) 
            return;

        Path history = dirGvt.resolve("history.txt");
        if (!Files.exists(history)) 
            return;

        List<String> lines = Files.readAllLines(history);
        int separator, versionNum;

        for (String line : lines) {
            separator = line.indexOf('|');
            if (separator >= 0) {
                versionNum = Integer.parseInt(line.substring(0, separator));
                String msg = line.substring(separator + 1).replace("\\n", "\n");
                versions.add(new Version(versionNum, msg, readFileList(versionNum)));
            }
        }
    }

    public Set<String> readFileList(int versionNum) throws IOException {
        Path file = dirGvt.resolve("version" + versionNum).resolve("file_list.txt");
        if (!Files.exists(file)) 
            return new HashSet<>();

        return new HashSet<>(Files.readAllLines(file));
    }

    public void saveVersion(Version version) throws IOException {
        Path versionDir = dirGvt.resolve("version" + version.getNumber());
        Files.createDirectories(versionDir);

        for (String file : version.getFiles()) {
            Path src = Paths.get(file);
            if (Files.exists(src)) 
                Files.copy(src, versionDir.resolve(src.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.write(versionDir.resolve("file_list.txt"), version.getFiles());

        Path history = dirGvt.resolve("history.txt");
        String message = version.getMessage().replace("\n", "\\n");

        Files.writeString(history, version.getNumber() + "|" + message + "\n",
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void restoreVersionFiles(int versionNum) throws IOException {
        Path versionDir = dirGvt.resolve("version" + versionNum);

        if (!Files.exists(versionDir))
            throw new IOException("");

        Set<String> files = readFileList(versionNum);
        for (String f : files) {
            Path src = versionDir.resolve(Paths.get(f).getFileName());
            if (Files.exists(src))
                Files.copy(src, Paths.get(f), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void addVersion(Version v) {
        versions.add(v);
    }
}

class Version {
    private final int number;
    private final String message;
    private final Set<String> files;

    public Version(int number, String message, Set<String> files) {
        this.number = number;
        this.message = message;
        this.files = files;
    }

    public int getNumber() { return number; }

    public String getMessage() { return message; }

    public Set<String> getFiles() { return files; }
}
