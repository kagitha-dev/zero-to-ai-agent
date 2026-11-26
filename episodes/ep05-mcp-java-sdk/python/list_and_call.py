"""
list_and_call.py · An MCP client with no LLM at all (Python version of Step1_ListAndCall.java).

What it does, in order:
  1. Start files_server.py as a child process and connect to it over stdio.
  2. initialize        -> the version check (the handshake)
  3. tools/list        -> "what tools do you have?"
  4. tools/call        -> "run read_file with path Trends.txt"

The SDK writes the JSON-RPC messages for us. To see the raw messages, try ../raw-jsonrpc/.

Run:
    python list_and_call.py
"""

import asyncio
import sys
from pathlib import Path

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

SERVER = Path(__file__).resolve().parent / "files_server.py"


async def main():
    # How to start the server: the same command you would type in a terminal
    server = StdioServerParameters(command=sys.executable, args=[str(SERVER)])

    async with stdio_client(server) as (read, write):          # starts the process, wires stdin/stdout
        async with ClientSession(read, write) as client:      # our MCP client
            info = await client.initialize()                    # 1 · the handshake
            print(f"connected to {info.server_info.name} {info.server_info.version}"
                  f" (protocol {info.protocol_version})\n")

            tools = await client.list_tools()                   # 2 · tools/list
            print("tools/list ->")
            for tool in tools.tools:
                print(f"  {tool.name}{list(tool.input_schema['properties'])}: {tool.description[:60]}…")

            result = await client.call_tool("read_file", {"path": "Trends.txt"})   # 3 · tools/call
            print('\ntools/call read_file {"path": "Trends.txt"} ->')
            print("  " + result.content[0].text.strip().replace("\n", "\n  "))


if __name__ == "__main__":
    asyncio.run(main())
