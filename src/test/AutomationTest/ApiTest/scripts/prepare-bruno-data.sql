-- =============================================================================
-- QNU-Events Bruno API Test — DB Fixture Preparation (PostgreSQL 17)
-- =============================================================================
-- Khởi tạo dữ liệu nền idempotent cho thư mục 07-DB-Fixture-Cases/
-- (bao phủ các ca API yêu cầu trạng thái DB đặc thù: LOCKED, CLASS_REPRESENTATIVE,
--  class_id=NULL, sự kiện CANCELLED, đăng ký CANCELLED/WAITING_LIST,
--  điểm danh PENDING/REJECTED, cấu hình QR 5s/0s và phiên hết hạn).
--
-- Cách chạy:
--   docker compose exec -T postgres psql -U qnu_events -d qnu_events < src\test\AutomationTest\ApiTest\scripts\prepare-bruno-data.sql
-- =============================================================================

BEGIN;

-- 1. Đảm bảo lớp K49CNTT1 & K49CNTT2 tồn tại
INSERT INTO org_class (id, code, name, faculty_name, cohort_year)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'K49CNTT1', 'K49 Công nghệ thông tin 1', 'Khoa Công nghệ Thông tin', 2024),
    ('00000000-0000-0000-0000-000000000002', 'K49CNTT2', 'K49 Công nghệ thông tin 2', 'Khoa Công nghệ Thông tin', 2024)
ON CONFLICT (code) DO NOTHING;

-- 2. Dọn sạch dữ liệu cũ của 6 sự kiện cố định & 2 phiên cố định để chạy lặp idempotent
DELETE FROM attendance_attempts
WHERE event_id IN (
    '30000000-0000-0000-0000-000000000001'::uuid,
    '30000000-0000-0000-0000-000000000002'::uuid,
    '30000000-0000-0000-0000-000000000003'::uuid,
    '30000000-0000-0000-0000-000000000004'::uuid,
    '30000000-0000-0000-0000-000000000005'::uuid,
    '30000000-0000-0000-0000-000000000006'::uuid
);

DELETE FROM attendances
WHERE event_id IN (
    '30000000-0000-0000-0000-000000000001'::uuid,
    '30000000-0000-0000-0000-000000000002'::uuid,
    '30000000-0000-0000-0000-000000000003'::uuid,
    '30000000-0000-0000-0000-000000000004'::uuid,
    '30000000-0000-0000-0000-000000000005'::uuid,
    '30000000-0000-0000-0000-000000000006'::uuid
);

DELETE FROM registrations
WHERE event_id IN (
    '30000000-0000-0000-0000-000000000001'::uuid,
    '30000000-0000-0000-0000-000000000002'::uuid,
    '30000000-0000-0000-0000-000000000003'::uuid,
    '30000000-0000-0000-0000-000000000004'::uuid,
    '30000000-0000-0000-0000-000000000005'::uuid,
    '30000000-0000-0000-0000-000000000006'::uuid
)
   OR student_id IN (SELECT id FROM app_user WHERE login_id = 'bruno_sv_empty');

DELETE FROM events
WHERE id IN (
    '30000000-0000-0000-0000-000000000001'::uuid,
    '30000000-0000-0000-0000-000000000002'::uuid,
    '30000000-0000-0000-0000-000000000003'::uuid,
    '30000000-0000-0000-0000-000000000004'::uuid,
    '30000000-0000-0000-0000-000000000005'::uuid,
    '30000000-0000-0000-0000-000000000006'::uuid
);

DELETE FROM auth_session
WHERE id IN (
    '20000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000002'::uuid
);

