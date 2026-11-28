# Nhật ký quyết định kỹ thuật (Decision Log)

> Quy tắc **R11 (`AGENTS.md`)**: mọi quyết định kỹ thuật chưa có trong SRS được ghi tại đây dưới dạng **Assumption (chưa duyệt)** cho đến khi nhóm/stakeholder phê duyệt và cập nhật đồng bộ vào [`docs/spec/QNU_Event_SRS.docx`](spec/QNU_Event_SRS.docx).  
> **Cập nhật 28/11/2026:** Toàn bộ quyết định kiến trúc & nghiệp vụ cho Sprint 1–3 (`D-1` → `D-10`, `A-001`, `A-01..06`, `FE-A01`, `ARCH-A01..07`, `A-QE8-01..11`, `A-AUTH-01..08`, `A-EVT-01..04`, `A-REV-01`, `TBD-EVT-03`) đã được phê duyệt thống nhất trước Sprint 2 (đồng bộ với `SRS v1.1`, [`Design/v2-schema-blueprint.md`](Design/v2-schema-blueprint.md) và [`api/error-codes.md`](api/error-codes.md)). Các mục thực sự phụ thuộc hạ tầng/nhà trường bên ngoài được giữ nguyên tại mục **Điểm mở**.

---

## A-001 · Bắt buộc truyền `version` khi `PUT /api/v1/events/{id}`

- **Ngày:** 15/11/2026 · **Phê duyệt:** 28/11/2026 (`D-1..D-10`)
- **Ticket:** QE-65 · **Agent:** A1 (Hoàng)
- **Bối cảnh:** `EventService.update` trước đây chấp nhận `version = null` và bỏ qua kiểm tra optimistic locking, trong khi hợp đồng API Sprint 1 ([docs/api/sprint1-contract.md](api/sprint1-contract.md)) quy định request `PUT` gồm `EventUpsert + version` kèm điều kiện "lệch version → 409". Hành vi cũ mở khe ghi đè nhầm khi hai người cùng sửa một bản nháp mà client quên gửi `version`.
- **Quyết định:**
  - `PUT` thiếu `version` → `400 VALIDATION_FAILED`, kèm `details.fields.version` giải thích lỗi.
  - `version` lệch giá trị hiện tại → `409 VERSION_CONFLICT` (giữ nguyên như trước).
- **Tác động:** FE form sửa sự kiện luôn đọc `version` từ `EventDetail` nên không bị ảnh hưởng; client cũ bỏ sót `version` sẽ nhận lỗi rõ ràng thay vì ghi đè ngầm. Hợp đồng API không đổi — đây là siết đúng cam kết đã đóng băng.
- **Trạng thái:** **`APPROVED (28/11/2026)`** — đồng bộ `SRS v1.1`.

---

## Nhóm 1 · Giả định nền tảng Sprint 1 & Quyết định chuẩn hóa Sprint 2–3 (`A-01..A-06`, `FE-A01`)

> **Nguồn:** [docs/agent/sprint1-rescue-plan.md](agent/sprint1-rescue-plan.md) (§6.9 & §7 T-05) và bộ quyết định chốt trước Sprint 2 ngày `28/11/2026` (`D-1` → `D-10`, `TBD-EVT-03`). Tất cả đã chuyển sang **`APPROVED (28/11/2026)`**.

