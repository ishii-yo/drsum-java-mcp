#!/bin/bash
# DrSum MCP Server startup script for Unix/Linux/macOS

set -e

echo "Starting DrSum MCP Server..."

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or higher is required"
    echo "Current Java version: $JAVA_VERSION"
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Apache Maven"
    exit 1
fi

# Build the project
echo "Building project..."
./mvnw clean compile -q
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to build project"
    exit 1
fi

# Start the server
echo "Starting server..."
./mvnw exec:java -Dexec.mainClass="com.example.drsum.DrSumMcpServer" -Dexec.cleanupDaemonThreads=false

echo "DrSum MCP Server stopped."