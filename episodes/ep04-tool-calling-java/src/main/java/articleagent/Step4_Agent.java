package articleagent;

import articleagent.Ollama.Message;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 4 · Our first AI agent:  LLM + tools + loop.
 *
 *   while (true) {
 *     reply = ollama.chat(messages, tools);
 *     if (no tool calls) break;            // rule 1: stop condition
 *     for (call : tool calls) {
 *       result = run(call);                // your Java method
 *       messages.add(toolMessage(result)); // the result goes back to the LLM
 *     }
 *   }
 *
 * The code never says "read, then list, then save". The LLM chooses every step,
 * including the two decisions: skip Tokens (tokens.md exists) and merge the two MCP lines.
 */
public class Step4_Agent {

    static final int MAX_STEPS = 10;   // rule 2: a confused model can't loop forever

    static final String SYSTEM = """
            You are an article-writing agent working on my computer. You cannot see any file unless you use a tool. Work one step at a time.
            Rules:
            - A topic is already written if the articles folder has a file named after it (tokens.md means Tokens is done). Skip it. Never read existing articles.
            - Topics that mean the same thing get one article.
            - Save each new article with save_file.
            - When every topic is handled, reply with a one-line summary and no tool call.""";

    public static void main(String[] args) throws Exception {
        Tools.resetArticles();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(SYSTEM));
        messages.add(Message.user(Step1_LlmAlone.TASK));
        System.out.println("you > " + Step1_LlmAlone.TASK + "\n");

        for (int step = 1; step <= MAX_STEPS; step++) {
            JsonNode reply = Ollama.chat(messages, Tools.DEFINITIONS);
            JsonNode calls = Ollama.toolCalls(reply);
            int tokensIn = reply.path("prompt_eval_count").asInt();

            // Rule 1 · stop condition: no tool call means the LLM says it's done
            if (calls.isEmpty()) {
                System.out.printf("REQ %d  no tool call: %s%n", step, Ollama.text(reply).trim());
                System.out.printf("        [tokens in: %d]%n", tokensIn);
                System.out.println("\nDone in " + step + " requests. workspace/articles now has:");
                System.out.println(Tools.listFiles("articles").indent(2));
                return;
            }

            messages.add(Message.assistant(reply));   // its request goes into the history

            for (JsonNode call : calls) {
                String name = call.path("function").path("name").asText();
                JsonNode arguments = call.path("function").path("arguments");

                String result = Tools.run(name, arguments);   // rule 3: errors come back as text
                messages.add(Message.tool(name, result));

                System.out.printf("REQ %d  %s(%s)%n", step, name, shortArgs(arguments));
                System.out.printf("        -> %s%n", oneLine(result));
            }
            System.out.printf("        [tokens in: %d, it grows every request (Ep 3)]%n", tokensIn);
        }

        System.out.println("\nStopped: hit MAX_STEPS = " + MAX_STEPS + ". The job may not be finished.");
    }

    /** Show arguments on one line, with long article text cut short. */
    static String shortArgs(JsonNode args) {
        List<String> parts = new ArrayList<>();
        args.fields().forEachRemaining(f -> {
            String v = f.getValue().asText();
            parts.add(f.getKey() + "=\"" + (v.length() > 30 ? v.substring(0, 30).replace("\n", " ") + "…" : v) + "\"");
        });
        return String.join(", ", parts);
    }

    static String oneLine(String s) {
        String flat = s.trim().replace("\n", " · ");
        return flat.length() > 90 ? flat.substring(0, 90) + "…" : flat;
    }
}
