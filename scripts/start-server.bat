@echo off
REM DrSum MCP Server startup script for Windows

echo Starting DrSum MCP Server...

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    exit /b 1
)

REM Check if Maven is available
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Apache Maven
    exit /b 1
)

# Build the project
echo Building project...
call .\mvnw.cmd clean compile -q
if %errorlevel% neq 0 (
    echo ERROR: Failed to build project
    exit /b 1
)

REM Start the server
echo Starting server...
call .\mvnw.cmd exec:java -Dexec.mainClass="com.example.drsum.DrSumMcpServer" -Dexec.cleanupDaemonThreads=false

echo DrSum MCP Server stopped.