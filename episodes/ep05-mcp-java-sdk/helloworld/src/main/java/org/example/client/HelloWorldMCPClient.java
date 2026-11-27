package org.example.client;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.client.McpClient;
import io.modelcontextprotocol.modern.client.McpSyncClient;
import io.modelcontextprotocol.modern.client.transport.StdioMcpClientTransport;

import java.util.Map;

public class HelloWorldMCPClient {

    public static void main(String[] args) {

        ServerParameters params = ServerParameters.builder("java")
                .args("-jar", "target/helloworld-mcp-server.jar")
                .build();

        try (StdioMcpClientTransport transport = StdioMcpClientTransport.builder(params).start()) {
            McpSyncClient client = McpClient.sync(transport).build();

            for (McpSchema.Tool tool : client.listTools().list().tools()) {
                System.out.println(tool.name());
            }

            var result = client.tool("sayHello")
                    .arguments(Map.of("name", "Prasad"))
                    .call();


            System.out.println("\nServer response:");

            System.out.println(result.content());


        }

    }

}
