package articleagent;

import articleagent.Ollama.Message;

import java.util.List;

/**
 * STEP 1 · The LLM alone.
 *
 * We give the LLM today's job, exactly like we chatted in Ep 3. No tools.
 * It can think and write, but it can't read Trends.txt, look at the folder, or save a file.
 * All an LLM outputs is text (Ep 2).
 */
public class Step1_LlmAlone {

    /** Today's job. The same sentence is used in every step. */
    public static final String TASK = """
            Read Trends.txt. For each topic, write a short article (about 150 words, in markdown) \
            and save it as its own .md file in the articles folder. \
            Skip topics that already have a file in the articles folder, \
            and merge topics that mean the same thing into one article.""";

    public static void main(String[] args) throws Exception {
        Tools.resetArticles();

        System.out.println("you > " + TASK + "\n");
        String answer = Ollama.text(Ollama.chat(List.of(Message.user(TASK))));
        System.out.println("ai  > " + answer);

        // Did anything actually happen on disk?
        System.out.println("\nWhat's really in workspace/articles:");
        System.out.println(Tools.listFiles("articles").indent(2));
        System.out.println("Nothing new. The LLM can only write text: it can't touch your computer.");
    }
}
