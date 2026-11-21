-- =============================================================================
-- QNU-Events JMeter Performance Test Data Cleanup (PostgreSQL 17)
-- =============================================================================
-- Xóa sạch dữ liệu giả lập (perf_sv_* và sự kiện [JMETER-*]) sau khi chạy tải.
-- Lưu ý: Không đụng vào audit_logs (bảng append-only theo trigger bất biến).
-- =============================================================================

BEGIN;

DELETE FROM attendance_attempts
WHERE student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'perf_sv_%')
   OR event_id IN (SELECT id FROM events WHERE title LIKE '[JMETER-%');

DELETE FROM attendances
WHERE student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'perf_sv_%')
   OR event_id IN (SELECT id FROM events WHERE title LIKE '[JMETER-%');

DELETE FROM registrations
WHERE student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'perf_sv_%')
   OR event_id IN (SELECT id FROM events WHERE title LIKE '[JMETER-%');

DELETE FROM event_audience
WHERE event_id IN (SELECT id FROM events WHERE title LIKE '[JMETER-%');

DELETE FROM events
WHERE title LIKE '[JMETER-%';

DELETE FROM auth_session
WHERE user_id IN (SELECT id FROM app_user WHERE login_id LIKE 'perf_sv_%');

DELETE FROM user_role
WHERE user_id IN (SELECT id FROM app_user WHERE login_id LIKE 'perf_sv_%');

DELETE FROM app_user
WHERE login_id LIKE 'perf_sv_%';

COMMIT;
