# Nhật ký quyết định kỹ thuật (Decision Log)

> Quy tắc R11 (AGENTS.md): mọi quyết định kỹ thuật chưa có trong SRS phải được ghi tại đây dưới dạng **Assumption (chưa duyệt)**. Không tự ý coi là đã chốt cho đến khi nhóm xác nhận và cập nhật vào SRS.

---

## A-001 · Bắt buộc truyền `version` khi `PUT /api/v1/events/{id}`

- **Ngày:** 15/11/2026
- **Ticket:** QE-65 · **Agent:** A1 (Hoàng)
- **Bối cảnh:** `EventService.update` trước đây chấp nhận `version = null` và bỏ qua kiểm tra optimistic locking, trong khi hợp đồng API Sprint 1 ([docs/api/sprint1-contract.md](api/sprint1-contract.md)) quy định request `PUT` gồm `EventUpsert + version` kèm điều kiện "lệch version → 409". Hành vi cũ mở khe ghi đè nhầm khi hai người cùng sửa một bản nháp mà client quên gửi `version`.
- **Quyết định (Assumption):**
  - `PUT` thiếu `version` → 400 `VALIDATION_FAILED`, kèm `details.fields.version` giải thích lỗi.
  - `version` lệch giá trị hiện tại → 409 `VERSION_CONFLICT` (giữ nguyên như trước).
- **Tác động:** FE form sửa sự kiện luôn đọc `version` từ `EventDetail` nên không bị ảnh hưởng; client cũ bỏ sót `version` sẽ nhận lỗi rõ ràng thay vì ghi đè ngầm. Hợp đồng API không đổi — đây là siết đúng cam kết đã đóng băng.
- **Trạng thái:** ASSUMPTION — chờ nhóm duyệt và ghi vào SRS.

---

## Nhóm 1 · Giả định nền tảng Sprint 1 (`A-01..A-06`, `FE-A01`)

> **Nguồn:** [docs/agent/sprint1-rescue-plan.md](agent/sprint1-rescue-plan.md) (§6.9 & §7 T-05, ghi nhận 09/11/2026 tại quyết định `D7`). Tất cả đều ở trạng thái **`ASSUMPTION — chưa duyệt`**.

| ID | Nội dung giả định | Liên quan | Tác động & Ghi chú hiện tại | Trạng thái |
|---|---|---|---|---|
| **A-01** | TTL phiên đăng nhập **8 giờ** (`28800s`), chưa triển khai cơ chế "ghi nhớ đăng nhập" ở BE Sprint 1. | `TBD-AUTH-01`, `QE-64`, `QE-90` | Đã sửa `AuthService` đọc từ `app.security.jwt-access-token-validity-seconds` (mặc định `28800s`) thay vì hardcode `8h` (24/11/2026). | **ASSUMPTION — chưa duyệt** |
| **A-02** | Sprint 1 khi hết suất sẽ từ chối `409 CAPACITY_FULL`, chưa bật Waiting List. | `QE-71`, `QE-39` | Bảng `registrations` trong `V1` đã có sẵn trạng thái `WAITING_LIST` để mở rộng ở Sprint 2. | **ASSUMPTION — chưa duyệt** |
| **A-03** | Ràng buộc `registrationDeadline <= attendanceCloseAt`; cửa sổ điểm danh cấu hình tự do để phục vụ demo Sprint 1. | `TBD-EVT-03`, `QE-65` | `EventValidator` đang kiểm tra mốc theo giả định này; cần chốt quan hệ thời gian với `startAt`/`endAt` ở Sprint 2 (`QE-15`). | **ASSUMPTION — chưa duyệt** |
| **A-04** | Chỉ các lỗi xác thực QR (`QR_*`) mới tính `counted = true` vào giới hạn 3 lần thử; các lỗi trước khi verify (`ATTENDANCE_NOT_OPEN`, `NOT_REGISTERED`, `ALREADY_CHECKED_IN`, `EVENT_NOT_PUBLISHED`) không tiêu tốn lượt thử. | `TBD-ATT-02`, `QE-56`, `QE-54` | Đang áp dụng tại `AttendanceService`; phạm vi chia sẻ 3 lượt giữa QR/GPS/Evidence cần chốt theo `TBD-ATT-02`. | **ASSUMPTION — chưa duyệt** |
| **A-05** | Grace period QR động mặc định = **3 giây** theo proposal; cửa sổ đồng bộ offline chưa xét trong Sprint 1. | `TBD-ATT-01`, `QE-56`, `QE-33` | Khớp `V1` (`qr_grace_seconds DEFAULT 3`) và `QrTokenServiceTest`; đồng bộ `QR_EPOCH_SECONDS=10` giữa DB, `.env.example`, `application.yml` và FE. | **ASSUMPTION — chưa duyệt** |
| **A-06** | Ánh xạ vai trò tạm thời Sprint 1: `CLASS_REPRESENTATIVE` = BCS; `EVENT_OWNER` = Admin/Event Owner; `CVHT` (cố vấn học tập trong SRS v1.0) chưa đưa vào `V1` (`user_role` CHECK constraint). | `sprint1-rescue-plan.md §3 C5`, `QE-6`, `QE-36` | Cần quyết định giữ 4 vai trò (`Proposal §2`) hay thêm `CVHT` (`SRS v1.0`) trong migration `V2+`. | **ASSUMPTION — chưa duyệt** |
| **FE-A01** | FE Sprint 1 lưu JWT access token và `user_info` trong bộ nhớ + `sessionStorage` (`qnu_events_access_token`, `qnu_events_user_info`), chấp nhận tạm rủi ro XSS ở mức MVP và không tự ý lưu `localStorage` mặc định. | `sprint1-rescue-plan.md §7 T-05`, `NFR-SEC-004` | Với checkbox `"Lưu đăng nhập"` trên `LoginPage`, mặc định vẫn giữ `sessionStorage` để tương thích test/E2E hiện có cho tới khi chuyển sang cookie `HttpOnly` (`A-AUTH-07`). | **ASSUMPTION — chưa duyệt** |

