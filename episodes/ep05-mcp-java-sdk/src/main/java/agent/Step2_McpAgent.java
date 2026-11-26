package agent;

import agent.Ollama.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import filesserver.FileTools;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * STEP 2 · The Ep 4 agent, now an MCP host.
 *
 * Same job as Ep 4 (Trends.txt -> rag.md + mcp.md, skip Tokens). Same loop. Same Ollama call.
 * Only TWO things change:
 *
 *   CHANGE 1 · where the tools come from:  tools/list from files-server   (Ep 4: a hand-written list)
 *   CHANGE 2 · where the tools run:        tools/call on files-server     (Ep 4: Tools.run(...) in our app)
 *
 * The LLM doesn't know or care that the tools come from MCP. It never talks to the server: our host does.
 */
public class Step2_McpAgent {

    static final int MAX_STEPS = 10;   // Ep 4 rule 2: a confused model can't loop forever

    static final String TASK = """
            Read Trends.txt. For each topic, write a short article (about 150 words, in markdown) \
            and save it as its own .md file in the articles folder. \
            Skip topics that already have a file in the articles folder, \
            and merge topics that mean the same thing into one article.""";

    static final String SYSTEM = """
            You are an article-writing agent working on my computer. You cannot see any file unless you use a tool. Work one step at a time.
            Rules:
            - A topic is already written if the articles folder has a file named after it (tokens.md means Tokens is done). Skip it. Never read existing articles.
            - Topics that mean the same thing get one article.
            - Save each new article with save_file.
            - When every topic is handled, reply with a one-line summary and no tool call.""";

    public static void main(String[] args) throws Exception {
        resetArticles();

        try (McpSyncClient mcp = FilesServerClient.connect()) {
            mcp.initialize();

            // CHANGE 1 · the tools come from the MCP server
            List<Tool> mcpTools = mcp.listTools().tools();
            List<Map<String, Object>> tools = mcpTools.stream().map(Step2_McpAgent::toOllamaTool).toList();
            System.out.println("→ tools/list            ← " + String.join(", ", mcpTools.stream().map(Tool::name).toList()) + "\n");

            List<Message> messages = new ArrayList<>();
            messages.add(Message.system(SYSTEM));
            messages.add(Message.user(TASK));
            System.out.println("you > " + TASK + "\n");

            // The Ep 4 loop. Unchanged.
            for (int step = 1; step <= MAX_STEPS; step++) {
                JsonNode reply = Ollama.chat(messages, tools);
                JsonNode calls = Ollama.toolCalls(reply);

                if (calls.isEmpty()) {                                   // Ep 4 rule 1: no tool call = done
                    System.out.printf("REQ %d  no tool call: %s%n", step, Ollama.text(reply).trim());
                    System.out.println("\nDone. workspace/articles now has:");
                    try (Stream<Path> files = Files.list(articlesFolder())) {
                        files.map(p -> "  " + p.getFileName()).sorted().forEach(System.out::println);
                    }
                    return;
                }

                messages.add(Message.assistant(reply));

                for (JsonNode call : calls) {
                    String name = call.path("function").path("name").asText();
                    Map<String, Object> arguments = Ollama.JSON.convertValue(
                            call.path("function").path("arguments"), new TypeReference<Map<String, Object>>() {});

                    // CHANGE 2 · the tool runs on the MCP server, through tools/call
                    CallToolResult result = mcp.callTool(new CallToolRequest(name, arguments));
                    String text = Step1_ListAndCall.text(result);
                    messages.add(Message.tool(name, text));

                    System.out.printf("REQ %d  LLM asked: %s(%s)%n", step, name, shortArgs(arguments));
                    System.out.printf("        → tools/call %s  ← %s%n", name, oneLine(text));
                }
            }
            System.out.println("\nStopped: hit MAX_STEPS = " + MAX_STEPS + ". The job may not be finished.");
        }
    }

    /**
     * CHANGE 1 · MCP tool -> Ollama tool. The same three fields, wrapped differently:
     *   MCP:    { name, description, inputSchema }
     *   Ollama: { type: "function", function: { name, description, parameters } }
     */
    static Map<String, Object> toOllamaTool(Tool tool) {
        return Map.of("type", "function",
                "function", Map.of("name", tool.name(),
                                   "description", tool.description(),
                                   "parameters", tool.inputSchema()));
    }

    // =====================================================================
    // Lab helpers (printing, and resetting the folder between runs)
    // =====================================================================

    static Path articlesFolder() {
        return FileTools.findWorkspace().resolve("articles");
    }

    /** Lab helper only: put articles/ back to just tokens.md, so every run starts the same. */
    static void resetArticles() throws Exception {
        Path articles = articlesFolder();
        if (!Files.isDirectory(articles)) {
            throw new IllegalStateException("workspace/articles not found. Run from the repo or from episodes/ep05-mcp-java-sdk");
        }
        try (Stream<Path> files = Files.list(articles)) {
            for (Path f : files.toList()) {
                if (!f.getFileName().toString().equals("tokens.md")) Files.delete(f);
            }
        }
    }

    static String shortArgs(Map<String, Object> args) {
        List<String> parts = new ArrayList<>();
        args.forEach((k, v) -> {
            String s = String.valueOf(v).replace("\n", " ");
            parts.add(k + "=\"" + (s.length() > 30 ? s.substring(0, 30) + "…" : s) + "\"");
        });
        return String.join(", ", parts);
    }

    static String oneLine(String s) {
        String flat = s.trim().replace("\n", " · ");
        return flat.length() > 90 ? flat.substring(0, 90) + "…" : flat;
    }
}
