package org.example.host;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * The LLM side of the host: one POST to Ollama's /api/chat.
 *
 * Same request as Ep 3 / Ep 4: model + messages + tools.
 * No extra JSON library: we reuse the JSON mapper that ships with the MCP SDK.
 */
public class Ollama {

    static final String URL = "http://localhost:11434/api/chat";
    static final String MODEL = "qwen3.5:4b";   // any Ollama model with the "tools" badge works

    static final McpJsonMapper JSON = McpJsonDefaults.getMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** Send the whole chat history + the tool list. Returns the reply's "message" object. */
    static Map<String, Object> chat(List<Map<String, Object>> messages,
                                    List<Map<String, Object>> tools) throws Exception {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", messages,
                "tools", tools,
                "think", false,                                   // qwen3.5: skip thinking, faster
                "stream", false,                                  // one JSON back, not a stream
                "options", Map.of("temperature", 0, "seed", 42)); // same answer every take

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> reply = JSON.readValue(response.body(), new TypeRef<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) reply.get("message");
        return message;
    }

    /** The tool requests inside a reply message (empty list = the LLM is done). */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> toolCalls(Map<String, Object> message) {
        Object calls = message.get("tool_calls");
        return calls instanceof List<?> ? (List<Map<String, Object>>) calls : List.of();
    }
}
