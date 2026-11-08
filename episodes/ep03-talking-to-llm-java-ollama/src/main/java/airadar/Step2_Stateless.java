package airadar;

import airadar.Ollama.Message;

import java.util.List;

/**
 * STEP 2 · Read the reply, and prove the API is stateless.
 *
 * Same experiment as the two cmd windows:
 *   Call 1: "My name is Prasad."
 *   Call 2: "What is my name?"  -> it does not know.
 *   Call 3: send the earlier messages again -> now it knows.
 */
public class Step2_Stateless {

    public static void main(String[] args) throws Exception {

        // Call 1: tell it my name
        var reply1 = Ollama.chat(List.of(
                new Message("user", "My name is Prasad.")));
        System.out.println("CALL 1  you: My name is Prasad.");
        System.out.println("        ai : " + Ollama.text(reply1));
        System.out.println();

        // Call 2: a brand-new request. Nothing from call 1 is sent.
        var reply2 = Ollama.chat(List.of(
                new Message("user", "What is my name?")));
        System.out.println("CALL 2  you: What is my name?");
        System.out.println("        ai : " + Ollama.text(reply2));
        System.out.println();

        // Call 3: WE resend the whole conversation. The model reads it all again.
        var reply3 = Ollama.chat(List.of(
                new Message("user", "My name is Prasad."),
                new Message("assistant", Ollama.text(reply1)),
                new Message("user", "What is my name?")));
        System.out.println("CALL 3  (history resent) you: What is my name?");
        System.out.println("        ai : " + Ollama.text(reply3));
        System.out.println();

        System.out.println(">> The model never remembers. Whoever calls it resends the conversation.");
    }
}
