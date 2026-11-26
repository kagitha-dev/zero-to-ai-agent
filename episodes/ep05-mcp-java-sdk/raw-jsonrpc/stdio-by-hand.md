# Talk to the MCP server by hand (no client, no SDK)

**stdio** means the server reads JSON-RPC from its **stdin** and writes answers to its **stdout**.
So you can be the client yourself: start the server in a terminal and paste messages into it.

Every message is **one line of JSON**. Paste one line, press Enter, and read the answer.

## 1 · Start the server

```bash
# Java (from episodes/ep05-mcp-java-sdk, after: mvn -q package)
java -jar target/files-server.jar

# or Python (from episodes/ep05-mcp-java-sdk/python)
python files_server.py
```

It prints `files-server ready (workspace: …)` on **stderr** and then waits. Nothing else is ever printed to stdout except MCP messages.

## 2 · Paste these lines, one at a time

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"terminal","version":"1.0"}}}
```
```json
{"jsonrpc":"2.0","method":"notifications/initialized"}
```
```json
{"jsonrpc":"2.0","id":2,"method":"tools/list"}
```
```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"read_file","arguments":{"path":"Trends.txt"}}}
```
```json
{"jsonrpc":"2.0","id":4,"method":"foo"}
```

Press `Ctrl+C` (or `Ctrl+D`) to stop the server.

## 3 · What you get back, and which slide it proves

Real output from the Java server (the Python server answers the same way).

| You sent | The server answered | Slide |
|---|---|---|
| `initialize` (id 1) | `{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","capabilities":{"logging":{},"tools":{"listChanged":false}},"serverInfo":{"name":"files-server","version":"1.0.0"}}}` | the version check, in "How it talks to the agent" |
| `notifications/initialized` (**no id**) | **nothing**: a notification never gets a reply | "With an id: a request. Without an id: a notification" |
| `tools/list` (id 2) | the 3 tools, each with `name`, `description`, `inputSchema` | "MCP's two key methods" |
| `tools/call` (id 3) | `{"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"RAG\nMCP\nModel Context Protocol\nTokens\n"}],"isError":false}}` | "An MCP message is just a JSON-RPC message" |
| `foo` (id 4) | `{"jsonrpc":"2.0","id":4,"error":{"code":-32601,"message":"Method not found: foo"}}` | "The answer comes back with the same id" (error codes) |

Watch the ids: if you paste lines 4 and 5 quickly, the answer for **id 4 can arrive before id 3**. That's fine. Each answer carries its id, so the client always knows which question it answers.

## Notes

- `initialize` + `notifications/initialized` is the handshake of the **2025-11-25** protocol (the "legacy" one the SDKs use by default). In the newest **2026-07-28** protocol there's no handshake: each request carries its version in a `_meta` field instead.
- The `tools/...` method names are the same in every MCP server. Only the tool names (`read_file`) and their arguments change.
