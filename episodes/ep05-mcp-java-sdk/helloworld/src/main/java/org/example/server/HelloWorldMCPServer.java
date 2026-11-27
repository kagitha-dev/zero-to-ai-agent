package org.example.server;


import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.server.McpRequestContext;
import io.modelcontextprotocol.modern.server.McpServer;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;
import io.modelcontextprotocol.modern.server.feature.McpSyncToolRepository;
import io.modelcontextprotocol.modern.server.feature.ToolsFeature;
import io.modelcontextprotocol.modern.server.feature.ToolsPage;
import io.modelcontextprotocol.modern.server.transport.StdioMcpTransport;

import java.util.List;
import java.util.Map;

public class HelloWorldMCPServer implements McpSyncToolRepository {

    // Sample data for the getProfile tool: name -> profile. Change these to your own.
    private static final Map<String, Map<String, Object>> PROFILES = Map.of(
            "Prasad", Map.of(
                    "qualification", "B.Tech, Computer Science",
                    "experience", "15 years",
                    "role", "Java architect",
                    "skills", List.of("Java", "Spring Boot", "AI agents")),
            "Anita", Map.of(
                    "qualification", "M.Sc, Data Science",
                    "experience", "6 years",
                    "role", "ML engineer",
                    "skills", List.of("Python", "RAG", "LLM evals")));

    private final McpSyncTool profileTool = McpSyncTool.of(
        McpSchema.Tool.builder("getProfile", Map.of(
                        "type", "object",
                        "properties", Map.of("name", Map.of("type", "string")),
                        "required", List.of("name")))
                .description("Returns a person's profile (qualification, experience, role, skills) by name")
                .build(),
            (ctx, request) -> {

                System.err.println("========== getProfile STARTED ==========");

                System.err.println("Arguments: " + request.arguments());

                String name =
                        (String) request.arguments().get("name");

                Map<String, Object> profile = PROFILES.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(name))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);

                if (profile == null) {
                    System.err.println("No profile for: " + name);
                    return McpSyncResponse.result(
                            McpSchema.CallToolResult.builder()
                                    .addContent(McpSchema.TextContent.builder("No profile found for " + name).build())
                                    .isError(true)
                                    .build()
                    );
                }

                String json;
                try {
                    json = McpJsonDefaults.getMapper().writeValueAsString(profile);
                } catch (java.io.IOException e) {
                    json = profile.toString();
                }

                System.err.println("Returning: " + json);

                return McpSyncResponse.result(
                        McpSchema.CallToolResult.builder()
                                .addContent(
                                        McpSchema.TextContent.builder(json).build()
                                )
                                .build()
                );
            }
        );

    private final McpSyncTool helloTool = McpSyncTool.of(
        McpSchema.Tool.builder("sayHello", Map.of(
                        "type", "object",
                        "properties", Map.of("name", Map.of("type", "string")),
                        "required", List.of("name")))
                .description("Greets a person by name")
                .build(),
            (ctx, request) -> {

                System.err.println("========== HANDLER STARTED ==========");

                System.err.println("Arguments: " + request.arguments());

                String name =
                        (String) request.arguments().get("name");

                String message = "Hello, " + name + "!";

                System.err.println("Executing: " + message);

                return McpSyncResponse.result(
                        McpSchema.CallToolResult.builder()
                                .addContent(
                                        McpSchema.TextContent.builder(message).build()
                                )
                                .build()
                );
            }
        );

    @Override
    public ToolsPage list(McpRequestContext ctx, String cursor) {
        System.err.println("Listing tools");
        return ToolsPage.of(List.of(this.helloTool.definition(), this.profileTool.definition()));
    }

    @Override
    public McpSyncTool find(McpRequestContext ctx, String name) {
        System.err.println("finding tool " + name);
        if ("sayHello".equals(name)) {
            System.err.println("Tool found");
            return helloTool;
        }
        if ("getProfile".equals(name)) {
            System.err.println("Tool found");
            return profileTool;
        }
        System.err.println("Tool not found");
        return null;
    }

    public static void main(String[] args) {

        McpServer server = McpServer.builder()
                .serverInfo(McpSchema.Implementation.builder("hello-world-server", "1.0.0").build())
                .feature(ToolsFeature.ofSync(new HelloWorldMCPServer()))
                .build();

        StdioMcpTransport transport = new StdioMcpTransport(server, McpJsonDefaults.getMapper());

        System.err.println("Running MCP Server");
        // Serve until stdin is closed or closeGracefully() is called.
        transport.start().block();

    }
}
