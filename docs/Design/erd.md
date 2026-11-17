# Thiết kế CSDL (ERD) — Sprint 1

> Lược đồ cơ sở dữ liệu quan hệ cho Sprint 1 của hệ thống **QNU-Events**, được ánh xạ trực tiếp từ file migration Flyway [`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql).

---

## 1. Sơ đồ Thực thể - Mối quan hệ (Entity-Relationship Diagram)

```mermaid
erDiagram
    org_class ||--o{ app_user : "thuoc lop"
    org_class ||--o{ event_audience : "duoc phep tham gia"
    app_user ||--o{ user_role : "co vai tro"
    app_user ||--o{ auth_session : "so huu phien"
    app_user ||--o{ events : "tao boi (owner)"
    app_user ||--o{ events : "cong bo boi"
    app_user ||--o{ registrations : "dang ky"
    app_user ||--o{ attendances : "diem danh"
    app_user ||--o{ attendance_attempts : "lich su thu diem danh"
    app_user ||--o{ audit_logs : "thuc hien boi"
    events ||--o{ event_audience : "gioi han doi tuong"
    events ||--o{ registrations : "co danh sach dang ky"
    events ||--o{ attendances : "co danh sach diem danh"
    events ||--o{ attendance_attempts : "ghi nhan lan thu"

    org_class {
        uuid id PK "gen_random_uuid()"
        varchar code UK "Ma lop (unique)"
        varchar name "Ten lop"
        varchar faculty_name "Khoa"
        smallint cohort_year "Khoa hoc"
        timestamptz created_at
    }

    app_user {
        uuid id PK "gen_random_uuid()"
        varchar login_id UK "MSSV hoac username (unique)"
        varchar password_hash "BCrypt(12)"
        varchar full_name "Ho va ten"
        varchar student_code UK "Ma sinh vien"
        uuid class_id FK "Lop hoc"
        varchar status "ACTIVE | LOCKED"
        timestamptz created_at
        timestamptz updated_at
    }

    user_role {
        uuid user_id PK,FK
        varchar role PK "STUDENT | CLASS_REPRESENTATIVE | EVENT_OWNER | SYSTEM_ADMIN"
    }

    auth_session {
        uuid id PK "gen_random_uuid()"
        uuid user_id FK "Nguoi dung"
        timestamptz issued_at "Thoi diem cap"
        timestamptz expires_at "Het han (8h)"
        timestamptz revoked_at "Thu hoi luc dang xuat"
    }

    events {
        uuid id PK "gen_random_uuid()"
        uuid owner_id FK "Nguoi tao"
        varchar title "Tieu de (3-200 ky tu)"
        text description "Mo ta"
        varchar location "Dia diem to chuc"
        timestamptz start_at "Bat dau"
        timestamptz end_at "Ket thuc (end_at > start_at)"
        timestamptz registration_deadline "Han chot dang ky"
        timestamptz attendance_open_at "Mo diem danh"
        timestamptz attendance_close_at "Dong diem danh"
        varchar attendance_method "DYNAMIC_QR | GPS | EVIDENCE"
        int qr_rotation_seconds "Mac dinh 10s (5-120s)"
        int qr_grace_seconds "Grace period 3s (0-10s)"
        int quota_total "Tong suat (quota > 0)"
        int registered_count "So suat da dang ky (0 <= reg <= quota)"
        varchar activity_type "Loai hoat dong"
        int activity_points "Diem ren luyen (>= 0)"
        varchar status "DRAFT | PUBLISHED | CANCELLED"
        int version "Optimistic lock (@Version)"
        int previewed_version "Version da xem truoc"
        timestamptz published_at
        uuid published_by FK
        timestamptz created_at
        timestamptz updated_at
    }

    event_audience {
        uuid event_id PK,FK
        uuid class_id PK,FK
    }

    registrations {
        uuid id PK "gen_random_uuid()"
        uuid event_id FK
        uuid student_id FK
        varchar status "REGISTERED | WAITING_LIST | CANCELLED | NO_SHOW"
        timestamptz created_at
        timestamptz updated_at
    }

    attendances {
        uuid id PK "gen_random_uuid()"
        uuid event_id FK
        uuid student_id FK
        varchar method "DYNAMIC_QR | GPS | EVIDENCE..."
        varchar status "PENDING | ACCEPTED | REJECTED"
        timestamptz recorded_at "Thoi diem server ghi nhan"
        timestamptz created_at
    }

    attendance_attempts {
        uuid id PK "gen_random_uuid()"
        uuid event_id FK
        uuid student_id FK
        varchar method
        varchar idempotency_key "Idempotency-Key tu client"
        varchar outcome "ACCEPTED | REJECTED"
        varchar reason_code "Ma ly do (QR_EXPIRED, etc.)"
        boolean counted "Co tinh vao 3 lan thu hay khong"
        smallint http_status "Ma HTTP response luu tru"
        jsonb response_body "Noi dung response (khong chua token)"
        timestamptz created_at
    }

    audit_logs {
        uuid id PK "gen_random_uuid()"
        timestamptz occurred_at "Thoi diem server"
        uuid actor_id FK "Nguoi thuc hien"
        varchar actor_role "Vai tro cua actor"
        varchar action "EVENT_PUBLISH | REGISTRATION_CREATE | ATTENDANCE_QR_ACCEPT..."
        varchar target_type "EVENT | REGISTRATION | ATTENDANCE"
        varchar target_id "ID doi tuong"
        varchar outcome "SUCCESS | REJECTED | DENIED"
        varchar reason_code "Ma ly do neu that bai"
        jsonb details "Chi tiet bo sung (khong chua bi mat)"
        varchar request_id "TraceId"
    }
```

---

## 2. Các Ràng buộc & Chỉ mục Đặc biệt (Constraints & Indexes)

1. **`uq_registrations_active`**:
   - `CREATE UNIQUE INDEX uq_registrations_active ON registrations(event_id, student_id) WHERE status IN ('REGISTERED', 'WAITING_LIST');`
   - *Ý nghĩa:* Một sinh viên chỉ được giữ tối đa 1 suất kích hoạt (Registered/Waiting) trong một sự kiện. Nếu đã `CANCELLED`, cho phép đăng ký lại.

2. **`uq_attendances_valid`**:
   - `CREATE UNIQUE INDEX uq_attendances_valid ON attendances(event_id, student_id) WHERE status = 'ACCEPTED';`
   - *Ý nghĩa:* Một sinh viên chỉ có duy nhất 1 bản ghi điểm danh thành công (`ACCEPTED`) cho mỗi sự kiện.

3. **`attendance_attempts` Unique**:
   - `UNIQUE (event_id, student_id, idempotency_key)`
   - *Ý nghĩa:* Đảm bảo tính Idempotent tuyệt đối cho API điểm danh QR khi client gửi lại cùng một key.

4. **Tính Bất biến của `audit_logs` (Append-Only Trigger)**:
   - Trigger `trg_audit_logs_immutable` chặn hoàn toàn các câu lệnh `UPDATE` hoặc `DELETE` trên bảng `audit_logs`.
