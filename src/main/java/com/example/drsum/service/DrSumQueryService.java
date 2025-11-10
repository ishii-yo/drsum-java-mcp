package com.example.drsum.service;

import com.example.drsum.connection.DrSumConnection;
import jp.co.dw_sapporo.drsum_ea.DWException;
import jp.co.dw_sapporo.drsum_ea.dbi.DWDbiCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

/**
 * Dr.Sumクエリサービス
 * 
 * SQLクエリを実行し、結果を返します。
 * 
 * 【責務】
 * - SQLクエリの実行
 * - クエリ結果のJSON整形
 */
public class DrSumQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumQueryService.class);
    
    private final DrSumConnection dsConnection;
    
    /**
     * コンストラクタ
     * 
     * @param connection Dr.Sum接続
     * @throws IllegalArgumentException connectionがnullの場合
     */
    public DrSumQueryService(DrSumConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("DrSumConnection cannot be null");
        }
        this.dsConnection = connection;
    }
    
    // ========================================================================
    // パブリックメソッド
    // ========================================================================
    
    /**
     * SQLクエリを実行
     * 
     * @param sql SQLクエリ文字列
     * @return JSON文字列としてのクエリ結果
     * @throws DWException クエリ実行に失敗した場合
     * @throws IllegalStateException 接続されていない場合
     * @throws IllegalArgumentException SQLがnullまたは空の場合
     */
    public String executeQuery(String sql) throws DWException {
        // パラメータを先に検証
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }
        
        // 接続をチェック
        if (!dsConnection.isConnected()) {
            throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
        }
        
        logger.info("Executing SQL query: {}", sql.substring(0, Math.min(sql.length(), 100)));
        
        jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
        DWDbiCursor cursor = conn.cursor();
        
        try {
            // クエリを実行
            cursor.execute(sql);
            
            // スキーマを取得
            jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema = cursor.m_oDescription;
            
            // 全結果を取得（大きな結果セットには注意）
            Vector<Vector<String>> results = cursor.fetchall();
            
            // JSONとしてフォーマット
            return formatQueryResultsAsJson(schema, results);
            
        } catch (DWException e) {
            logger.error("Failed to execute query: {}", e.getMessage());
            throw e;
        } finally {
            cursor.close();
        }
    }
    
    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================
    
    /**
     * クエリ結果をJSONとしてフォーマット
     * 
     * @param schema カラム情報
     * @param results 結果行
     * @return JSON文字列
     */
    private String formatQueryResultsAsJson(jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema,
                                           Vector<Vector<String>> results) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // カラム情報をフォーマット
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
        
        // 結果データをフォーマット
        json.append("  \"rows\": [\n");
        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                Vector<String> row = results.get(i);
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