---

## Nhóm 2 · Kiến trúc MVP & Module Boundaries — `QE-6` (`ARCH-A01..ARCH-A07`)

> **Nguồn:** [docs/architecture/mvp-architecture-and-boundaries.md](architecture/mvp-architecture-and-boundaries.md) (18/11/2026, `QE-6`). Tất cả đều ở trạng thái **`ASSUMPTION — chưa duyệt`**.

| ID | Giả định kỹ thuật (`ASSUMPTION — chưa duyệt`) | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **ARCH-A01** | Duy trì **Modular Monolith** (`Spring Boot 4.1.2` + `PostgreSQL 17`) làm kiến trúc triển khai duy nhất cho toàn bộ MVP; không tách microservices, không thêm Redis hay Kafka. | Phù hợp quy mô thí điểm 01 khoa (~3.000 sinh viên); toàn bộ giao dịch atomic quota/attendance xử lý trực tiếp trên PostgreSQL. | `QE-6`, `Proposal §3.3` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A02** | Áp dụng mẫu **Transactional Outbox** (`outbox_events` trên PostgreSQL + `@Scheduled` poller dùng `FOR UPDATE SKIP LOCKED`) cho post-commit side effects (notifications, no-show evaluation, retention cleanup). | Đáp ứng `NFR-QUAL-002` / `NFR-SAFE-004` (sống sót qua restart, không giữ row lock khi chạy I/O ngoài) mà không cần broker ngoài. | `QE-6`, `QE-14`, `QE-47` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A03** | Trừu tượng hóa lưu trữ file minh chứng qua port nội bộ `EvidenceStoragePort`, hỗ trợ Local / MinIO / S3 với bucket riêng tư và URL có thời hạn ngắn được kiểm tra quyền. | Tránh khóa chặt vào một nhà cung cấp lưu trữ trong lúc `TBD-SEC-02` chưa được chốt. | `QE-6`, `TBD-SEC-02`, `NFR-SEC-007` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A04** | Bổ sung role `CVHT` vào `user_role` CHECK constraint ở migration `V2+` (nếu stakeholder duyệt SRS 5 roles) cùng các bảng gán phạm vi (`bcs_class_assignment`, `advisor_class_assignment`, `unit`). | Hợp nhất `V1` (4 roles) với SRS v1.0 / Figma (5 roles gồm `CVHT`) và scoped Admin (`REQ-AUTH-009`). | `QE-6`, `QE-35`, `QE-36` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A05** | Chuẩn hóa sai lệch hợp đồng Sprint 1: giữ `GET /api/v1/me/history` trả `Page<HistoryItem>` (cập nhật FE `fetchHistory()`/`HistoryPage.tsx`), thống nhất cấu hình QR mặc định về `10s`, và đồng bộ type FE `alreadyRegistered` thay `cached`. | Khắc phục lỗi breaking runtime ở `/history` trên `develop` và lệch cấu hình giữa BE `.env.example`/`application.yml` với `Event`/`V1`/FE. | `QE-6`, `QE-72`, `QE-70` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A06** | Loại bỏ việc giữ outer DB connection lồng `REQUIRES_NEW` trên luồng tải cao `P1` (100 req/s); rà soát kích thước HikariCP pool (`maximum-pool-size: 30`) và kiểm thử tải tại `QE-46`. | Ngăn nguy cơ cạn kiệt connection pool khi nhiều request đồng thời mở giao dịch ghi log/attempt lồng nhau. | `QE-6`, `QE-8`, `QE-46` | **ASSUMPTION — chưa duyệt** |
| **ARCH-A07** | Bắt buộc fail-fast khi khởi động nếu thiếu hoặc yếu `JWT_SECRET` / `QR_SIGNING_SECRET` ở mọi profile ngoài `dev`/`test` (loại bỏ chuỗi fallback dev trong `JwtService`, `SecurityConfiguration`, `QrTokenService`). | Đáp ứng `AGENTS.md` R7 trước khi đưa ứng dụng lên staging/production; giữ test/CI Sprint 1 không bị gãy trước khi cập nhật env CI. | `QE-6`, `QE-64`, `R7` | **ASSUMPTION — chưa duyệt** |