| ID | Nội dung quyết định | Liên quan | Tác động & Ghi chú đồng bộ | Trạng thái |
|---|---|---|---|---|
| **A-01** | Sprint 1 giữ TTL phiên **8 giờ** (`28800s`). Từ Sprint 2 (`QE-30`), tích `"Lưu đăng nhập"` cấp **Refresh Token 7 ngày qua cookie `HttpOnly; Secure; SameSite=Lax`** (+ CSRF protection, max 5 phiên/user — `A-AUTH-07`). | `TBD-AUTH-01`, `QE-64`, `QE-30`, `D-10` | `AuthService` đọc TTL từ `app.security.jwt-access-token-validity-seconds`; `auth_session` bổ sung `refresh_token_hash`, `remember_me` ở `V2`. | **`APPROVED (28/11/2026)`** |
| **A-02** | Sprint 1 khi hết suất từ chối `409 CAPACITY_FULL`. Từ Sprint 2 (`QE-12/39`), bật **`WAITING_LIST` theo `event_pools`** (`1 seat = 1 pool`) + bộ đếm tuần tự `events.next_wait_order` → `registrations.wait_order`. | `QE-71`, `QE-39`, `TBD-REG-01`, `D-4` | Chi tiết tại [`Design/quota-rules.md`](Design/quota-rules.md) & [`Design/v2-schema-blueprint.md`](Design/v2-schema-blueprint.md). | **`APPROVED (28/11/2026)`** |
| **A-03** | Ràng buộc `startAt < endAt`, `attendanceOpenAt < attendanceCloseAt`, `registrationDeadline <= attendanceCloseAt`. Khi sự kiện bật `EVIDENCE`, bắt buộc `evidence_deadline >= attendance_close_at` (mặc định `+24h`). Lịch tuần/tháng `UC007` lọc chặt theo vai trò (`STUDENT` / `EVENT_OWNER` / `CVHT`). | `TBD-EVT-03`, `QE-65`, `UC007`, `R2` | Chốt đủ mốc thời gian & phạm vi lịch `UC007`, đóng `TBD-EVT-03` trong `SRS v1.1`. | **`APPROVED (28/11/2026)`** |
| **A-04** | Giới hạn 3 lần thử (`counted = true`) dùng chung cho nhóm xác thực tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`). **`EVIDENCE` là kênh dự phòng riêng**, không bị chặn bởi 3 lượt lỗi; QR/GPS hợp lệ ghi đè `PENDING` Evidence thành `REJECTED` (`SUPERSEDED_BY_ACCEPTED`). | `TBD-ATT-02`, `QE-56`, `QE-54`, `D-7` | Các lỗi tiền điều kiện (`ATTENDANCE_NOT_OPEN`, `NOT_REGISTERED`, `ALREADY_CHECKED_IN`) và lần thử thứ 4 (`409 ATTEMPT_LIMIT_REACHED`) có `counted = false`. | **`APPROVED (28/11/2026)`** |
| **A-05** | Grace period QR động = **3 giây** trên epoch `10s`. **Cấm tuyệt đối Offline Prefetch**; hỗ trợ **Student Offline Store-and-Forward** trên QR đang chiếu với cửa sổ **`OFFLINE_SYNC_MAX_WINDOW = 24 giờ`** sau `attendance_close_at` (`events.allow_offline_sync`, `is_offline_late_sync`). | `TBD-ATT-01`, `QE-56`, `QE-33`, `D-6` | Đồng bộ trong [`architecture/dynamic-qr-specification.md`](architecture/dynamic-qr-specification.md) §4 và `SRS v1.1`. | **`APPROVED (28/11/2026)`** |
| **A-06** | Áp dụng chính thức **5 vai trò** (`V2` thêm `CVHT`): `STUDENT`, `CLASS_REPRESENTATIVE` (BCS duyệt Evidence theo lớp), `CVHT` (xử lý Appeal Vòng 1 theo lớp & lịch lớp), `EVENT_OWNER` (sự kiện, Phúc khảo Vòng 2 & gỡ No-show), `SYSTEM_ADMIN` (duy nhất CRUD/Import dữ liệu nền, không duyệt Evidence/Appeal thường nhật). | `QE-6`, `QE-36`, `D-1`, `D-2`, `D-3` | Sprint 1 `V1` giữ 4 vai trò; `V2__sprint2_core_extensions.sql` bổ sung `'CVHT'`, `bcs_class_assignment`, `advisor_class_assignment`. | **`APPROVED (28/11/2026)`** |
| **FE-A01** | FE Sprint 1 lưu JWT và `user_info` trong bộ nhớ + `sessionStorage`. Từ Sprint 2 (`QE-30`), checkbox `"Lưu đăng nhập"` dùng Refresh Cookie `HttpOnly; Secure; SameSite=Lax` + CSRF + Access Token ngắn hạn. | `NFR-SEC-004`, `QE-30`, `D-10` | Giữ tương thích 45/45 test FE Sprint 1 (12 files) trước khi triển khai `QE-30`. | **`APPROVED (28/11/2026)`** |

---

## Nhóm 2 · Kiến trúc MVP & Module Boundaries — `QE-6` (`ARCH-A01..ARCH-A07`)

> **Nguồn:** [docs/architecture/mvp-architecture-and-boundaries.md](architecture/mvp-architecture-and-boundaries.md) (`QE-6`, `D-1`, `D-8`). Tất cả đã chuyển sang **`APPROVED (28/11/2026)`**.

| ID | Quyết định kỹ thuật | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **ARCH-A01** | Duy trì **Modular Monolith** (`Spring Boot 4.1.2` + `PostgreSQL 17`) làm kiến trúc triển khai duy nhất cho toàn bộ MVP; không tách microservices, không thêm Redis hay Kafka. | Phù hợp quy mô thí điểm 01 khoa (~3.000 sinh viên); toàn bộ giao dịch atomic quota/attendance xử lý trực tiếp trên PostgreSQL. | `QE-6`, `Proposal §3.3` | **`APPROVED (28/11/2026)`** |
| **ARCH-A02** | Áp dụng mẫu **Transactional Outbox** (`outbox_events` trên PostgreSQL + `@Scheduled` poller dùng `FOR UPDATE SKIP LOCKED`) cho post-commit side effects (notifications, no-show evaluation, retention cleanup). | Đáp ứng `NFR-QUAL-002` / `NFR-SAFE-004` (sống sót qua restart, không giữ row lock khi chạy I/O ngoài) mà không cần broker ngoài. | `QE-6`, `QE-14`, `QE-47` | **`APPROVED (28/11/2026)`** |
| **ARCH-A03** | Trừu tượng hóa lưu trữ minh chứng qua `EvidenceStoragePort` với adapter mặc định **Local Private Filesystem** (ngoài public root, UUID key, magic bytes JPEG/PNG `< 5 MB`, serve qua BE kiểm tra RBAC/scope lớp + `Cache-Control: private, no-store`), sẵn sàng cắm MinIO/S3; retention **`150 ngày`** từ `events.end_at` (loại trừ Appeal mở, xác minh backup trước khi xóa). | Đóng `TBD-SEC-02` và `TBD-DATA-03` theo quyết định `D-8`. | `QE-6`, `TBD-SEC-02`, `TBD-DATA-03`, `D-8` | **`APPROVED (28/11/2026)`** |
| **ARCH-A04** | Bổ sung role **`CVHT`** vào `user_role` CHECK constraint ở `V2__sprint2_core_extensions.sql` cùng `bcs_class_assignment`, `advisor_class_assignment` và `app_user.unit_id`. | Hợp nhất `V1` với SRS v1.1 / Figma (5 roles gồm `CVHT`), phân tách 3 tầng Evidence/Appeal (`D-1`, `D-2`) và scoped Admin (`REQ-AUTH-009`). | `QE-6`, `QE-35`, `QE-36`, `D-1`, `D-2` | **`APPROVED (28/11/2026)`** |
| **ARCH-A05** | Chuẩn hóa sai lệch hợp đồng Sprint 1: giữ `GET /api/v1/me/history` trả `Page<HistoryItem>`, thống nhất cấu hình QR mặc định về `10s`, và đồng bộ type FE `alreadyRegistered` thay `cached`. | **Đã triển khai hoàn tất trên `main` (`26/11/2026`).** | `QE-6`, `QE-72`, `QE-70` | **`APPROVED (28/11/2026)`** |
| **ARCH-A06** | Loại bỏ việc giữ outer DB connection lồng `REQUIRES_NEW` trên luồng tải cao `P1` (100 req/s); rà soát kích thước HikariCP pool (`maximum-pool-size: 30`) và kiểm thử tải tại `QE-46`. | Đã chuyển `RegistrationService` sang `TransactionTemplate`; tiếp tục tối ưu `AttendanceService` theo `A-QE8-01` ở `QE-54`. | `QE-6`, `QE-8`, `QE-46` | **`APPROVED (28/11/2026)`** |
| **ARCH-A07** | Bắt buộc fail-fast khi khởi động nếu thiếu hoặc yếu `JWT_SECRET` / `QR_SIGNING_SECRET` ở mọi profile ngoài `dev`/`demo`/`test`. | **Đã triển khai hoàn tất trên `main` (`24–26/11/2026` — `QE-90`, `BUG-07`).** | `QE-6`, `QE-64`, `R7` | **`APPROVED (28/11/2026)`** |

---

## Nhóm 3 · Attendance Consistency, Quota & Offline Sync — `QE-8` (`A-QE8-01..A-QE8-11`)

> **Nguồn:** [docs/architecture/attendance-consistency-design.md](architecture/attendance-consistency-design.md) (`QE-8`, `D-4` → `D-9`). Tất cả đã chuyển sang **`APPROVED (28/11/2026)`**.

| ID | Quyết định kỹ thuật | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-QE8-01** | Loại bỏ `REQUIRES_NEW` lồng trong transaction giữ row lock tại `RegistrationService` và `AttendanceService`; dùng mẫu `CheckInResult` commit attempt + attendance + success audit trong **1 transaction**, còn rejection audit ghi ngoài TX giữ lock. | Tránh split-brain và tránh giữ đồng thời 2 connection HikariCP trên luồng nóng `P1`. | `QE-8`, `QE-54`, `QE-94` | **`APPROVED (28/11/2026)`** |
| **A-QE8-02** | Bổ sung `payload_hash` (`SHA-256`) vào `attendance_attempts` ở `V2`; từ chối **`400 VALIDATION_FAILED`** (`IDEMPOTENCY_PAYLOAD_MISMATCH`) nếu tái sử dụng `Idempotency-Key` với payload hoặc method khác. | Ngăn reuse nhầm `Idempotency-Key` giữa các lần/phương thức check-in khác nhau (`QE-54`, `QE-33`). | `QE-8`, `QE-54`, `QE-33` | **`APPROVED (28/11/2026)`** |
| **A-QE8-03** | Hỗ trợ tùy chọn header `Idempotency-Key` trên Sprint 2 `POST /api/v1/events/{id}/registrations` và `DELETE /api/v1/events/{id}/registrations/me`. | Chống retry mạng ngay sau thao tác hủy gây tái đăng ký nhầm ngoài ý muốn. | `QE-8`, `QE-40`, `src/api/events.ts` | **`APPROVED (28/11/2026)`** |
| **A-QE8-04** | Ghi nhận bản ghi `ATTEMPT_LIMIT_REACHED` với `counted = false` (giữ tổng số `counted = true` `<= 3`) và trả `attemptCount`, `attemptsRemaining`, `maxAttempts`, `canRetry` trong `CheckInStatus`/error details. | Phục vụ hiển thị `Lượt gửi xác nhận: x/3` theo Figma & `GET /attendance/me` (`QE-98`). | `QE-8`, `QE-54`, `QE-98` | **`APPROVED (28/11/2026)`** |
| **A-QE8-05** | Chuẩn hóa **`409 ATTEMPT_LIMIT_REACHED`** (thay `429` ở `SRS UC015 5a` cũ) và dùng chung giới hạn 3 lần thử cho kênh tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`), giữ `EVIDENCE`/Appeal làm kênh dự phòng riêng (`D-7`). | Đóng `TBD-ATT-02` và đồng bộ `SRS v1.1` với `docs/api/error-codes.md`. | `QE-8`, `TBD-ATT-02`, `D-7` | **`APPROVED (28/11/2026)`** |
| **A-QE8-06** | Cấu hình `allowed_radius_meters` (`10–500m`) theo từng sự kiện, **mặc định `50m`** (`SRS REQ-EVT-009`; `60m` trên Figma là ví dụ cấu hình 1 sự kiện). Ngoài bán kính trả **`422 GPS_OUT_OF_RANGE`** (`counted = true`); trong bán kính nhưng đáng ngờ vẫn `ACCEPTED` + `suspicious_flag = true`. *(Chỉ ngưỡng `accuracy`/tốc độ chi tiết `TBD-ATT-03` giữ mở cho tinh chỉnh thử nghiệm.)* | Đóng xung đột `60m` vs `50m` (`D-7`) và vá mâu thuẫn ngoài bán kính. | `QE-8`, `QE-55`, `D-7` | **`APPROVED (28/11/2026)`** |
| **A-QE8-07** | Chốt **`OFFLINE_SYNC_MAX_WINDOW = 24 giờ`** sau `attendance_close_at` (kết thúc trước job `NO_SHOW`, hết hạn trả `422 OFFLINE_SYNC_WINDOW_EXPIRED`), **cấm Offline Prefetch**, hỗ trợ SV Store-and-Forward với cờ `events.allow_offline_sync` & `is_offline_late_sync = true`, chỉ cho SV đã `REGISTERED` trước (`D-6`). | Đóng `TBD-ATT-01` trong `SRS v1.1`. | `QE-8`, `QE-33`, `TBD-ATT-01`, `D-6` | **`APPROVED (28/11/2026)`** |
| **A-QE8-08** | Từ chối hủy `REGISTERED` khi `server_time >= start_at - 12h` bằng **`409 CANCELLATION_CUTOFF_PASSED`** (thay `400` ở `SRS v1.0` và chuẩn hóa theo `error-codes.md`); **miễn mốc 12h cho `WAITING_LIST`**; **cho phép đăng ký lại sau `CANCELLED`** nếu còn hạn (`D-5`). | Thống nhất `SRS v1.1`, `quota-rules.md`, `V1 uq_registrations_active` và `error-codes.md`. | `QE-8`, `QE-40`, `UC011`, `D-5` | **`APPROVED (28/11/2026)`** |
| **A-QE8-09** | Áp dụng mô hình **`event_pools`** (`1 seat = 1 pool`, `registrations.pool_id`) + bộ đếm tuần tự `events.next_wait_order` → `wait_order` + `FOR UPDATE SKIP LOCKED`; skip `#01` không đủ điều kiện khi promote; **tiếp tục auto-promote sau `registration_deadline`** nếu còn trước `start_at - 12h` (`D-4`, `D-5`). | Đóng `TBD-EVT-01`, `TBD-REG-01`, `TBD-REG-02` và thống nhất `QE-38`/`QE-8`. | `QE-8`, `QE-39`, `QE-40`, `D-4` | **`APPROVED (28/11/2026)`** |
| **A-QE8-10** | Đồng bộ mặc định `QR_EPOCH_SECONDS = 10s` khớp `V1`/FE, loại bỏ fallback secret ngoài `dev`/`demo`/`test` (`R7`), và giữ định dạng `qr1` cho Sprint 1–2 (nâng cấp `qr2.<eventId>.<epoch>.<nonce>.<sig>` khi triển khai phiên chiếu `nonce` nếu cần). | Khắc phục drift cấu hình `15s` vs `10s` (đã fix trên `main`). | `QE-8`, `QE-44`, `QE-70` | **`APPROVED (28/11/2026)`** |
| **A-QE8-11** | Job `NO_SHOW` (`QE-14`, 7 ngày vào `student_restrictions`) chỉ chạy sau **`MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`**; bỏ qua SV có `EVIDENCE` `PENDING`, `WAITING_LIST`, `CANCELLED` và tạm dừng khi có Appeal Vòng 1 hợp lệ; ban chồng nhau lấy **`MAX(current_banned_until, now + 7 days)`** (`D-9`). | Đóng `TBD-REG-03`, bảo vệ SV hợp lệ không bị phạt oan. | `QE-8`, `QE-14`, `TBD-REG-03`, `D-9` | **`APPROVED (28/11/2026)`** |

