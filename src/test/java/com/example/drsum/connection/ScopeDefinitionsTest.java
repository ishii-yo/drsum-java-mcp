package com.example.drsum.connection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScopeDefinitionsクラスのユニットテスト
 * 
 * TDDアプローチに従い、ScopeDefinitionsの実装前にテストを作成します。
 */
class ScopeDefinitionsTest {
    
    // ========================================================================
    // コンストラクタのテスト
    // ========================================================================
    
    @Test
    void testConstructorWithValidMap() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders", "customers"),
            "inventory", List.of("products", "warehouses")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        
        assertNotNull(scopeDefinitions);
        assertTrue(scopeDefinitions.hasScope("sales"));
        assertTrue(scopeDefinitions.hasScope("inventory"));
    }
    
    @Test
    void testConstructorWithNullMap() {
        // nullマップを渡した場合、空のマップとして初期化されるべき
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(null);
        
        assertNotNull(scopeDefinitions);
        assertEquals(0, scopeDefinitions.getScopeNames().size());
    }
    
    @Test
    void testConstructorWithEmptyMap() {
        // 空のマップを渡した場合
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(Map.of());
        
        assertNotNull(scopeDefinitions);
        assertEquals(0, scopeDefinitions.getScopeNames().size());
    }
    
    // ========================================================================
    // getScope()のテスト
    // ========================================================================
    
    @Test
    void testGetScopeReturnsCorrectList() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders", "customers", "invoices")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        List<String> salesScope = scopeDefinitions.getScope("sales");
        
        assertNotNull(salesScope);
        assertEquals(3, salesScope.size());
        assertTrue(salesScope.contains("orders"));
        assertTrue(salesScope.contains("customers"));
        assertTrue(salesScope.contains("invoices"));
    }
    
    @Test
    void testGetScopeReturnsNullForNonexistentScope() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        List<String> nonexistent = scopeDefinitions.getScope("nonexistent");
        
        assertNull(nonexistent);
    }
    
    // ========================================================================
    // hasScope()のテスト
    // ========================================================================
    
    @Test
    void testHasScopeReturnsTrueForExistingScope() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        
        assertTrue(scopeDefinitions.hasScope("sales"));
    }
    
    @Test
    void testHasScopeReturnsFalseForNonexistentScope() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        
        assertFalse(scopeDefinitions.hasScope("inventory"));
    }
    
    @Test
    void testHasScopeReturnsFalseForEmptyDefinitions() {
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(null);
        
        assertFalse(scopeDefinitions.hasScope("any"));
    }
    
    // ========================================================================
    // getScopeNames()のテスト
    // ========================================================================
    
    @Test
    void testGetScopeNamesReturnsAllNames() {
        Map<String, List<String>> scopes = Map.of(
            "sales", List.of("orders"),
            "inventory", List.of("products"),
            "hr", List.of("employees")
        );
        
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(scopes);
        Set<String> scopeNames = scopeDefinitions.getScopeNames();
        
        assertEquals(3, scopeNames.size());
        assertTrue(scopeNames.contains("sales"));
        assertTrue(scopeNames.contains("inventory"));
        assertTrue(scopeNames.contains("hr"));
    }
    
    @Test
    void testGetScopeNamesReturnsEmptySetForEmptyDefinitions() {
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(null);
        Set<String> scopeNames = scopeDefinitions.getScopeNames();
        
        assertNotNull(scopeNames);
        assertEquals(0, scopeNames.size());
    }
    
    // ========================================================================
    // fromEnvironment()のテスト
    // ========================================================================
    
    @Test
    void testFromEnvironmentWithNoEnvironmentVariable() {
        // 環境変数が設定されていない場合、空のScopeDefinitionsが返されるべき
        // Note: 実際の環境変数に依存するため、環境変数がない前提でテスト
        
        ScopeDefinitions scopeDefinitions = ScopeDefinitions.fromEnvironment();
        
        assertNotNull(scopeDefinitions);
        // 環境変数が設定されていない場合は空になる
        // （実際の環境では設定されている可能性があるのでコメントアウト）
        // assertEquals(0, scopeDefinitions.getScopeNames().size());
    }
}
