# Ep 5 · MCP: plug-and-play tools with the Java MCP SDK

**Zero to AI Agent · Java lab (+ Python version)**

In Ep 4 our three tools lived **inside** our agent. Any other AI app that wanted them would need its own copy.
**MCP (Model Context Protocol)** is one standard way to offer tools: build them **once** as an **MCP server**, and any MCP host can plug in.

> **Same Ep 4 loop. The tools now live in a server.**

📺 **Video:** _link coming soon_ · 🖥️ **Slides:** [open in your browser](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep05-mcp-java-sdk/slides/ep05-mcp-slides.html) · [source](slides/ep05-mcp-slides.html)

## Today's job

Exactly the Ep 4 job: read `workspace/Trends.txt`, write one article per topic, skip `tokens.md` (already written), and merge MCP + Model Context Protocol.
The right result is still **`rag.md` and `mcp.md`**.

What changes is **where the tools live**:

```
Ep 4:  agent ── read_file / list_files / save_file (Java methods inside the agent)

Ep 5:  agent (MCP host) ── MCP client ══ stdio ══ files-server (MCP server)
                                                   └─ read_file · list_files · save_file
```

## What you'll learn

- **RPC:** calling a function that runs somewhere else, as if it were local.
- **JSON-RPC 2.0:** a request has four fields (`jsonrpc`, `method`, `params`, `id`); the answer comes back with the **same id**; no id means a **notification** (no reply).
- **An MCP message is just a JSON-RPC message.** MCP only agrees on the method names: `tools/list` ("what tools do you have?") and `tools/call` ("run this tool").
- **Host, client, server:** the host is the AI app, it has one MCP client per server, and each server wraps a real system and offers its tools.
- **Two ways to connect:** **stdio** (same computer, the host starts the server) and **Streamable HTTP** (a web service). This lab uses stdio.
- **The LLM never talks to an MCP server.** The host does. The Ep 4 loop doesn't change, only where the tool runs.

## Before you run

Same setup as [Ep 4](../ep04-tool-calling-java): **Java 17+**, **Maven**, and Ollama with the model pulled.

```bash
ollama pull qwen3.5:4b
```

Optional, for the demo: **Node.js** (for MCP Inspector) and **Claude Desktop** or **VS Code**.

## Run the lab

```bash
cd episodes/ep05-mcp-java-sdk
mvn -q package                                                    # builds target/files-server.jar (the MCP server)
mvn -q exec:java -Dexec.mainClass=agent.Step1_ListAndCall         # an MCP client, no LLM
mvn -q exec:java -Dexec.mainClass=agent.Step2_McpAgent            # the Ep 4 agent, now an MCP host
```

Run `mvn package` first: the client starts the server with `java -jar target/files-server.jar`, so the jar must exist.
`Step2_McpAgent` resets `workspace/articles/` back to just `tokens.md` on every run.

## The code

### The MCP server (`filesserver` package)

| Class | What it is |
|---|---|
| `FileTools` | The three Ep 4 tools, **copied unchanged**: plain Java methods, nothing AI |
| `FilesServer` | **The MCP server.** Each tool = name + description + input schema + the Java method. Served over **stdio** |

### The host side (`agent` package)

| Class | What it shows |
|---|---|
| `FilesServerClient` | **The MCP client**: starts `files-server.jar` and connects over stdio (`StdioClientTransport`) |
| `Step1_ListAndCall` | `initialize` → `tools/list` → `tools/call read_file`. No LLM at all |
| `Step2_McpAgent` | **The agent.** The Ep 4 loop with only two changes, marked `CHANGE 1` and `CHANGE 2` in the code |
| `Ollama` | The Ep 4 helper, unchanged |

The two changes from Ep 4:

| | Ep 4 | Ep 5 |
|---|---|---|
| **CHANGE 1** · where the tools come from | `Tools.DEFINITIONS`, written by hand | `mcp.listTools()`, converted to Ollama's `tools` format |
| **CHANGE 2** · where the tools run | `Tools.run(name, args)` in our app | `mcp.callTool(new CallToolRequest(name, args))` on the server |

### Python version

The same server, client and agent with the official MCP Python SDK: see [`python/`](python).

## Demo: see the protocol for yourself

### 1 · Talk to the server by hand

Start the server and paste JSON-RPC lines into it. You are the client. See [`raw-jsonrpc/stdio-by-hand.md`](raw-jsonrpc/stdio-by-hand.md).

```bash
java -jar target/files-server.jar
{"jsonrpc":"2.0","id":2,"method":"tools/list"}          ← after the 2 handshake lines in the guide
```

### 2 · MCP Inspector (the official testing tool)

```bash
npx @modelcontextprotocol/inspector java -jar target/files-server.jar
```

It opens in your browser: list the tools, call `read_file`, and watch the same JSON-RPC messages in its history.

### 3 · Built once, works in another app

The same jar, unchanged, in **Claude Desktop** (`Settings → Developer → Edit Config`, then restart Claude Desktop):

```json
{
  "mcpServers": {
    "files-server": {
      "command": "java",
      "args": ["-jar", "/FULL/PATH/zero-to-ai-agent/episodes/ep05-mcp-java-sdk/target/files-server.jar",
               "/FULL/PATH/zero-to-ai-agent/episodes/ep05-mcp-java-sdk/workspace"]
    }
  }
}
```

Or in **VS Code**, in `.vscode/mcp.json`:

```json
{
  "servers": {
    "files-server": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "/FULL/PATH/.../target/files-server.jar", "/FULL/PATH/.../workspace"]
    }
  }
}
```

Use **full paths** (on Windows, write `\\` in JSON: `"C:\\Users\\you\\..."`). Then ask: *"List the files in my articles folder."*

## Notes

- **stdout belongs to MCP.** In a stdio server, never `System.out.println`: it breaks the connection. Log to `System.err`.
- **Protocol versions.** This lab uses the SDK's standard (legacy) client and server, which speak MCP **2025-11-25**: they start with an `initialize` handshake. The SDK also has a "modern" API for the newest **2026-07-28** revision, which has no handshake (every request carries its version in `_meta`). A modern client can't talk to a legacy server, so match your other side. The `tools/list` and `tools/call` method names are the same in both.
- **Safety.** `files-server` only works inside `workspace/`, and `save_file` never overwrites (Ep 4 rule 4).
- A small local model is not perfect. If it skips a step or misses the duplicate, rerun, or try `qwen3.5:9b`.

## Part 2 · Hello World MCP

[`helloworld/`](helloworld) is the smallest version of all this, written with the SDK's **modern** API: a server with `sayHello` and `getProfile`, a plain client, and a host that puts an Ollama LLM in front of the client (host → client → server). See its [README](helloworld/README.md).

## Next

[← Ep 4](../ep04-tool-calling-java) · [Ep 6 · RAG →](../ep06-rag-java)
