package com.example.drsum.service;

import com.example.drsum.connection.DrSumConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DrSumQueryServiceクラスのユニットテスト
 * 
 * Note: このテストは実際のDr.Sum APIに依存するため、
 * 基本的な構造とバリデーションのテストのみを行います。
 * 実際のクエリ実行テストは統合テストで実施されます。
 */
class DrSumQueryServiceTest {
    
    private DrSumConnection mockConnection;
    
    @BeforeEach
    void setUp() {
        mockConnection = new DrSumConnection();
    }
    
    // ========================================================================
    // コンストラクタのテスト
    // ========================================================================
    
    @Test
    @DisplayName("Constructor should throw exception when connection is null")
    void testConstructorWithNullConnection() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new DrSumQueryService(null);
        });
        
        assertTrue(exception.getMessage().contains("DrSumConnection cannot be null"));
    }
    
    @Test
    @DisplayName("Constructor should accept valid connection")
    void testConstructorWithValidConnection() {
        DrSumQueryService service = new DrSumQueryService(mockConnection);
        assertNotNull(service);
    }
    
    // ========================================================================
    // executeQuery()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("executeQuery() should throw exception when SQL is null")
    void testExecuteQueryWithNullSql() {
        DrSumQueryService service = new DrSumQueryService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.executeQuery(null);
        });
        
        assertTrue(exception.getMessage().contains("SQL query cannot be null or empty"));
    }
    
    @Test
    @DisplayName("executeQuery() should throw exception when SQL is empty")
    void testExecuteQueryWithEmptySql() {
        DrSumQueryService service = new DrSumQueryService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.executeQuery("");
        });
        
        assertTrue(exception.getMessage().contains("SQL query cannot be null or empty"));
    }
    
    @Test
    @DisplayName("executeQuery() should throw exception when SQL is whitespace")
    void testExecuteQueryWithWhitespaceSql() {
        DrSumQueryService service = new DrSumQueryService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.executeQuery("   ");
        });
        
        assertTrue(exception.getMessage().contains("SQL query cannot be null or empty"));
    }
    
    @Test
    @DisplayName("executeQuery() should throw exception when not connected")
    void testExecuteQueryWhenNotConnected() {
        DrSumQueryService service = new DrSumQueryService(mockConnection);
        
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.executeQuery("SELECT * FROM test");
        });
        
        assertTrue(exception.getMessage().contains("Not connected"));
    }
    
    // Note: 実際のクエリ実行テストはDr.Sumサーバーが必要なため、
    // 統合テスト（DrSumMcpServerTest）で実施されます。
}
