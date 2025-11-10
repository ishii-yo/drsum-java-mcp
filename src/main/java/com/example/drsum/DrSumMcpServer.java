package com.example.drsum;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import jp.co.dw_sapporo.drsum_ea.DWException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.drsum.connection.ConnectionConfig;
import com.example.drsum.connection.DrSumConnection;
import com.example.drsum.connection.ScopeDefinitions;
import com.example.drsum.service.DrSumQueryService;
import com.example.drsum.service.DrSumMetadataService;

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
 * 【責務】
 * - MCPサーバーの初期化と起動
 * - ツール定義（list_tables, get_metadata, execute_query）
 * - リクエストハンドリング（サービス層への委譲）
 */
public class DrSumMcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumMcpServer.class);

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
    // 定数
    // ========================================================================
    
    /**
     * デフォルトのサンプル行数
     */
    private static final int DEFAULT_SAMPLE_ROWS = 3;
    
    // ========================================================================
    // ユーティリティメソッド
    // ========================================================================
    
    /**
     * JSON用に文字列をエスケープ
     * 
     * 特殊文字をエスケープしてJSON文字列として安全に使用できるようにします。
     * 
     * @param value エスケープする文字列
     * @return エスケープされた文字列
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
}