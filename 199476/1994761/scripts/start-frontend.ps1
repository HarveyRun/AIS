$projectRoot = Split-Path -Parent $PSScriptRoot
$listener = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "Frontend is already running: http://127.0.0.1:5173" -ForegroundColor Green
    exit 0
}

Start-Process -FilePath "npm.cmd" `
    -ArgumentList "run", "dev", "--", "--host", "127.0.0.1" `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $projectRoot "frontend.log") `
    -RedirectStandardError (Join-Path $projectRoot "frontend-error.log") `
    -WindowStyle Hidden

Write-Host "Frontend is starting: http://127.0.0.1:5173" -ForegroundColor Green
