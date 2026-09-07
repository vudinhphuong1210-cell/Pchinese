@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "BACKEND_DIR=%PROJECT_ROOT%backend"
set "FRONTEND_DIR=%PROJECT_ROOT%frontend"

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [ERROR] Cannot find backend\pom.xml.
  exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Cannot find frontend\package.json.
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven is required but was not found on PATH.
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm is required but was not found on PATH.
  exit /b 1
)

echo Starting Pchinese backend and frontend in separate windows...
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:3000
echo.
echo Ensure PostgreSQL and local backend secrets are configured before using the API.

if not exist "%FRONTEND_DIR%\node_modules" (
  echo Installing frontend dependencies...
  pushd "%FRONTEND_DIR%"
  call npm ci
  if errorlevel 1 (
    popd
    echo [ERROR] Frontend dependency installation failed.
    exit /b 1
  )
  popd
)

start "Pchinese Backend" /D "%BACKEND_DIR%" cmd.exe /k "mvn spring-boot:run"
start "Pchinese Frontend" /D "%FRONTEND_DIR%" cmd.exe /k "npm run dev"

endlocal
