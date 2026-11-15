# Ep 4 · From LLM to AI agent — tool calling in Java

**Zero to AI Agent · Java lab**

Episodes 1 to 3 were about the LLM: what it is, how it thinks, and how to call it from Java. This episode builds our **first AI agent**, from scratch, with no framework:

> **Agent = LLM + tools + loop**

📺 **Video:** _link coming soon_ · 🖥️ **Slides:** [open in your browser](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep04-tool-calling-java/slides/ep04-tool-calling-agent-slides.html) · [source](slides/ep04-tool-calling-agent-slides.html)

## Today's job

`workspace/Trends.txt` has the topics I want to write about:

```
RAG
MCP
Model Context Protocol
Tokens
```

Write one article per topic and save each as its own `.md` file in `workspace/articles/`, with two conditions:

- **Skip topics I already wrote.** `tokens.md` is already in the folder.
- **Merge similar topics.** MCP and Model Context Protocol are the same thing.

The right result is **two new files**: `rag.md` and `mcp.md`. Those two conditions are *decisions*, and we want the LLM to make them.

## What you'll learn

- **The LLM alone can only think and write.** It can't read a file, look at a folder or save anything.
- **Why not just a program?** Java can read and save, and even skip a file that exists. But no `if` statement knows that "MCP" and "Model Context Protocol" are the same topic. The decisions need the LLM.
- **Tool calling:** describe a Java method to the LLM (name, description, JSON schema). The LLM replies with `tool_calls` instead of text. **The LLM never runs anything. It asks, and your code runs it.** The result goes back as a `"tool"` message.
- **The agent loop:** ask, run, send back, until a reply has no tool call. The code never lists the steps; the LLM chooses every one.
- **Program vs agent:** if your code decides the steps, it's a program that uses an LLM. If the LLM decides the steps, it's an agent.
- **Four rules for the loop:** stop when there's no tool call · cap the steps (`MAX_STEPS`) · send errors back as text · be careful with write tools (`save_file` never overwrites).

## Before you run

Same setup as [Ep 3](../ep03-talking-to-llm-java-ollama): **Java 17+**, **Maven**, and Ollama with the model pulled.

```bash
ollama pull qwen3.5:4b
ollama show qwen3.5:4b     # "tools" must be listed under Capabilities
```

## Run the lab

Run these **from this folder**, because the tools work inside `workspace/`:

```bash
cd episodes/ep04-tool-calling-java
mvn -q compile
mvn -q exec:java -Dexec.mainClass=articleagent.Step1_LlmAlone
mvn -q exec:java -Dexec.mainClass=articleagent.Step2_PlainProgram
mvn -q exec:java -Dexec.mainClass=articleagent.Step3_ToolCall
mvn -q exec:java -Dexec.mainClass=articleagent.Step4_Agent
```

Or open the repo in IntelliJ and press **Run** on any `StepN` class (set the working directory to this folder).

Every step first resets `workspace/articles/` back to just `tokens.md`, so you can run them in any order, as often as you like.

### The article agent steps (`articleagent` package)

| Step | Class | What it shows |
|---|---|---|
| 1 | `Step1_LlmAlone` | The job sent to the LLM with no tools: it answers in text and nothing changes on disk |
| 2 | `Step2_PlainProgram` | A plain Java loop: skipping works, merging fails, and you get a duplicate `model-context-protocol.md` |
| 3 | `Step3_ToolCall` | One tool call, printed in four phases: describe the tools → the LLM asks → your code runs it → send the result back |
| 4 | `Step4_Agent` | **The agent:** the full loop with `MAX_STEPS`, logging every request and the growing token count |

| Helper | What it does |
|---|---|
| `Ollama.java` | The Ep 3 helper plus one new field, `tools`, and the `tool_calls` / `tool_name` message fields |
| `Tools.java` | The three tools: plain Java methods, the descriptions the LLM reads, and `run(name, args)` |

### The simplest version (`ai_simplest` package)

| Class | What it shows |
|---|---|
| `SimpleAgent` | The whole agent (LLM + 3 tools + loop) in one file |

## The three tools

| Java method | Tool name | Arguments |
|---|---|---|
| `Tools.readFile` | `read_file` | `path` |
| `Tools.listFiles` | `list_files` | `folder` |
| `Tools.saveFile` | `save_file` | `name`, `content` (new files only, never overwrites) |

## Try it without Java

- [`curls/ep04-postman-curls.txt`](curls/ep04-postman-curls.txt): the agent replayed one request at a time. Paste any of them into Postman (**Import → Raw text**) or a terminal.
- [`tools-syntax/`](tools-syntax): the JSON shapes, side by side
  - `01-tool-definition-anatomy.txt`: what each field of a tool definition means
  - `02-article-agent-tools.txt`: the three tools, plus the full `tools` array ready to paste
  - `03-tool-call-round-trip.txt`: what the LLM sends back, what you send back, and the whole history

## The API in one round trip

```json
// 1 · you send: messages + tools
{ "model": "qwen3.5:4b", "messages": [ ... ], "tools": [ ... ], "think": false, "stream": false }

// 2 · the LLM asks
{ "message": { "role": "assistant", "content": "",
               "tool_calls": [ { "function": { "name": "read_file", "arguments": { "path": "Trends.txt" } } } ] } }

// 3 · your code runs Tools.readFile("Trends.txt"), then you send back the same history plus:
{ "role": "tool", "tool_name": "read_file", "content": "RAG\nMCP\nModel Context Protocol\nTokens" }
```

## Notes

- A small local model is not perfect. Every call uses `temperature 0` and a fixed `seed`, so the same run repeats. If it skips a step or misses the duplicate, try sharper tool descriptions, or a bigger model such as `qwen3.5:9b`.
- Every tool result stays in the history, so the window grows with each article (Ep 3). `num_ctx` is set to 8192 to leave room.

## Next

[← Ep 3](../ep03-talking-to-llm-java-ollama) · [Ep 5 · MCP: plug-and-play tools →](../ep05-mcp-java-sdk)

Our three tools only live inside this Java app. In Ep 5, MCP lets any AI app use them.
