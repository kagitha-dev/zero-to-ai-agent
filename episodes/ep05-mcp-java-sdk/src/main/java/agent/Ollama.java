package agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * The same Ollama helper as Ep 4, unchanged (only the package name differs).
 *
 * Ep 4 recap: the request has a "tools" field.
 *
 * Messages now have two more roles to handle:
 *   - an "assistant" message can carry tool_calls instead of text (the LLM asking for a tool)
 *   - a "tool" message carries the result of a tool your code ran
 */
public class Ollama {

    public static final String URL = "http://localhost:11434/api/chat";
    public static final String MODEL = "qwen3.5:4b";   // has the "tools" badge on ollama.com

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    public static final ObjectMapper JSON = new ObjectMapper();

    /**
     * One chat message. Empty fields are left out of the JSON.
     *   role:       "system", "user", "assistant" or "tool"
     *   content:    the text
     *   tool_calls: only on assistant messages that ask for tools
     *   tool_name:  only on tool messages: which tool this result is for
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Message(String role,
                          String content,
                          @JsonProperty("tool_calls") JsonNode toolCalls,
                          @JsonProperty("tool_name") String toolName) {

        public static Message system(String text) { return new Message("system", text, null, null); }
        public static Message user(String text)   { return new Message("user", text, null, null); }

        /** Put the LLM's reply back into the history exactly as it came, tool_calls included. */
        public static Message assistant(JsonNode reply) {
            JsonNode msg = reply.path("message");
            JsonNode calls = msg.has("tool_calls") ? msg.get("tool_calls") : null;
            return new Message("assistant", msg.path("content").asText(""), calls, null);
        }

        /** The result of a tool your code ran, sent back to the LLM. */
        public static Message tool(String toolName, String result) {
            return new Message("tool", result, null, toolName);
        }
    }

    /** Chat without tools, exactly like Ep 3. */
    public static JsonNode chat(List<Message> messages) throws Exception {
        return chat(messages, null);
    }

    /** Chat with tools: the only new thing is the "tools" field in the body. */
    public static JsonNode chat(List<Message> messages, Object tools) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", messages);
        if (tools != null) body.put("tools", tools);   // NEW in Ep 4
        body.put("think", false);                      // skip qwen3.5's thinking: faster
        body.put("stream", false);                     // one JSON back, not a stream
        body.put("options", Map.of(
                "temperature", 0, "seed", 42,          // same answer on every retake
                "num_ctx", 8192));                     // room for 3 articles in the history

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

    /** The tool requests in the reply: message.tool_calls (an empty array if there are none). */
    public static JsonNode toolCalls(JsonNode reply) {
        JsonNode calls = reply.path("message").path("tool_calls");
        return calls.isArray() ? calls : JSON.createArrayNode();
    }

    /** Pretty JSON, for printing on screen. */
    public static String pretty(Object o) {
        try { return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}