-- 3. Tạo 7 tài khoản đặc thù dùng chung BCrypt cost 12 hash của sv001 (Demo@12345)
WITH ref_hash AS (
    SELECT password_hash FROM app_user WHERE login_id = 'sv001' LIMIT 1
)
INSERT INTO app_user (id, login_id, password_hash, full_name, student_code, class_id, status)
SELECT v.id, v.login_id, ref_hash.password_hash, v.full_name, v.student_code, v.class_id, v.status
FROM ref_hash, (
    VALUES
        ('10000000-0000-0000-0000-000000000001'::uuid, 'bruno_locked',    'Bruno Tài Khoản Bị Khóa',     '4988000001', '00000000-0000-0000-0000-000000000001'::uuid, 'LOCKED'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'bruno_bcs01',     'Bruno Ban Cán Sự Lớp',        '4988000002', '00000000-0000-0000-0000-000000000001'::uuid, 'ACTIVE'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'bruno_sv_noclass','Bruno SV Không Lớp',          '4988000003', NULL::uuid,                                   'ACTIVE'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'bruno_sv_rereg',  'Bruno SV Đăng Ký Lại',        '4988000004', '00000000-0000-0000-0000-000000000001'::uuid, 'ACTIVE'),
        ('10000000-0000-0000-0000-000000000005'::uuid, 'bruno_sv_waiting','Bruno SV Waiting List',       '4988000005', '00000000-0000-0000-0000-000000000001'::uuid, 'ACTIVE'),
        ('10000000-0000-0000-0000-000000000006'::uuid, 'bruno_sv_empty',  'Bruno SV Lịch Sử Rỗng',       '4988000006', '00000000-0000-0000-0000-000000000001'::uuid, 'ACTIVE'),
        ('10000000-0000-0000-0000-000000000007'::uuid, 'bruno_sv_hist_edge','Bruno SV Lịch Sử Biên',     '4988000007', '00000000-0000-0000-0000-000000000001'::uuid, 'ACTIVE')
) AS v(id, login_id, full_name, student_code, class_id, status)
ON CONFLICT (login_id) DO UPDATE
    SET status = EXCLUDED.status,
        class_id = EXCLUDED.class_id,
        password_hash = EXCLUDED.password_hash;

INSERT INTO user_role (user_id, role)
SELECT u.id, r.role
FROM app_user u
JOIN (
    VALUES
        ('bruno_locked',      'STUDENT'),
        ('bruno_bcs01',       'CLASS_REPRESENTATIVE'),
        ('bruno_sv_noclass',  'STUDENT'),
        ('bruno_sv_rereg',    'STUDENT'),
        ('bruno_sv_waiting',  'STUDENT'),
        ('bruno_sv_empty',    'STUDENT'),
        ('bruno_sv_hist_edge','STUDENT')
) AS r(login_id, role) ON u.login_id = r.login_id
ON CONFLICT DO NOTHING;

-- 4. Tạo 2 auth_session đặc thù cho TC-AUTH-10 (user LOCKED có session còn hạn) & TC-AUTH-11 (sv001 có session hết hạn)
INSERT INTO auth_session (id, user_id, issued_at, expires_at, revoked_at)
SELECT '20000000-0000-0000-0000-000000000001'::uuid, u.id, now() - interval '10 minutes', now() + interval '8 hours', NULL
FROM app_user u WHERE u.login_id = 'bruno_locked';

INSERT INTO auth_session (id, user_id, issued_at, expires_at, revoked_at)
SELECT '20000000-0000-0000-0000-000000000002'::uuid, u.id, now() - interval '9 hours', now() - interval '5 minutes', NULL
FROM app_user u WHERE u.login_id = 'sv001';

-- 5. Tạo 6 sự kiện fixture cố định thuộc owner01
WITH owner_ref AS (
    SELECT id FROM app_user WHERE login_id = 'owner01' LIMIT 1
)
INSERT INTO events (
    id, owner_id, title, description, location,
    start_at, end_at, registration_deadline, attendance_open_at, attendance_close_at,
    attendance_method, qr_rotation_seconds, qr_grace_seconds,
    quota_total, registered_count, activity_type, activity_points,
    status, version, previewed_version, published_at, published_by
)
SELECT
    e.id, owner_ref.id, e.title, 'Sự kiện fixture Bruno DB-Edge', 'Hội trường A - QNU',
    now() - interval '10 minutes', now() + interval '2 hours',
    now() + interval '1 hour', now() - interval '10 minutes', now() + interval '2 hours',
    'DYNAMIC_QR', e.qr_rot, e.qr_grace,
    20, e.reg_cnt, 'Hội thảo khoa học', 5,
    e.status, 0, 0,
    CASE WHEN e.status = 'PUBLISHED' THEN now() - interval '1 hour' ELSE NULL END,
    CASE WHEN e.status = 'PUBLISHED' THEN owner_ref.id ELSE NULL END