---

## Nhóm 4 · Auth & Session Lifecycle — `QE-36` (`A-AUTH-01..A-AUTH-08`)

> **Nguồn:** [docs/architecture/auth-and-session-lifecycle.md](architecture/auth-and-session-lifecycle.md) (`QE-36`, `D-10`). Tất cả đã chuyển sang **`APPROVED (28/11/2026)`**.

| ID | Quyết định kỹ thuật | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-AUTH-01** | Production fail-fast lúc khởi động nếu `JWT_SECRET` thiếu hoặc `< 32` byte; `AuthService` đọc TTL từ `app.security.jwt-access-token-validity-seconds`. | **Đã hiện thực trên `main` (`QE-90` / `BUG-07`, `24/11/2026`).** | `QE-36`, `QE-30`, `QE-90`, `R7` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-02** | Mọi thao tác `LOCK`, `GRANT_ROLE`, `REVOKE_ROLE`, thay đổi `class_id`/`unit_id`, và đổi/đặt lại mật khẩu đều thu hồi toàn bộ `auth_session` đang active trong cùng DB transaction (kèm tăng `auth_version`). | Bảo đảm hiệu lực tức thì theo `REQ-AUTH-010/011`. | `QE-36`, `QE-35`, `QE-30` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-03** | Ở `QE-30` (Sprint 2), chạy `BCrypt` match trước khi kiểm tra `UserStatus.LOCKED`; sai mật khẩu trên tài khoản `LOCKED` trả `401 INVALID_CREDENTIALS`, đúng mật khẩu mới trả `403 FORBIDDEN`. | Bịt lỗ hổng dò trạng thái khóa tài khoản tại `AuthService.java`; cập nhật `AuthIT` & Bruno test kèm theo trong PR `QE-30`. | `QE-36`, `QE-30`, `REQ-AUTH-004` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-04** | Giới hạn tối đa **5 phiên active đồng thời** mỗi tài khoản (thu hồi FIFO phiên cũ nhất khi vượt ngưỡng). | Cho phép SV/Owner dùng đồng thời laptop và điện thoại khi điểm danh. | `QE-36`, `QE-57`, `D-10` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-05** | Google OAuth2 dùng Authorization Code + PKCE (hoặc verify `id_token` qua Google JWKS tại BE), bắt buộc `email_verified = true` + khớp chính xác domain từ **`APP_GOOGLE_ALLOWED_DOMAINS=st.qnu.edu.vn,qnu.edu.vn`** (cấm `endsWith`), **chỉ liên kết `app_user` đã cấp trước**, bind nguyên tử `google_sub`, **mở cho mọi vai trò đã cấp tài khoản có email QNU** (`D-10`). | Đóng `TBD-AUTH-03` về mặt kiến trúc/allowlist; chỉ giữ mở thông tin vận hành Google Cloud Console (`Q-AUTH-03`). | `QE-36`, `QE-74`, `R9`, `D-10` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-06** | Bộ đếm brute-force (`5` lần sai liên tiếp → khóa tạm `15` phút theo `REQ-AUTH-015`) lưu tập trung trên PostgreSQL (`failed_login_attempts`, `locked_until`) kèm rate-limit theo IP và thông báo chống user enumeration. | Đáp ứng `NFR-QUAL-011` và chống DoS khóa nhầm hàng loạt. | `QE-36`, `QE-30`, `REQ-AUTH-015` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-07** | FE Sprint 1 tạm dùng `sessionStorage` (`FE-A01`). Từ Sprint 2 (`QE-30`), tích `"Lưu đăng nhập"` dùng Access Token ngắn hạn + **Refresh Token 7 ngày qua cookie `HttpOnly; Secure; SameSite=Lax`** lưu hash trong `auth_session` + bảo vệ CSRF. | Đóng `TBD-AUTH-01` phần cơ chế phiên & `"Lưu đăng nhập"`. | `QE-36`, `QE-30`, `NFR-SEC-004`, `D-10` | **`APPROVED (28/11/2026)`** |
| **A-AUTH-08** | Cấp tài khoản SV từ CSV (`QE-73`, chỉ `SYSTEM_ADMIN`): sinh mật khẩu tạm ngẫu nhiên mạnh 1 lần hoặc activation token 1 lần + `must_change_password = true` (không log credential — `R7`). Re-import mặc định **`allowClassTransfer = false`** (`D-10`); Admin chủ động bật cờ mới ghi đè lớp kèm `audit_logs` (`STUDENT_CLASS_TRANSFER`). | Đóng `TBD-DATA-01`, `TBD-AUTH-01`, thống nhất `Proposal §11.2` và `import-contract`. | `QE-36`, `QE-73`, `R7`, `D-3`, `D-10` | **`APPROVED (28/11/2026)`** |

