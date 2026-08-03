$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listener) {
    Write-Host "Java service is not running."
    exit 0
}

$process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
if (-not $process.CommandLine -or $process.CommandLine -notmatch "diancheng-server") {
    throw "Port 8080 is owned by another program; nothing was stopped."
}

Stop-Process -Id $listener.OwningProcess -Force
Write-Host "Java service stopped." -ForegroundColor Green
