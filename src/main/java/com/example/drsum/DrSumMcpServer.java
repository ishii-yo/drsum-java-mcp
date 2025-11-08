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
 * DrSum MCP Server メインクラス
 * 
 * このサーバーはModel Context Protocol (MCP)を実装し、
 * AIアシスタントやアプリケーションにDr.Sumデータベース分析機能を提供します。
 * 
 * 【デモ・チュートリアル目的】
 * このコードは、MCPサーバーの基本的な実装パターンを示すデモです。
 * 単一ファイル内で以下の要素を含みます：
 * 1. MCPサーバーの初期化とツール登録
 * 2. 3つのツール定義（list_tables, get_metadata, execute_query）
 * 3. オンデマンド接続パターン（接続→処理→切断）
 * 4. Dr.Sum APIの基本的な使用方法
 */
public class DrSumMcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumMcpServer.class);
    
    // Dr.Sum接続用の環境変数名
    private static final String ENV_DRSUM_HOST = "DRSUM_HOST";
    private static final String ENV_DRSUM_PORT = "DRSUM_PORT";
    private static final String ENV_DRSUM_USERNAME = "DRSUM_USERNAME";
    private static final String ENV_DRSUM_PASSWORD = "DRSUM_PASSWORD";
    private static final String ENV_DRSUM_DATABASE = "DRSUM_DATABASE";
    private static final String ENV_DRSUM_SCOPES = "DRSUM_SCOPES";

    /**
     * メインメソッド - MCPサーバーを起動します
     * 
     * 処理の流れ:
     * 1. STDIO通信用のトランスポートを作成
     * 2. サーバーの機能（capabilities）を定義
     * 3. 3つのツールを登録
     * 4. サーバーを起動して待機
     */
    public static void main(String[] args) {
        try {
            logger.info("DrSum MCP Server を起動中...");
            
            // STDIO通信用のトランスポートプロバイダーを作成
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(McpJsonMapper.getDefault());
            
            // サーバーの機能を定義
            McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true) // ツール機能を有効化
                    .prompts(true) // プロンプト機能を有効化（空リストを返す）
                    .resources(false, false) // リソース機能を有効化（空リストを返す）
                    .build();
            
            // サーバー情報を作成
            McpSchema.Implementation serverInfo = new McpSchema.Implementation(
                    "drsum-mcp-server", 
                    "1.0.0"
            );
            
            // サーバーをビルド
            var server = McpServer.sync(transportProvider)
                    .serverInfo(serverInfo)
                    .capabilities(capabilities)
                    .instructions("DrSum MCP Server provides Dr.Sum database analysis capabilities. " +
                                "Connection information is configured via environment variables " +
                                "(DRSUM_HOST, DRSUM_PORT, DRSUM_USERNAME, DRSUM_PASSWORD, DRSUM_DATABASE). " +
                                "DRSUM_SCOPES can be set to define named scopes that group related tables. " +
                                "Use 'list_tables' to get a list of all tables and views in the database, " +
                                "use 'get_metadata' to retrieve detailed table information with sample data, " +
                                "and 'execute_query' to run SQL queries. " +
                                "Connections are established on-demand for each tool call.")
                    .build();
            
            // ツールを登録（ビルダーパターンを使用）
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
            
            // 注: プロンプトとリソースは登録されていないため、空のリストが返されます
            // これはMCPプロトコルの要件を満たしつつ、不要な機能を追加しない設計です
            
            logger.info("DrSum MCP Server が正常に起動しました");
            
            // サーバーを実行し続ける
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("DrSum MCP Server をシャットダウン中...");
                transportProvider.closeGracefully().block();
            }));
            
            // メインスレッドをブロックしてサーバーを実行し続ける
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("DrSum MCP Server の起動に失敗しました", e);
            System.exit(1);
        }
    }
    
    // ========================================================================
    // ツールスキーマ ヘルパーメソッド
    // ========================================================================
    
    /**
     * ヘルパー: 文字列型のプロパティを作成
     * ツールパラメータの定義を簡潔にします。
     */
    private static Map<String, Object> createStringProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }
    
    /**
     * ヘルパー: 整数型のプロパティを作成
     * ツールパラメータの定義を簡潔にします。
     */
    private static Map<String, Object> createIntegerProperty(String description, Integer defaultValue) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        if (defaultValue != null) {
            prop.put("default", defaultValue);
        }
        return prop;
    }
    
    /**
     * ヘルパー: プロパティと必須フィールドからJSONスキーマを作成
     * スキーマ作成ロジックを一元化します。
     */
    private static McpSchema.JsonSchema createJsonSchema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema(
                "object",
                properties,
                required != null ? required : new ArrayList<>(),
                null,
                null,
                null
        );
    }
    
    // ========================================================================
    // ツール定義
    // ========================================================================
    
    /**
     * list_tables ツールの定義を作成
     * 
     * このツールはDr.Sumデータベース内の全テーブルとビューのリストを返します。
     * 接続情報は各呼び出し時に環境変数から読み込まれます。
     */
    private static McpSchema.Tool createListTablesTool() {
        // パラメータのプロパティを定義
        Map<String, Object> properties = new HashMap<>();
        properties.put("scope", createStringProperty(
            "Optional scope name to filter tables/views. " +
            "If specified, only tables/views defined in the scope will be returned. " +
            "If omitted, all tables/views are returned."));
        
        // スキーマを作成（必須パラメータなし - すべてオプショナル）
        McpSchema.JsonSchema inputSchema = createJsonSchema(properties, null);
        
        return McpSchema.Tool.builder()
                .name("list_tables")
                .description("Get a list of all tables and views in the Dr.Sum database. " +
                           "Returns table names with type information (table or view). " +
                           "Optionally accepts a 'scope' parameter to filter results to only tables/views defined in that scope.")
                .inputSchema(inputSchema)
                .build();
    }
    
    /**
     * get_metadata ツールの定義を作成
     * 
     * このツールは指定されたテーブルのメタデータとサンプルデータを返します。
     * 接続情報は各呼び出し時に環境変数から読み込まれます。
     */
    private static McpSchema.Tool createGetMetadataTool() {
        // パラメータのプロパティを定義
        Map<String, Object> properties = new HashMap<>();
        properties.put("table_name", createStringProperty("Name of the table to get metadata for"));
        properties.put("sample_rows", createIntegerProperty("Number of sample rows to retrieve", 3));
        
        // 必須パラメータを定義
        List<String> required = new ArrayList<>();
        required.add("table_name");
        
        // スキーマを作成
        McpSchema.JsonSchema inputSchema = createJsonSchema(properties, required);
        
        return McpSchema.Tool.builder()
                .name("get_metadata")
                .description("Get table metadata with sample data from Dr.Sum. " +
                           "Parameters: table_name (string, required), " +
                           "sample_rows (integer, optional, default=3)")
                .inputSchema(inputSchema)
                .build();
    }
    
    /**
     * execute_query ツールの定義を作成
     * 
     * このツールはDr.SumデータベースでSQLクエリを実行します。
     * 接続情報は各呼び出し時に環境変数から読み込まれます。
     */
    private static McpSchema.Tool createExecuteQueryTool() {
        // パラメータのプロパティを定義
        Map<String, Object> properties = new HashMap<>();
        properties.put("sql_query", createStringProperty("SQL query to execute"));
        
        // 必須パラメータを定義
        List<String> required = new ArrayList<>();
        required.add("sql_query");
        
        // スキーマを作成
        McpSchema.JsonSchema inputSchema = createJsonSchema(properties, required);
        
        return McpSchema.Tool.builder()
                .name("execute_query")
                .description("Execute a SQL query on Dr.Sum database. " +
                           "Connection is established from environment variables. " +
                           "Parameters: sql_query (string, required)")
                .inputSchema(inputSchema)
                .build();
    }
    
    // ========================================================================
    // ツールリクエストハンドラ
    // ========================================================================
    
    /**
     * 共通処理: Dr.Sum接続を使った処理を実行
     * 
     * このメソッドは「接続→処理→切断」のパターンを共通化します。
     * 環境変数から接続情報を読み込み、接続を確立し、処理を実行し、必ず切断します。
     * 
     * @param operation 接続を使って実行する処理
     * @return ツール実行結果
     */
    private static McpSchema.CallToolResult executeWithConnection(
            java.util.function.Function<DrSumConnection, String> operation) {
        
        DrSumConnection connection = null;
        
        try {
            // 環境変数から接続設定を作成
            ConnectionConfig config = ConnectionConfig.fromEnvironment();
            connection = new DrSumConnection();
            connection.connect(config);
            
            logger.info("Dr.Sumに接続しました");
            
            // 処理を実行
            String result = operation.apply(connection);
            
            // 成功レスポンスを作成
            McpSchema.TextContent content = new McpSchema.TextContent(result);
            logger.info("処理が正常に完了しました");
            return McpSchema.CallToolResult.builder().content(List.of(content)).build();
            
        } catch (DWException e) {
            logger.error("Dr.Sumエラー: {}", e.getMessage());
            return createErrorResult("Dr.Sum処理に失敗しました: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("無効なリクエスト: {}", e.getMessage());
            return createErrorResult(e.getMessage());
        } catch (Exception e) {
            logger.error("リクエスト処理エラー", e);
            return createErrorResult("内部エラー: " + e.getMessage());
        } finally {
            // 必ず切断
            if (connection != null && connection.isConnected()) {
                try {
                    connection.disconnect();
                    logger.info("Dr.Sumから切断しました");
                } catch (DWException e) {
                    logger.error("切断エラー: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * list_tables ツールのリクエストを処理
     * 
     * オンデマンド接続パターン: 接続→テーブルリスト取得→切断
     */
    private static McpSchema.CallToolResult handleListTablesRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        logger.info("list_tables リクエストを処理中");
        
        // オプショナルなscopeパラメータを抽出
        Map<String, Object> arguments = request.arguments();
        String scopeName = arguments.containsKey("scope") 
                ? (String) arguments.get("scope") 
                : null;
        
        if (scopeName != null && !scopeName.trim().isEmpty()) {
            logger.info("スコープフィルタが指定されました: {}", scopeName);
        }
        
        // 共通接続処理を使って実行
        return executeWithConnection(connection -> {
            try {
                // 環境変数からスコープ定義を読み込み
                ScopeDefinitions scopeDefinitions = ScopeDefinitions.fromEnvironment();
                
                // メタデータサービスを作成してテーブルリストを取得
                DrSumMetadataService metadataService = new DrSumMetadataService(connection);
                return metadataService.getTableList(scopeName, scopeDefinitions);
            } catch (DWException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * get_metadata ツールのリクエストを処理
     * 
     * オンデマンド接続パターン: 接続→メタデータ取得→切断
     */
    private static McpSchema.CallToolResult handleGetMetadataRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        logger.info("get_metadata リクエストを処理中");
        
        // リクエストからパラメータを抽出
        Map<String, Object> arguments = request.arguments();
        String tableName = (String) arguments.get("table_name");
        Integer sampleRows = arguments.containsKey("sample_rows") 
                ? ((Number) arguments.get("sample_rows")).intValue() 
                : DEFAULT_SAMPLE_ROWS;
        
        // パラメータを検証
        if (tableName == null || tableName.trim().isEmpty()) {
            return createErrorResult("table_name パラメータは必須です");
        }
        
        // 共通接続処理を使って実行
        return executeWithConnection(connection -> {
            try {
                // メタデータサービスを作成してメタデータを取得
                DrSumMetadataService metadataService = new DrSumMetadataService(connection);
                return metadataService.getTableMetadata(tableName, sampleRows);
            } catch (DWException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * execute_query ツールのリクエストを処理
     * 
     * オンデマンド接続パターン: 接続→クエリ実行→切断
     */
    private static McpSchema.CallToolResult handleExecuteQueryRequest(
            McpSyncServerExchange exchange, 
            McpSchema.CallToolRequest request) {
        
        logger.info("execute_query リクエストを処理中");
        
        // リクエストからパラメータを抽出
        Map<String, Object> arguments = request.arguments();
        String sqlQuery = (String) arguments.get("sql_query");
        
        // パラメータを検証
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return createErrorResult("sql_query パラメータは必須です");
        }
        
        // 共通接続処理を使って実行
        return executeWithConnection(connection -> {
            try {
                // クエリサービスを作成してクエリを実行
                DrSumQueryService queryService = new DrSumQueryService(connection);
                return queryService.executeQuery(sqlQuery);
            } catch (DWException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * エラー結果を作成
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
    // ユーティリティメソッド
    // ========================================================================
    
    /**
     * JSON用に文字列をエスケープ
     * 
     * 特殊文字をエスケープしてJSON文字列として安全に使用できるようにします。
     * DrSumQueryServiceとDrSumMetadataServiceで共通利用されます。
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    // ========================================================================
    // 内部クラス - Dr.Sum接続管理
    // ========================================================================
    
    /**
     * デフォルトのサンプル行数
     */
    private static final int DEFAULT_SAMPLE_ROWS = 3;
    
    /**
     * Dr.Sum接続設定クラス
     * 
     * 接続情報を保持します。パスワードはログ出力時に伏せ字化されます。
     * 
     * 【デモ・チュートリアルポイント】
     * - 環境変数から設定を読み込むパターンの実装例
     * - セキュリティ: パスワードの伏せ字化
     * - バリデーション: 必須パラメータのチェック
     */
    static class ConnectionConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;  // 機密情報
        private final String database;
        
        public ConnectionConfig(String host, int port, String username, 
                              String password, String database) {
            // パラメータのバリデーション
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("ホストはnullまたは空にできません");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("ポートは1から65535の間である必要があります");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("ユーザー名はnullまたは空にできません");
            }
            if (database == null || database.trim().isEmpty()) {
                throw new IllegalArgumentException("データベース名はnullまたは空にできません");
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
            // ログ出力用にパスワードを伏せ字化
            return String.format("ConnectionConfig{host='%s', port=%d, username='%s', password=****, database='%s'}",
                               host, port, username, database);
        }
        
        /**
         * 環境変数からConnectionConfigを作成
         * 
         * @return 環境変数から作成されたConnectionConfig
         * @throws IllegalStateException 必須の環境変数が設定されていない場合
         */
        public static ConnectionConfig fromEnvironment() {
            logger.info("環境変数からDr.Sum接続情報を読み込み中");
            
            // 環境変数を読み込み
            String host = System.getenv(ENV_DRSUM_HOST);
            String portStr = System.getenv(ENV_DRSUM_PORT);
            String username = System.getenv(ENV_DRSUM_USERNAME);
            String password = System.getenv(ENV_DRSUM_PASSWORD);
            String database = System.getenv(ENV_DRSUM_DATABASE);
            
            // 必須の環境変数を検証
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalStateException(
                    "環境変数 " + ENV_DRSUM_HOST + " が設定されていません。" +
                    "MCP設定でDr.Sumの接続情報を設定してください。");
            }
            if (portStr == null || portStr.trim().isEmpty()) {
                throw new IllegalStateException(
                    "環境変数 " + ENV_DRSUM_PORT + " が設定されていません。" +
                    "MCP設定でDr.Sumの接続情報を設定してください。");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalStateException(
                    "環境変数 " + ENV_DRSUM_USERNAME + " が設定されていません。" +
                    "MCP設定でDr.Sumの接続情報を設定してください。");
            }
            if (database == null || database.trim().isEmpty()) {
                throw new IllegalStateException(
                    "環境変数 " + ENV_DRSUM_DATABASE + " が設定されていません。" +
                    "MCP設定でDr.Sumの接続情報を設定してください。");
            }
            
            // ポート番号をパース
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "環境変数 " + ENV_DRSUM_PORT + " は有効な整数である必要があります。取得値: " + portStr);
            }
            
            // パスワードは空でも可
            if (password == null) {
                password = "";
            }
            
            logger.info("環境変数からの接続情報読み込み成功 (host={}, port={}, username={}, database={})",
                       host, port, username, database);
            
            return new ConnectionConfig(host, port, username, password, database);
        }
    }
    
    /**
     * スコープ定義クラス
     * 
     * テーブル/ビューのスコープによるフィルタリングを管理します。
     * 
     * 【デモ・チュートリアルポイント】
     * - JSON設定の読み込みパターン
     * - オプショナル機能の実装方法（スコープが未設定でも動作）
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
                json.append("\"name\": \"").append(DrSumMcpServer.escapeJson(col.m_sName)).append("\", ");
                json.append("\"display_name\": \"").append(DrSumMcpServer.escapeJson(col.m_sDisplay)).append("\", ");
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
                            json.append("\"").append(DrSumMcpServer.escapeJson(value)).append("\"");
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
    }
    
    // ========================================================================
    // 内部クラス - Dr.Sumメタデータサービス
    // ========================================================================
    
    /**
     * Dr.Sumメタデータサービス
     * 
     * テーブルのメタデータとサンプルデータを取得します。
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
                    return "{\"database\": \"" + DrSumMcpServer.escapeJson(dbName) + "\", \"tables\": [], \"views\": []}";
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
            json.append("  \"database\": \"").append(DrSumMcpServer.escapeJson(dbName)).append("\",\n");
            
            // Format tables
            json.append("  \"tables\": [\n");
            for (int i = 0; i < tables.size(); i++) {
                json.append("    \"").append(DrSumMcpServer.escapeJson(tables.get(i))).append("\"");
                if (i < tables.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ],\n");
            
            // Format views
            json.append("  \"views\": [\n");
            for (int i = 0; i < views.size(); i++) {
                json.append("    \"").append(DrSumMcpServer.escapeJson(views.get(i))).append("\"");
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
            json.append("  \"table\": \"").append(DrSumMcpServer.escapeJson(tableName)).append("\",\n");
            json.append("  \"columns\": [\n");
            
            // Format columns
            for (int i = 0; i < schema.length; i++) {
                jp.co.dw_sapporo.drsum_ea.DWColumnInfo col = schema[i];
                json.append("    {\n");
                json.append("      \"name\": \"").append(DrSumMcpServer.escapeJson(col.m_sName)).append("\",\n");
                json.append("      \"display_name\": \"").append(DrSumMcpServer.escapeJson(col.m_sDisplay)).append("\",\n");
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
                            json.append("\"").append(DrSumMcpServer.escapeJson(value)).append("\"");
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
    }
}