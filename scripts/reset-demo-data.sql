-- QNU-Events: Reset transaction data for rehearsal and demo
-- Keeps user accounts, roles, and org classes intact.

TRUNCATE TABLE 
    attendances, 
    attendance_attempts, 
    registrations, 
    event_audience, 
    events, 
    audit_logs 
CASCADE;
