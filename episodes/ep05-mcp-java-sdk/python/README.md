# Ep 5 · Python version

The same MCP server and the same agent as the Java lab, written with the official
[MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk) (`mcp` 2.x).

| File | Java original | What it shows |
|---|---|---|
| `files_server.py` | `filesserver/FilesServer.java` | **The MCP server**: `read_file`, `list_files`, `save_file` over stdio |
| `list_and_call.py` | `agent/Step1_ListAndCall.java` | **An MCP client, no LLM**: `initialize` → `tools/list` → `tools/call` |
| `mcp_agent.py` | `agent/Step2_McpAgent.java` | **The Ep 4 agent as an MCP host**: tools come from `tools/list`, run with `tools/call` |

## Setup

Needs **Python 3.10+**, and for `mcp_agent.py`, Ollama running with the model pulled (`ollama pull qwen3.5:4b`).

```bash
cd episodes/ep05-mcp-java-sdk/python
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Run

```bash
python list_and_call.py            # starts files_server.py for you, lists and calls the tools
python mcp_agent.py                # the full Trends.txt job: rag.md + mcp.md, Tokens skipped
```

You never start `files_server.py` yourself for these two: the client starts it as a child process
and talks to it over stdin / stdout. That's what **stdio** means.

To talk to the server **by hand**, start it on its own and paste JSON-RPC lines into it:
see [`../raw-jsonrpc/stdio-by-hand.md`](../raw-jsonrpc/stdio-by-hand.md).

```bash
python files_server.py
```

## The one rule for stdio servers

`print()` goes to stdout, and stdout belongs to MCP. A stray `print("hello")` breaks the
connection. Log to stderr instead: `print(..., file=sys.stderr)`.

## Python vs Java, side by side

| | Python SDK | Java SDK |
|---|---|---|
| Server | `mcp = MCPServer("files-server")` | `McpServer.sync(stdio).serverInfo("files-server", "1.0.0")` |
| A tool | `@mcp.tool(description=...)` on a function. The schema comes from the type hints | `SyncToolSpecification`: `Tool.builder(name, schema)` + a call handler |
| stdio | `mcp.run(transport="stdio")` | `new StdioServerTransportProvider(...)` |
| Client | `stdio_client(params)` + `ClientSession` | `new StdioClientTransport(params, ...)` + `McpClient.sync(...)` |
| Calls | `initialize()`, `list_tools()`, `call_tool(name, args)` | `initialize()`, `listTools()`, `callTool(new CallToolRequest(name, args))` |
