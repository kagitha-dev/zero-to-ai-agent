package airadar;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * STEP 1 · Your first API call.
 *
 * Calling an AI is just sending JSON to a URL.
 * No library, no SDK: only Java's built-in HttpClient.
 * This is the same request as the curl command, written in Java.
 */
public class Step1_FirstCall {

    public static void main(String[] args) throws Exception {

        // The exact JSON the curl command sent
        String body = """
            {
              "model": "qwen3.5:4b",
              "messages": [
                { "role": "user", "content": "Explain RAG in two sentences." }
              ],
              "think": false,
              "stream": false
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Print everything Ollama sent back: raw JSON
        System.out.println(response.body());
    }
}
