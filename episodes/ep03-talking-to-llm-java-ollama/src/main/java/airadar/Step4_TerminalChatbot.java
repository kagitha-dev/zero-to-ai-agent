package airadar;

import airadar.Ollama.Message;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * STEP 4 · The terminal chatbot (this episode's build).
 *
 * The "memory" is just this Java list. Every turn we resend ALL of it.
 * After each reply we print the token counts, so you can watch them grow.
 *
 * Commands:  /clear  forget the conversation   /exit  quit
 */
public class Step4_TerminalChatbot {

    public static void main(String[] args) throws Exception {
        List<Message> history = new ArrayList<>();
        history.add(new Message("system", Step3_SystemPrompt.AI_RADAR_SYSTEM));

        Scanner in = new Scanner(System.in);
        System.out.println("AI Radar chatbot · " + Ollama.MODEL + " · /clear to forget, /exit to quit");

        while (true) {
            System.out.print("\nyou > ");
            if (!in.hasNextLine()) break;
            String input = in.nextLine().trim();
            if (input.isEmpty()) continue;
            if (input.equals("/exit")) break;
            if (input.equals("/clear")) {
                history.subList(1, history.size()).clear();   // keep only the system prompt
                System.out.println("(conversation cleared: the model has forgotten everything)");
                continue;
            }

            history.add(new Message("user", input));

            JsonNode reply = Ollama.chat(history);            // the WHOLE history goes every time
            String answer = Ollama.text(reply);
            history.add(new Message("assistant", answer));

            System.out.println("ai  > " + answer);

            // Tokens: what we sent (prompt) and what it wrote (output)
            int in_tokens  = reply.path("prompt_eval_count").asInt();
            int out_tokens = reply.path("eval_count").asInt();
            System.out.printf("      [messages sent: %d · tokens in: %d · tokens out: %d]%n",
                    history.size() - 1, in_tokens, out_tokens);
        }
        System.out.println("bye");
    }
}
