package com.example.drsum.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * スコープ定義クラス
 * 
 * テーブル/ビューのスコープによるフィルタリングを管理します。
 * 
 * 【責務】
 * - スコープ定義の保持と検索
 * - 環境変数からのJSON設定読み込み
 * - スコープに基づくテーブルフィルタリング
 */
public class ScopeDefinitions {
    
    private static final Logger logger = LoggerFactory.getLogger(ScopeDefinitions.class);
    
    // Dr.Sumスコープ定義用の環境変数名
    private static final String ENV_DRSUM_SCOPES = "DRSUM_SCOPES";
    
    private final Map<String, List<String>> scopes;
    
    /**
     * コンストラクタ
     * 
     * @param scopes スコープ名とテーブルリストのマップ（nullの場合は空のマップとして初期化）
     */
    public ScopeDefinitions(Map<String, List<String>> scopes) {
        this.scopes = scopes != null ? scopes : new HashMap<>();
    }
    
    // ========================================================================
    // パブリックメソッド
    // ========================================================================
    
    /**
     * 指定されたスコープ名のテーブル/ビューリストを取得
     * 
     * @param scopeName スコープ名
     * @return テーブル/ビュー名のリスト、スコープが見つからない場合はnull
     */
    public List<String> getScope(String scopeName) {
        return scopes.get(scopeName);
    }
    
    /**
     * スコープが存在するかチェック
     * 
     * @param scopeName スコープ名
     * @return スコープが存在する場合true
     */
    public boolean hasScope(String scopeName) {
        return scopes.containsKey(scopeName);
    }
    
    /**
     * 全てのスコープ名を取得
     * 
     * @return スコープ名のセット
     */
    public Set<String> getScopeNames() {
        return scopes.keySet();
    }
    
    // ========================================================================
    // 静的ファクトリメソッド
    // ========================================================================
    
    /**
     * 環境変数からスコープ定義を読み込み
     * 
     * 期待されるフォーマット: スコープ名をキー、テーブル配列を値とするJSONオブジェクト
     * 例: {"bug_analysis": ["bug_reports", "error_logs"], "sales": ["orders"]}
     * 
     * 環境変数が設定されていない場合や、パースに失敗した場合は、
     * 空のScopeDefinitionsインスタンスを返します（例外は投げない）。
     * 
     * @return ScopeDefinitionsインスタンス（設定されていない場合は空）
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
    
    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================
    
    /**
     * JSONスコープ定義をパース（Jacksonを使用）
     * 
     * 期待されるフォーマット: {"scope_name": ["table1", "table2"], ...}
     * 
     * @param json パースするJSON文字列
     * @return スコープ名とテーブルリストのマップ
     * @throws Exception JSONパースに失敗した場合
     */
    private static Map<String, List<String>> parseJsonScopes(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, List<String>>> typeRef = 
            new TypeReference<Map<String, List<String>>>() {};
        return mapper.readValue(json, typeRef);
    }
}
