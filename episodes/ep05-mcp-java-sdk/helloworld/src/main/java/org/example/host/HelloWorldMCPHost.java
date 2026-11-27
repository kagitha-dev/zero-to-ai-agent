package org.example.host;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.client.McpClient;
import io.modelcontextprotocol.modern.client.McpSyncClient;
import io.modelcontextprotocol.modern.client.transport.StdioMcpClientTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The MCP HOST: the AI app.
 *
 *   LLM (Ollama)  <->  HOST  ->  MCP CLIENT  ->  MCP SERVER (HelloWorldMCPServer)
 *
 * In HelloWorldMCPClient, main() hard-coded "sayHello" and "Prasad".
 * Here the LLM decides which tool to call and with what name. The host only carries messages:
 *   - the LLM never talks to the server
 *   - the server never talks to the LLM
 *   - the host's MCP client is the only part that speaks MCP
 *
 * Run: mvn package (builds target/helloworld-mcp-server.jar), start Ollama, then run this class.
 * Optional: pass your own question as the program arguments.
 */
public class HelloWorldMCPHost {

    static final int MAX_STEPS = 5;   // a confused model can't loop forever

    static final String SYSTEM = """
            You are a helpful assistant. Use the tools when they help.
            When you have everything you need, answer the user in a short, friendly reply.""";

    public static void main(String[] args) throws Exception {

        String question = args.length > 0
                ? String.join(" ", args)
                : "Say hello to Prasad, then tell me his qualification and experience.";

        // 1 · HOST -> starts its MCP CLIENT, and the client starts the SERVER (same as HelloWorldMCPClient)
        ServerParameters params = ServerParameters.builder("java")
                .args("-jar", "target/helloworld-mcp-server.jar")
                .build();

        try (StdioMcpClientTransport transport = StdioMcpClientTransport.builder(params).start()) {
            McpSyncClient client = McpClient.sync(transport).build();

            // 2 · CLIENT -> SERVER : tools/list
            List<McpSchema.Tool> mcpTools = client.listTools().list().tools();
            System.out.println("[client -> server] tools/list");
            System.out.println("[server -> client] " + mcpTools.stream().map(McpSchema.Tool::name).toList() + "\n");

            // The LLM can't read MCP tools directly: the host rewraps them in Ollama's "tools" format
            List<Map<String, Object>> tools = mcpTools.stream().map(HelloWorldMCPHost::toOllamaTool).toList();

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM));
            messages.add(Map.of("role", "user", "content", question));
            System.out.println("[user  -> host]   " + question + "\n");

            // The Ep 4 loop: ask the LLM, run the tools it asks for, repeat until it answers in text
            for (int step = 1; step <= MAX_STEPS; step++) {

                // 3 · HOST -> LLM : the chat history + the tools
                System.out.println("[host  -> LLM]    chat + " + tools.size() + " tools  (step " + step + ")");
                Map<String, Object> reply = Ollama.chat(messages, tools);
                List<Map<String, Object>> calls = Ollama.toolCalls(reply);

                if (calls.isEmpty()) {
                    // 7 · LLM -> HOST : final answer, no more tool calls
                    System.out.println("[LLM   -> host]   final answer\n");
                    System.out.println("assistant > " + reply.get("content"));
                    return;
                }

                messages.add(reply);   // keep the LLM's tool request in the history, as it came

                for (Map<String, Object> call : calls) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> function = (Map<String, Object>) call.get("function");
                    String name = (String) function.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) function.get("arguments");

                    // 4 · LLM -> HOST : "please run this tool"
                    System.out.println("[LLM   -> host]   tool_call " + name + arguments);

                    // 5 · HOST -> CLIENT -> SERVER : tools/call
                    System.out.println("[host  -> client -> server] tools/call " + name);
                    McpSchema.CallToolResult result = client.tool(name)
                            .arguments(arguments)
                            .call();

                    // 6 · SERVER -> CLIENT -> HOST : the result goes back into the history
                    String text = text(result);
                    System.out.println("[server -> client -> host] " + text + "\n");
                    messages.add(Map.of("role", "tool", "tool_name", name, "content", text));
                }
            }
            System.out.println("Stopped: hit MAX_STEPS = " + MAX_STEPS);
        }
    }

    /**
     * MCP tool -> Ollama tool. The same three fields, wrapped differently:
     *   MCP:    { name, description, inputSchema }
     *   Ollama: { type: "function", function: { name, description, parameters } }
     */
    static Map<String, Object> toOllamaTool(McpSchema.Tool tool) {
        return Map.of("type", "function",
                "function", Map.of(
                        "name", tool.name(),
                        "description", Objects.requireNonNullElse(tool.description(), ""),
                        "parameters", tool.inputSchema()));
    }

    /** All the text parts of a tool result, joined. */
    static String text(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .collect(Collectors.joining("\n"));
    }
}
