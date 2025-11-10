package com.example.drsum.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConnectionConfigクラスのユニットテスト
 * 
 * TDDアプローチに従い、ConnectionConfigの実装前にテストを作成します。
 */
class ConnectionConfigTest {
    
    // テスト用の環境変数名
    private static final String ENV_DRSUM_HOST = "DRSUM_HOST";
    private static final String ENV_DRSUM_PORT = "DRSUM_PORT";
    private static final String ENV_DRSUM_USERNAME = "DRSUM_USERNAME";
    private static final String ENV_DRSUM_PASSWORD = "DRSUM_PASSWORD";
    private static final String ENV_DRSUM_DATABASE = "DRSUM_DATABASE";
    
    // 環境変数の元の値を保存（テスト後に復元するため）
    private String originalHost;
    private String originalPort;
    private String originalUsername;
    private String originalPassword;
    private String originalDatabase;
    
    @BeforeEach
    void setUp() {
        // 元の環境変数を保存
        originalHost = System.getenv(ENV_DRSUM_HOST);
        originalPort = System.getenv(ENV_DRSUM_PORT);
        originalUsername = System.getenv(ENV_DRSUM_USERNAME);
        originalPassword = System.getenv(ENV_DRSUM_PASSWORD);
        originalDatabase = System.getenv(ENV_DRSUM_DATABASE);
    }
    
    @AfterEach
    void tearDown() {
        // 環境変数を復元
        // Note: System.getenv()は読み取り専用なので、
        // 実際には環境変数を変更するテストはシステムプロパティを使うなど
        // 別のアプローチが必要になる場合があります
    }
    
    // ========================================================================
    // コンストラクタのテスト
    // ========================================================================
    
    @Test
    void testConstructorWithValidParameters() {
        // 正常なパラメータでインスタンスを作成
        ConnectionConfig config = new ConnectionConfig(
            "localhost",
            8400,
            "testuser",
            "testpass",
            "testdb"
        );
        
        assertEquals("localhost", config.getHost());
        assertEquals(8400, config.getPort());
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
        assertEquals("testdb", config.getDatabase());
    }
    
    @Test
    void testConstructorWithNullPassword() {
        // パスワードがnullの場合、空文字列に変換されるべき
        ConnectionConfig config = new ConnectionConfig(
            "localhost",
            8400,
            "testuser",
            null,
            "testdb"
        );
        
        assertEquals("", config.getPassword());
    }
    
    @Test
    void testConstructorWithEmptyPassword() {
        // 空のパスワードは許可されるべき
        ConnectionConfig config = new ConnectionConfig(
            "localhost",
            8400,
            "testuser",
            "",
            "testdb"
        );
        
        assertEquals("", config.getPassword());
    }
    
    // ========================================================================
    // バリデーションのテスト
    // ========================================================================
    
    @Test
    void testConstructorThrowsExceptionForNullHost() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(null, 8400, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ホスト"));
    }
    
    @Test
    void testConstructorThrowsExceptionForEmptyHost() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("", 8400, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ホスト"));
    }
    
    @Test
    void testConstructorThrowsExceptionForWhitespaceHost() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("   ", 8400, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ホスト"));
    }
    
    @Test
    void testConstructorThrowsExceptionForInvalidPortNegative() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", -1, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ポート"));
    }
    
    @Test
    void testConstructorThrowsExceptionForInvalidPortZero() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 0, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ポート"));
    }
    
    @Test
    void testConstructorThrowsExceptionForInvalidPortTooLarge() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 65536, "testuser", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ポート"));
    }
    
    @Test
    void testConstructorThrowsExceptionForNullUsername() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 8400, null, "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ユーザー名"));
    }
    
    @Test
    void testConstructorThrowsExceptionForEmptyUsername() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 8400, "", "testpass", "testdb");
        });
        assertTrue(exception.getMessage().contains("ユーザー名"));
    }
    
    @Test
    void testConstructorThrowsExceptionForNullDatabase() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 8400, "testuser", "testpass", null);
        });
        assertTrue(exception.getMessage().contains("データベース"));
    }
    
    @Test
    void testConstructorThrowsExceptionForEmptyDatabase() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 8400, "testuser", "testpass", "");
        });
        assertTrue(exception.getMessage().contains("データベース"));
    }
    
    // ========================================================================
    // toString()のテスト（パスワード伏せ字化）
    // ========================================================================
    
    @Test
    void testToStringMasksPassword() {
        ConnectionConfig config = new ConnectionConfig(
            "localhost",
            8400,
            "testuser",
            "secretpassword",
            "testdb"
        );
        
        String toString = config.toString();
        
        // パスワードが伏せ字になっていることを確認
        assertTrue(toString.contains("****"));
        assertFalse(toString.contains("secretpassword"));
        
        // 他の情報は含まれていることを確認
        assertTrue(toString.contains("localhost"));
        assertTrue(toString.contains("8400"));
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("testdb"));
    }
    
    // ========================================================================
    // fromEnvironment()のテスト
    // ========================================================================
    // Note: 環境変数を動的に設定するのは難しいため、
    // 実際の環境でテストするか、モックを使用する必要があります。
    // ここでは基本的な構造のみを示します。
    
    @Test
    void testFromEnvironmentWithMissingHost() {
        // 環境変数が設定されていない場合のテスト
        // 実際の実装では、System.getenv()をモック化するか、
        // 環境変数を設定するテストユーティリティが必要
        
        // このテストは環境変数が設定されていない環境でのみ有効
        // 実際のCI/CD環境では別のアプローチが必要
        
        // 一時的にスキップ（実装時にモック化を検討）
    }
}
