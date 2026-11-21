-- =============================================================================
-- QNU-Events JMeter Performance Test Data Preparation (PostgreSQL 17)
-- =============================================================================
-- Tái sử dụng password_hash BCrypt cost 12 từ tài khoản sv001 (Demo@12345)
-- giúp khởi tạo nhanh 30..3.000 sinh viên giả lập trong < 1 giây (SRS §4.1.1).
--
-- Cách chạy (mặc định 100 SV; đổi :student_count = 3000 cho tải P1 đầy đủ):
--   psql -U qnu_events -d qnu_events -v student_count=100 -f prepare-perf-data.sql
-- =============================================================================

\if :{?student_count}
\else
\set student_count 100
\endif

BEGIN;

-- Bảo đảm lớp K49CNTT1 tồn tại
INSERT INTO org_class (id, code, name, faculty_name, cohort_year)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'K49CNTT1',
    'K49 Công nghệ thông tin 1',
    'Khoa Công nghệ Thông tin',
    2024
)
ON CONFLICT (code) DO NOTHING;

-- Tạo N tài khoản sinh viên perf_sv_0001 .. perf_sv_NNNN (idempotent)
WITH ref_hash AS (
    SELECT password_hash
    FROM app_user
    WHERE login_id = 'sv001'
    LIMIT 1
),
seq AS (
    SELECT generate_series(1, :student_count) AS n
),
inserted_users AS (
    INSERT INTO app_user (id, login_id, password_hash, full_name, student_code, class_id, status)
    SELECT
        gen_random_uuid(),
        'perf_sv_' || lpad(seq.n::text, 4, '0'),
        ref_hash.password_hash,
        'Sinh viên Perf ' || lpad(seq.n::text, 4, '0'),
        '4999' || lpad(seq.n::text, 6, '0'),
        '00000000-0000-0000-0000-000000000001'::uuid,
        'ACTIVE'
    FROM seq, ref_hash
    ON CONFLICT (login_id) DO NOTHING
    RETURNING id
)
INSERT INTO user_role (user_id, role)
SELECT u.id, 'STUDENT'
FROM app_user u
WHERE u.login_id LIKE 'perf_sv_%'
ON CONFLICT DO NOTHING;

COMMIT;

-- Kiểm tra số lượng tài khoản hiệu năng đã sẵn sàng
SELECT count(*) AS ready_perf_students
FROM app_user
WHERE login_id LIKE 'perf_sv_%' AND status = 'ACTIVE';
