package filesserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The three tools from Ep 4, copied over unchanged: plain Java methods, nothing AI about them.
 *
 * In Ep 4 they lived inside the agent. In Ep 5 they live inside the MCP server (FilesServer),
 * so any MCP host can use them.
 *
 * Every tool works inside the "workspace" folder only, and returns a String.
 * Errors are returned as text, not thrown, so the LLM can read them and decide what to do.
 */
public class FileTools {

    private final Path workspace;
    private final Path articles;

    public FileTools(Path workspace) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.articles = this.workspace.resolve("articles");
    }

    public Path workspace() { return workspace; }

    /** Read a text file, e.g. "Trends.txt". */
    public String readFile(String path) {
        try {
            Path file = inWorkspace(path);
            if (!Files.isRegularFile(file)) return "error: file not found: " + path;
            return Files.readString(file);
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /** List the files in a folder, e.g. "articles". One name per line. */
    public String listFiles(String folder) {
        try {
            Path dir = inWorkspace(folder);
            if (!Files.isDirectory(dir)) return "error: folder not found: " + folder;
            try (Stream<Path> files = Files.list(dir)) {
                String names = files.map(p -> p.getFileName().toString()).sorted()
                        .collect(Collectors.joining("\n"));
                return names.isEmpty() ? "(empty folder)" : names;
            }
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /** Save a NEW .md file in the articles folder. Never overwrites (Ep 4 rule 4: careful with write tools). */
    public String saveFile(String name, String content) {
        try {
            String fileName = Path.of(name).getFileName().toString();   // "articles/rag.md" -> "rag.md"
            if (!fileName.endsWith(".md")) return "error: only .md files can be saved, got: " + name;
            Path file = articles.resolve(fileName);
            if (Files.exists(file)) return "error: articles/" + fileName + " already exists. Not overwritten.";
            Files.createDirectories(articles);
            Files.writeString(file, content);
            return "saved articles/" + fileName + " (" + content.split("\\s+").length + " words)";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /** Resolve a path and refuse anything outside the workspace folder. */
    private Path inWorkspace(String path) {
        Path p = workspace.resolve(path == null ? "" : path).normalize();
        if (!p.startsWith(workspace)) throw new IllegalArgumentException("outside the workspace: " + path);
        return p;
    }

    /**
     * Find this lab's "workspace" folder (the one with Trends.txt), whatever folder the program runs from:
     * the episode folder, the repo root (IntelliJ's default), or anywhere below them.
     */
    public static Path findWorkspace() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            for (Path candidate : List.of(dir.resolve("workspace"),
                                          dir.resolve("episodes/ep05-mcp-java-sdk/workspace"))) {
                if (Files.isRegularFile(candidate.resolve("Trends.txt"))) return candidate.toAbsolutePath().normalize();
            }
        }
        return Path.of("workspace").toAbsolutePath().normalize();
    }
}
