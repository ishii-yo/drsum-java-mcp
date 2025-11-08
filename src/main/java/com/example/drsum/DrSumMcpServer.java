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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
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
    
    // Environment variable names for Dr.Sum connection
    private static final String ENV_DRSUM_HOST = "DRSUM_HOST";
    private static final String ENV_DRSUM_PORT = "DRSUM_PORT";
    private static final String ENV_DRSUM_USERNAME = "DRSUM_USERNAME";
    private static final String ENV_DRSUM_PASSWORD = "DRSUM_PASSWORD";
    private static final String ENV_DRSUM_DATABASE = "DRSUM_DATABASE";
    private static final String ENV_DRSUM_SCOPES = "DRSUM_SCOPES";

    public static void main(String[] args) {
        try {
            logger.info("Starting DrSum MCP Server...");
            
            // Create STDIO transport provider for communication
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(McpJsonMapper.getDefault());
            
            // Define server capabilities
            McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true) // Enable tools
                    .prompts(true) // Enable prompts (will return empty list)
                    .resources(false, false) // Enable resources (will return empty list)
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
                                "Connection information is configured via environment variables " +
                                "(DRSUM_HOST, DRSUM_PORT, DRSUM_USERNAME, DRSUM_PASSWORD, DRSUM_DATABASE). " +
                                "Use 'list_tables' to get a list of all tables and views in the database, " +
                                "use 'get_metadata' to retrieve detailed table information with sample data, " +
                                "and 'execute_query' to run SQL queries. " +
                                "Connections are established on-demand for each tool call.")
                    .build();
            
            // Register tools using the builder pattern (non-deprecated API)
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createListTablesTool())
                    .callHandler(DrSumMcpServer::handleListTablesRequest)
                    .build());
            
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createGetMetadataTool())
                    .callHandler(DrSumMcpServer::handleGetMetadataRequest)
                    .build());
            
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createExecuteQueryTool())
                    .callHandler(DrSumMcpServer::handleExecuteQueryRequest)
                    .build());
            
            // Note: No prompts or resources are registered, so prompts/list and resources/list will return empty lists
            // This satisfies the MCP protocol requirement without adding unnecessary functionality
            
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
     * Creates the list_tables tool definition.
     * Connection information is read from environment variables on each call.
     */
    private static McpSchema.Tool createListTablesTool() {
        // propertiesを定義
        Map<String, Object> properties = new HashMap<>();
        
        // scopeプロパティ（オプショナル）
        Map<String, Object> scopeProp = new HashMap<>();
        scopeProp.put("type", "string");
        scopeProp.put("description", "Optional scope name to filter tables/views. " +
                     "If specified, only tables/views defined in the scope will be returned. " +
                     "If omitted, all tables/views are returned.");
        properties.put("scope", scopeProp);
        
        // inputSchemaを作成（requiredは空 - すべてオプショナル）
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                properties,
                new ArrayList<>(),  // no required parameters
                null,
                null,
                null
        );
        
        return McpSchema.Tool.builder()
                .name("list_tables")
                .description("Get a list of all tables and views in the Dr.Sum database. " +
                           "No parameters required. Returns table names with type information (table or view). " +
                           "Optionally accepts a 'scope' parameter to filter results.")
                .inputSchema(inputSchema)
                .build();
    }
    
    /**
     * Creates the get_metadata tool definition.
     * Connection information is read from environment variables on each call.
     */
    private static McpSchema.Tool createGetMetadataTool() {
        // propertiesを定義
        Map<String, Object> properties = new HashMap<>();
        
        // table_nameプロパティ
        Map<String, Object> tableNameProp = new HashMap<>();
        tableNameProp.put("type", "string");
        tableNameProp.put("description", "Name of the table to get metadata for");
        properties.put("table_name", tableNameProp);
        
        // sample_rowsプロパティ
        Map<String, Object> sampleRowsProp = new HashMap<>();
        sampleRowsProp.put("type", "integer");
        sampleRowsProp.put("description", "Number of sample rows to retrieve");
        sampleRowsProp.put("default", 3);
        properties.put("sample_rows", sampleRowsProp);
        
        // 必須パラメータ
        List<String> required = new ArrayList<>();
        required.add("table_name");
        
        // inputSchemaを作成
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                properties,
                required,
                null,
                null,
                null
        );
        
        return McpSchema.Tool.builder()
                .name("get_metadata")
                .description("Get table metadata with sample data from Dr.Sum. " +
                           "Parameters: table_name (string, required), " +
                           "sample_rows (integer, optional, default=3)")
                .inputSchema(inputSchema)
                .build();
    }
    
    /**
     * Creates the execute_query tool definition.
     * Connection information is read from environment variables on each call.
     */
    private static McpSchema.Tool createExecuteQueryTool() {
        // propertiesを定義
        Map<String, Object> properties = new HashMap<>();
        
        // sql_queryプロパティ
        Map<String, Object> sqlQueryProp = new HashMap<>();
        sqlQueryProp.put("type", "string");
        sqlQueryProp.put("description", "SQL query to execute");
        properties.put("sql_query", sqlQueryProp);
        
        // 必須パラメータ
        List<String> required = new ArrayList<>();
        required.add("sql_query");
        
        // inputSchemaを作成
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                properties,
                required,
                null,
                null,
                null
        );
        
        return McpSchema.Tool.builder()
                .name("execute_query")
                .description("Execute a SQL query on Dr.Sum database. " +
                           "Connection is established from environment variables. " +
                           "Parameters: sql_query (string, required)")
                .inputSchema(inputSchema)
                .build();
    }
    
    /**
     * Handles list_tables tool requests.
     * Uses on-demand connection pattern: connects, retrieves table list, disconnects.
     */
    private static McpSchema.CallToolResult handleListTablesRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        DrSumConnection connection = null;
        
        try {
            logger.info("Processing list_tables request");
            
            // Extract optional scope parameter
            Map<String, Object> arguments = request.arguments();
            String scopeName = arguments.containsKey("scope") 
                    ? (String) arguments.get("scope") 
                    : null;
            
            if (scopeName != null && !scopeName.trim().isEmpty()) {
                logger.info("Scope filter requested: {}", scopeName);
            }
            
            // Create connection from environment variables
            ConnectionConfig config = ConnectionConfig.fromEnvironment();
            connection = new DrSumConnection();
            connection.connect(config);
            
            logger.info("Connected to Dr.Sum for table list retrieval");
            
            // Load scope definitions from environment
            ScopeDefinitions scopeDefinitions = ScopeDefinitions.fromEnvironment();
            
            // Create metadata service and retrieve table list
            DrSumMetadataService metadataService = new DrSumMetadataService(connection);
            String tableList = metadataService.getTableList(scopeName, scopeDefinitions);
            
            // Create success response
            McpSchema.TextContent content = new McpSchema.TextContent(tableList);
            
            logger.info("Successfully processed list_tables request");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sum table list retrieval error: {}", e.getMessage());
            return createErrorResult("Failed to retrieve table list: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Invalid request: {}", e.getMessage());
            return createErrorResult(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing list_tables request", e);
            return createErrorResult("Internal error: " + e.getMessage());
        } finally {
            // Always disconnect
            if (connection != null && connection.isConnected()) {
                try {
                    connection.disconnect();
                    logger.info("Disconnected from Dr.Sum after table list retrieval");
                } catch (DWException e) {
                    logger.error("Error disconnecting from Dr.Sum: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handles get_metadata tool requests.
     * Uses on-demand connection pattern: connects, retrieves metadata, disconnects.
     */
    private static McpSchema.CallToolResult handleGetMetadataRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        DrSumConnection connection = null;
        
        try {
            logger.info("Processing get_metadata request");
            
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
            
            // Create connection from environment variables
            ConnectionConfig config = ConnectionConfig.fromEnvironment();
            connection = new DrSumConnection();
            connection.connect(config);
            
            logger.info("Connected to Dr.Sum for metadata retrieval");
            
            // Create metadata service and retrieve metadata
            DrSumMetadataService metadataService = new DrSumMetadataService(connection);
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
        } finally {
            // Always disconnect
            if (connection != null && connection.isConnected()) {
                try {
                    connection.disconnect();
                    logger.info("Disconnected from Dr.Sum after metadata retrieval");
                } catch (DWException e) {
                    logger.error("Error disconnecting from Dr.Sum: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handles execute_query tool requests.
     * Uses on-demand connection pattern: connects, executes query, disconnects.
     */
    private static McpSchema.CallToolResult handleExecuteQueryRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        DrSumConnection connection = null;
        
        try {
            logger.info("Processing execute_query request");
            
            // Extract parameters from request
            Map<String, Object> arguments = request.arguments();
            String sqlQuery = (String) arguments.get("sql_query");
            
            // Validate parameters
            if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                return createErrorResult("sql_query parameter is required");
            }
            
            // Create connection from environment variables
            ConnectionConfig config = ConnectionConfig.fromEnvironment();
            connection = new DrSumConnection();
            connection.connect(config);
            
            logger.info("Connected to Dr.Sum for query execution");
            
            // Create query service and execute query
            DrSumQueryService queryService = new DrSumQueryService(connection);
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
        } finally {
            // Always disconnect
            if (connection != null && connection.isConnected()) {
                try {
                    connection.disconnect();
                    logger.info("Disconnected from Dr.Sum after query execution");
                } catch (DWException e) {
                    logger.error("Error disconnecting from Dr.Sum: {}", e.getMessage());
                }
            }
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
        
        /**
         * Creates ConnectionConfig from environment variables.
         * 
         * @return ConnectionConfig created from environment variables
         * @throws IllegalStateException if required environment variables are not set
         */
        public static ConnectionConfig fromEnvironment() {
            logger.info("Reading Dr.Sum connection information from environment variables");
            
            // Read environment variables
            String host = System.getenv(ENV_DRSUM_HOST);
            String portStr = System.getenv(ENV_DRSUM_PORT);
            String username = System.getenv(ENV_DRSUM_USERNAME);
            String password = System.getenv(ENV_DRSUM_PASSWORD);
            String database = System.getenv(ENV_DRSUM_DATABASE);
            
            // Validate required environment variables
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Environment variable " + ENV_DRSUM_HOST + " is not set. " +
                    "Please configure Dr.Sum connection information in MCP settings.");
            }
            if (portStr == null || portStr.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Environment variable " + ENV_DRSUM_PORT + " is not set. " +
                    "Please configure Dr.Sum connection information in MCP settings.");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Environment variable " + ENV_DRSUM_USERNAME + " is not set. " +
                    "Please configure Dr.Sum connection information in MCP settings.");
            }
            if (database == null || database.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Environment variable " + ENV_DRSUM_DATABASE + " is not set. " +
                    "Please configure Dr.Sum connection information in MCP settings.");
            }
            
            // Parse port number
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "Environment variable " + ENV_DRSUM_PORT + " must be a valid integer. Got: " + portStr);
            }
            
            // Password can be empty
            if (password == null) {
                password = "";
            }
            
            logger.info("Successfully read connection information from environment (host={}, port={}, username={}, database={})",
                       host, port, username, database);
            
            return new ConnectionConfig(host, port, username, password, database);
        }
    }
    
    /**
     * Scope definition holder.
     * Manages table/view scopes for filtering.
     */
    static class ScopeDefinitions {
        private final Map<String, List<String>> scopes;
        
        public ScopeDefinitions(Map<String, List<String>> scopes) {
            this.scopes = scopes != null ? scopes : new HashMap<>();
        }
        
        /**
         * Gets the list of tables/views for a given scope name.
         * 
         * @param scopeName Scope name
         * @return List of table/view names, or null if scope not found
         */
        public List<String> getScope(String scopeName) {
            return scopes.get(scopeName);
        }
        
        /**
         * Checks if a scope exists.
         * 
         * @param scopeName Scope name
         * @return true if scope exists
         */
        public boolean hasScope(String scopeName) {
            return scopes.containsKey(scopeName);
        }
        
        /**
         * Gets all scope names.
         * 
         * @return Set of scope names
         */
        public java.util.Set<String> getScopeNames() {
            return scopes.keySet();
        }
        
        /**
         * Reads scope definitions from environment variable.
         * Expected format: JSON object with scope names as keys and table arrays as values
         * Example: {"bug_analysis": ["bug_reports", "error_logs"], "sales": ["orders"]}
         * 
         * @return ScopeDefinitions instance (empty if not configured)
         */
        public static ScopeDefinitions fromEnvironment() {
            String scopesJson = System.getenv(ENV_DRSUM_SCOPES);
            
            if (scopesJson == null || scopesJson.trim().isEmpty()) {
                logger.info("DRSUM_SCOPES environment variable not set - scope filtering disabled");
                return new ScopeDefinitions(new HashMap<>());
            }
            
            try {
                Map<String, List<String>> scopes = parseJsonScopes(scopesJson);
                logger.info("Loaded {} scope(s) from environment: {}", scopes.size(), scopes.keySet());
                return new ScopeDefinitions(scopes);
            } catch (Exception e) {
                logger.error("Failed to parse DRSUM_SCOPES: {}. Scope filtering disabled.", e.getMessage());
                return new ScopeDefinitions(new HashMap<>());
            }
        }
        
        /**
         * Parses JSON scope definitions using Jackson.
         * Expected format: {"scope_name": ["table1", "table2"], ...}
         * 
         * @param json JSON string to parse
         * @return Map of scope names to table lists
         * @throws Exception If JSON parsing fails
         */
        private static Map<String, List<String>> parseJsonScopes(String json) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, List<String>>> typeRef = 
                new TypeReference<Map<String, List<String>>>() {};
            return mapper.readValue(json, typeRef);
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
         * Gets list of all tables and views in the database.
         * 
         * @param scopeName Optional scope name to filter results (can be null)
         * @param scopeDefinitions Scope definitions for filtering
         * @return JSON string containing table and view information
         * @throws DWException If table list retrieval fails
         * @throws IllegalStateException If not connected
         * @throws IllegalArgumentException If scope is specified but not found
         */
        public String getTableList(String scopeName, ScopeDefinitions scopeDefinitions) throws DWException {
            // Check connection
            if (!dsConnection.isConnected()) {
                throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
            }
            
            jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
            String dbName = conn.m_sDatabase;
            
            logger.info("Retrieving table list for database: {}", dbName);
            
            // Validate scope if specified
            List<String> scopeFilter = null;
            if (scopeName != null && !scopeName.trim().isEmpty()) {
                if (scopeDefinitions == null || !scopeDefinitions.hasScope(scopeName)) {
                    throw new IllegalArgumentException(
                        "Scope '" + scopeName + "' not found. Available scopes: " + 
                        (scopeDefinitions != null ? scopeDefinitions.getScopeNames() : "[]"));
                }
                scopeFilter = scopeDefinitions.getScope(scopeName);
                logger.info("Applying scope filter '{}' with {} table(s)", scopeName, scopeFilter.size());
            }
            
            try {
                // Get table list
                jp.co.dw_sapporo.drsum_ea.DWTableInfo[] tableList = conn.getTableList(dbName);
                
                if (tableList == null || tableList.length == 0) {
                    logger.warn("No tables found in database: {}", dbName);
                    return "{\"database\": \"" + escapeJson(dbName) + "\", \"tables\": [], \"views\": []}";
                }
                
                // Separate tables and views
                java.util.List<String> tables = new java.util.ArrayList<>();
                java.util.List<String> views = new java.util.ArrayList<>();
                
                for (jp.co.dw_sapporo.drsum_ea.DWTableInfo tableInfo : tableList) {
                    String tableName = tableInfo.m_sName;
                    
                    // Apply scope filter if specified
                    if (scopeFilter != null && !isInScope(tableName, scopeFilter)) {
                        continue;  // Skip tables not in scope
                    }
                    
                    // Check if it's a view by getting view info
                    jp.co.dw_sapporo.drsum_ea.DWViewInfo viewInfo = conn.getViewInfo(dbName, tableName);
                    
                    if (viewInfo != null && viewInfo.m_iType != 0) {
                        views.add(tableName);
                    } else {
                        tables.add(tableName);
                    }
                }
                
                logger.info("Found {} tables and {} views{}", 
                           tables.size(), views.size(),
                           scopeName != null ? " (filtered by scope: " + scopeName + ")" : "");
                
                // Format as JSON
                return formatTableListAsJson(dbName, tables, views);
                
            } catch (DWException e) {
                logger.error("Failed to retrieve table list: {}", e.getMessage());
                throw e;
            }
        }
        
        /**
         * Checks if a table name is in the scope filter.
         * Case-insensitive comparison.
         */
        private boolean isInScope(String tableName, List<String> scopeFilter) {
            for (String scopeTable : scopeFilter) {
                if (scopeTable.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Gets list of all tables and views in the database (without scope filtering).
         * Kept for backward compatibility.
         * 
         * @return JSON string containing table and view information
         * @throws DWException If table list retrieval fails
         * @throws IllegalStateException If not connected
         */
        public String getTableList() throws DWException {
            return getTableList(null, null);
        }
        
        /**
         * Formats table list as JSON.
         */
        private String formatTableListAsJson(String dbName, 
                                            java.util.List<String> tables, 
                                            java.util.List<String> views) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"database\": \"").append(escapeJson(dbName)).append("\",\n");
            
            // Format tables
            json.append("  \"tables\": [\n");
            for (int i = 0; i < tables.size(); i++) {
                json.append("    \"").append(escapeJson(tables.get(i))).append("\"");
                if (i < tables.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ],\n");
            
            // Format views
            json.append("  \"views\": [\n");
            for (int i = 0; i < views.size(); i++) {
                json.append("    \"").append(escapeJson(views.get(i))).append("\"");
                if (i < views.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ],\n");
            
            json.append("  \"total_count\": ").append(tables.size() + views.size()).append("\n");
            json.append("}");
            
            return json.toString();
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
                        String sql = String.format("SELECT * FROM %s LIMIT %d", tableName, sampleRows);
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
                json.append("      \"unique\": ").append(col.m_iUnique != 0).append(",\n");
                json.append("      \"nullable\": ").append(col.m_iNull == 0).append(",\n");
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
                case 0: return "VARCHAR";
                case 1: return "INTEGER";
                case 2: return "REAL";
                case 3: return "DATE";
                case 4: return "TIME";
                case 5: return "TIMESTAMP";
                case 6: return "OBJECT";
                case 7: return "NUMERIC";
                case 12: return "INTERVAL";
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