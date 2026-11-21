-- =============================================================================
-- QNU-Events Bruno API Test — Cleanup Test Data (PostgreSQL 17)
-- =============================================================================
-- Dọn sạch toàn bộ sự kiện [BRU-*], dữ liệu đăng ký/điểm danh liên quan và
-- các phiên/user bruno_* (chỉ xóa user khi chưa bị audit_logs append-only tham chiếu).
-- Lưu ý: Không bao giờ UPDATE/DELETE bảng audit_logs (trigger trg_audit_logs_immutable).
-- =============================================================================

BEGIN;

DELETE FROM attendance_attempts
WHERE event_id IN (SELECT id FROM events WHERE title LIKE '[BRU-%' OR title = 'AI1')
   OR student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'bruno_%');

DELETE FROM attendances
WHERE event_id IN (SELECT id FROM events WHERE title LIKE '[BRU-%' OR title = 'AI1')
   OR student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'bruno_%');

DELETE FROM registrations
WHERE event_id IN (SELECT id FROM events WHERE title LIKE '[BRU-%' OR title = 'AI1')
   OR student_id IN (SELECT id FROM app_user WHERE login_id LIKE 'bruno_%');

DELETE FROM event_audience
WHERE event_id IN (SELECT id FROM events WHERE title LIKE '[BRU-%' OR title = 'AI1');

DELETE FROM events
WHERE title LIKE '[BRU-%' OR title = 'AI1';

DELETE FROM auth_session
WHERE id IN (
    '20000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000002'::uuid
)
   OR user_id IN (SELECT id FROM app_user WHERE login_id LIKE 'bruno_%');

DELETE FROM user_role
WHERE user_id IN (
    SELECT id FROM app_user
    WHERE login_id LIKE 'bruno_%'
      AND id NOT IN (SELECT actor_id FROM audit_logs WHERE actor_id IS NOT NULL)
);

DELETE FROM app_user
WHERE login_id LIKE 'bruno_%'
  AND id NOT IN (SELECT actor_id FROM audit_logs WHERE actor_id IS NOT NULL);

COMMIT;
