@echo off
setlocal EnableExtensions
title Green Channel System Stopper

echo.
echo ========================================
echo Green Channel System - Stop
echo ========================================

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ports=@(8080,5175); $ids=Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort } | Select-Object -ExpandProperty OwningProcess -Unique; if ($ids) { $ids | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host ('Stopped PID ' + $_) } } else { Write-Host 'No process is listening on ports 8080 or 5175.' }"

pause
