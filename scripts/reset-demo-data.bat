@echo off
setlocal
echo ==============================================
echo  QNU-Events: Reset Demo Database Data
echo ==============================================

set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%.."
set "SQL_FILE=%SCRIPT_DIR%reset-demo-data.sql"

if not exist "%SQL_FILE%" (
    echo [ERROR] SQL file not found: "%SQL_FILE%"
    exit /b 1
)

if exist "%REPO_ROOT%\.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%REPO_ROOT%\.env") do (
        if "%%A"=="DB_USERNAME" if not defined DB_USERNAME set "DB_USERNAME=%%B"
        if "%%A"=="DB_NAME" if not defined DB_NAME set "DB_NAME=%%B"
    )
)

if not defined DB_USERNAME set "DB_USERNAME=qnu_events"
if not defined DB_NAME set "DB_NAME=qnu_events"

pushd "%REPO_ROOT%"
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "%DB_USERNAME%" -d "%DB_NAME%" < "%SQL_FILE%"
set "EXIT_CODE=%ERRORLEVEL%"
popd

if %EXIT_CODE% EQU 0 (
    echo [SUCCESS] Demo transaction data has been reset cleanly.
    echo Accounts ^(owner01, owner02, sv001, sv002, sv003, admin01^) and classes remain intact.
) else (
    echo [ERROR] Failed to reset database. Ensure PostgreSQL is running via: docker compose up -d --wait postgres
)
exit /b %EXIT_CODE%
