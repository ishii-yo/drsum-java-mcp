package com.example.drsum.service;

import com.example.drsum.connection.DrSumConnection;
import com.example.drsum.connection.ScopeDefinitions;
import jp.co.dw_sapporo.drsum_ea.DWException;
import jp.co.dw_sapporo.drsum_ea.dbi.DWDbiCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Dr.Sumメタデータサービス
 * 
 * テーブルのメタデータとサンプルデータを取得します。
 * 
 * 【責務】
 * - テーブル一覧の取得
 * - テーブルメタデータの取得
 * - サンプルデータの取得
 */
public class DrSumMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DrSumMetadataService.class);
    
    private final DrSumConnection dsConnection;
    
    /**
     * コンストラクタ
     * 
     * @param connection Dr.Sum接続
     * @throws IllegalArgumentException connectionがnullの場合
     */
    public DrSumMetadataService(DrSumConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("DrSumConnection cannot be null");
        }
        this.dsConnection = connection;
    }
    
    // ========================================================================
    // パブリックメソッド
    // ========================================================================
    
    /**
     * データベース内の全テーブルとビューのリストを取得
     * 
     * @param scopeName フィルタリング用のオプショナルなスコープ名（nullの場合は全て取得）
     * @param scopeDefinitions フィルタリング用のスコープ定義
     * @return テーブルとビュー情報を含むJSON文字列
     * @throws DWException テーブルリスト取得に失敗した場合
     * @throws IllegalStateException 接続されていない場合
     * @throws IllegalArgumentException スコープが指定されているが見つからない場合
     */
    public String getTableList(String scopeName, ScopeDefinitions scopeDefinitions) throws DWException {
        // 接続をチェック
        if (!dsConnection.isConnected()) {
            throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
        }
        
        jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
        String dbName = conn.m_sDatabase;
        
        logger.info("Retrieving table list for database: {}", dbName);
        
        // スコープを検証（指定されている場合）
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
            // テーブルリストを取得
            jp.co.dw_sapporo.drsum_ea.DWTableInfo[] tableList = conn.getTableList(dbName);
            
            if (tableList == null || tableList.length == 0) {
                logger.warn("No tables found in database: {}", dbName);
                return "{\"database\": \"" + escapeJson(dbName) + "\", \"tables\": [], \"views\": []}";
            }
            
            // テーブルとビューを分離
            List<String> tables = new ArrayList<>();
            List<String> views = new ArrayList<>();
            
            for (jp.co.dw_sapporo.drsum_ea.DWTableInfo tableInfo : tableList) {
                String tableName = tableInfo.m_sName;
                
                // スコープフィルタを適用（指定されている場合）
                if (scopeFilter != null && !isInScope(tableName, scopeFilter)) {
                    continue;  // スコープに含まれないテーブルをスキップ
                }
                
                // ビュー情報を取得してビューかどうかチェック
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
            
            // JSONとしてフォーマット
            return formatTableListAsJson(dbName, tables, views);
            
        } catch (DWException e) {
            logger.error("Failed to retrieve table list: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * データベース内の全テーブルとビューのリストを取得（スコープフィルタリングなし）
     * 後方互換性のために保持
     * 
     * @return テーブルとビュー情報を含むJSON文字列
     * @throws DWException テーブルリスト取得に失敗した場合
     * @throws IllegalStateException 接続されていない場合
     */
    public String getTableList() throws DWException {
        return getTableList(null, null);
    }
    
    /**
     * サンプルデータ付きでテーブルメタデータを取得
     * 
     * @param tableName テーブル名
     * @param sampleRows 取得するサンプル行数
     * @return JSON文字列としてフォーマットされたメタデータ
     * @throws DWException メタデータ取得に失敗した場合
     * @throws IllegalStateException 接続されていない場合
     * @throws IllegalArgumentException パラメータが不正な場合
     */
    public String getTableMetadata(String tableName, int sampleRows) throws DWException {
        // パラメータを先に検証（接続チェックの前）
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        
        if (sampleRows < 0) {
            throw new IllegalArgumentException("Sample rows must be non-negative");
        }
        
        // 次に接続をチェック
        if (!dsConnection.isConnected()) {
            throw new IllegalStateException("Not connected to Dr.Sum. Please configure connection first.");
        }
        
        jp.co.dw_sapporo.drsum_ea.dbi.DWDbiConnection conn = dsConnection.getConnection();
        String dbName = conn.m_sDatabase;
        
        logger.info("Retrieving metadata for table: {} with {} sample rows", tableName, sampleRows);
        
        try {
            // スキーマ情報を取得
            jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema = conn.getSchema(dbName, tableName);
            
            if (schema == null || schema.length == 0) {
                throw new DWException("Table not found or has no columns: " + tableName);
            }
            
            // サンプルデータを取得
            Vector<Vector<String>> samples = null;
            if (sampleRows > 0) {
                DWDbiCursor cursor = conn.cursor();
                try {
                    // サンプルデータ取得にLIMIT句を使用
                    String sql = String.format("SELECT * FROM %s LIMIT %d", tableName, sampleRows);
                    cursor.execute(sql);
                    samples = cursor.fetchmany(sampleRows);
                } finally {
                    cursor.close();
                }
            }
            
            // JSONとしてフォーマット
            return formatMetadataAsJson(tableName, schema, samples);
            
        } catch (DWException e) {
            logger.error("Failed to retrieve metadata for table {}: {}", tableName, e.getMessage());
            throw e;
        }
    }
    
    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================
    
    /**
     * テーブル名がスコープフィルタに含まれているかチェック
     * 大文字小文字を区別しない比較
     * 
     * @param tableName チェックするテーブル名
     * @param scopeFilter スコープフィルタ
     * @return スコープに含まれている場合true
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
     * テーブルリストをJSONとしてフォーマット
     * 
     * @param dbName データベース名
     * @param tables テーブル名のリスト
     * @param views ビュー名のリスト
     * @return JSON文字列
     */
    private String formatTableListAsJson(String dbName, List<String> tables, List<String> views) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"database\": \"").append(escapeJson(dbName)).append("\",\n");
        
        // テーブルをフォーマット
        json.append("  \"tables\": [\n");
        for (int i = 0; i < tables.size(); i++) {
            json.append("    \"").append(escapeJson(tables.get(i))).append("\"");
            if (i < tables.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // ビューをフォーマット
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
     * メタデータとサンプルデータをJSONとしてフォーマット
     * 
     * @param tableName テーブル名
     * @param schema カラムスキーマ
     * @param samples サンプルデータ
     * @return JSON文字列
     */
    private String formatMetadataAsJson(String tableName, 
                                      jp.co.dw_sapporo.drsum_ea.DWColumnInfo[] schema,
                                      Vector<Vector<String>> samples) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"table\": \"").append(escapeJson(tableName)).append("\",\n");
        json.append("  \"columns\": [\n");
        
        // カラムをフォーマット
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
        
        // サンプルデータをフォーマット
        if (samples != null && !samples.isEmpty()) {
            json.append("[\n");
            for (int i = 0; i < samples.size(); i++) {
                Vector<String> row = samples.get(i);
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
     * Dr.Sum型コードを型名に変換
     * 
     * @param typeCode 型コード
     * @return 型名
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
