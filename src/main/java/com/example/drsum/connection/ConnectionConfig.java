package com.example.drsum.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dr.Sum接続設定クラス
 * 
 * 接続情報を保持します。パスワードはログ出力時に伏せ字化されます。
 * 
 * 【責務】
 * - 接続パラメータの保持とバリデーション
 * - 環境変数からの設定読み込み
 * - セキュリティ: パスワードの伏せ字化
 */
public class ConnectionConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionConfig.class);
    
    // Dr.Sum接続用の環境変数名
    private static final String ENV_DRSUM_HOST = "DRSUM_HOST";
    private static final String ENV_DRSUM_PORT = "DRSUM_PORT";
    private static final String ENV_DRSUM_USERNAME = "DRSUM_USERNAME";
    private static final String ENV_DRSUM_PASSWORD = "DRSUM_PASSWORD";
    private static final String ENV_DRSUM_DATABASE = "DRSUM_DATABASE";
    
    private final String host;
    private final int port;
    private final String username;
    private final String password;  // 機密情報
    private final String database;
    
    /**
     * コンストラクタ
     * 
     * @param host Dr.Sumサーバーのホスト名
     * @param port Dr.Sumサーバーのポート番号
     * @param username ユーザー名
     * @param password パスワード（nullの場合は空文字列に変換）
     * @param database データベース名
     * @throws IllegalArgumentException パラメータが不正な場合
     */
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
    
    // ========================================================================
    // Getter メソッド
    // ========================================================================
    
    public String getHost() { 
        return host; 
    }
    
    public int getPort() { 
        return port; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public String getPassword() { 
        return password; 
    }
    
    public String getDatabase() { 
        return database; 
    }
    
    // ========================================================================
    // Object メソッドのオーバーライド
    // ========================================================================
    
    /**
     * 文字列表現を返す
     * ログ出力用にパスワードを伏せ字化します。
     * 
     * @return 接続情報の文字列表現
     */
    @Override
    public String toString() {
        // ログ出力用にパスワードを伏せ字化
        return String.format("ConnectionConfig{host='%s', port=%d, username='%s', password=****, database='%s'}",
                           host, port, username, database);
    }
    
    // ========================================================================
    // 静的ファクトリメソッド
    // ========================================================================
    
    /**
     * 環境変数からConnectionConfigを作成
     * 
     * 以下の環境変数を読み込みます：
     * - DRSUM_HOST: Dr.Sumサーバーのホスト名（必須）
     * - DRSUM_PORT: Dr.Sumサーバーのポート番号（必須）
     * - DRSUM_USERNAME: ユーザー名（必須）
     * - DRSUM_PASSWORD: パスワード（省略可、空文字列として扱われる）
     * - DRSUM_DATABASE: データベース名（必須）
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
