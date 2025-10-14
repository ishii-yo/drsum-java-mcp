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
 * Dr.Sum database analysis capabilities to AI assistants and applications.
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
            
            // Build the server first
            var server = McpServer.sync(transportProvider)
                    .serverInfo(serverInfo)
                    .capabilities(capabilities)
                    .instructions("DrSum MCP Server provides Dr.Sum database analysis capabilities. " +
                                "Use 'configure_connection' to connect to Dr.Sum, " +
                                "'get_metadata' to retrieve table information, " +
                                "'execute_query' to run SQL queries, " +
                                "and 'disconnect' to close the connection.")
                    .build();
            
            // Register tools using the builder pattern (non-deprecated API)
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createConfigureConnectionTool())
                    .callHandler(DrSumMcpServer::handleConfigureConnectionRequest)
                    .build());
            
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createDisconnectTool())
                    .callHandler(DrSumMcpServer::handleDisconnectRequest)
                    .build());
            
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createGetMetadataTool())
                    .callHandler(DrSumMcpServer::handleGetMetadataRequest)
                    .build());
            
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createExecuteQueryTool())
                    .callHandler(DrSumMcpServer::handleExecuteQueryRequest)
                    .build());
            
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
     * Creates the get_metadata tool definition.
     */
    private static McpSchema.Tool createGetMetadataTool() {
        return McpSchema.Tool.builder()
                .name("get_metadata")
                .description("Get table metadata with sample data from Dr.Sum. " +
                           "Parameters: table_name (string, required), " +
                           "sample_rows (integer, optional, default=3)")
                .build();
    }
    
    /**
     * Creates the execute_query tool definition.
     */
    private static McpSchema.Tool createExecuteQueryTool() {
        return McpSchema.Tool.builder()
                .name("execute_query")
                .description("Execute a SQL query on Dr.Sum database. " +
                           "Parameters: sql_query (string, required)")
                .build();
    }
    
    /**
     * Handles configure_connection tool requests.
     */
    private static McpSchema.CallToolResult handleConfigureConnectionRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        try {
            logger.info("Processing configure_connection request");
            
            // Extract and validate parameters from request
            Map<String, Object> arguments = request.arguments();
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
            McpSchema.CallToolRequest request) {
        
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
     * Handles get_metadata tool requests.
     */
    private static McpSchema.CallToolResult handleGetMetadataRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        try {
            logger.info("Processing get_metadata request");
            
            // Check connection
            if (!drSumConnection.isConnected()) {
                return createErrorResult("Not connected to Dr.Sum server. Please configure connection first.");
            }
            
            // Extract parameters from request
            Map<String, Object> arguments = request.arguments();
            String tableName = (String) arguments.get("table_name");
            Integer sampleRows = arguments.containsKey("sample_rows") 
                    ? ((Number) arguments.get("sample_rows")).intValue() 
                    : DEFAULT_SAMPLE_ROWS;
            
            // Validate parameters
            if (tableName == null || tableName.trim().isEmpty()) {
                return createErrorResult("table_name parameter is required");
            }
            
            // Create metadata service and retrieve metadata
            DrSumMetadataService metadataService = new DrSumMetadataService(drSumConnection);
            String metadata = metadataService.getTableMetadata(tableName, sampleRows);
            
            // Create success response
            McpSchema.TextContent content = new McpSchema.TextContent(metadata);
            
            logger.info("Successfully processed get_metadata request for table: {}", tableName);
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sum metadata retrieval error: {}", e.getMessage());
            return createErrorResult("Failed to retrieve metadata: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return createErrorResult(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing get_metadata request", e);
            return createErrorResult("Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Handles execute_query tool requests.
     */
    private static McpSchema.CallToolResult handleExecuteQueryRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        try {
            logger.info("Processing execute_query request");
            
            // Check connection
            if (!drSumConnection.isConnected()) {
                return createErrorResult("Not connected to Dr.Sum server. Please configure connection first.");
            }
            
            // Extract parameters from request
            Map<String, Object> arguments = request.arguments();
            String sqlQuery = (String) arguments.get("sql_query");
            
            // Validate parameters
            if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                return createErrorResult("sql_query parameter is required");
            }
            
            // Create query service and execute query
            DrSumQueryService queryService = new DrSumQueryService(drSumConnection);
            String results = queryService.executeQuery(sqlQuery);
            
            // Create success response
            McpSchema.TextContent content = new McpSchema.TextContent(results);
            
            logger.info("Successfully processed execute_query request");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sum query execution error: {}", e.getMessage());
            return createErrorResult("Failed to execute query: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return createErrorResult(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing execute_query request", e);
            return createErrorResult("Internal error: " + e.getMessage());
        }
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
     * Default number of sample rows to fetch with metadata.
     */
    private static final int DEFAULT_SAMPLE_ROWS = 3;
    
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
    
    // ========================================================================
    // Inner Classes - Dr.Sum Query Service
    // ========================================================================
    
    /**
     * Dr.Sum query service.
     * Executes SQL queries and returns results.
     */
    static class DrSumQueryService {
        private final DrSumConnection dsConnection;
        
        public DrSumQueryService(DrSumConnection connection) {
            if (connection == null) {
                throw new IllegalArgumentException("DrSumConnection cannot be null");
            }
            this.dsConnection = connection;
        }
        
        /**
         * Executes a SQL query.
         * 
         * @param sql SQL query string
         * @return Query results as JSON string
         * @throws DWException If query execution fails
         * @throws IllegalStateException If not connected
         */
        public String executeQuery(String sql) throws DWException {
            // Validate parameters first
            if (sql == null || sql.trim().isEmpty()) {
                throw new IllegalArgumentException("SQL query cannot be null or empty");
            }
            
            // Check connection
            if (!dsConnection.isConnected()) {
                throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
            }
            
            logger.info("Executing SQL query: {}", sql.substring(0, Math.min(sql.length(), 100)));
            
            jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
            jp.co.dw_sapporo.drsum_ea.dbi.DWDbiCursor cursor = conn.cursor();
            
            try {
                // Execute query
                cursor.execute(sql);
                
                // Get schema
                jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema = cursor.m_oDescription;
                
                // Fetch all results (be careful with large result sets)
                java.util.Vector<java.util.Vector<String>> results = cursor.fetchall();
                
                // Format as JSON
                return formatQueryResultsAsJson(schema, results);
                
            } catch (DWException e) {
                logger.error("Failed to execute query: {}", e.getMessage());
                throw e;
            } finally {
                cursor.close();
            }
        }
        
        /**
         * Formats query results as JSON.
         */
        private String formatQueryResultsAsJson(jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema,
                                               java.util.Vector<java.util.Vector<String>> results) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            
            // Format column information
            json.append("  \"columns\": [\n");
            for (int i = 0; i < schema.length; i++) {
                jp.co.dw_sapporo.drsum_ea.DWColumnInfo col = schema[i];
                json.append("    {");
                json.append("\"name\": \"").append(escapeJson(col.m_sName)).append("\", ");
                json.append("\"display_name\": \"").append(escapeJson(col.m_sDisplay)).append("\", ");
                json.append("\"type\": ").append(col.m_iType);
                json.append("}");
                if (i < schema.length - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ],\n");
            
            // Format result data
            json.append("  \"rows\": [\n");
            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    java.util.Vector<String> row = results.get(i);
                    json.append("    [");
                    for (int j = 0; j < row.size(); j++) {
                        String value = row.get(j);
                        if (value == null) {
                            json.append("null");
                        } else {
                            json.append("\"").append(escapeJson(value)).append("\"");
                        }
                        if (j < row.size() - 1) {
                            json.append(", ");
                        }
                    }
                    json.append("]");
                    if (i < results.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
            }
            json.append("  ],\n");
            json.append("  \"row_count\": ").append(results != null ? results.size() : 0).append("\n");
            json.append("}");
            
            return json.toString();
        }
        
        /**
         * Escapes special characters for JSON.
         */
        private String escapeJson(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
    }
    
    // ========================================================================
    // Inner Classes - Dr.Sum Metadata Service
    // ========================================================================
    
    /**
     * Dr.Sum metadata service.
     * Retrieves table metadata and sample data.
     */
    static class DrSumMetadataService {
        private final DrSumConnection dsConnection;
        
        public DrSumMetadataService(DrSumConnection connection) {
            if (connection == null) {
                throw new IllegalArgumentException("DrSumConnection cannot be null");
            }
            this.dsConnection = connection;
        }
        
        /**
         * Gets table metadata with sample data.
         * 
         * @param tableName Table name
         * @param sampleRows Number of sample rows to retrieve
         * @return Formatted metadata as JSON string
         * @throws DWException If metadata retrieval fails
         * @throws IllegalStateException If not connected
         */
        public String getTableMetadata(String tableName, int sampleRows) throws DWException {
            // Validate parameters first (before connection check)
            if (tableName == null || tableName.trim().isEmpty()) {
                throw new IllegalArgumentException("Table name cannot be null or empty");
            }
            
            if (sampleRows < 0) {
                throw new IllegalArgumentException("Sample rows must be non-negative");
            }
            
            // Then check connection
            if (!dsConnection.isConnected()) {
                throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
            }
            
            jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
            String dbName = conn.m_sDatabase;
            
            logger.info("Retrieving metadata for table: {} with {} sample rows", tableName, sampleRows);
            
            try {
                // Get schema information
                jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema = conn.getSchema(dbName, tableName);
                
                if (schema == null || schema.length == 0) {
                    throw new DWException("Table not found or has no columns: " + tableName);
                }
                
                // Get sample data
                java.util.Vector<java.util.Vector<String>> samples = null;
                if (sampleRows > 0) {
                    jp.co.dw_sapporo.drsum_ea.dbi.DWDbiCursor cursor = conn.cursor();
                    try {
                        // Use LIMIT clause to get sample data
                        String sql = String.format("SELECT * FROM \"%s\" LIMIT %d", tableName, sampleRows);
                        cursor.execute(sql);
                        samples = cursor.fetchmany(sampleRows);
                    } finally {
                        cursor.close();
                    }
                }
                
                // Format as JSON
                return formatMetadataAsJson(tableName, schema, samples);
                
            } catch (DWException e) {
                logger.error("Failed to retrieve metadata for table {}: {}", tableName, e.getMessage());
                throw e;
            }
        }
        
        /**
         * Formats metadata and sample data as JSON.
         */
        private String formatMetadataAsJson(String tableName, 
                                          jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema,
                                          java.util.Vector<java.util.Vector<String>> samples) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"table\": \"").append(escapeJson(tableName)).append("\",\n");
            json.append("  \"columns\": [\n");
            
            // Format columns
            for (int i = 0; i < schema.length; i++) {
                jp.co.dw_sapporo.drsum_ea.DWColumnInfo col = schema[i];
                json.append("    {\n");
                json.append("      \"name\": \"").append(escapeJson(col.m_sName)).append("\",\n");
                json.append("      \"display_name\": \"").append(escapeJson(col.m_sDisplay)).append("\",\n");
                json.append("      \"type\": ").append(col.m_iType).append(",\n");
                json.append("      \"type_name\": \"").append(getTypeName(col.m_iType)).append("\",\n");
                json.append("      \"nullable\": ").append(col.m_iNull != 0).append(",\n");
                json.append("      \"precision\": ").append(col.m_iPrecision).append(",\n");
                json.append("      \"scale\": ").append(col.m_iScale).append("\n");
                json.append("    }");
                if (i < schema.length - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("  ],\n");
            json.append("  \"sample_data\": ");
            
            // Format sample data
            if (samples != null && !samples.isEmpty()) {
                json.append("[\n");
                for (int i = 0; i < samples.size(); i++) {
                    java.util.Vector<String> row = samples.get(i);
                    json.append("    [");
                    for (int j = 0; j < row.size(); j++) {
                        String value = row.get(j);
                        if (value == null) {
                            json.append("null");
                        } else {
                            json.append("\"").append(escapeJson(value)).append("\"");
                        }
                        if (j < row.size() - 1) {
                            json.append(", ");
                        }
                    }
                    json.append("]");
                    if (i < samples.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("  ]\n");
            } else {
                json.append("[]\n");
            }
            
            json.append("}");
            return json.toString();
        }
        
        /**
         * Converts Dr.Sum type code to type name.
         */
        private String getTypeName(int typeCode) {
            switch (typeCode) {
                case 1: return "CHAR";
                case 2: return "INTEGER";
                case 3: return "DECIMAL";
                case 4: return "FLOAT";
                case 5: return "DOUBLE";
                case 6: return "DATE";
                case 7: return "TIME";
                case 8: return "TIMESTAMP";
                case 12: return "VARCHAR";
                default: return "UNKNOWN(" + typeCode + ")";
            }
        }
        
        /**
         * Escapes special characters for JSON.
         */
        private String escapeJson(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
    }
}