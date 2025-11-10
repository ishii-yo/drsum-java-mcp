package com.example.drsum.service;

import com.example.drsum.connection.DrSumConnection;
import com.example.drsum.connection.ScopeDefinitions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DrSumMetadataServiceクラスのユニットテスト
 * 
 * Note: このテストは実際のDr.Sum APIに依存するため、
 * 基本的な構造とバリデーションのテストのみを行います。
 * 実際のメタデータ取得テストは統合テストで実施されます。
 */
class DrSumMetadataServiceTest {
    
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
            new DrSumMetadataService(null);
        });
        
        assertTrue(exception.getMessage().contains("DrSumConnection cannot be null"));
    }
    
    @Test
    @DisplayName("Constructor should accept valid connection")
    void testConstructorWithValidConnection() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        assertNotNull(service);
    }
    
    // ========================================================================
    // getTableList()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("getTableList() should throw exception when not connected")
    void testGetTableListWhenNotConnected() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.getTableList();
        });
        
        assertTrue(exception.getMessage().contains("Not connected"));
    }
    
    @Test
    @DisplayName("getTableList() with scope should throw exception when not connected")
    void testGetTableListWithScopeWhenNotConnected() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        ScopeDefinitions scopeDefinitions = new ScopeDefinitions(new HashMap<>());
        
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.getTableList(null, scopeDefinitions);
        });
        
        assertTrue(exception.getMessage().contains("Not connected"));
    }
    
    // ========================================================================
    // getTableMetadata()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("getTableMetadata() should throw exception when table name is null")
    void testGetTableMetadataWithNullTableName() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata(null, 3);
        });
        
        assertTrue(exception.getMessage().contains("Table name cannot be null or empty"));
    }
    
    @Test
    @DisplayName("getTableMetadata() should throw exception when table name is empty")
    void testGetTableMetadataWithEmptyTableName() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("", 3);
        });
        
        assertTrue(exception.getMessage().contains("Table name cannot be null or empty"));
    }
    
    @Test
    @DisplayName("getTableMetadata() should throw exception when table name is whitespace")
    void testGetTableMetadataWithWhitespaceTableName() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("   ", 3);
        });
        
        assertTrue(exception.getMessage().contains("Table name cannot be null or empty"));
    }
    
    @Test
    @DisplayName("getTableMetadata() should throw exception when sample rows is negative")
    void testGetTableMetadataWithNegativeSampleRows() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("test_table", -1);
        });
        
        assertTrue(exception.getMessage().contains("Sample rows must be non-negative"));
    }
    
    @Test
    @DisplayName("getTableMetadata() should throw exception when not connected")
    void testGetTableMetadataWhenNotConnected() {
        DrSumMetadataService service = new DrSumMetadataService(mockConnection);
        
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.getTableMetadata("test_table", 3);
        });
        
        assertTrue(exception.getMessage().contains("Not connected"));
    }
    
    // Note: 実際のメタデータ取得テストはDr.Sumサーバーが必要なため、
    // 統合テスト（DrSumMcpServerTest）で実施されます。
}