---

## Nhóm 3 · Attendance Consistency, Quota & Offline Sync — `QE-8` (`A-QE8-01..A-QE8-11`)

> **Nguồn:** [docs/architecture/attendance-consistency-design.md](architecture/attendance-consistency-design.md) (18/11/2026, `QE-8`). Tất cả đều ở trạng thái **`ASSUMPTION — chưa duyệt`**.

| ID | Giả định kỹ thuật (`ASSUMPTION — chưa duyệt`) | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-QE8-01** | Loại bỏ `REQUIRES_NEW` lồng trong transaction đang giữ row lock tại `RegistrationService` và `AttendanceService`; dùng mẫu `CheckInResult` để commit attempt + attendance + audit thành công trong **1 transaction duy nhất**, còn audit reject ghi sau khi transaction ngoài kết thúc. | Tránh split-brain và tránh giữ đồng thời 2 connection HikariCP trên luồng nóng `P1`. *(Cập nhật 24/11/2026: `RegistrationService` đã chuyển sang `TransactionTemplate` ghi audit sau commit/rollback để fix `QE-94` / `TC-REG-20`; `AttendanceService` tiếp tục tối ưu ở Sprint 2.)* | `QE-8`, `QE-54`, `QE-94`, `AuditService` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-02** | Bổ sung `payload_hash` (`SHA-256`) vào `attendance_attempts` ở migration `V2+`; từ chối `400 VALIDATION_FAILED` (`IDEMPOTENCY_PAYLOAD_MISMATCH`) nếu tái sử dụng `Idempotency-Key` với payload hoặc method khác. | Ngăn reuse nhầm `Idempotency-Key` giữa các lần/phương thức check-in khác nhau (`QE-54`, `QE-33`). | `QE-8`, `QE-54`, `QE-33` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-03** | Hỗ trợ tùy chọn header `Idempotency-Key` trên Sprint 2 `POST /api/v1/events/{id}/registrations` và `DELETE /api/v1/events/{id}/registrations/me`. | Chống retry mạng ngay sau thao tác hủy gây tái đăng ký nhầm ngoài ý muốn. | `QE-8`, `QE-40`, `src/api/events.ts` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-04** | Ghi nhận bản ghi `ATTEMPT_LIMIT_REACHED` với `counted = false` (giữ tổng số `counted = true` trong DB `<= 3`) và trả `attemptsUsed`, `attemptsRemaining`, `maxAttempts` trong DTO/error details. | Phục vụ hiển thị `Lượt gửi xác nhận: x/3` theo Figma scanner và đảm bảo bất biến giới hạn 3 lần thử. | `QE-8`, `QE-54`, Figma | **ASSUMPTION — chưa duyệt** |
| **A-QE8-05** | Giữ mã lỗi `409 ATTEMPT_LIMIT_REACHED` theo hợp đồng Sprint 1 (thay vì `429` trong `SRS UC015 5a`) và dùng chung giới hạn 3 lần thử cho các kênh tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`), giữ `EVIDENCE`/Appeal làm kênh dự phòng. | Nhất quán hợp đồng `sprint1-contract.md` và tránh khóa kênh nộp minh chứng khi sinh viên gặp sự cố thiết bị; chờ chốt `TBD-ATT-02`. | `QE-8`, `TBD-ATT-02`, `QE-54` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-06** | Cho phép cấu hình `allowed_radius_meters` theo từng sự kiện (`REQ-ATT-007`), đề xuất mặc định `60m` (theo Figma; Proposal/SRS gợi ý `40–50m`); ngưỡng cờ `suspicious_flag` (`accuracy`, vận tốc dịch chuyển) chờ chốt `TBD-ATT-03`. | Dung hòa khác biệt giữa Figma (`60m`) và Proposal/SRS (`40–50m`) bằng cấu hình theo sự kiện. | `QE-8`, `QE-55`, `TBD-ATT-03` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-07** | Đề xuất cửa sổ đồng bộ offline (`OFFLINE_SYNC_MAX_WINDOW`) là **24 giờ** sau `attendance_close_at` (phải kết thúc trước job `NO_SHOW`), tách biệt hoàn toàn với 13s QR online; `OFFLINE_QR` chỉ áp dụng cho sinh viên đã `REGISTERED` trước đó. | Đáp ứng cảnh báo `TBD-ATT-01` của SRS rằng 13s QR online không phải là offline window, đồng thời giới hạn rủi ro replay ảnh chụp QR offline. | `QE-8`, `QE-33`, `TBD-ATT-01` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-08** | Từ chối hủy đăng ký khi `server_time >= start_at - 12h` bằng `409 CANCELLATION_CUTOFF_PASSED` (thay vì `400` ở `UC011 3a.1`) và miễn ngưỡng 12h cho sinh viên tự rút khỏi `WAITING_LIST`. | Đồng bộ nhóm lỗi trạng thái `409` của `REGISTRATION_CLOSED` và cho phép rút khỏi hàng chờ trước giờ diễn ra. | `QE-8`, `QE-40`, `UC011` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-09** | Cấp phát `wait_order` bằng bộ đếm tuần tự trên `events` (giải quyết tie-breaking `TBD-REG-01`); quy tắc kết hợp nhiều pool (`AND`/`OR`) và việc giữ hay loại ứng viên `#01` khi bị `NO_SHOW` ban / promote sau `registration_deadline` chờ chốt `TBD-EVT-01`, `TBD-REG-02`. | Tránh trùng thứ tự FIFO khi nhiều request đồng thời rơi vào `WAITING_LIST`. | `QE-8`, `QE-39`, `QE-40`, `TBD-EVT-01` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-10** | Đổi mặc định `QR_EPOCH_SECONDS` / `app.qr.epoch-seconds` từ `15` về `10` khớp `V1`/FE, loại bỏ fallback secret ngoài `dev`/`test` (`R7`), và nâng cấp định dạng token lên `qr2.<eventId>.<epoch>.<nonce>.<sig>` nếu `QE-44` yêu cầu `random_nonce`. | Khắc phục drift cấu hình `15s` vs `10s` và chuẩn bị cho yêu cầu `random_nonce` ở `SRS UC015`. | `QE-8`, `QE-44`, `QE-70` | **ASSUMPTION — chưa duyệt** |
| **A-QE8-11** | Job `NO_SHOW` (`QE-14`, chặn đăng ký 7 ngày) chỉ chạy sau mốc `attendance_close_at + OFFLINE_SYNC_MAX_WINDOW` và không đánh `NO_SHOW` đối với sinh viên đang có `EVIDENCE` ở trạng thái `PENDING`. | Tránh phạt nhầm sinh viên đang chờ BCS duyệt minh chứng hoặc đang trong cửa sổ đồng bộ offline (`TBD-REG-03`). | `QE-8`, `QE-14`, `TBD-REG-03` | **ASSUMPTION — chưa duyệt** |