FROM owner_ref, (
    VALUES
        ('30000000-0000-0000-0000-000000000001'::uuid, '[BRU-DB] Sự kiện CANCELLED',          10, 3, 0, 'CANCELLED'),
        ('30000000-0000-0000-0000-000000000002'::uuid, '[BRU-DB] Sự kiện Active WaitingList', 10, 3, 0, 'PUBLISHED'),
        ('30000000-0000-0000-0000-000000000003'::uuid, '[BRU-DB] Sự kiện Re-Register',        10, 3, 0, 'PUBLISHED'),
        ('30000000-0000-0000-0000-000000000004'::uuid, '[BRU-DB] Sự kiện QR 5s Grace 0s',      5, 0, 0, 'PUBLISHED'),
        ('30000000-0000-0000-0000-000000000005'::uuid, '[BRU-DB] Sự kiện Hist Pending/Rej',   10, 3, 1, 'PUBLISHED'),
        ('30000000-0000-0000-0000-000000000006'::uuid, '[BRU-DB] Sự kiện Hist Cancelled+Reg', 10, 3, 1, 'PUBLISHED')
) AS e(id, title, qr_rot, qr_grace, reg_cnt, status);

INSERT INTO event_audience (event_id, class_id)
VALUES
    ('30000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000001'::uuid),
    ('30000000-0000-0000-0000-000000000002'::uuid, '00000000-0000-0000-0000-000000000001'::uuid),
    ('30000000-0000-0000-0000-000000000003'::uuid, '00000000-0000-0000-0000-000000000001'::uuid),
    ('30000000-0000-0000-0000-000000000004'::uuid, '00000000-0000-0000-0000-000000000001'::uuid),
    ('30000000-0000-0000-0000-000000000005'::uuid, '00000000-0000-0000-0000-000000000001'::uuid),
    ('30000000-0000-0000-0000-000000000006'::uuid, '00000000-0000-0000-0000-000000000001'::uuid)
ON CONFLICT DO NOTHING;

-- 6. Seed các dòng registrations & attendances biên cho TC-REG-15, TC-QR-08, TC-4.6-15
-- TC-QR-08: WAITING_LIST
INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000002'::uuid, u.id, 'WAITING_LIST', now(), now()
FROM app_user u WHERE u.login_id = 'bruno_sv_waiting';

-- TC-REG-15: chỉ có dòng CANCELLED cũ
INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000003'::uuid, u.id, 'CANCELLED', now() - interval '15 minutes', now() - interval '15 minutes'
FROM app_user u WHERE u.login_id = 'bruno_sv_rereg';

-- TC-4.6-15 (a): REGISTERED + attendances PENDING và REJECTED (không có ACCEPTED)
INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000005'::uuid, u.id, 'REGISTERED', now() - interval '5 minutes', now() - interval '5 minutes'
FROM app_user u WHERE u.login_id = 'bruno_sv_hist_edge';

INSERT INTO attendances (id, event_id, student_id, method, status, recorded_at, created_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000005'::uuid, u.id, 'DYNAMIC_QR', 'PENDING', now() - interval '4 minutes', now() - interval '4 minutes'
FROM app_user u WHERE u.login_id = 'bruno_sv_hist_edge';

INSERT INTO attendances (id, event_id, student_id, method, status, recorded_at, created_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000005'::uuid, u.id, 'DYNAMIC_QR', 'REJECTED', now() - interval '3 minutes', now() - interval '3 minutes'
FROM app_user u WHERE u.login_id = 'bruno_sv_hist_edge';

-- TC-4.6-15 (b): 1 dòng CANCELLED (tạo trước) + 1 dòng REGISTERED (tạo sau)
INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000006'::uuid, u.id, 'CANCELLED', now() - interval '10 minutes', now() - interval '10 minutes'
FROM app_user u WHERE u.login_id = 'bruno_sv_hist_edge';

INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
SELECT gen_random_uuid(), '30000000-0000-0000-0000-000000000006'::uuid, u.id, 'REGISTERED', now() - interval '1 minute', now() - interval '1 minute'
FROM app_user u WHERE u.login_id = 'bruno_sv_hist_edge';

COMMIT;

SELECT login_id, status, class_id FROM app_user WHERE login_id LIKE 'bruno_%' ORDER BY login_id;
