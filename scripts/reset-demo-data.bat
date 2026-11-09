@echo off
echo ==============================================
echo  QNU-Events: Reset Demo Database Data
echo ==============================================
docker exec -i postgres psql -U postgres -d qnu_events -c "TRUNCATE TABLE attendances, attendance_attempts, registrations, event_audience, events, audit_logs CASCADE;"
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Demo transaction data has been reset cleanly.
    echo Accounts (owner01, sv001, sv002, sv003, admin01) and classes remain intact.
) else (
    echo [ERROR] Failed to reset database. Is PostgreSQL running in Docker?
)
pause
