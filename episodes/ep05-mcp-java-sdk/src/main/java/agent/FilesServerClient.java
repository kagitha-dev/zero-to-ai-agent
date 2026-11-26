package agent;

import filesserver.FileTools;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * OUR MCP CLIENT · connects to files-server over stdio.
 *
 * "stdio" means: start the server as a small program, then talk to it through its stdin / stdout.
 * The command is exactly what you would type in a terminal:
 *
 *   java -jar target/files-server.jar /full/path/to/workspace
 */
public class FilesServerClient {

    public static McpSyncClient connect() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");   // SDK logs: warnings only

        Path jar = findServerJar();
        Path workspace = FileTools.findWorkspace();

        // 1 · How to start the server (the same command as in a terminal, or in Claude Desktop's config)
        ServerParameters command = ServerParameters.builder(javaBinary())
                .args("-jar", jar.toString(), workspace.toString())
                .build();

        // 2 · The stdio transport: starts that process and wires up its stdin / stdout
        StdioClientTransport stdio = new StdioClientTransport(command, McpJsonDefaults.getMapper());

        // 3 · The MCP client
        return McpClient.sync(stdio)
                .clientInfo(new Implementation("article-agent", "1.0.0"))
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }

    /** The same java that runs this program, so the server uses the same Java version. */
    private static String javaBinary() {
        return ProcessHandle.current().info().command().orElse("java");
    }

    /** target/files-server.jar, built by "mvn package", found from the episode folder or the repo root. */
    private static Path findServerJar() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            for (Path candidate : List.of(dir.resolve("target/files-server.jar"),
                                          dir.resolve("episodes/ep05-mcp-java-sdk/target/files-server.jar"))) {
                if (Files.isRegularFile(candidate)) return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("target/files-server.jar not found. Build it first, from "
                + "episodes/ep05-mcp-java-sdk:  mvn -q package");
    }
}
