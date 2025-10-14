package com.example.drsum;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Main class for the DrSum MCP Server.
 * 
 * This server implements the Model Context Protocol (MCP) to provide
 * document summarization capabilities to AI assistants and applications.
 */
public class DrSumMcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumMcpServer.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting DrSum MCP Server...");
            
            // Create STDIO transport provider for communication
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(McpJsonMapper.getDefault());
            
            // Define server capabilities
            McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true) // Enable tools
                    .build();
            
            // Create server info
            McpSchema.Implementation serverInfo = new McpSchema.Implementation(
                    "drsum-mcp-server", 
                    "1.0.0"
            );
            
            // Create tools
            McpSchema.Tool summarizeTool = createSummarizeTool();
            
            // Create tool specification
            McpServerFeatures.SyncToolSpecification toolSpec = 
                    new McpServerFeatures.SyncToolSpecification(
                            summarizeTool,
                            DrSumMcpServer::handleSummarizeRequest
                    );
            
            // Build and start the server
            var server = McpServer.sync(transportProvider)
                    .serverInfo(serverInfo)
                    .capabilities(capabilities)
                    .instructions("DrSum MCP Server provides document summarization capabilities. " +
                                "Use the 'summarize' tool to generate summaries of text content.")
                    .tools(List.of(toolSpec))
                    .build();
            
            logger.info("DrSum MCP Server started successfully");
            
            // Keep the server running
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down DrSum MCP Server...");
                transportProvider.closeGracefully().block();
            }));
            
            // Block main thread to keep server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start DrSum MCP Server", e);
            System.exit(1);
        }
    }
    
    /**
     * Creates the summarize tool definition.
     */
    private static McpSchema.Tool createSummarizeTool() {
        return McpSchema.Tool.builder()
                .name("summarize")
                .description("Summarizes the provided text content")
                .build();
    }
    
    /**
     * Handles summarize tool requests.
     */
    private static McpSchema.CallToolResult handleSummarizeRequest(
            McpSyncServerExchange exchange, 
            Map<String, Object> arguments) {
        
        try {
            logger.info("Processing summarize request");
            
            // Extract parameters
            String text = (String) arguments.get("text");
            Integer maxSentences = arguments.containsKey("max_sentences") 
                    ? ((Number) arguments.get("max_sentences")).intValue() 
                    : 3;
            
            if (text == null || text.trim().isEmpty()) {
                return createErrorResult("Text parameter is required and cannot be empty");
            }
            
            // Perform summarization
            String summary = summarizeText(text, maxSentences);
            
            // Create successful response
            McpSchema.TextContent content = new McpSchema.TextContent(summary);
            
            logger.info("Successfully processed summarize request");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (Exception e) {
            logger.error("Error processing summarize request", e);
            return createErrorResult("Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Simple text summarization logic.
     * In a real implementation, this would use advanced NLP techniques or AI models.
     */
    private static String summarizeText(String text, int maxSentences) {
        // Simple sentence-based summarization
        String[] sentences = text.split("[.!?]+");
        
        if (sentences.length <= maxSentences) {
            return text.trim();
        }
        
        // Take the first maxSentences sentences as a simple summary
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(maxSentences, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                summary.append(sentence);
                if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?")) {
                    summary.append(".");
                }
                if (i < maxSentences - 1) {
                    summary.append(" ");
                }
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Creates an error result.
     */
    private static McpSchema.CallToolResult createErrorResult(String errorMessage) {
        McpSchema.TextContent errorContent = new McpSchema.TextContent(
                "Error: " + errorMessage
        );
        return McpSchema.CallToolResult.builder()
                .content(List.of(errorContent))
                .isError(true)
                .build();
    }
}