---

## Điểm mở Vận hành & Hạ tầng Bên ngoài Còn giữ (`R12`)

> Phần lớn các điểm mở nghiệp vụ (`D-1` → `D-10`, `A-EVT-01..04`, `TBD-AUTH-01..03`, `TBD-EVT-01..03`, `TBD-REG-01..03`, `TBD-ATT-01..02`, `TBD-REV-01..03`, `TBD-DATA-01`, `TBD-DATA-03`, `TBD-SEC-02`) đã được nhóm/PO chốt thống nhất ngày `28/11/2026` và cập nhật vào [`docs/spec/QNU_Event_SRS.docx`](spec/QNU_Event_SRS.docx) (`v1.1`). Các hạng mục **thực sự phụ thuộc môi trường vận hành / nhà trường bên ngoài** được giữ nguyên trạng thái **Open** theo `R11`:
> - **`Q-AUTH-03` (Google Cloud Console):** Project Google Cloud, loại OAuth Consent Screen (`Internal` vs `External`), `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` và Redirect URI staging/production thực tế.
> - **Thông số tinh chỉnh hiện trường & hiệu năng (`SRS Appendix C`):** `TBD-ATT-03` (ngưỡng `accuracy` GPS & vận tốc dịch chuyển chi tiết của cờ đáng ngờ), `TBD-DATA-02` (thứ tự cột chi tiết của template Export Excel), `TBD-NFR-01..05` (hạ tầng & số liệu đo tải thực tế), `TBD-SEC-01` (cơ cấu xoay khóa định kỳ & cost factor sản xuất), `TBD-SEC-03..04` (pháp lý dữ liệu cá nhân của trường, MFA bắt buộc hay tùy chọn & kiểm thử thâm nhập), `TBD-OTH-01` (dung lượng bàn giao cuối kỳ).
> - **Lưu ý điều phối PR nhánh Sprint 2:** Các PR `#10/#11/#12` (`QE-12`, `QE-39`, `QE-40`) cần được rebase sau khi `A0` hợp nhất `V2__sprint2_core_extensions.sql` theo đúng [`docs/Design/v2-schema-blueprint.md`](Design/v2-schema-blueprint.md) (`R6`).

