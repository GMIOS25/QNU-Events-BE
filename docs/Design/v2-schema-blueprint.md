# Blueprint Schema `V2` Thống nhất — Đầu vào Đóng băng cho Sprint 2–3 (`V2__sprint2_core_extensions.sql`)

> **Trạng thái:** ✅ **APPROVED (28/11/2026 — `D-1`, `D-3`, `D-4`, `D-6`, `D-7`, `D-8`, `D-9`, `D-10`, `A-EVT-03`, `TBD-EVT-03`)**  
> **Quy tắc `AGENTS.md` `R6`:** Schema `V1` ([`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql) — mô tả tại [`erd.md`](erd.md)) là bất biến. **Chỉ Agent nền tảng `A0` (`@GMIOS25`) mới được phép tạo file migration `V2__sprint2_core_extensions.sql` duy nhất**, tổng hợp toàn bộ lược đồ dưới đây để thay thế các file `V2__event_pools.sql` rời rạc trên các PR draft `#10/#11/#12`.  
> **Tham chiếu:** [`../decisions.md`](../decisions.md) · [`quota-rules.md`](quota-rules.md) · [`../architecture/attendance-consistency-design.md`](../architecture/attendance-consistency-design.md) · [`../architecture/auth-and-session-lifecycle.md`](../architecture/auth-and-session-lifecycle.md) · [`../import-contract/README.md`](../import-contract/README.md)

---

## 1. Xác thực, Vai trò & Phiên (`D-1`, `D-10`)

- **`user_role`:**
  - Cập nhật constraint `CHECK (role IN ('STUDENT', 'CLASS_REPRESENTATIVE', 'EVENT_OWNER', 'CVHT', 'SYSTEM_ADMIN'))` *(chính thức bổ sung vai trò thứ 5 `CVHT` — Cố vấn học tập)*.
- **`app_user`:**
  - `email varchar(255)` kèm unique index không phân biệt hoa thường:
    `CREATE UNIQUE INDEX uq_app_user_email_lower ON app_user (LOWER(email)) WHERE email IS NOT NULL;`
  - `google_sub varchar(128) UNIQUE` *(đối chiếu OAuth2 `QE-74`, không ghi đè âm thầm)*.
  - `must_change_password boolean NOT NULL DEFAULT false` *(bắt buộc đổi mật khẩu lần đầu cho tài khoản cấp qua CSV/reset — `QE-73`, `QE-30`)*.
  - `failed_login_attempts smallint NOT NULL DEFAULT 0`, `locked_until timestamptz` *(chống brute-force tập trung: 5 lần sai → khóa tạm 15 phút — `REQ-AUTH-015`, `QE-30`)*.
  - `auth_version int NOT NULL DEFAULT 0` *(phục vụ vô hiệu hóa phiên/claim tức thì khi đổi quyền hoặc khóa tài khoản)*.
  - `unit_id uuid` *(phạm vi đơn vị tổ chức của `EVENT_OWNER` theo `REQ-AUTH-009`, `QE-35`)*.
- **`auth_session`:**
  - `refresh_token_hash varchar(64)` *(SHA-256 của Refresh Token dùng cho luồng `"Lưu đăng nhập"` qua cookie `HttpOnly; Secure; SameSite=Lax`, TTL 7 ngày — `QE-30`)*.
  - `remember_me boolean NOT NULL DEFAULT false`.

---

## 2. Tổ chức, Phân công Lớp & Hồ sơ Sinh viên (`D-1`, `D-3`, `D-10`)

- **`org_department`:** `id uuid PK`, `code varchar(30) NOT NULL UNIQUE`, `name varchar(150) NOT NULL`, `parent_id uuid REFERENCES org_department(id)`, `created_at timestamptz NOT NULL DEFAULT now()`.
- **`org_specialization`:** `id uuid PK`, `code varchar(10) NOT NULL UNIQUE`, `name varchar(150) NOT NULL`, `abbreviation varchar(30)`, `department_id uuid REFERENCES org_department(id)`, `parent_id uuid REFERENCES org_specialization(id)`, `created_at timestamptz NOT NULL DEFAULT now()`.
- **`intake_program`:** `id uuid PK`, `code varchar(40) NOT NULL UNIQUE`, `intake_no smallint NOT NULL`, `start_year smallint NOT NULL`, `department_id uuid REFERENCES org_department(id)`, `specialization_id uuid REFERENCES org_specialization(id)`, `created_at timestamptz NOT NULL DEFAULT now()`.
- **`org_class` (mở rộng bảng `V1`):**
  - Thêm `department_id uuid REFERENCES org_department(id)`, `specialization_id uuid REFERENCES org_specialization(id)`.
- **`student_profile`:**
  - `user_id uuid PRIMARY KEY REFERENCES app_user(id)`, `personal_email varchar(150)` *(**không** đặt `UNIQUE`, tách biệt hoàn toàn với `app_user.email` QNU)*, `birth_date date`, `gender varchar(10)`, `id_card varchar(20)`, `study_status_code smallint`, `intake_no smallint`, `updated_at timestamptz NOT NULL DEFAULT now()`.
- **`bcs_class_assignment`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `user_id uuid NOT NULL REFERENCES app_user(id)`, `class_id uuid NOT NULL REFERENCES org_class(id)`, `assigned_by uuid NOT NULL REFERENCES app_user(id)`, `assigned_at timestamptz NOT NULL DEFAULT now()`, `revoked_at timestamptz`.
- **`advisor_class_assignment`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `user_id uuid NOT NULL REFERENCES app_user(id)`, `class_id uuid NOT NULL REFERENCES org_class(id)`, `assigned_by uuid NOT NULL REFERENCES app_user(id)`, `assigned_at timestamptz NOT NULL DEFAULT now()`, `revoked_at timestamptz`.

---

## 3. Sự kiện, Quota Pools & Waiting List (`D-4`, `D-5`, `D-6`, `D-7`, `A-EVT-03`, `TBD-EVT-03`)

- **`events` (mở rộng bảng `V1`):**
  - `next_wait_order bigint NOT NULL DEFAULT 0` *(bộ đếm tuần tự loại bỏ hòa timestamp khi vào `WAITING_LIST` — `TBD-REG-01`)*.
  - `registration_closed_early boolean NOT NULL DEFAULT false` *(owner đóng/mở đăng ký sớm trước `registration_deadline` — `A-EVT-03`)*.
  - `allow_offline_sync boolean NOT NULL DEFAULT true` *(bật/tắt chế độ SV Store-and-Forward ngoại tuyến theo từng sự kiện — `D-6`)*.
  - `gps_lat double precision`, `gps_lng double precision`, `allowed_radius_meters int DEFAULT 50 CHECK (allowed_radius_meters BETWEEN 10 AND 500)` *(bán kính GPS cấu hình theo sự kiện, mặc định `50m` khớp `SRS REQ-EVT-009` — `D-7`)*.
  - `evidence_deadline timestamptz`, kèm `CONSTRAINT ck_events_evidence_deadline CHECK (evidence_deadline IS NULL OR evidence_deadline >= attendance_close_at)` *(mặc định ứng dụng `+24 giờ` sau `attendance_close_at` khi bật `EVIDENCE`)*.
  - `cancel_reason varchar(500)` *(lý do hủy sự kiện đã công bố 10–500 ký tự — `QE-32`, `UC008`)*.
- **`event_pools`:** *(Mô hình `1 seat = 1 pool` theo `quota-rules.md`)*
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`
  - `event_id uuid NOT NULL REFERENCES events(id)`
  - `scope_type varchar(20) NOT NULL CHECK (scope_type IN ('LOP', 'KHOA', 'KHOI', 'CLB', 'ALL'))`
  - `scope_ref varchar(100) NOT NULL`
  - `capacity int NOT NULL CHECK (capacity >= 1)`
  - `used int NOT NULL DEFAULT 0 CHECK (used >= 0 AND used <= capacity)`
  - `priority int NOT NULL` *(số nhỏ hơn = ưu tiên cao hơn)*
  - `allow_overflow boolean NOT NULL DEFAULT false`
  - `is_mandatory boolean NOT NULL DEFAULT false`
  - `reserved_until timestamptz`
  - `created_at timestamptz NOT NULL DEFAULT now()`
  - Ràng buộc: `UNIQUE (event_id, scope_type, scope_ref)` và `CHECK (NOT is_mandatory OR NOT allow_overflow)`.
- **`registrations` (mở rộng bảng `V1`):**
  - `pool_id uuid REFERENCES event_pools(id)` *(định danh đúng 01 pool đã cấp hoặc hàng chờ của pool đó)*
  - `waiting_at timestamptz`, `wait_order bigint`
  - Index đôn suất: `CREATE INDEX ix_registrations_waitlist ON registrations (event_id, pool_id, wait_order ASC) WHERE status = 'WAITING_LIST';`

---

## 4. Điểm danh, GPS, Evidence, Appeal, No-show & Outbox (`D-2`, `D-6`, `D-7`, `D-8`, `D-9`)

- **`attendance_attempts` (mở rộng bảng `V1`):**
  - `payload_hash varchar(64)` *(SHA-256 chống reuse `Idempotency-Key` sai lệch payload/method — trả `400 VALIDATION_FAILED`)*.
- **`attendances` (mở rộng bảng `V1`):**
  - `gps_lat double precision`, `gps_lng double precision`, `gps_accuracy double precision`
  - `suspicious_flag boolean NOT NULL DEFAULT false`, `suspicious_reason varchar(200)`
  - `is_offline_late_sync boolean NOT NULL DEFAULT false` *(đánh dấu bản ghi đồng bộ ngoại tuyến nhận sau `attendance_close_at` trong cửa sổ 24h)*.
- **`attendance_evidences`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `attendance_id uuid NOT NULL REFERENCES attendances(id)`, `event_id uuid NOT NULL REFERENCES events(id)`, `student_id uuid NOT NULL REFERENCES app_user(id)`, `submitted_by uuid NOT NULL REFERENCES app_user(id)`, `is_proxy boolean NOT NULL DEFAULT false`, `storage_key varchar(255) NOT NULL`, `mime_type varchar(50) NOT NULL`, `file_size_bytes int NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes < 5242880)`, `reviewed_by uuid REFERENCES app_user(id)`, `reviewed_at timestamptz`, `rejection_reason varchar(255)`, `archived_at timestamptz`, `backup_verified_at timestamptz`, `deleted_at timestamptz`, `created_at timestamptz NOT NULL DEFAULT now()`.
  - *(Quy chuẩn ghi nhận lý do duyệt/ghi đè: Thông tin BCS duyệt/từ chối `reviewed_by`, `reviewed_at`, `rejection_reason` — bao gồm mã `SUPERSEDED_BY_ACCEPTED` khi QR/GPS hợp lệ ghi đè `PENDING` Evidence — được lưu trực tiếp trên `attendance_evidences.rejection_reason`; lý do thất bại ở các lượt tự động nằm trên `attendance_attempts.reason_code`.)*
- **`appeals`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `event_id uuid NOT NULL REFERENCES events(id)`, `student_id uuid NOT NULL REFERENCES app_user(id)`, `attendance_id uuid REFERENCES attendances(id)`, `round smallint NOT NULL CHECK (round IN (1, 2))`, `parent_appeal_id uuid REFERENCES appeals(id)`, `category varchar(40) NOT NULL`, `status varchar(30) NOT NULL`, `reason text NOT NULL`, `attachments jsonb`, `t0_anchor_at timestamptz NOT NULL`, `deadline_at timestamptz NOT NULL`, `decided_by uuid REFERENCES app_user(id)`, `decided_role varchar(30)`, `decision_note text`, `decided_at timestamptz`, `created_at timestamptz NOT NULL DEFAULT now()`, `UNIQUE (event_id, student_id, round)`.
- **`student_restrictions`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `student_id uuid NOT NULL REFERENCES app_user(id)`, `source_event_id uuid NOT NULL REFERENCES events(id)`, `banned_until timestamptz NOT NULL`, `paused_by_appeal_id uuid REFERENCES appeals(id)`, `lifted_by uuid REFERENCES app_user(id)`, `lifted_reason varchar(500)`, `lifted_at timestamptz`, `created_at timestamptz NOT NULL DEFAULT now()`.
- **`outbox_events`:**
  - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `event_type varchar(80) NOT NULL`, `payload jsonb NOT NULL`, `status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSED', 'DEAD_LETTER'))`, `retry_count smallint NOT NULL DEFAULT 0`, `next_attempt_at timestamptz NOT NULL DEFAULT now()`, `created_at timestamptz NOT NULL DEFAULT now()`, `processed_at timestamptz`.

---

## 5. Ánh xạ Tên Bảng Cũ sang Tên Chuẩn `V2`

Mọi tài liệu kiến trúc/kiểm thử không còn dùng tên giả định cũ mà thống nhất theo tên chuẩn sau:

| Tên giả định cũ đã loại bỏ | Tên chuẩn `V2` thống nhất |
|---|---|
| `event_quota_pool` / `event_quota_pools` + `registration_pool_allocations` | `event_pools` + `registrations.pool_id` (`1 seat = 1 pool`) |
| `student_penalties` | `student_restrictions` |
| `org_faculty` / `org_major` | `org_department` / `org_specialization` (+ `intake_program`) |
| `login_attempt_tracker` | `app_user.failed_login_attempts`, `app_user.locked_until` |
| `admin_role_audit` | `audit_logs` + `bcs_class_assignment` / `advisor_class_assignment` / `app_user.auth_version` |
| `oauth_identity_link` | `app_user.email` (`uq_app_user_email_lower`) + `app_user.google_sub` |
| `offline_sync_batches` | `attendance_attempts` + `attendances.is_offline_late_sync` |
| `proxy_attendance_items` | `attendance_evidences` (`submitted_by`, `is_proxy`) + `attendances` (`PROXY_EVIDENCE`) |
| `appeal_histories` | `appeals` (`round IN (1, 2)`, `parent_appeal_id`) + `audit_logs` |
| `retention_archive_log` | `attendance_evidences` (`archived_at`, `backup_verified_at`, `deleted_at`) + `audit_logs` |
| `location_radius_meters` | `events.allowed_radius_meters` |