---

## Nhóm 4 · Auth & Session Lifecycle — `QE-36` (`A-AUTH-01..A-AUTH-08`)

> **Nguồn:** [docs/architecture/auth-and-session-lifecycle.md](architecture/auth-and-session-lifecycle.md) (18/11/2026, `QE-36`). Tất cả đều ở trạng thái **`ASSUMPTION — chưa duyệt`**.

| ID | Giả định kỹ thuật (`ASSUMPTION — chưa duyệt`) | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-AUTH-01** | Production phải fail-fast khi khởi động nếu `JWT_SECRET` thiếu hoặc `< 32` byte (loại bỏ dev fallback trong `JwtService` và `SecurityConfiguration`); `AuthService` đọc TTL từ `app.security.jwt-access-token-validity-seconds` thay vì hardcode `8h`. | *(Cập nhật 24/11/2026: Đã hiện thực fail-fast ngoài profile `dev`/`demo`/`test` và đọc TTL từ config theo `QE-90` / `BUG-07`.)* | `QE-36`, `QE-30`, `QE-90`, `R7` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-02** | Mọi thao tác `LOCK`, `GRANT_ROLE`, `REVOKE_ROLE`, thay đổi `class_id`/`unit_id`, và đổi/đặt lại mật khẩu đều thu hồi toàn bộ `auth_session` đang active trong cùng DB transaction. | Bảo đảm hiệu lực tức thì theo `REQ-AUTH-010/011` dù `CurrentUserProvider` đọc claim tĩnh từ JWT. | `QE-36`, `QE-35`, `QE-30` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-03** | Ở luồng login mật khẩu, luôn chạy `BCrypt` match trước khi kiểm tra `UserStatus.LOCKED`; sai mật khẩu trên tài khoản `LOCKED` trả `401 INVALID_CREDENTIALS` thay vì `403 FORBIDDEN`. | Bịt lỗ hổng dò trạng thái khóa tài khoản tại `AuthService.java:51-53`. *(Sprint 1 giữ nguyên hành vi cũ để tương thích `AuthIT` & Bruno `TC-AUTH-WB-02`; áp dụng ở Sprint 2 kèm cập nhật test.)* | `QE-36`, `QE-30`, `QE-90`, `REQ-AUTH-004` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-04** | Giới hạn tối đa **5 phiên active đồng thời** mỗi tài khoản (thu hồi FIFO phiên cũ nhất khi vượt ngưỡng) thay vì giới hạn 1 phiên duy nhất. | Cho phép sinh viên/chủ sự kiện đăng nhập đồng thời trên laptop và điện thoại khi điểm danh mà không bị văng phiên. | `QE-36`, `QE-57` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-05** | Google OAuth2 dùng Authorization Code + PKCE (hoặc verify `id_token` qua Google JWKS phía backend), bắt buộc `email_verified=true` + domain trong allowlist tường minh, ánh xạ vào `app_user` đã cấp trước và bind nguyên tử `google_sub` (`UNIQUE`) + `LOWER(email)` (`UNIQUE`) trong `V2`, không ghi đè âm thầm. | Tuân thủ `REQ-AUTH-002`, `NFR-SEC-001`, không tự cấp tài khoản hay leo thang quyền, chống chiếm tài khoản nếu email được tái cấp phát. | `QE-36`, `QE-74`, `R9` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-06** | Bộ đếm brute-force (`5` lần sai liên tiếp → khóa `15` phút theo `REQ-AUTH-015`) lưu tập trung trên PostgreSQL (hoặc store dùng chung) kèm rate-limit theo IP và thông báo chống user enumeration. | Bảo đảm nhất quán khi scale đa instance (`NFR-QUAL-011`) và giảm rủi ro DoS khóa tài khoản hàng loạt. | `QE-36`, `QE-30`, `REQ-AUTH-015` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-07** | Lộ trình lưu token phía FE: Sprint 1 tạm dùng `sessionStorage` (`FE-A01`); Production chuyển sang Access Token ngắn hạn + phiên `"Lưu đăng nhập"` qua cookie `HttpOnly; Secure; SameSite=Lax` kèm bảo vệ CSRF (`NFR-SEC-004`). | Giảm bề mặt tấn công XSS so với lưu token dài hạn trong Web Storage. | `QE-36`, `QE-30`, `NFR-SEC-004` | **ASSUMPTION — chưa duyệt** |
| **A-AUTH-08** | Cấp phát tài khoản sinh viên từ CSV (`QE-73`): sinh mật khẩu tạm ngẫu nhiên hoặc token kích hoạt 1 lần qua email QNU + cờ `must_change_password = true`; tuyệt đối không dùng mật khẩu mặc định chung và không ghi credential vào `audit_logs` hay file kết quả. | Đáp ứng `R7` và `NFR-SEC-003` trong lúc chờ stakeholder chốt kênh cấp phát chính thức tại `TBD-AUTH-01`. | `QE-36`, `QE-73`, `R7` | **ASSUMPTION — chưa duyệt** |

