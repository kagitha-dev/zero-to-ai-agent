# Ep 5 · Part 2 · Hello World MCP (modern Java SDK)

The smallest MCP setup there is: one server with two tools, a plain client, and a host that adds an LLM.
It uses the **modern** (stateless, spec 2026-07-28) API of the MCP Java SDK 2.2.0 (`io.modelcontextprotocol.modern`), so there's no `initialize` handshake.

```
HelloWorldMCPClient:   main() ──> MCP client ══ stdio ══> HelloWorldMCPServer
                       (main() hard-codes "sayHello" and "Prasad")

HelloWorldMCPHost:     Ollama LLM <──> HOST ──> MCP client ══ stdio ══> HelloWorldMCPServer
                       (the LLM picks the tool and the name)
```

## The code

| Class | What it is |
|---|---|
| `server/HelloWorldMCPServer` | **MCP server.** Tools: `sayHello(name)` and `getProfile(name)`, which looks the name up in the `PROFILES` map at the top. Served over stdio |
| `client/HelloWorldMCPClient` | **MCP client only.** Starts the server, `tools/list`, then `tools/call sayHello` with a hard-coded name. No LLM |
| `host/HelloWorldMCPHost` | **MCP host.** Lists the tools through its client, gives them to the LLM, and runs each `tool_call` the LLM makes through the client (`tools/call`), until the LLM answers in text |
| `host/Ollama` | One POST to `http://localhost:11434/api/chat`, using the MCP SDK's own JSON mapper |

## Run it

You need Java 21, Maven and Ollama with a tool-calling model:

```bash
ollama pull qwen3.5:4b
cd episodes/ep05-mcp-java-sdk/helloworld
mvn package                       # builds target/helloworld-mcp-server.jar (the server)
```

Then run these from your IDE, with the **working directory set to this `helloworld` folder** (the client starts `target/helloworld-mcp-server.jar` from there):

- `org.example.client.HelloWorldMCPClient`: client → server only
- `org.example.host.HelloWorldMCPHost`: host → client → server, with the LLM. Pass a question as program arguments, or it asks: *"Say hello to Prasad, then tell me his qualification and experience."*

Server logs go to **stderr** (`System.err`), because stdout carries the JSON-RPC messages. `run-server.bat` starts the server alone and saves its logs to `server.log`.

## What the host prints

```
[client -> server] tools/list
[server -> client] [sayHello, getProfile]

[user  -> host]   Say hello to Prasad, then tell me his qualification and experience.

[host  -> LLM]    chat + 2 tools  (step 1)
[LLM   -> host]   tool_call sayHello{name=Prasad}
[host  -> client -> server] tools/call sayHello
[server -> client -> host] Hello, Prasad!
...
[LLM   -> host]   final answer

assistant > ...
```

The exact tool calls and wording depend on the model.
