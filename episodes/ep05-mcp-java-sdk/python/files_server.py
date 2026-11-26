"""
files_server.py · Our MCP server (Python version of filesserver/FilesServer.java).

The same three tools as Ep 4: read_file, list_files, save_file.
In Ep 4 they lived inside the agent. Now they live in their own small program,
an MCP server, so ANY MCP host can use them: our agent, Claude Desktop, VS Code...

It talks over stdio: JSON-RPC messages come in on stdin and go out on stdout.
So never print() to stdout here: stdout belongs to MCP. Log to stderr instead.

Run it on its own (it waits for JSON-RPC on stdin, see ../raw-jsonrpc/):
    python files_server.py
    python files_server.py /full/path/to/workspace
"""

import sys
from pathlib import Path

from mcp.server.mcpserver import MCPServer


def find_workspace() -> Path:
    """The folder with Trends.txt: from the first argument, or searched from here upwards."""
    if len(sys.argv) > 1:
        return Path(sys.argv[1]).resolve()
    here = Path(__file__).resolve().parent
    for folder in [here, *here.parents]:
        for candidate in (folder / "workspace", folder / "episodes/ep05-mcp-java-sdk/workspace"):
            if (candidate / "Trends.txt").is_file():
                return candidate
    return (here.parent / "workspace").resolve()


WORKSPACE = find_workspace()
ARTICLES = WORKSPACE / "articles"


def in_workspace(path: str) -> Path:
    """Resolve a path and refuse anything outside the workspace folder."""
    p = (WORKSPACE / (path or "")).resolve()
    if p != WORKSPACE and WORKSPACE not in p.parents:
        raise ValueError(f"outside the workspace: {path}")
    return p


# The MCP server. The name is what hosts show to the user.
mcp = MCPServer("files-server", version="1.0.0", log_level="WARNING")


# Each tool = a plain Python function + a description the LLM reads.
# The SDK builds the input schema from the type hints (path: str -> {"path": {"type": "string"}}).
# structured_output=False keeps tools/list simple: name, description, inputSchema. Nothing else.
# Errors are returned as text, like Ep 4, so the LLM can read them and decide what to do.

@mcp.tool(description="Read a text file from my computer and return its content. Example path: Trends.txt",
          structured_output=False)
def read_file(path: str) -> str:
    try:
        file = in_workspace(path)
        if not file.is_file():
            return f"error: file not found: {path}"
        return file.read_text(encoding="utf-8")
    except Exception as e:
        return f"error: {e}"


@mcp.tool(description="List the file names in a folder on my computer. In the articles folder, each file is an "
                      "article that is already written: tokens.md means the Tokens article exists, "
                      "so do not write or read it again.",
          structured_output=False)
def list_files(folder: str) -> str:
    try:
        directory = in_workspace(folder)
        if not directory.is_dir():
            return f"error: folder not found: {folder}"
        names = sorted(p.name for p in directory.iterdir())
        return "\n".join(names) if names else "(empty folder)"
    except Exception as e:
        return f"error: {e}"


@mcp.tool(description="Save one article as a new markdown file in the articles folder. Fails if the file already exists.",
          structured_output=False)
def save_file(name: str, content: str) -> str:
    try:
        file_name = Path(name).name                       # "articles/rag.md" -> "rag.md"
        if not file_name.endswith(".md"):
            return f"error: only .md files can be saved, got: {name}"
        file = ARTICLES / file_name
        if file.exists():                                 # Ep 4 rule 4: careful with write tools
            return f"error: articles/{file_name} already exists. Not overwritten."
        ARTICLES.mkdir(parents=True, exist_ok=True)
        file.write_text(content, encoding="utf-8")
        return f"saved articles/{file_name} ({len(content.split())} words)"
    except Exception as e:
        return f"error: {e}"


if __name__ == "__main__":
    print(f"files-server ready (workspace: {WORKSPACE})", file=sys.stderr)   # stderr, never stdout
    mcp.run(transport="stdio")
