package ai_simplest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class HistoryChatbot {

    public record Message(String role, String content) {}

    public static void main(String[] args) throws Exception {

        Scanner userInput = new Scanner(System.in);

        List<Message> history = new ArrayList<>();
        while (true) {
            history.add(new Message("user", userInput.nextLine()));
            String body = getBody(history);
            String reply = chat(body);
            System.out.println(reply);
            history.add(new Message("assistant", reply));
        }


    }

    public static String getBody(List<Message> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "qwen3.5:4b");
        body.put("messages", messages);
        body.put("think", false);
        body.put("stream", false);
        return new ObjectMapper().writeValueAsString(body);
    }

    public static String chat(String body) throws  Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Print everything Ollama sent back: raw JSON
        String reply = new ObjectMapper().readTree(response.body()).path("message").path("content")
                .asText();

        return reply;
    }

}
