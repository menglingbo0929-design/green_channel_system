@echo off
setlocal EnableExtensions
title Green Channel System Launcher

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"

echo.
echo ========================================
echo Green Channel System - Start
echo ========================================

if not exist "%BACKEND_DIR%\mvnw.cmd" goto backend_missing
if not exist "%FRONTEND_DIR%\package.json" goto frontend_missing

where java >nul 2>&1
if errorlevel 1 goto java_missing

where npm >nul 2>&1
if errorlevel 1 goto npm_missing

if not defined SPRING_DATASOURCE_PASSWORD set /p SPRING_DATASOURCE_PASSWORD=Enter MySQL root password:
if not defined SPRING_DATASOURCE_PASSWORD goto password_missing

if not exist "%FRONTEND_DIR%\node_modules\vite\bin\vite.js" (
  echo Installing frontend dependencies...
  pushd "%FRONTEND_DIR%"
  call npm ci
  if errorlevel 1 (
    popd
    goto npm_install_failed
  )
  popd
)

echo Starting backend at http://127.0.0.1:8080
start "Green Channel Backend" /min /D "%BACKEND_DIR%" cmd /k call mvnw.cmd spring-boot:run

echo Starting frontend at http://127.0.0.1:5175
start "Green Channel Frontend" /min /D "%FRONTEND_DIR%" cmd /k npm run dev -- --host 127.0.0.1

echo Waiting for frontend...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(30); do { try { $client=[Net.Sockets.TcpClient]::new(); $client.Connect('127.0.0.1',5175); $client.Dispose(); exit 0 } catch { Start-Sleep -Milliseconds 500 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 goto frontend_timeout

start "" "http://127.0.0.1:5175/login"
echo.
echo Browser opened. Run stop-system.bat to stop services.
pause
exit /b 0

:backend_missing
echo ERROR: Backend folder was not found.
goto failed
:frontend_missing
echo ERROR: Frontend folder was not found.
goto failed
:java_missing
echo ERROR: Java was not found in PATH.
goto failed
:npm_missing
echo ERROR: npm was not found in PATH.
goto failed
:password_missing
echo ERROR: Database password cannot be empty.
goto failed
:npm_install_failed
echo ERROR: Frontend dependency installation failed.
goto failed
:frontend_timeout
echo ERROR: Frontend did not respond within 30 seconds.
goto failed
:failed
pause
exit /b 1
