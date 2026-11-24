package ai_simplest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The whole agent in one file: LLM + tools + loop.
 * Run it from the episode folder, so "workspace/" is found.
 */
public class SimpleAgent {

    static final ObjectMapper JSON = new ObjectMapper();
    static final Path WORKSPACE = Path.of("workspace");

    public static void main(String[] args) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are an article-writing agent. You cannot see any file unless you use a tool. "
                        + "A topic is already written if the articles folder has a file named after it: skip it, never read it. "
                        + "Topics that mean the same thing get one article. When every topic is handled, reply with a summary and no tool call."));
        messages.add(Map.of("role", "user", "content",
                "Read Trends.txt. For each topic, write a short article (about 150 words) and save it as its own "
                        + ".md file in the articles folder. Skip topics that already have a file, and merge topics that mean the same thing."));

        for (int step = 1; step <= 10; step++) {                       // step limit
            JsonNode message = chat(messages).path("message");
            JsonNode calls = message.path("tool_calls");

            if (!calls.isArray() || calls.isEmpty()) {                  // no tool call = done
                System.out.println("Done: " + message.path("content").asText());
                return;
            }

            messages.add(JSON.convertValue(message, new TypeReference<Map<String, Object>>() {}));      // keep the LLM's request in the history

            for (JsonNode call : calls) {
                String name = call.path("function").path("name").asText();
                JsonNode a = call.path("function").path("arguments");
                String result = run(name, a);                          // the LLM asks, our Java code runs it
                System.out.println(name + " -> " + result.replace("\n", ", "));
                messages.add(Map.of("role", "tool", "tool_name", name, "content", result));
            }
        }
        System.out.println("Stopped after 10 steps.");
    }

    // ---- The three tools: plain Java ----
    static String run(String name, JsonNode a) {
        try {
            switch (name) {
                case "read_file": {
                    Path file = WORKSPACE.resolve(a.path("path").asText());
                    return Files.exists(file) ? Files.readString(file) : "error: file not found";
                }
                case "list_files": {
                    try (var files = Files.list(WORKSPACE.resolve(a.path("folder").asText()))) {
                        return String.join("\n", files.map(p -> p.getFileName().toString()).sorted().toList());
                    }
                }
                case "save_file": {
                    Path file = WORKSPACE.resolve("articles").resolve(Path.of(a.path("name").asText()).getFileName());
                    if (Files.exists(file)) return "error: already exists, not overwritten";
                    Files.writeString(file, a.path("content").asText());
                    return "saved " + file.getFileName();
                }
                default:
                    return "error: unknown tool " + name;
            }
        } catch (Exception e) {
            return "error: " + e.getMessage();                         // errors go back to the LLM
        }
    }

    // ---- How the LLM sees the tools ----
    static List<Map<String, Object>> tools() {
        return List.of(
                tool("read_file", "Read a text file from my computer. Example: Trends.txt", "path"),
                tool("list_files", "List the files in a folder on my computer. Example: articles", "folder"),
                tool("save_file", "Save an article as a new .md file in the articles folder", "name", "content"));
    }

    static Map<String, Object> tool(String name, String description, String... params) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (String p : params) props.put(p, Map.of("type", "string"));
        return Map.of("type", "function", "function", Map.of(
                "name", name,
                "description", description,
                "parameters", Map.of("type", "object", "properties", props, "required", List.of(params))));
    }

    // ---- Same /api/chat call as Ep 3, plus "tools" ----
    static JsonNode chat(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "qwen3.5:4b");
        body.put("messages", messages);
        body.put("tools", tools());
        body.put("think", false);
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();

        String response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        return JSON.readTree(response);
    }
}
