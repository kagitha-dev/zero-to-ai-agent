"""
mcp_agent.py · The Ep 4 agent, now an MCP host (Python version of Step2_McpAgent.java).

Same job as Ep 4: read Trends.txt, write one article per topic, skip what exists,
merge MCP + Model Context Protocol. Same loop, same Ollama call.

Only TWO things change from Ep 4:
  1. Where the tools come from:  tools/list from the MCP server (not a hand-written list)
  2. Where the tools run:        tools/call on the MCP server  (not a local function)

Run (Ollama running, `ollama pull qwen3.5:4b` done):
    python mcp_agent.py
"""

import asyncio
import json
import shutil
import sys
import urllib.request
from pathlib import Path

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL = "qwen3.5:4b"
MAX_STEPS = 10                     # Ep 4 rule 2: a confused model can't loop forever

HERE = Path(__file__).resolve().parent
SERVER = HERE / "files_server.py"
WORKSPACE = HERE.parent / "workspace"

TASK = ("Read Trends.txt. For each topic, write a short article (about 150 words, in markdown) "
        "and save it as its own .md file in the articles folder. "
        "Skip topics that already have a file in the articles folder, "
        "and merge topics that mean the same thing into one article.")

SYSTEM = """You are an article-writing agent working on my computer. You cannot see any file unless you use a tool. Work one step at a time.
Rules:
- A topic is already written if the articles folder has a file named after it (tokens.md means Tokens is done). Skip it. Never read existing articles.
- Topics that mean the same thing get one article.
- Save each new article with save_file.
- When every topic is handled, reply with a one-line summary and no tool call."""


# ---------------------------------------------------------------------------
# Ollama: the same /api/chat call as Ep 3 and Ep 4
# ---------------------------------------------------------------------------

def ollama_chat(messages, tools):
    body = {
        "model": MODEL,
        "messages": messages,
        "tools": tools,
        "think": False,
        "stream": False,
        "options": {"temperature": 0, "seed": 42, "num_ctx": 8192},
    }
    request = urllib.request.Request(OLLAMA_URL, data=json.dumps(body).encode("utf-8"),
                                     headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read())


# ---------------------------------------------------------------------------
# CHANGE 1 · MCP tool -> Ollama tool. Same three fields, different wrapping.
#   MCP:    { name, description, inputSchema }
#   Ollama: { type: "function", function: { name, description, parameters } }
# ---------------------------------------------------------------------------

def to_ollama_tool(tool):
    return {"type": "function",
            "function": {"name": tool.name, "description": tool.description, "parameters": tool.input_schema}}


def result_text(result):
    """CallToolResult -> plain text for the LLM. Errors stay text (Ep 4 rule 3)."""
    text = "\n".join(c.text for c in result.content if c.type == "text")
    return ("error: " + text) if result.is_error and not text.startswith("error") else text


def reset_articles():
    """Lab helper only: put articles/ back to just tokens.md, so every run starts the same."""
    articles = WORKSPACE / "articles"
    for f in articles.iterdir():
        if f.name != "tokens.md":
            shutil.rmtree(f) if f.is_dir() else f.unlink()


async def main():
    reset_articles()
    server = StdioServerParameters(command=sys.executable, args=[str(SERVER), str(WORKSPACE)])

    async with stdio_client(server) as (read, write):
        async with ClientSession(read, write) as mcp:
            await mcp.initialize()

            # CHANGE 1 · the tools come from the MCP server
            listed = await mcp.list_tools()
            tools = [to_ollama_tool(t) for t in listed.tools]
            print(f"→ tools/list            ← {', '.join(t.name for t in listed.tools)}\n")

            messages = [{"role": "system", "content": SYSTEM}, {"role": "user", "content": TASK}]
            print(f"you > {TASK}\n")

            # The Ep 4 loop. Unchanged.
            for step in range(1, MAX_STEPS + 1):
                reply = ollama_chat(messages, tools)
                message = reply["message"]
                calls = message.get("tool_calls") or []

                if not calls:                                   # Ep 4 rule 1: no tool call = done
                    print(f"REQ {step}  no tool call: {message.get('content', '').strip()}")
                    print("\nDone. workspace/articles now has:")
                    for f in sorted((WORKSPACE / "articles").iterdir()):
                        print("  " + f.name)
                    return

                messages.append({"role": "assistant", "content": message.get("content", ""),
                                 "tool_calls": calls})

                for call in calls:
                    name = call["function"]["name"]
                    args = call["function"]["arguments"]

                    # CHANGE 2 · the tool runs on the MCP server, through tools/call
                    result = result_text(await mcp.call_tool(name, args))
                    messages.append({"role": "tool", "tool_name": name, "content": result})

                    print(f"REQ {step}  LLM asked: {name}({short(args)})")
                    print(f"        → tools/call {name}  ← {one_line(result)}")

            print(f"\nStopped: hit MAX_STEPS = {MAX_STEPS}. The job may not be finished.")


def short(args):
    return ", ".join(f'{k}="{(str(v)[:30] + "…") if len(str(v)) > 30 else v}"'.replace("\n", " ") for k, v in args.items())


def one_line(text):
    flat = text.strip().replace("\n", " · ")
    return flat[:90] + "…" if len(flat) > 90 else flat


if __name__ == "__main__":
    asyncio.run(main())
