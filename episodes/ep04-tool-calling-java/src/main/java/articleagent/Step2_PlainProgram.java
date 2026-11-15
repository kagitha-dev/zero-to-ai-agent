package articleagent;

import articleagent.Ollama.Message;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * STEP 2 · Why not just write a program?
 *
 * Java reads the file and saves the files. The LLM only writes the text (like Ep 3).
 * Here, YOUR CODE decides the steps.
 *
 *   - "Skip topics I already wrote":  works, because the file name matches (tokens.md).
 *   - "Merge similar topics":         fails. No if-statement knows that
 *                                      "MCP" and "Model Context Protocol" are the same topic.
 *
 * Result: a duplicate article, model-context-protocol.md.
 */
public class Step2_PlainProgram {

    public static void main(String[] args) throws Exception {
        Tools.resetArticles();

        List<String> topics = Files.readAllLines(Tools.WORKSPACE.resolve("Trends.txt")).stream()
                .map(String::trim).filter(t -> !t.isEmpty()).toList();

        for (String topic : topics) {
            Path file = Tools.ARTICLES.resolve(slug(topic) + ".md");

            if (Files.exists(file)) {                                   // skip what exists: this works
                System.out.printf("%-24s -> %-28s skipped (file exists)%n", topic, file.getFileName());
                continue;
            }

            String article = Ollama.text(Ollama.chat(List.of(Message.user(   // the LLM, from Ep 3
                    "Write a short article (about 150 words, in markdown) on: " + topic))));
            Files.writeString(file, article);
            System.out.printf("%-24s -> %-28s written%n", topic, file.getFileName());
        }

        System.out.println("\nworkspace/articles now has:");
        System.out.println(Tools.listFiles("articles").indent(2));
        System.out.println("mcp.md and model-context-protocol.md are the same topic: a duplicate.");
        System.out.println("if (sameTopic(\"MCP\", \"Model Context Protocol\")) ... no if-statement understands meaning.");
    }

    /** "Model Context Protocol" -> "model-context-protocol" */
    static String slug(String topic) {
        return topic.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
