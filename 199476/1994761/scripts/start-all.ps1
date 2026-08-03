$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "start-backend.ps1")
& (Join-Path $PSScriptRoot "start-frontend.ps1")
Write-Host "Diancheng is running: http://127.0.0.1:5173" -ForegroundColor Cyan
