$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot "backend"
$jarPath = Join-Path $backendRoot "target\diancheng-server-1.0.0.jar"
$logPath = Join-Path $backendRoot "server.log"
$errorLogPath = Join-Path $backendRoot "server-error.log"

try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/health" -TimeoutSec 2
    if ($health.ok) {
        Write-Host "Java service is already running: http://127.0.0.1:8080" -ForegroundColor Green
        exit 0
    }
} catch {
    # The service is not running yet.
}

$mysql = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "MySQL80 service was not found. Install or start MySQL 8.0 first."
}
if ($mysql.Status -ne "Running") {
    Start-Service -Name "MySQL80"
}

if (-not (Test-Path -LiteralPath $jarPath)) {
    Push-Location $backendRoot
    try { & mvn -DskipTests package } finally { Pop-Location }
}

Start-Process -FilePath "java" `
    -ArgumentList "-jar", $jarPath `
    -WorkingDirectory $backendRoot `
    -RedirectStandardOutput $logPath `
    -RedirectStandardError $errorLogPath `
    -WindowStyle Hidden

for ($attempt = 0; $attempt -lt 30; $attempt += 1) {
    Start-Sleep -Milliseconds 500
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/health" -TimeoutSec 2
        if ($health.ok) {
            Write-Host "Java service started: http://127.0.0.1:8080" -ForegroundColor Green
            exit 0
        }
    } catch {
        # Keep waiting for Spring Boot.
    }
}

throw "Java service startup timed out. Check backend/server-error.log."
