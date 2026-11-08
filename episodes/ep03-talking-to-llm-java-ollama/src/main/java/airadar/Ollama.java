package airadar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny helper so the next steps stay short.
 * It does exactly what Step 1 did: build JSON, POST it, read JSON back.
 */
public class Ollama {

    public static final String URL = "http://localhost:11434/api/chat";
    public static final String MODEL = "qwen3.5:4b";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    public static final ObjectMapper JSON = new ObjectMapper();

    /** One chat message: role is "system", "user" or "assistant". */
    public record Message(String role, String content) {}

    /** Send the full list of messages. Returns Ollama's whole JSON reply. */
    public static JsonNode chat(List<Message> messages) throws Exception {
        return chat(messages, null);
    }

    /** Same, but with an optional JSON schema that forces the reply's shape. */
    public static JsonNode chat(List<Message> messages, Object format) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("think", false);              // skip qwen3.5's thinking: faster
        body.put("stream", false);             // one JSON back, not a stream
        body.put("options", Map.of("temperature", 0, "seed", 42)); // same answer on every retake
        if (format != null) body.put("format", format);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned " + response.statusCode() + ": " + response.body());
        }
        return JSON.readTree(response.body());
    }

    /** Just the text of the reply: message.content */
    public static String text(JsonNode reply) {
        return reply.path("message").path("content").asText();
    }
}