---

## Nhóm 5 · Event Lifecycle & Edge Case — `QE-15` (`A-EVT-01..04`)

> **Nguồn:** Task card QE-15 & quyết định chuẩn hóa `28/11/2026` (`R1`, `R2`); chi tiết quota tại [Design/quota-rules.md](Design/quota-rules.md). Tất cả đã **`APPROVED (28/11/2026)`**.

| ID | Nội dung quyết định | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-EVT-01** | **Sửa sự kiện đã `PUBLISHED`**: khi sự kiện **đã có registration**, chỉ cho sửa trường an toàn (`title`, `description`) **VÀ cho phép điều chỉnh `quotaTotal` / `event_pools.capacity`** theo đúng `BR-QTA-10` ([`quota-rules.md` §7](Design/quota-rules.md): không giảm thấp hơn `registered_count`/`used`; $\sum \text{capacity} \le \text{quotaTotal}$; **khi tăng sức chứa tự động kích hoạt auto-promote `WAITING_LIST` FIFO trong cùng giao dịch**), kèm `@Version`. **Cấm** đổi thời gian, địa điểm hoặc đối tượng lớp (`409 VERSION_CONFLICT`). Nếu `PUBLISHED` nhưng **chưa có registration nào**: cho sửa toàn bộ nhưng phải re-preview trước khi có hiệu lực. | Thống nhất `QE-15` với `quota-rules.md §7` (`R1`), tránh lỗi vận hành khi sự kiện đã mở đăng ký cần nới thêm phòng/suất mà không làm lệch lịch/lớp sinh viên đã giữ chỗ. | `QE-15`, `QE-32`, `TBD-EVT-02`, `quota-rules.md` | **`APPROVED (28/11/2026)`** |
| **A-EVT-02** | **"Xóa" sự kiện**: `DRAFT` chưa có đăng ký cho phép xóa cứng sau xác nhận (`REQ-EVT-015`); sự kiện đã `PUBLISHED` tuyệt đối không xóa cứng mà chuyển trạng thái `CANCELLED` (ẩn khỏi SV mặc định `404`, giữ nguyên registrations/attendances phục vụ lịch sử và không phạt No-show). | Ràng buộc FK từ `registrations`/`attendances` & lịch sử điểm rèn luyện (`EventIT` `QE-31`). | `QE-15`, `QE-31`, `EventStatus` | **`APPROVED (28/11/2026)`** |
| **A-EVT-03** | **Đóng/mở đăng ký sớm**: owner được đóng đăng ký sớm (`registration_closed_early = true`); được mở lại nếu chưa quá `registrationDeadline` gốc. Quá deadline gốc: không mở lại. | Bổ sung `events.registration_closed_early` trong `V2__sprint2_core_extensions.sql`. | `QE-15`, `REQ-REG-013`, `V2` | **`APPROVED (28/11/2026)`** |
| **A-EVT-04** | **Cửa sổ điểm danh cố định**: `attendanceOpenAt`/`attendanceCloseAt` không đóng/mở tay tùy tiện sau khi publish; mọi thay đổi đi qua luồng sửa sự kiện có kiểm soát (`A-EVT-01`). | Tránh lệch pha QR đang chiếu và `ATTENDANCE_NOT_OPEN`. | `QE-15`, `QE-56`, `A-05` | **`APPROVED (28/11/2026)`** |