---

## Điểm mở chờ Stakeholder (`SRS Appendix C` & `Q-AUTH-01..07` — R12)

> Theo **R12 (`AGENTS.md`)**, các mục sau hiện **CHƯA ĐƯỢC CHỐT** (trạng thái **Open**) và không được tự coi là đặc tả chính thức cho đến khi Stakeholder / PO xác nhận và cập nhật `docs/QNU_Event_SRS.docx`:
> - **Auth & Google SSO (`Q-AUTH-01..07` trong [auth-and-session-lifecycle.md](architecture/auth-and-session-lifecycle.md)):** Domain email QNU chính thức (`@st.qnu.edu.vn` / `@qnu.edu.vn`, `TBD-AUTH-03`), phạm vi role dùng Google OAuth2, Google Cloud project & consent screen, cơ chế cấp/reset mật khẩu ban đầu & `"Lưu đăng nhập"` (`TBD-AUTH-01`), mở khóa tài khoản (`TBD-AUTH-02`), cấu hình brute-force phân tán (`TBD-SEC-01`), MFA cho Admin (`TBD-SEC-04`), và quyết định thêm role `CVHT` (`SRS v1.0`) + quyền màn Import SV (`SYSTEM_ADMIN` vs Figma).
> - **Event, Registration, Attendance, Review & Data (`SRS Appendix C`):** `TBD-EVT-01..03`, `TBD-REG-01..03`, `TBD-ATT-01..03` (đặc biệt `OFFLINE_SYNC_MAX_WINDOW` tách biệt khỏi 13s QR online và phạm vi 3 attempt), `TBD-REV-01..03`, `TBD-DATA-01..03`, `TBD-NFR-01..03`.
> - **Ghi chú PR đang mở:** Các PR #10/#11/#12 (`QE-12`, `QE-39`, `QE-40`) có đề xuất thêm mục `A-003` và file `V2__event_pools.sql` chưa được merge vào `develop` (đang bị chặn bởi rà soát quy tắc R6 về migration `V2` của A0; xem chi tiết tại [docs/scrum/sprint1-tech-lead-review.md](scrum/sprint1-tech-lead-review.md)).

