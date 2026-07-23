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

powershell -NoProfile -ExecutionPolicy Bypass -Command "$client=[Net.Sockets.TcpClient]::new(); try { $client.Connect('127.0.0.1',3306); exit 0 } catch { exit 1 } finally { $client.Dispose() }"
if errorlevel 1 goto mysql_unavailable

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

powerShell -NoProfile -ExecutionPolicy Bypass -Command "$client=[Net.Sockets.TcpClient]::new(); try { $client.Connect('127.0.0.1',8083); exit 0 } catch { exit 1 } finally { $client.Dispose() }"
if errorlevel 1 (
  echo Starting backend at http://127.0.0.1:8083
  start "Green Channel Backend" /min /D "%BACKEND_DIR%" cmd /k call mvnw.cmd spring-boot:run
) else (
  echo Backend is already listening at http://127.0.0.1:8083, reusing it.
)

echo Waiting for backend...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(60); do { try { $client=[Net.Sockets.TcpClient]::new(); $client.Connect('127.0.0.1',8083); $client.Dispose(); exit 0 } catch { Start-Sleep -Milliseconds 500 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 goto backend_timeout

powerShell -NoProfile -ExecutionPolicy Bypass -Command "$client=[Net.Sockets.TcpClient]::new(); try { $client.Connect('127.0.0.1',5175); exit 0 } catch { exit 1 } finally { $client.Dispose() }"
if errorlevel 1 (
  echo Starting frontend at http://127.0.0.1:5175
  start "Green Channel Frontend" /min /D "%FRONTEND_DIR%" cmd /k npm run dev -- --host 127.0.0.1
) else (
  echo Frontend is already listening at http://127.0.0.1:5175, reusing it.
)

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
:mysql_unavailable
echo ERROR: MySQL is not listening on 127.0.0.1:3306.
echo Start the MySQL80 Windows service as an administrator, then run this script again.
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
:backend_timeout
echo ERROR: Backend did not respond on port 8083 within 60 seconds.
echo Check the Green Channel Backend window for database password or migration errors.
goto failed
:failed
pause
exit /b 1
