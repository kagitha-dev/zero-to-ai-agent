# Zero to AI Agent — Build AI Agents in Java from Scratch

![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)
![Maven](https://img.shields.io/badge/build-Maven-blue)
![Ollama](https://img.shields.io/badge/LLM-Ollama%20(local%2C%20free)-black)
![License: MIT](https://img.shields.io/badge/license-MIT-green)

A free, hands-on series that takes you from **"what is AI, really?"** to a **working AI agent in Java**, one episode at a time.

Every episode has slides, and from Episode 3 on, every episode has a Java lab you can run yourself. You don't need any AI background, a GPU or a paid API key: the labs run on a free local model through [Ollama](https://ollama.com).

**You'll learn:** how LLMs work · calling an LLM from Java · system prompts and structured JSON output · tool calling · MCP (Model Context Protocol) with the Java MCP SDK · RAG with embeddings and a vector store · building a real agent with LangChain4j and Spring Boot.

---

## Episodes

| # | Episode | Type | Slides | Code / Lab |
|---|---|---|---|---|
| 1 | **The AI Map** — AI vs ML vs neural networks vs GenAI vs LLMs | Foundations | [Open slides](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep01-the-ai-map/slides/ep01-what-is-ai-slides.html) | [Teachable Machine lab](episodes/ep01-the-ai-map) |
| 2 | **How an LLM actually "thinks"** — tokens, weights, embeddings, next-token prediction | Foundations | [PDF](episodes/ep02-how-an-llm-thinks/slides/ep02-how-an-llm-thinks.pdf) · [PPTX](episodes/ep02-how-an-llm-thinks/slides/ep02-how-an-llm-thinks.pptx) | [Tiny LLM Lab](https://kagitha-dev.github.io/tiny-llm-lab/) (browser) |
| 3 | **Talking to an LLM with code** — your first API call in Java, with Ollama | Java lab | [Open slides](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep03-talking-to-llm-java-ollama/slides/ep03-ollama-java-slides.html) | [ep03 code](episodes/ep03-talking-to-llm-java-ollama) |
| 4 | **Giving the LLM hands** — tool calling | Java lab | coming soon | [ep04](episodes/ep04-tool-calling-java) |
| 5 | **MCP: plug-and-play tools** — the official Java MCP SDK | Java lab | coming soon | [ep05](episodes/ep05-mcp-java-sdk) |
| 6 | **RAG: giving the agent your knowledge** — embeddings and vector search | Java lab | coming soon | [ep06](episodes/ep06-rag-java) |
| 7 | **From demo to real agent** — LangChain4j + Spring Boot | Java lab | coming soon | [ep07](episodes/ep07-langchain4j-agent) |

Each episode folder has its own README with what it covers, what you need, and how to run it.

## How the series fits together

Episodes 1 and 2 explain what a model *is*: weights learned from examples, then frozen, predicting one token at a time.

Episode 3 shows that **calling an AI is just sending JSON to a URL**, and that the model is stateless: **the model doesn't remember, your app does.** Everything the model knows during a request has to be inside its context window.

From there, each episode puts something new into that window:

```
Context window · everything the model sees
├── System prompt ............ Ep 3  the standing rules
├── Live data from tools ..... Ep 4  get_order(4521) returns the real order
├── Tools from anywhere ...... Ep 5  one standard (MCP) instead of custom glue
├── Your knowledge ........... Ep 6  only the paragraph that matches (RAG)
└── All of it, production-ready  Ep 7  LangChain4j + a web chat UI
```

## Tools used

| Purpose | Tool |
|---|---|
| Language | Java 17 or newer (records, text blocks and the built-in `HttpClient`) |
| Build tool | Maven |
| IDE | IntelliJ IDEA Community (free) or VS Code with the Java extensions |
| LLM API | The official Java SDK of your chosen provider (Anthropic, OpenAI and Google all have one) |
| Free/local option | [Ollama](https://ollama.com), over plain HTTP or through Spring AI / LangChain4j |
| Tokenizer demo | An online tokenizer playground; [GPT-Tokenizer](https://gpt-tokenizer.dev/) for token counting in Java |
| MCP | Official [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk), Claude Desktop as the host, MCP Inspector for testing |
| RAG | An embeddings API, an in-memory store first, then pgvector or Chroma |
| Framework (Ep 7) | [LangChain4j](https://github.com/langchain4j/langchain4j) |
| UI (Ep 7) | A Spring Boot web endpoint with a simple HTML chat page |

## Quick start

```bash
git clone https://github.com/kagitha-dev/zero-to-ai-agent.git
cd zero-to-ai-agent

# 1. Install Ollama from https://ollama.com/download, then get the model used in the labs
ollama pull qwen3.5:4b

# 2. Build every Java lab
mvn compile

# 3. Run one (from inside its folder)
cd episodes/ep03-talking-to-llm-java-ollama
mvn -q exec:java -Dexec.mainClass=airadar.Step1_FirstCall
```

Or open the repo in IntelliJ IDEA (**File → Open → `pom.xml`**) and press **Run** next to any `main` method.

## Repository layout

```
zero-to-ai-agent/
├── pom.xml                                  Maven parent: one module per Java episode
├── episodes/
│   ├── ep01-the-ai-map/                     slides (HTML)
│   ├── ep02-how-an-llm-thinks/              slides (PPTX + PDF); lab lives in its own repo, tiny-llm-lab
│   ├── ep03-talking-to-llm-java-ollama/     slides (HTML) + Java lab
│   ├── ep04-tool-calling-java/
│   ├── ep05-mcp-java-sdk/
│   ├── ep06-rag-java/
│   └── ep07-langchain4j-agent/
└── LICENSE
```

## Follow along

- ⭐ **Star this repo** to get notified when the next episode's code lands.
- Questions or a bug in a lab? [Open an issue](https://github.com/kagitha-dev/zero-to-ai-agent/issues).
- More from the author: [kagitha.com](https://www.kagitha.com/)

## Author

**Prasad Kagitha** · [@kagitha-dev](https://github.com/kagitha-dev)

Licensed under the [MIT License](LICENSE). Slides and labs are free to use for learning and teaching.
