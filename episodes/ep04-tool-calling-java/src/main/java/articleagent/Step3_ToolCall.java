package articleagent;

import articleagent.Ollama.Message;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 3 · Tool calling, one round trip.
 *
 * The four phases from the slides, printed one by one:
 *   1. Describe the tools   - send the same /api/chat request, plus a "tools" field
 *   2. The LLM asks         - the reply has tool_calls instead of an article
 *   3. Your code runs it    - a plain Java method
 *   4. Send the result back - a "tool" message, then the LLM asks for its next step
 *
 * The LLM never runs anything. It asks, and your code runs it.
 * (No loop yet: Step 4 adds it.)
 */
public class Step3_ToolCall {

    public static void main(String[] args) throws Exception {
        Tools.resetArticles();

        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(Step4_Agent.SYSTEM));      // the same rules as the agent
        messages.add(Message.user(Step1_LlmAlone.TASK));

        // ---- 1 · Describe the tools ----
        System.out.println("=== 1 · Describe the tools (sent in the \"tools\" field) ===");
        System.out.println(Ollama.pretty(Tools.DEFINITIONS.get(0)));
        System.out.println("... plus list_files and save_file, same shape\n");

        JsonNode reply = Ollama.chat(messages, Tools.DEFINITIONS);

        // ---- 2 · The LLM asks ----
        System.out.println("=== 2 · The LLM asks (look: no article, just tool_calls) ===");
        System.out.println(Ollama.pretty(reply.path("message")) + "\n");

        JsonNode calls = Ollama.toolCalls(reply);
        if (calls.isEmpty()) {
            System.out.println("The model answered without asking for a tool. Run it again, or check the model has the 'tools' badge.");
            return;
        }
        messages.add(Message.assistant(reply));   // keep its request in the history (stateless, Ep 3)

        // ---- 3 · Your code runs it ----
        System.out.println("=== 3 · Your Java code runs it ===");
        for (JsonNode call : calls) {
            String name = call.path("function").path("name").asText();
            JsonNode arguments = call.path("function").path("arguments");
            String result = Tools.run(name, arguments);
            System.out.println(name + "(" + arguments + ") returned:\n" + result.indent(2));
            messages.add(Message.tool(name, result));   // ---- 4 · the result goes back as role "tool"
        }

        // ---- 4 · Send the result back ----
        System.out.println("=== 4 · Send the result back: same history + a \"tool\" message ===");
        System.out.println(Ollama.pretty(messages.get(messages.size() - 1)) + "\n");

        JsonNode next = Ollama.chat(messages, Tools.DEFINITIONS);
        System.out.println("The LLM's next reply:");
        System.out.println(Ollama.pretty(next.path("message")));
        System.out.println("\nIt asks for the next step by itself. Put this in a loop, and you have an agent: Step 4.");
    }
}