---

## Nhóm 5 · Event Lifecycle & Edge Case — `QE-15` (`A-EVT-01..04`)

> **Nguồn:** Task card QE-15 Sprint 1 (20/11/2026, A1 Hoàng). Chốt các edge case vòng đời sự kiện để đưa vào SRS. Tất cả **`ASSUMPTION — chưa duyệt`**; đặc tả chi tiết quota nhiều tầng xem [Design/quota-rules.md](Design/quota-rules.md) (QE-38).

| ID | Nội dung giả định | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-EVT-01** | **Sửa sự kiện đã PUBLISHED**: chỉ cho sửa field an toàn (`title`, `description`) kèm kiểm `version` (optimistic lock); cấm đổi thời gian/địa điểm/quota/audience khi sự kiện **đã có registration** (409 `VERSION_CONFLICT`). Nếu chưa có registration nào: cho sửa toàn bộ nhưng phải xem trước lại (re-preview) trước khi thay đổi có hiệu lực. | Hành vi hiện tại (`EventService.update`) cấm sửa hoàn toàn khi PUBLISHED — quá nghiêm ngặt với chỉnh tiêu đề/mô tả (lỗi chính tả), nhưng mở toàn bộ sẽ làm lệch điều kiện đăng ký mà sinh viên đã giữ chỗ. Giải phóng Sprint 2 (story "Sửa/xóa event đã publish" trong backlog_solutions §3.1). | `QE-15`, `TBD-EVT-02`, `backlog_solutions §3.1` | **ASSUMPTION — chưa duyệt** |
| **A-EVT-02** | **"Xóa" sự kiện**: không xóa cứng — chuyển trạng thái `CANCELLED` (status đã có trong V1 và hợp đồng API). Sự kiện CANCELLED: ẩn khỏi danh sách/chi tiết sinh viên (mặc định 404), không nhận đăng ký mới; các registration cũ **giữ nguyên** phục vụ lịch sử và tính điểm rèn luyện. | Ràng buộc FK từ `registrations`/`attendances` + yêu cầu lịch sử điểm rèn luyện không được mồ côi. `EventStatus.CANCELLED` đã được QE-31 xử lý ở danh sách/chi tiết (EventIT #19). | `QE-15`, `QE-31`, `EventStatus` | **ASSUMPTION — chưa duyệt** |
| **A-EVT-03** | **Đóng/mở đăng ký sớm**: owner được đóng registration trước `registrationDeadline` (khóa sớm khi đủ người/thay đổi kế hoạch); được **mở lại** nếu chưa quá `registrationDeadline` gốc. Quá deadline gốc: không mở lại dưới mọi hình thức (tránh thay đổi điều kiện sau khi sinh viên đã bị từ chối). | Cần endpoint riêng Sprint 2 (hiện chỉ có deadline tĩnh); quy tắc `registrationOpen` server tính đã có sẵn (`EventService.toDetail`) nên chỉ thêm cờ `registration_closed` + điều kiện tính. | `QE-15`, `REQ-REG-013` (đề xuất), Sprint 2 backlog | **ASSUMPTION — chưa duyệt** |
| **A-EVT-04** | **Cửa sổ điểm danh cố định**: `attendanceOpenAt`/`attendanceCloseAt` không đóng/mở tay sau khi publish — QR động phụ thuộc cửa sổ này (epoch + grace); mọi thay đổi phải qua luồng sửa sự kiện (A-EVT-01) với re-preview. | Tránh lệch pha giữa QR đang chiếu và cửa sổ server kiểm tra (`ATTENDANCE_NOT_OPEN`); nhất quán với A-05 (grace 3s) và thiết kế attendance-consistency. | `QE-15`, `QE-56`, `A-05`, `A-QE8-*` | **ASSUMPTION — chưa duyệt** |

---

## Nhóm 6 · Appeal Deadline & SLA — `QE-41` (`A-REV-01`)

> **Nguồn:** Jira `QE-41` (comment `#10083`, ngày 18/11/2026, A4 Nguyễn Nhật Khánh), đồng bộ vào Decision Log ngày 24/11/2026 theo R11.

| ID | Nội dung giả định | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-REV-01** | **Thời hạn nộp & xử lý khiếu nại (Appeal Deadline):** Sinh viên được gửi tối đa **01 appeal Round 1 trong vòng 5 ngày lịch kể từ `T0`** và tối đa **01 appeal Round 2 trong vòng 3 ngày lịch** kể từ thời điểm nhận thông báo kết quả Round 1. Deadline tính theo giờ máy chủ **GMT+7** và kết thúc lúc **`23:59:59`** ngày cuối cùng; mọi request quá hạn bị từ chối và không tạo hồ sơ. Tổng thời gian xử lý không vượt mốc đóng tuyệt đối **30 ngày lịch kể từ `T0`**. SLA xử lý của CVHT/Admin/Event Owner là SLA nội bộ (quá SLA chỉ cảnh báo/leo thang, **không** auto-approve/auto-reject). Với appeal liên quan đến `no-show restriction`, Round 1 hợp lệ tạm dừng bộ đếm restriction cho đến khi có kết quả `Accepted`/`Rejected`. | Làm rõ điểm mở `TBD-REV-02` / `TBD-REV-03` giữa Proposal và SRS trước khi triển khai `QE-22`, `QE-59`, `QE-60` và tích hợp chế tài `QE-14` ở Sprint 2–3. Cần PO xác nhận định nghĩa chính thức mốc `T0` (thời điểm công bố kết quả điểm danh hay `attendance_close_at`) và cập nhật vào SRS. | `QE-41`, `QE-22`, `QE-14`, `QE-25`, `UC022`, `UC023`, `TBD-REV-02` | **ASSUMPTION — chưa duyệt** |


---

## Nhóm 7 · Điểm danh GPS & Kiểm tra Bán kính — `QE-55` (`A-QE55-01..04`)

> **Nguồn:** Triển khai `QE-55` (Parent `QE-17`, Agent A3 — Đan `@Dan28-05`, ngày 28/11/2026). Tất cả đều ở trạng thái **`ASSUMPTION — chưa duyệt`** chờ PO / Tech Lead xác nhận.

| ID | Nội dung giả định (`ASSUMPTION — chưa duyệt`) | Lý do & Tác động | Liên quan | Trạng thái |
|---|---|---|---|---|
| **A-QE55-01** | Bán kính điểm danh GPS mặc định khi chủ sự kiện không nhập là **`60 m`** (khớp Figma; cấu hình qua `app.gps.default-radius-meters: 60`), nhưng mỗi sự kiện vẫn cấu hình riêng `allowed_radius_meters`. | Giải quyết chênh lệch tham chiếu giữa Proposal/SRS (`40–50m`) và Figma (`60m`). | `QE-55`, `QE-17`, `A-QE8-06`, `REQ-ATT-007` | **ASSUMPTION — chưa duyệt** |
| **A-QE55-02** | Trên Web MVP, loại bỏ trường `isMocked` khỏi payload API (do trình duyệt `navigator.geolocation` không cung cấp tín hiệu này đáng tin cậy); tiêu chí gắn cờ `suspicious_flag = true` (`LOW_GPS_ACCURACY`) là khi **`accuracyMeters > 100.0 m`** (cấu hình qua `app.gps.max-trusted-accuracy-meters: 100.0`). Khi trong bán kính nhưng vượt ngưỡng sai số, vẫn trả `200 ACCEPTED` (không lộ cờ `suspicious` về response sinh viên) và lưu cờ trong DB/Audit Log để BTC/BCS hậu kiểm. | Khớp `BR-ATT-07` và `SRS UC016` Bước 8a, đồng thời ngăn client thăm dò ngưỡng phát hiện gian lận qua DevTools. | `QE-55`, `BR-ATT-07`, `TBD-ATT-03`, `UC016` | **ASSUMPTION — chưa duyệt** |
| **A-QE55-03** | Điểm danh GPS nằm ngoài bán kính cho phép (`422 GPS_OUT_OF_RANGE`) được tính **`counted = true`** vào quỹ tối đa **3 lần thử** dùng chung trên mỗi `(event_id, student_id)`; lần thứ 4 bị chặn với `409 ATTEMPT_LIMIT_REACHED` (`counted = false`). Các lỗi quyền định vị trên trình duyệt (`PERMISSION_DENIED`, `POSITION_UNAVAILABLE`, `TIMEOUT`) chặn cục bộ ở FE, không gọi API và không trừ lượt. | Đồng bộ cơ chế giới hạn 3 lần thử của `QE-54` giữa Dynamic QR và GPS theo `A-QE8-05` / `TBD-ATT-02`. | `QE-55`, `QE-54`, `TBD-ATT-02`, `UC016` | **ASSUMPTION — chưa duyệt** |
| **A-QE55-04** | Chuẩn hóa đường dẫn API điểm danh GPS thành **`POST /api/v1/events/{id}/attendance/gps`** (bắt buộc header `Idempotency-Key`) thay cho đường dẫn `/api/v1/attendance/verify-gps` trong bản nháp `SRS UC016` Bước 4. | Đồng bộ 100% quy ước RESTful theo tài nguyên sự kiện của Sprint 1 (`POST /api/v1/events/{id}/attendance/qr`, `GET /api/v1/events/{id}/attendance/me`). | `QE-55`, `UC016`, `sprint1-contract.md` | **ASSUMPTION — chưa duyệt** |
