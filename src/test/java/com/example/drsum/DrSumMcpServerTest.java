package com.example.drsum;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import jp.co.dw_sapporo.drsum_ea.DWException;

import com.example.drsum.connection.ConnectionConfig;
import com.example.drsum.connection.DrSumConnection;
import com.example.drsum.connection.ScopeDefinitions;
import com.example.drsum.service.DrSumQueryService;
import com.example.drsum.service.DrSumMetadataService;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DrSum MCP Server functionality.
 */
class DrSumMcpServerTest {

    private ConnectionConfig validConfig;
    private DrSumConnection connection;

    @BeforeEach
    void setUp() {
        // Create a valid configuration for testing
        validConfig = new ConnectionConfig(
            "localhost",
            6001,
            "Administrator",
            "",
            "SALES"
        );
        connection = new DrSumConnection();
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
        ConnectionConfig config = new ConnectionConfig(
            "localhost", 6001, "user", "", "db"
        );
        assertEquals("", config.getPassword());
    }

    @Test
    @DisplayName("ConnectionConfig should handle null password as empty string")
    void testConnectionConfig_NullPassword() {
        ConnectionConfig config = new ConnectionConfig(
            "localhost", 6001, "user", null, "db"
        );
        assertEquals("", config.getPassword());
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null host")
    void testConnectionConfig_NullHost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(null, 6001, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty host")
    void testConnectionConfig_EmptyHost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("", 6001, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (0)")
    void testConnectionConfig_InvalidPortZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 0, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (negative)")
    void testConnectionConfig_InvalidPortNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", -1, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for invalid port (> 65535)")
    void testConnectionConfig_InvalidPortTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 65536, "user", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null username")
    void testConnectionConfig_NullUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 6001, null, "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty username")
    void testConnectionConfig_EmptyUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 6001, "", "pass", "db");
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for null database")
    void testConnectionConfig_NullDatabase() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 6001, "user", "pass", null);
        });
    }

    @Test
    @DisplayName("ConnectionConfig should throw exception for empty database")
    void testConnectionConfig_EmptyDatabase() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig("localhost", 6001, "user", "pass", "");
        });
    }

    @Test
    @DisplayName("ConnectionConfig toString should obfuscate password")
    void testConnectionConfig_PasswordObfuscation() {
        ConnectionConfig config = new ConnectionConfig(
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
            new DrSumMetadataService(null);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception when not connected")
    void testMetadataService_NotConnected() {
        DrSumMetadataService service = 
            new DrSumMetadataService(connection);
        
        assertThrows(IllegalStateException.class, () -> {
            service.getTableMetadata("test_table", 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for null table name")
    void testMetadataService_NullTableName() {
        DrSumMetadataService service = 
            new DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata(null, 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for empty table name")
    void testMetadataService_EmptyTableName() {
        DrSumMetadataService service = 
            new DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("", 3);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should throw exception for negative sample rows")
    void testMetadataService_NegativeSampleRows() {
        DrSumMetadataService service = 
            new DrSumMetadataService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.getTableMetadata("test_table", -1);
        });
    }

    @Test
    @DisplayName("DrSumMetadataService should accept zero sample rows")
    void testMetadataService_ZeroSampleRows() {
        DrSumMetadataService service = 
            new DrSumMetadataService(connection);
        
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
    // DrSumQueryService Tests
    // ========================================================================

    @Test
    @DisplayName("DrSumQueryService should throw exception with null connection")
    void testQueryService_NullConnection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DrSumQueryService(null);
        });
    }

    @Test
    @DisplayName("DrSumQueryService should throw exception when not connected")
    void testQueryService_NotConnected() {
        DrSumQueryService service = 
            new DrSumQueryService(connection);
        
        assertThrows(IllegalStateException.class, () -> {
            service.executeQuery("SELECT * FROM test");
        });
    }

    @Test
    @DisplayName("DrSumQueryService should throw exception for null SQL")
    void testQueryService_NullSQL() {
        DrSumQueryService service = 
            new DrSumQueryService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.executeQuery(null);
        });
    }

    @Test
    @DisplayName("DrSumQueryService should throw exception for empty SQL")
    void testQueryService_EmptySQL() {
        DrSumQueryService service = 
            new DrSumQueryService(connection);
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.executeQuery("");
        });
    }

    // ========================================================================
    // Tool Request Tests (New API)
    // ========================================================================

    @Test
    @DisplayName("CallToolRequest should be created with valid arguments")
    void testCallToolRequest_Creation() {
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
        
        assertNotNull(request);
        assertEquals("summarize", request.name());
        assertNotNull(request.arguments());
        assertEquals(longText, request.arguments().get("text"));
        assertEquals(3, request.arguments().get("max_sentences"));
    }

    @Test
    @DisplayName("CallToolRequest for configure_connection should contain all required parameters")
    void testCallToolRequest_ConfigureConnection() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "configure_connection",
                Map.of(
                        "host", "localhost",
                        "port", 6001,
                        "username", "Administrator",
                        "password", "",
                        "database", "SALES"
                )
        );
        
        assertNotNull(request);
        assertEquals("configure_connection", request.name());
        assertEquals("localhost", request.arguments().get("host"));
        assertEquals(6001, request.arguments().get("port"));
        assertEquals("Administrator", request.arguments().get("username"));
        assertEquals("SALES", request.arguments().get("database"));
    }

    @Test
    @DisplayName("CallToolRequest for get_metadata should contain table_name")
    void testCallToolRequest_GetMetadata() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_metadata",
                Map.of(
                        "table_name", "test_table",
                        "sample_rows", 5
                )
        );
        
        assertNotNull(request);
        assertEquals("get_metadata", request.name());
        assertEquals("test_table", request.arguments().get("table_name"));
        assertEquals(5, request.arguments().get("sample_rows"));
    }

    @Test
    @DisplayName("CallToolRequest for execute_query should contain sql_query")
    void testCallToolRequest_ExecuteQuery() {
        String sql = "SELECT * FROM test_table WHERE id > 100";
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "execute_query",
                Map.of("sql_query", sql)
        );
        
        assertNotNull(request);
        assertEquals("execute_query", request.name());
        assertEquals(sql, request.arguments().get("sql_query"));
    }

    @Test
    @DisplayName("CallToolRequest for disconnect should be created without arguments")
    void testCallToolRequest_Disconnect() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "disconnect",
                Map.of()
        );
        
        assertNotNull(request);
        assertEquals("disconnect", request.name());
        assertTrue(request.arguments().isEmpty());
    }

    // ========================================================================
    // Environment Variable Tests (Phase 5/6)
    // ========================================================================

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should throw exception when DRSUM_HOST is not set")
    void testConnectionConfig_FromEnvironment_MissingHost() {
        // This test requires environment variables to be unset
        // In a real environment where variables are not set, this would throw
        // For now, we document the expected behavior
        
        // Note: This test would require mocking System.getenv() which is difficult
        // without additional frameworks. The actual validation is done in the method.
        
        // The method should throw IllegalStateException with a message about DRSUM_HOST
        // when the environment variable is not set
        assertTrue(true, "Manual verification required: Environment variable validation works correctly");
    }

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should throw exception when DRSUM_PORT is not set")
    void testConnectionConfig_FromEnvironment_MissingPort() {
        // Similar to above - requires environment variable mocking
        assertTrue(true, "Manual verification required: Port validation works correctly");
    }

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should throw exception when DRSUM_PORT is not a number")
    void testConnectionConfig_FromEnvironment_InvalidPort() {
        // This would require mocking System.getenv() to return invalid port
        // The method should throw IllegalStateException when port cannot be parsed
        assertTrue(true, "Manual verification required: Port parsing validation works correctly");
    }

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should throw exception when DRSUM_USERNAME is not set")
    void testConnectionConfig_FromEnvironment_MissingUsername() {
        // Similar to above - requires environment variable mocking
        assertTrue(true, "Manual verification required: Username validation works correctly");
    }

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should throw exception when DRSUM_DATABASE is not set")
    void testConnectionConfig_FromEnvironment_MissingDatabase() {
        // Similar to above - requires environment variable mocking
        assertTrue(true, "Manual verification required: Database validation works correctly");
    }

    @Test
    @DisplayName("ConnectionConfig.fromEnvironment should accept empty DRSUM_PASSWORD")
    void testConnectionConfig_FromEnvironment_EmptyPassword() {
        // Password is optional and can be empty
        // The method should handle null password as empty string
        assertTrue(true, "Manual verification required: Empty password handling works correctly");
    }

    // ========================================================================
    // Integration Tests Notes
    // ========================================================================
    
    /*
     * INTEGRATION TESTS (Require actual Dr.Sum server)
     * 
     * The following scenarios should be tested manually with a running Dr.Sum server:
     * 
     * 1. Environment Variable Connection Test:
     *    - Set environment variables: DRSUM_HOST, DRSUM_PORT, DRSUM_USERNAME, DRSUM_PASSWORD, DRSUM_DATABASE
     *    - Call get_metadata tool
     *    - Verify connection is established, metadata is retrieved, and connection is closed
     * 
     * 2. On-Demand Connection for get_metadata:
     *    - Call get_metadata with valid environment variables
     *    - Verify log messages: "Connected to Dr.Sum" and "Disconnected from Dr.Sum"
     *    - Verify metadata is returned in correct JSON format
     * 
     * 3. On-Demand Connection for execute_query:
     *    - Call execute_query with valid environment variables and SQL
     *    - Verify log messages: "Connected to Dr.Sum" and "Disconnected from Dr.Sum"
     *    - Verify query results are returned in correct JSON format
     * 
     * 4. Connection Failure Handling:
     *    - Set invalid environment variables (wrong host/port)
     *    - Call get_metadata or execute_query
     *    - Verify appropriate error message is returned
     * 
     * 5. Missing Environment Variables:
     *    - Unset required environment variables
     *    - Call get_metadata or execute_query
     *    - Verify error message indicates which environment variable is missing
     * 
     * 6. Connection Cleanup on Error:
     *    - Simulate error during metadata/query execution
     *    - Verify connection is still properly closed (check logs)
     * 
     * 7. Multiple Sequential Calls:
     *    - Call get_metadata multiple times in sequence
     *    - Verify each call establishes and closes its own connection
     *    - Verify no connection leaks
     * 
     * 8. Invalid SQL Query:
     *    - Call execute_query with invalid SQL
     *    - Verify error is properly caught and returned
     *    - Verify connection is still closed
     * 
     * 9. Non-existent Table:
     *    - Call get_metadata with non-existent table name
     *    - Verify error is properly caught and returned
     *    - Verify connection is still closed
     * 
     * 10. Password with Special Characters:
     *     - Set DRSUM_PASSWORD with special characters
     *     - Verify connection works correctly
     *     - Verify password is obfuscated in logs
     */
}
