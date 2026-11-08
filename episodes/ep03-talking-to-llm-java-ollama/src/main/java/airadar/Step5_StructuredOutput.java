package airadar;

import airadar.Ollama.Message;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * STEP 5 · Structured JSON output: AI Radar's first real job.
 *
 * For each news item in feed.json:
 *   1) Is it a major model launch? -> JSON forced by a schema, read into a Java record
 *   2) If yes, write a news flashcard -> JSON again
 */
public class Step5_StructuredOutput {

    /** The shape we want back for every news item. */
    record Verdict(boolean isMajorLaunch, String modelName, String provider, String reason) {}

    /** The shape of a flashcard. */
    record Flashcard(String title, String summary, String whyItMatters) {}

    static final String CLASSIFY_SYSTEM = """
        You classify AI company news for a news site.
        isMajorLaunch is true only when a NEW AI model or a major model update is announced.
        Customer stories, partnerships, policy posts and small app features are false.
        modelName and provider: fill them for launches, otherwise use an empty string.
        reason: one short sentence.
        """;

    static final String FLASHCARD_SYSTEM = """
        You write news flashcards for students.
        title: under 8 words. summary: one sentence. whyItMatters: one sentence.
        Use only facts from the news item. Do not invent numbers.
        """;

    // JSON schemas: Ollama's "format" field makes the model follow these exactly
    static final Map<String, Object> VERDICT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "isMajorLaunch", Map.of("type", "boolean"),
                    "modelName", Map.of("type", "string"),
                    "provider", Map.of("type", "string"),
                    "reason", Map.of("type", "string")),
            "required", List.of("isMajorLaunch", "modelName", "provider", "reason"));

    static final Map<String, Object> FLASHCARD_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "title", Map.of("type", "string"),
                    "summary", Map.of("type", "string"),
                    "whyItMatters", Map.of("type", "string")),
            "required", List.of("title", "summary", "whyItMatters"));

    public static void main(String[] args) throws Exception {
        JsonNode feed = Ollama.JSON.readTree(Files.readString(Path.of("feed.json")));

        for (JsonNode item : feed) {
            String news = item.path("source").asText() + ": " + item.path("title").asText()
                    + "\n" + item.path("description").asText();

            // 1) Classify -> JSON -> Java record
            JsonNode reply = Ollama.chat(List.of(
                    new Message("system", CLASSIFY_SYSTEM),
                    new Message("user", news)), VERDICT_SCHEMA);
            Verdict v = Ollama.JSON.readValue(Ollama.text(reply), Verdict.class);

            System.out.println((v.isMajorLaunch() ? "[LAUNCH] " : "[skip]   ")
                    + item.path("title").asText() + "\n          " + v.reason());

            if (!v.isMajorLaunch()) continue;

            // 2) Launch -> flashcard JSON -> Java record
            JsonNode cardReply = Ollama.chat(List.of(
                    new Message("system", FLASHCARD_SYSTEM),
                    new Message("user", news)), FLASHCARD_SCHEMA);
            Flashcard card = Ollama.JSON.readValue(Ollama.text(cardReply), Flashcard.class);
            printCard(v, card);
        }
    }

    static void printCard(Verdict v, Flashcard c) {
        String line = "-".repeat(64);
        System.out.println("          +" + line);
        System.out.println("          | " + v.provider() + " · " + v.modelName());
        System.out.println("          | " + c.title().toUpperCase());
        System.out.println("          | " + c.summary());
        System.out.println("          | Why it matters: " + c.whyItMatters());
        System.out.println("          +" + line);
    }
}
