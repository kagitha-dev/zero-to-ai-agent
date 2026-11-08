# Ep 3 · Talking to an LLM with code — your first API call in Java (with Ollama)

**Zero to AI Agent · Java lab**

Last episode the model ended as a file of weights, and a model file on its own does nothing. This episode runs one on your own computer with **Ollama**, then calls it from **Java**, using no AI library and only the JDK's built-in `HttpClient`.

📺 **Video:** _link coming soon_ · 🖥️ **Slides:** [open in your browser](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep03-talking-to-llm-java-ollama/slides/ep03-ollama-java-slides.html) · [source](slides/ep03-ollama-java-slides.html)

## What you'll learn

- **What Ollama is:** a free app that downloads, organises and runs models locally. The model is the movie; Ollama is the video player. It's private, works offline and costs nothing per question.
- **How a request flows:** your code → `localhost:11434` (Ollama's HTTP API) → the model in memory. Your code never opens the model file.
- **Calling an AI is just sending JSON to a URL.**
- **LLMs are stateless.** The model doesn't remember; your app does, by resending the history every time.
- **System prompts:** the standing rules that shape every answer.
- **Structured output:** a JSON schema in Ollama's `format` field makes the reply fit a Java `record`.
- **Why context matters:** everything the model sees has to fit in the context window, which sets up tools (Ep 4), MCP (Ep 5) and RAG (Ep 6).

## Before you run

1. Install **Java 17+** and **Maven**.
2. Install Ollama from [ollama.com/download](https://ollama.com/download) and check it with `ollama --version`.
3. Pull the model (about 3.4 GB, downloaded once):

   ```bash
   ollama pull qwen3.5:4b
   ```

4. Check Ollama is running (it runs in the background after install):

   ```bash
   curl http://localhost:11434
   ```

### Ollama commands used in the episode

| Command | What it does |
|---|---|
| `ollama --version` | Check the install |
| `ollama run qwen3.5:4b` | Download if needed, load into memory and open a chat |
| `ollama list` | Models on disk |
| `ollama ps` | Models loaded in memory |
| `ollama stop qwen3.5:4b` | Unload from memory (the file stays on disk) |
| `ollama rm qwen3.5:4b` | Delete from disk |

## Run the lab

Run these **from this folder**, because Step 5 reads `feed.json` from here:

```bash
cd episodes/ep03-talking-to-llm-java-ollama
mvn -q compile
mvn -q exec:java -Dexec.mainClass=airadar.Step1_FirstCall
mvn -q exec:java -Dexec.mainClass=airadar.Step2_Stateless
mvn -q exec:java -Dexec.mainClass=airadar.Step3_SystemPrompt
mvn -q exec:java -Dexec.mainClass=airadar.Step4_TerminalChatbot
mvn -q exec:java -Dexec.mainClass=airadar.Step5_StructuredOutput
```

Or open the repo in IntelliJ and press **Run** on any `StepN` class.

### The AI Radar steps (`airadar` package)

| Step | Class | What it shows |
|---|---|---|
| 1 | `Step1_FirstCall` | Calling an AI is just sending JSON to a URL: it prints the raw JSON reply |
| 2 | `Step2_Stateless` | The API forgets between calls, and resending the history fixes it |
| 3 | `Step3_SystemPrompt` | The same question with and without a system prompt |
| 4 | `Step4_TerminalChatbot` | A chatbot whose "memory" is a Java `List`, with token counts |
| 5 | `Step5_StructuredOutput` | AI Radar classifies `feed.json` and writes flashcards as JSON, read into Java records |

`Ollama.java` is a small helper used by steps 2–5. It does exactly what Step 1 does. Every call sends `"think": false`, `temperature 0` and a fixed `seed`, so re-runs give the same output.

### The simplest versions (`ai_simplest` package)

| Class | What it shows |
|---|---|
| `SimpleChatbot` | One call to `/api/chat`, written out in full in a single file |
| `HistoryChatbot` | A terminal chat loop that keeps a `List<Message>` history and resends it each turn |

## The API in one request

```json
POST http://localhost:11434/api/chat
{
  "model": "qwen3.5:4b",
  "messages": [ { "role": "user", "content": "Explain RAG in two sentences." } ],
  "think": false,
  "stream": false
}
```

`qwen3.5` thinks before answering by default; `"think": false` skips that, so replies are faster.

## Next

[← Ep 2](../ep02-how-an-llm-thinks) · [Ep 4 · Giving the LLM hands (tool calling) →](../ep04-tool-calling-java)
