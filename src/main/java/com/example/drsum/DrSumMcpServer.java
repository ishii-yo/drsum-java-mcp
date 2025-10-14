package com.example.drsum;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import jp.co.dw_sapporo.drsum_ea.DWException;
import jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Main class for the DrSum MCP Server.
 * 
 * This server implements the Model Context Protocol (MCP) to provide
 * Dr.Sum database analysis capabilities and document summarization to AI assistants and applications.
 */
public class DrSumMcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumMcpServer.class);
    
    // Dr.Sum connection instance (shared across tools)
    private static DrSumConnection drSumConnection = new DrSumConnection();

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
            McpSchema.Tool configureConnectionTool = createConfigureConnectionTool();
            McpSchema.Tool disconnectTool = createDisconnectTool();
            
            // Create tool specifications
            McpServerFeatures.SyncToolSpecification summarizeSpec = 
                    new McpServerFeatures.SyncToolSpecification(
                            summarizeTool,
                            DrSumMcpServer::handleSummarizeRequest
                    );
            
            McpServerFeatures.SyncToolSpecification configureConnectionSpec = 
                    new McpServerFeatures.SyncToolSpecification(
                            configureConnectionTool,
                            DrSumMcpServer::handleConfigureConnectionRequest
                    );
            
            McpServerFeatures.SyncToolSpecification disconnectSpec = 
                    new McpServerFeatures.SyncToolSpecification(
                            disconnectTool,
                            DrSumMcpServer::handleDisconnectRequest
                    );
            
            // Build and start the server
            var server = McpServer.sync(transportProvider)
                    .serverInfo(serverInfo)
                    .capabilities(capabilities)
                    .instructions("DrSum MCP Server provides Dr.Sum database analysis and document summarization capabilities. " +
                                "Use 'configure_connection' to connect to Dr.Sum, " +
                                "'get_metadata' to retrieve table information, " +
                                "'execute_query' to run SQL queries, " +
                                "and 'disconnect' to close the connection.")
                    .tools(List.of(summarizeSpec, configureConnectionSpec, disconnectSpec))
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
     * Creates the configure_connection tool definition.
     */
    private static McpSchema.Tool createConfigureConnectionTool() {
        return McpSchema.Tool.builder()
                .name("configure_connection")
                .description("Configure and establish connection to Dr.Sum server. " +
                           "Parameters: host (string), port (integer), username (string), " +
                           "password (string, optional), database (string)")
                .build();
    }
    
    /**
     * Creates the disconnect tool definition.
     */
    private static McpSchema.Tool createDisconnectTool() {
        return McpSchema.Tool.builder()
                .name("disconnect")
                .description("Disconnect from Dr.Sum server")
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
     * Handles configure_connection tool requests.
     */
    private static McpSchema.CallToolResult handleConfigureConnectionRequest(
            McpSyncServerExchange exchange, 
            Map<String, Object> arguments) {
        
        try {
            logger.info("Processing configure_connection request");
            
            // Extract and validate parameters
            String host = (String) arguments.get("host");
            Integer port = arguments.containsKey("port") 
                    ? ((Number) arguments.get("port")).intValue() 
                    : null;
            String username = (String) arguments.get("username");
            String password = arguments.containsKey("password") 
                    ? (String) arguments.get("password") 
                    : "";
            String database = (String) arguments.get("database");
            
            // Validate required parameters
            if (host == null || host.trim().isEmpty()) {
                return createErrorResult("Host parameter is required");
            }
            if (port == null) {
                return createErrorResult("Port parameter is required");
            }
            if (username == null || username.trim().isEmpty()) {
                return createErrorResult("Username parameter is required");
            }
            if (database == null || database.trim().isEmpty()) {
                return createErrorResult("Database parameter is required");
            }
            
            // Create connection configuration
            ConnectionConfig config = new ConnectionConfig(host, port, username, password, database);
            
            // Establish connection
            drSumConnection.connect(config);
            
            // Create success response
            String successMessage = String.format(
                "Successfully connected to Dr.Sum server at %s:%d, database: %s",
                host, port, database
            );
            McpSchema.TextContent content = new McpSchema.TextContent(successMessage);
            
            logger.info("Successfully processed configure_connection request");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sum connection error: {}", e.getMessage());
            return createErrorResult("Failed to connect to Dr.Sum: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid connection parameters: {}", e.getMessage());
            return createErrorResult("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing configure_connection request", e);
            return createErrorResult("Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Handles disconnect tool requests.
     */
    private static McpSchema.CallToolResult handleDisconnectRequest(
            McpSyncServerExchange exchange, 
            Map<String, Object> arguments) {
        
        try {
            logger.info("Processing disconnect request");
            
            if (!drSumConnection.isConnected()) {
                return createErrorResult("Not connected to Dr.Sum server");
            }
            
            // Disconnect
            drSumConnection.disconnect();
            
            // Create success response
            McpSchema.TextContent content = new McpSchema.TextContent(
                "Successfully disconnected from Dr.Sum server"
            );
            
            logger.info("Successfully processed disconnect request");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sum disconnection error: {}", e.getMessage());
            return createErrorResult("Failed to disconnect from Dr.Sum: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing disconnect request", e);
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
    
    // ========================================================================
    // Inner Classes - Dr.Sum Connection Management
    // ========================================================================
    
    /**
     * Dr.Sum connection configuration holder.
     * Stores connection information with password obfuscation for logging.
     */
    static class ConnectionConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;  // Sensitive information
        private final String database;
        
        public ConnectionConfig(String host, int port, String username, 
                              String password, String database) {
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be null or empty");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            if (database == null || database.trim().isEmpty()) {
                throw new IllegalArgumentException("Database cannot be null or empty");
            }
            
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password != null ? password : "";
            this.database = database;
        }
        
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDatabase() { return database; }
        
        @Override
        public String toString() {
            // Obfuscate password for logging
            return String.format("ConnectionConfig{host='%s', port=%d, username='%s', password=****, database='%s'}",
                               host, port, username, database);
        }
    }
    
    /**
     * Dr.Sum connection manager.
     * Manages connection lifecycle and provides access to DWDbiConnection.
     */
    static class DrSumConnection {
        private DWDbiConnection connection;
        private ConnectionConfig config;
        
        /**
         * Establishes connection to Dr.Sum server.
         * 
         * @param config Connection configuration
         * @throws DWException If connection fails
         */
        public void connect(ConnectionConfig config) throws DWException {
            if (config == null) {
                throw new IllegalArgumentException("ConnectionConfig cannot be null");
            }
            
            // Close existing connection if any
            if (isConnected()) {
                logger.info("Closing existing connection before establishing new one");
                disconnect();
            }
            
            logger.info("Connecting to Dr.Sum: {}", config);
            
            try {
                // Create DBI connection
                this.connection = new DWDbiConnection(
                    config.getHost(), 
                    config.getPort(), 
                    config.getUsername(), 
                    config.getPassword()
                );
                
                // Open database
                this.connection.openDatabase(config.getDatabase());
                this.config = config;
                
                logger.info("Successfully connected to Dr.Sum database: {}", config.getDatabase());
            } catch (DWException e) {
                logger.error("Failed to connect to Dr.Sum: {}", e.getMessage());
                this.connection = null;
                this.config = null;
                throw e;
            }
        }
        
        /**
         * Checks if connection is established and active.
         * 
         * @return true if connected, false otherwise
         */
        public boolean isConnected() {
            return connection != null && connection.m_hDatabase != 0;
        }
        
        /**
         * Disconnects from Dr.Sum server.
         * 
         * @throws DWException If disconnection fails
         */
        public void disconnect() throws DWException {
            if (connection != null) {
                try {
                    logger.info("Disconnecting from Dr.Sum");
                    connection.close();
                    logger.info("Successfully disconnected from Dr.Sum");
                } finally {
                    connection = null;
                    config = null;
                }
            }
        }
        
        /**
         * Gets the underlying DWDbiConnection.
         * 
         * @return DWDbiConnection instance
         * @throws IllegalStateException If not connected
         */
        public DWDbiConnection getConnection() {
            if (!isConnected()) {
                throw new IllegalStateException("Not connected to Dr.Sum. Please connect first.");
            }
            return connection;
        }
        
        /**
         * Gets the current connection configuration.
         * 
         * @return ConnectionConfig or null if not connected
         */
        public ConnectionConfig getConfig() {
            return config;
        }
    }
}