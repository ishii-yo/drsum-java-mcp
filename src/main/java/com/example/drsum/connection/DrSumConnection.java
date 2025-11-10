package com.example.drsum.connection;

import jp.co.dw_sapporo.drsum_ea.DWException;
import jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dr.Sum接続マネージャー
 * 
 * Dr.Sumへの接続ライフサイクルを管理し、DWDbiConnectionへのアクセスを提供します。
 * 
 * 【責務】
 * - Dr.Sumサーバーへの接続確立と切断
 * - 接続状態の管理
 * - DWDbiConnectionのラッパー
 */
public class DrSumConnection {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumConnection.class);
    
    private DWDbiConnection connection;
    private ConnectionConfig config;
    
    /**
     * デフォルトコンストラクタ
     */
    public DrSumConnection() {
        this.connection = null;
        this.config = null;
    }
    
    // ========================================================================
    // パブリックメソッド
    // ========================================================================
    
    /**
     * Dr.Sumサーバーへの接続を確立
     * 
     * @param config 接続設定
     * @throws DWException 接続に失敗した場合
     * @throws IllegalArgumentException configがnullの場合
     */
    public void connect(ConnectionConfig config) throws DWException {
        if (config == null) {
            throw new IllegalArgumentException("ConnectionConfig cannot be null");
        }
        
        // 既存の接続があれば閉じる
        if (isConnected()) {
            logger.info("Closing existing connection before establishing new one");
            disconnect();
        }
        
        logger.info("Connecting to Dr.Sum: {}", config);
        
        try {
            // DBI接続を作成
            this.connection = new DWDbiConnection(
                config.getHost(), 
                config.getPort(), 
                config.getUsername(), 
                config.getPassword()
            );
            
            // データベースを開く
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
     * 接続が確立されアクティブかチェック
     * 
     * @return 接続されている場合true、それ以外はfalse
     */
    public boolean isConnected() {
        return connection != null && connection.m_hDatabase != 0;
    }
    
    /**
     * Dr.Sumサーバーから切断
     * 
     * @throws DWException 切断に失敗した場合
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
     * 内部のDWDbiConnectionを取得
     * 
     * @return DWDbiConnectionインスタンス
     * @throws IllegalStateException 接続されていない場合
     */
    public DWDbiConnection getConnection() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to Dr.Sum. Please connect first.");
        }
        return connection;
    }
    
    /**
     * 現在の接続設定を取得
     * 
     * @return ConnectionConfig、接続されていない場合はnull
     */
    public ConnectionConfig getConfig() {
        return config;
    }
}
