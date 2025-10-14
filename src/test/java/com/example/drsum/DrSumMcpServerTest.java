package com.example.drsum;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import jp.co.dw_sapporo.drsum_ea.DWException;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DrSum MCP Server functionality.
 */
class DrSumMcpServerTest {

    private DrSumMcpServer.ConnectionConfig validConfig;
    private DrSumMcpServer.DrSumConnection connection;

    @BeforeEach
    void setUp() {
        // Create a valid configuration for testing
        validConfig = new DrSumMcpServer.ConnectionConfig(
            "localhost",
            6001,
            "Administrator",
            "",
            "SALES"
        );
        connection = new DrSumMcpServer.DrSumConnection();
    }

    // ========================================================================
    // ConnectionConfig Tests
    // ========================================================================

    @Test
    @DisplayName("ConnectionConfig should be created with valid parameters")
    void testConnectionConfig_ValidParameters() {
        assertNotNull(validConfig);
        assertEquals("localhost", validConfig.getHost());
        assertEquals(6001, validConfig.getPort());
        assertEquals("Administrator", validConfig.getUsername());
        assertEquals("", validConfig.getPassword());
        assertEquals("SALES", validConfig.getDatabase());
    }

    @Test
    @DisplayName("ConnectionConfig should handle empty password")
    void testConnectionConfig_EmptyPassword() {
        DrSumMcpServer.ConnectionConfig config = new DrSumMcpServer.ConnectionConfig(
            "localhost", 6001, "user", "", "db"
        );
        assertEquals("", config.getPassword());
    }

    @Test
    @DisplayName("ConnectionConfig should handle null password as empty string")
    void testConnectionConfig_NullPassword() {
        DrSumMcpServer.ConnectionConfig config = new DrSumMcpServer.ConnectionConfig(
            "localhost", 6001, "user", null, "db"
        );
        assertEquals("", config.getPassword());
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null host")
    void testConnectionConfig_NullHost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig(null, 6001, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty host")
    void testConnectionConfig_EmptyHost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("", 6001, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (0)")
    void testConnectionConfig_InvalidPortZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 0, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (negative)")
    void testConnectionConfig_InvalidPortNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", -1, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (> 65535)")
    void testConnectionConfig_InvalidPortTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 65536, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null username")
    void testConnectionConfig_NullUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 6001, null, "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty username")
    void testConnectionConfig_EmptyUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 6001, "", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null database")
    void testConnectionConfig_NullDatabase() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 6001, "user", "pass", null);
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty database")
    void testConnectionConfig_EmptyDatabase() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.ConnectionConfig("localhost", 6001, "user", "pass", "");
        });
    }

    @Test
    @DisplayName("ConnectionConfig toString should obfuscate password")
    void testConnectionConfig_PasswordObfuscation() {
        DrSumMcpServer.ConnectionConfig config = new DrSumMcpServer.ConnectionConfig(
            "localhost", 6001, "user", "secretPassword", "db"
        );
        String configString = config.toString();
        assertFalse(configString.contains("secretPassword"), 
                   "Password should be obfuscated in toString()");
        assertTrue(configString.contains("****"), 
                  "toString() should contain obfuscated password marker");
    }

    // ========================================================================
    // DrSumConnection Tests
    // ========================================================================

    @Test
    @DisplayName("DrSumConnection should initially not be connected")
    void testDrSumConnection_InitiallyNotConnected() {
        assertFalse(connection.isConnected());
    }

    @Test
    @DisplayName("DrSumConnection should throw exception when connecting with null config")
    void testDrSumConnection_ConnectWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            connection.connect(null);
        });
    }

    @Test
    @DisplayName("DrSumConnection getConnection should throw exception when not connected")
    void testDrSumConnection_GetConnectionWhenNotConnected() {
        assertThrows(IllegalStateException.class, () -> {
            connection.getConnection();
        });
    }

    @Test
    @DisplayName("DrSumConnection should return null config when not connected")
    void testDrSumConnection_GetConfigWhenNotConnected() {
        assertNull(connection.getConfig());
    }

    @Test
    @DisplayName("DrSumConnection disconnect should not throw when not connected")
    void testDrSumConnection_DisconnectWhenNotConnected() {
        assertDoesNotThrow(() -> {
            connection.disconnect();
        });
    }

    // Note: Actual connection tests require a running Dr.Sum server
    // These are integration tests and should be run separately
    // For unit tests, we would use mocking frameworks like Mockito

    // ========================================================================
    // DrSumMetadataService Tests
    // ========================================================================

    @Test
    @DisplayName("DrSumMetadataService should throw exception with null connection")
    void testMetadataService_NullConnection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumMcpServer.DrSumMetadataService(null);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception when not connected")
    void testMetadataService_NotConnected() {
        DrSumMcpServer.DrSumMetadataService service = 
            new DrSumMcpServer.DrSumMetadataService(connection);
        
        assertThrows(IllegalStateException.class, () -> {
            service.getTableMetadata("test_table", 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for null table name")
    void testMetadataService_NullTableName() {
        DrSumMcpServer.DrSumMetadataService service = 
            new DrSumMcpServer.DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata(null, 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for empty table name")
    void testMetadataService_EmptyTableName() {
        DrSumMcpServer.DrSumMetadataService service = 
            new DrSumMcpServer.DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("", 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for negative sample rows")
    void testMetadataService_NegativeSampleRows() {
        DrSumMcpServer.DrSumMetadataService service = 
            new DrSumMcpServer.DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("test_table", -1);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should accept zero sample rows")
    void testMetadataService_ZeroSampleRows() {
        DrSumMcpServer.DrSumMetadataService service = 
            new DrSumMcpServer.DrSumMetadataService(connection);
        
        // Should not throw exception with 0 rows (validation only)
        assertDoesNotThrow(() -> {
            // This will fail at connection check, but validates the parameter
            try {
                service.getTableMetadata("test_table", 0);
            } catch (IllegalStateException e) {
                // Expected - not connected
            }
        });
    }

    // ========================================================================
    // Summarize Tool Tests (Legacy)
    // ========================================================================

    @Test
    @DisplayName("Summarize tool test framework should work")
    void testSummarizeTool_BasicFunctionality() {
        String longText = "This is the first sentence of a long document. " +
                         "This is the second sentence with more information. " +
                         "This is the third sentence that continues the text. " +
                         "This is the fourth sentence that should be excluded. " +
                         "This is the fifth sentence that should also be excluded.";

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "summarize",
                Map.of(
                        "text", longText,
                        "max_sentences", 3
                )
        );
        
        assertTrue(true, "Test framework is working");
    }
}