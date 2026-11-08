package airadar;

import airadar.Ollama.Message;

import java.util.List;

/**
 * STEP 3 · The system prompt.
 *
 * Same user question, asked twice:
 *   A) no system prompt
 *   B) with AI Radar's system prompt (the standing rules)
 */
public class Step3_SystemPrompt {

    static final String AI_RADAR_SYSTEM = """
        You are AI Radar, a writer who explains AI to students.
        Write in simple English, under 150 words.
        Always use three short sections with these headings: What it is, Why it matters, How it works.
        """;

    public static void main(String[] args) throws Exception {
        String question = "Write a short teaching article on RAG.";

        System.out.println("=== A) WITHOUT a system prompt ===");
        var plain = Ollama.chat(List.of(
                new Message("user", question)));
        System.out.println(Ollama.text(plain));

        System.out.println();
        System.out.println("=== B) WITH the AI Radar system prompt ===");
        var radar = Ollama.chat(List.of(
                new Message("system", AI_RADAR_SYSTEM),   // the job description
                new Message("user", question)));          // today's task
        System.out.println(Ollama.text(radar));
    }
}
