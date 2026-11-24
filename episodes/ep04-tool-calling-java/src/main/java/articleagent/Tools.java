package articleagent;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The agent's tools, in three parts:
 *
 *   1. Plain Java methods.             Nothing AI about them.
 *   2. Descriptions the LLM reads.     Name, what it does, what it needs (a JSON schema).
 *   3. run(name, args).                The LLM asks for a tool by name, we run the Java method.
 *
 * Every tool works inside the "workspace" folder only, and returns a String.
 * Errors are returned as text, not thrown, so the LLM can read them and decide what to do.
 */
public class Tools {

    public static final Path WORKSPACE = Path.of("workspace").toAbsolutePath().normalize();
    public static final Path ARTICLES  = WORKSPACE.resolve("articles");

    // =====================================================================
    // 1. Plain Java methods
    // =====================================================================

    /** Read a text file, e.g. "Trends.txt". */
    public static String readFile(String path) {
        try {
            Path file = inWorkspace(path);
            if (!Files.isRegularFile(file)) return "error: file not found: " + path;
            return Files.readString(file);
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /** List the files in a folder, e.g. "articles". One name per line. */
    public static String listFiles(String folder) {
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

    /** Save a NEW .md file in the articles folder. Never overwrites (rule 4: careful with write tools). */
    public static String saveFile(String name, String content) {
        try {
            String fileName = Path.of(name).getFileName().toString();   // "articles/rag.md" -> "rag.md"
            if (!fileName.endsWith(".md")) return "error: only .md files can be saved, got: " + name;
            Path file = ARTICLES.resolve(fileName);
            if (Files.exists(file)) return "error: articles/" + fileName + " already exists. Not overwritten.";
            Files.createDirectories(ARTICLES);
            Files.writeString(file, content);
            return "saved articles/" + fileName + " (" + content.split("\\s+").length + " words)";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // =====================================================================
    // 2. Descriptions the LLM reads (sent in the "tools" field of every request)
    //    The description is how the LLM chooses a tool, so write it like a prompt.
    // =====================================================================

    public static final List<Map<String, Object>> DEFINITIONS = List.of(
            tool("read_file",
                    "Read a text file from my computer and return its content. Example path: Trends.txt",
                    param("path", "The file path, for example Trends.txt")),
            tool("list_files",
                    "List the file names in a folder on my computer. In the articles folder, each file is an article that is already written: "
                            + "tokens.md means the Tokens article exists, so do not write or read it again.",
                    param("folder", "The folder name, for example articles")),
            tool("save_file",
                    "Save one article as a new markdown file in the articles folder. Fails if the file already exists.",
                    param("name", "The file name, for example rag.md"),
                    param("content", "The full article, in markdown"))
    );

    // =====================================================================
    // 3. Run a tool by name: the LLM asks, our Java code does it
    // =====================================================================

    public static String run(String name, JsonNode args) {
        return switch (name) {
            case "read_file"  -> readFile(args.path("path").asText());
            case "list_files" -> listFiles(args.path("folder").asText());
            case "save_file"  -> saveFile(args.path("name").asText(), args.path("content").asText());
            default           -> "error: unknown tool: " + name;   // rule 3: tell the LLM, don't crash
        };
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Builds one tool definition in the shape Ollama expects. */
    @SafeVarargs
    private static Map<String, Object> tool(String name, String description, Map.Entry<String, String>... params) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (var p : params) properties.put(p.getKey(), Map.of("type", "string", "description", p.getValue()));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", properties.keySet().stream().toList());

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    private static Map.Entry<String, String> param(String name, String description) {
        return Map.entry(name, description);
    }

    /** Resolve a path and refuse anything outside the workspace folder. */
    private static Path inWorkspace(String path) {
        Path p = WORKSPACE.resolve(path == null ? "" : path).normalize();
        if (!p.startsWith(WORKSPACE)) throw new IllegalArgumentException("outside the workspace: " + path);
        return p;
    }

    /** Put articles/ back to the start of the episode: only tokens.md. */
    public static void resetArticles() throws IOException {
        if (!Files.isRegularFile(WORKSPACE.resolve("Trends.txt"))) {
            throw new IllegalStateException("workspace/Trends.txt not found. Run from the episode folder: "
                    + "cd episodes/ep04-tool-calling-java");
        }
        Files.createDirectories(ARTICLES);
        try (Stream<Path> files = Files.list(ARTICLES)) {
            for (Path f : files.toList()) {
                if (!f.getFileName().toString().equals("tokens.md")) Files.delete(f);
            }
        }
    }
}
