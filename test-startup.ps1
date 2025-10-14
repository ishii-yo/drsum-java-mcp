# Test script to capture what the MCP server outputs at startup
$env:DRSUM_HOST = "qa-drsum57"
$env:DRSUM_PORT = "6001"
$env:DRSUM_USERNAME = "Administrator"
$env:DRSUM_PASSWORD = ""
$env:DRSUM_DATABASE = "SALES"

Write-Host "Starting MCP server and capturing first 10 lines of output..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C after a few seconds to stop" -ForegroundColor Yellow
Write-Host ""

# Start the process and capture output
$process = Start-Process -FilePath "C:\java\jdk-17\bin\java.exe" `
    -ArgumentList "-Dfile.encoding=UTF-8", "-jar", "target\drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar" `
    -NoNewWindow `
    -PassThru `
    -RedirectStandardOutput "startup-stdout.log" `
    -RedirectStandardError "startup-stderr.log"

# Wait a bit for startup
Start-Sleep -Seconds 3

# Kill the process
Stop-Process -Id $process.Id -Force

Write-Host "`n=== STDOUT (first 20 lines) ===" -ForegroundColor Cyan
Get-Content "startup-stdout.log" -TotalCount 20 -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host $_ -ForegroundColor White
}

Write-Host "`n=== STDERR (first 20 lines) ===" -ForegroundColor Magenta  
Get-Content "startup-stderr.log" -TotalCount 20 -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host $_ -ForegroundColor White
}

Write-Host "`nLog files saved to:" -ForegroundColor Green
Write-Host "  - startup-stdout.log" -ForegroundColor Green
Write-Host "  - startup-stderr.log" -ForegroundColor Green
