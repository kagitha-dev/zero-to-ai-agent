package filesserver;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * OUR MCP SERVER · files-server
 *
 * The same three tools as Ep 4 (read_file, list_files, save_file), now offered the MCP way,
 * so ANY MCP host can use them: our agent, Claude Desktop, VS Code...
 *
 * Each tool = what the LLM reads (name, description, input schema) + the Java method that runs it.
 *
 * It talks over stdio: JSON-RPC messages come in on stdin and go out on stdout.
 * So NEVER System.out.println here: stdout belongs to MCP. Log to System.err instead.
 *
 * Build and run on its own (it then waits for JSON-RPC on stdin, see raw-jsonrpc/):
 *   mvn -q package
 *   java -jar target/files-server.jar                       (finds ./workspace by itself)
 *   java -jar target/files-server.jar /full/path/workspace   (or tell it where)
 */
public class FilesServer {

    public static void main(String[] args) {
        // The SDK logs through SLF4J -> stderr. Keep it quiet: warnings only.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        FileTools files = new FileTools(args.length > 0 ? Path.of(args[0]) : FileTools.findWorkspace());

        // 1 · The three tools: name + description + input schema + the Java method to run
        SyncToolSpecification readFile = tool("read_file",
                "Read a text file from my computer and return its content. Example path: Trends.txt",
                schema("path", "The file path, for example Trends.txt"),
                a -> files.readFile(a.get("path")));

        SyncToolSpecification listFiles = tool("list_files",
                "List the file names in a folder on my computer. In the articles folder, each file is an article that is already written: "
                        + "tokens.md means the Tokens article exists, so do not write or read it again.",
                schema("folder", "The folder name, for example articles"),
                a -> files.listFiles(a.get("folder")));

        SyncToolSpecification saveFile = tool("save_file",
                "Save one article as a new markdown file in the articles folder. Fails if the file already exists.",
                schema("name", "The file name, for example rag.md",
                       "content", "The full article, in markdown"),
                a -> files.saveFile(a.get("name"), a.get("content")));

        // 2 · The transport: stdio (stdin / stdout)
        StdioServerTransportProvider stdio = new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        // 3 · The server: a name, what it offers (tools), and the tools themselves
        McpServer.sync(stdio)
                .serverInfo("files-server", "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(readFile, listFiles, saveFile)
                .build();

        // The SDK now reads stdin on its own thread, so the program keeps running.
        System.err.println("files-server ready (workspace: " + files.workspace() + ")");   // stderr, never stdout
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** One MCP tool: its definition (what tools/list shows) + its handler (what tools/call runs). */
    static SyncToolSpecification tool(String name, String description, Map<String, Object> inputSchema,
                                      Function<Map<String, String>, String> javaMethod) {
        return SyncToolSpecification.builder()
                .tool(Tool.builder(name, inputSchema).description(description).build())
                .callHandler((exchange, request) -> {
                    String result = javaMethod.apply(asStrings(request.arguments()));
                    return CallToolResult.builder()
                            .addTextContent(result)
                            .isError(result.startsWith("error:"))   // still text: the LLM reads it (Ep 4 rule 3)
                            .build();
                })
                .build();
    }

    /** A JSON schema with string parameters, all required: schema("path", "The file path", ...). */
    static Map<String, Object> schema(String... namesAndDescriptions) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < namesAndDescriptions.length; i += 2) {
            properties.put(namesAndDescriptions[i],
                    Map.of("type", "string", "description", namesAndDescriptions[i + 1]));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        return schema;
    }

    /** The arguments arrive as a JSON object; our tools only take strings. */
    private static Map<String, String> asStrings(Map<String, Object> arguments) {
        Map<String, String> out = new LinkedHashMap<>();
        if (arguments != null) arguments.forEach((k, v) -> out.put(k, v == null ? null : String.valueOf(v)));
        return out;
    }
}
