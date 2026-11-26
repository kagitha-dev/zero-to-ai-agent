package agent;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;

/**
 * STEP 1 · An MCP client with no LLM at all.
 *
 *   1. start files-server and connect over stdio
 *   2. initialize   -> the version check (the handshake)
 *   3. tools/list   -> "what tools do you have?"
 *   4. tools/call   -> "run read_file with path Trends.txt"
 *
 * The SDK writes the JSON-RPC messages for us. To see the raw messages, try raw-jsonrpc/.
 */
public class Step1_ListAndCall {

    public static void main(String[] args) {
        try (McpSyncClient client = FilesServerClient.connect()) {

            InitializeResult info = client.initialize();                           // 1 · handshake
            System.out.printf("connected to %s %s (protocol %s)%n%n",
                    info.serverInfo().name(), info.serverInfo().version(), info.protocolVersion());

            ListToolsResult tools = client.listTools();                            // 2 · tools/list
            System.out.println("tools/list ->");
            for (Tool tool : tools.tools()) {
                Object params = ((Map<?, ?>) tool.inputSchema().get("properties")).keySet();
                System.out.printf("  %s%s: %s%n", tool.name(), params, cut(tool.description(), 60));
            }

            CallToolResult result = client.callTool(                               // 3 · tools/call
                    new CallToolRequest("read_file", Map.of("path", "Trends.txt")));
            System.out.println("\ntools/call read_file {\"path\": \"Trends.txt\"} ->");
            System.out.println(text(result).strip().indent(2));
        }
    }

    /** The text inside a tool result: content[].text */
    static String text(CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        result.content().forEach(c -> { if (c instanceof TextContent t) sb.append(t.text()); });
        return sb.toString();
    }

    static String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
