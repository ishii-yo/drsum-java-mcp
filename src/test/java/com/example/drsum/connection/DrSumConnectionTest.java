package com.example.drsum.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DrSumConnectionクラスのユニットテスト
 * 
 * Note: このテストは実際のDr.Sum APIに依存するため、
 * 単体テストとしては構造のテストのみを行います。
 * 実際の接続テストは統合テストで実施されます。
 */
class DrSumConnectionTest {
    
    private DrSumConnection connection;
    
    @BeforeEach
    void setUp() {
        connection = new DrSumConnection();
    }
    
    // ========================================================================
    // コンストラクタのテスト
    // ========================================================================
    
    @Test
    @DisplayName("DrSumConnection should be instantiated")
    void testConstructor() {
        DrSumConnection conn = new DrSumConnection();
        assertNotNull(conn);
        assertFalse(conn.isConnected());
        assertNull(conn.getConfig());
    }
    
    // ========================================================================
    // connect()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("connect() should throw exception when config is null")
    void testConnectWithNullConfig() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            connection.connect(null);
        });
        
        assertTrue(exception.getMessage().contains("ConnectionConfig cannot be null"));
    }
    
    // ========================================================================
    // isConnected()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("isConnected() should return false initially")
    void testIsConnectedInitially() {
        assertFalse(connection.isConnected());
    }
    
    // ========================================================================
    // getConnection()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("getConnection() should throw exception when not connected")
    void testGetConnectionWhenNotConnected() {
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            connection.getConnection();
        });
        
        assertTrue(exception.getMessage().contains("Not connected"));
    }
    
    // ========================================================================
    // getConfig()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("getConfig() should return null when not connected")
    void testGetConfigWhenNotConnected() {
        assertNull(connection.getConfig());
    }
    
    // ========================================================================
    // disconnect()のテスト
    // ========================================================================
    
    @Test
    @DisplayName("disconnect() should not throw exception when not connected")
    void testDisconnectWhenNotConnected() {
        assertDoesNotThrow(() -> {
            connection.disconnect();
        });
    }
    
    // Note: 実際の接続テストはDr.Sumサーバーが必要なため、
    // 統合テスト（DrSumMcpServerTest）で実施されます。
}