---

## Nhóm 6 · Appeal Deadline, Thẩm quyền 3 tầng & SLA — `QE-41` (`A-REV-01`)

> **Nguồn:** Jira `QE-41` (`18/11/2026`) và quyết định `D-2`, `D-9` (`28/11/2026`). Đã **`APPROVED (28/11/2026)`**.

| ID | Nội dung quyết định | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-REV-01** | **Phân cấp 3 tầng & Thời hạn Appeal:** <br>• **Thẩm quyền:** **BCS** duyệt/từ chối Evidence lớp được phân công (`UC020`/`QE-43`) → **CVHT** tiếp nhận & xử lý **Appeal Vòng 1** theo lớp phụ trách (`UC022`/`QE-59`) → **EVENT_OWNER** xử lý **Phúc khảo Vòng 2** (quyết định cuối, `UC023–024`/`QE-60`) & gỡ chế tài No-show. (`SYSTEM_ADMIN` không phải business reviewer.)<br>• **Thời hạn:** Tối đa **01 đơn Vòng 1 trong vòng 5 ngày lịch từ `T0`**; tối đa **01 đơn Vòng 2 trong vòng 3 ngày lịch** từ kết quả Vòng 1; trần đóng tuyệt đối **30 ngày lịch từ `T0`** (giờ server **GMT+7**, kết thúc `23:59:59` ngày cuối).<br>• **Mốc `T0` chính thức:** `T0 = reviewed_at` đối với Evidence bị BCS từ chối; `T0 = attendance_close_at` (hoặc thời điểm ghi nhận chốt `NO_SHOW`) đối với lỗi QR/GPS hoặc khiếu nại No-show.<br>• **SLA & No-show:** Quá SLA xử lý chỉ cảnh báo nội bộ (**không** auto-approve/auto-reject); Round 1 hợp lệ tạm dừng bộ đếm `NO_SHOW` đến khi có quyết định cuối. | Đóng hoàn toàn `TBD-REV-01..03`, `D-2`, `D-9`; đồng bộ `SRS v1.1`, `traceability-matrix.md` và `business-state-transition-matrix.md` (xóa mốc `"48h"` cũ). | `QE-41`, `QE-22`, `QE-14`, `QE-43`, `QE-59`, `QE-60` | **`APPROVED (28/11/2026)`** |
