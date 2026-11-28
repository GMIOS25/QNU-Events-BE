# QNU Events Backend

Backend Spring Boot cho hệ thống quản lý đăng ký và xác thực tham gia hoạt động sinh viên Đại học Quy Nhơn, xây dựng theo [proposal v2](docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx) và [đặc tả SRS v1.1](docs/spec/QNU_Event_SRS.docx).

Trên nhánh `main` và `develop` hiện tại đã triển khai hoàn chỉnh lát cắt dọc Sprint 1 (`V1__sprint1_core_schema.sql`, xác thực JWT HS256 + thu hồi phiên `auth_session`, phân quyền `SecurityConfiguration`, CRUD/preview/publish/lọc sự kiện, giữ suất nguyên tử, Dynamic QR HMAC-SHA256 + `Idempotency-Key` + giới hạn 3 lần thử + khôi phục `/attendance/me`, lịch sử tham gia phân trang và `audit_logs` bất biến). Các tính năng mở rộng Sprint 2–3 (vai trò `CVHT`, Google OAuth2/OIDC, khóa đăng nhập 5 lần sai & Refresh Cookie `HttpOnly`, quota nhiều tầng `event_pools` & waiting list, GPS, minh chứng & retention 150 ngày, đồng bộ offline Store-and-Forward 24h, khiếu nại 2 vòng, import/export CSV/Excel và API quản trị `SYSTEM_ADMIN`) đã đóng băng thiết kế tại [docs/Design/v2-schema-blueprint.md](docs/Design/v2-schema-blueprint.md) và [docs/decisions.md](docs/decisions.md).

## Stack và dependency

| Thành phần | Mục đích |
| --- | --- |
| Java 21, Spring Boot 4.1.2, Maven Wrapper | Runtime và build thống nhất |
| Spring MVC, Validation | REST API và kiểm tra dữ liệu đầu vào |
| Spring Data JPA, PostgreSQL 17 | Giao dịch, khóa quota và ràng buộc dữ liệu (`ddl-auto: validate`) |
| Spring Security, OAuth2 Client, Resource Server | RBAC, phát hành/xác thực JWT HS256 (`NimbusJwtEncoder`/`NimbusJwtDecoder`) và nền tảng Google OAuth2 cho Sprint 2+ |
| Flyway + PostgreSQL module | Quản lý migration SQL (`V1__sprint1_core_schema.sql`) |
| springdoc 3.2.0 | OpenAPI và Swagger UI (bật ở profile `dev`, hỗ trợ Bearer JWT) |
| Apache Commons CSV 1.14.1, Apache POI 6.0.0 | Chuẩn bị cho import/export CSV và Excel ở Sprint 2+ |
| ZXing 3.5.4 | Chuẩn bị tạo ảnh QR phía server khi cần; token QR động `qr1` hiện ký/xác thực bằng HMAC-SHA256 (`QrTokenService`) |
| Actuator, DevTools | Health check và hỗ trợ phát triển |
| Spring Boot Test, Spring Security Test | JUnit 5, MockMvc và integration test trên PostgreSQL 17 |

JWT/JOSE dùng Spring Security Resource Server (`NimbusJwtEncoder`/`NimbusJwtDecoder`), không thêm JJWT. PostgreSQL 17 là nguồn dữ liệu chính; chưa dùng Redis, Kafka hay microservices cho MVP 3.000 sinh viên. Lưu trữ minh chứng (`evidence/`) chuẩn hóa qua cổng `EvidenceStoragePort` với adapter mặc định **Local Private Filesystem** ngoài web root (pluggable MinIO/S3 cho staging/prod, retention 150 ngày).

Các phiên bản Spring, PostgreSQL JDBC, Hibernate, Flyway và thư viện test do Spring Boot BOM quản lý. Tham khảo: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [springdoc](https://springdoc.org/faq.html).

## API đã triển khai (Sprint 1)

Mọi response lỗi trả về `ApiError` JSON chuẩn (`code`, `message`, `details`, `traceId`, `timestamp`) qua `GlobalExceptionHandler`, `AuthenticationEntryPoint` và `AccessDeniedHandler`. Chi tiết hợp đồng xem [docs/api/sprint1-contract.md](docs/api/sprint1-contract.md).

| Nhóm / Quyền | Method & Path | Mô tả |
| --- | --- | --- |
| Public | `GET /actuator/health/**` | Kiểm tra trạng thái ứng dụng (`show-details: never`) |
| Public | `POST /api/v1/auth/login` | Đăng nhập `loginId` + `password`, cấp JWT HS256 (TTL 8h) và tạo bản ghi `auth_session` |
| Authenticated | `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` | Thu hồi phiên hiện tại (`revoked_at`) / lấy thông tin người dùng |
| `EVENT_OWNER` | `GET /api/v1/classes` | Lấy danh sách lớp (`org_class`) để chọn đối tượng tham gia |
| `EVENT_OWNER` | `POST /api/v1/events`, `PUT /api/v1/events/{id}` | Tạo sự kiện `DRAFT`, sửa bản nháp với optimistic locking (`version`) |
| `EVENT_OWNER` | `GET /api/v1/events/{id}/preview`, `POST /api/v1/events/{id}/publish` | Xem trước (`previewed_version`) và công bố (`PUBLISHED`, ghi audit) |
| `EVENT_OWNER` | `GET /api/v1/events/{id}/qr` | Phát hành mã QR động `qr1.<eventId>.<epoch>.<sig>` trong cửa sổ điểm danh |
| `STUDENT` / `EVENT_OWNER` | `GET /api/v1/events`, `GET /api/v1/events/{id}` | Tìm kiếm/lọc (`q`, `activityType`, `page`, `size`) và xem chi tiết (ẩn nháp/ngoài lớp thành `404`) |
| `STUDENT` | `POST /api/v1/events/{id}/registrations`, `GET /api/v1/events/{id}/registrations/me` | Đăng ký giữ suất nguyên tử theo `quota_total` và xem trạng thái đăng ký |
| `STUDENT` | `POST /api/v1/events/{id}/attendance/qr`, `GET /api/v1/events/{id}/attendance/me` | Điểm danh QR động (header `Idempotency-Key`, grace 3s, tối đa 3 lần thử sai, chống trùng `ACCEPTED`) và khôi phục trạng thái/số lượt thử sau reload (`QE-98`) |
| `STUDENT` | `GET /api/v1/me/history` | Lịch sử tham gia phân trang `Page<HistoryItem>` (hỗ trợ lọc `academicYear`, `semester`, `attendanceStatus`, `upcoming`, `activityType`) |

Các request không nằm trong danh sách trên bị từ chối bởi `anyRequest().denyAll()` trong `SecurityConfiguration`.

## Chạy local với PostgreSQL Compose

Cần JDK 21 trở lên và Docker Desktop đang chạy. Maven được Wrapper tải tự động.

`compose.yaml` khởi tạo service `postgres` (`postgres:17-alpine`, project `qnu-events`, DB `${DB_NAME:-qnu_events}`, user `${DB_USERNAME:-qnu_events}`, mật khẩu `${DB_PASSWORD:-qnu_events_dev}`, bind `127.0.0.1:${DB_PORT:-5432}:5432`).

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --wait postgres
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Linux/macOS:

```bash
cp .env.example .env
docker compose up -d --wait postgres
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Hoặc mở [workspace VS Code](QNU-Events-BE.code-workspace), chọn **Terminal → Run Task → Spring Boot: Run (dev)** sau khi PostgreSQL sẵn sàng.

- Health: `http://localhost:8080/actuator/health`
- Swagger (profile `dev`): `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON (profile `dev`): `http://localhost:8080/v3/api-docs`
- Database: `localhost:5432/qnu_events` (biến phát triển mặc định trong `.env.example`, mặc định chu kỳ QR `QR_EPOCH_SECONDS=10` và grace `QR_GRACE_PERIOD_SECONDS=3`).

### Dữ liệu mẫu & Reset demo (`dev` / `demo`)

Khi chạy với profile `dev` hoặc `demo`, `DemoDataSeeder` tự động nạp idempotent:
- Lớp: `K49CNTT1`, `K49CNTT2`
- Tài khoản (mật khẩu mặc định `${DEMO_PASSWORD:Demo@12345}`, băm BCrypt): `owner01`, `owner02` (`EVENT_OWNER`), `sv001`, `sv002` (`STUDENT`, `K49CNTT1`), `sv003` (`STUDENT`, `K49CNTT2`), `admin01` (`SYSTEM_ADMIN`).

Để xóa sạch dữ liệu giao dịch (`attendances`, `attendance_attempts`, `registrations`, `event_audience`, `events`, `audit_logs`) mà vẫn giữ tài khoản và lớp học:

- Windows: `scripts\reset-demo-data.bat`
- Linux/macOS: `bash scripts/reset-demo-data.sh`

Cả hai script chạy `docker compose exec -T postgres psql` bằng `DB_USERNAME` (`qnu_events` mặc định) và `DB_NAME` (`qnu_events` mặc định) trên [scripts/reset-demo-data.sql](scripts/reset-demo-data.sql).

`.env` được Spring đọc như file Java properties: viết `KEY=value`, không đặt dấu nháy quanh giá trị. Biến môi trường hệ điều hành được ưu tiên hơn `.env`. Nếu volume PostgreSQL đã khởi tạo trước đó, sửa `.env` sẽ không tự đổi user/password/database trong volume hiện có.

Dừng PostgreSQL nhưng giữ nguyên dữ liệu: `docker compose stop`.

## GitHub Codespaces

Cấu hình nằm trong [.devcontainer/devcontainer.json](.devcontainer/devcontainer.json).

1. Đưa các thay đổi lên branch của repository GitHub.
2. Mở repository → **Code → Codespaces → Create codespace on branch**.
3. Container tự cài Java 21, kéo PostgreSQL, tải Maven và chạy build.
4. Trong terminal chạy:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. Mở port **8080** từ tab **Ports**, rồi truy cập `/swagger-ui/index.html` hoặc `/actuator/health` (giữ port ở chế độ **Private**).

Dev container cũng dùng được qua VS Code → **Dev Containers: Reopen in Container** khi Docker đang chạy.

## Kiểm tra

Trên nhánh `main` và `develop` hiện có tổng cộng **151 phương thức `@Test`** trong `src/test/java` gồm **40 unit `@Test`** (8 file `*Test.java`) và **111 integration `@Test`** (10 file `*IT.java`):

- **Unit test (40/40 pass, không cần PostgreSQL):** `QrTokenServiceTest` (13), `SecurityConfigurationTest` (8), `AttendanceServiceTest` (5), `EventServiceAndValidatorTest` (4), `GlobalExceptionHandlerTest` (3), `SessionJwtValidatorTest` (3), `RegistrationServiceTest` (2), `HistoryServiceTest` (2).

  ```powershell
  .\mvnw.cmd -B -ntp test
  ```

- **Unit + Integration test (151 tests, yêu cầu PostgreSQL 17 đang chạy):** `AttendanceIT` (25), `EventIT` (21), `HistoryIT` (15), `RegistrationIT` (12), `BusinessStateTransitionIT` (11), `AuthIT` (8), `MigrationIT` (7), `SharedKernelSecurityIT` (5), `ApplicationIT` (4), `AuditIT` (3) cùng 40 unit tests.

  ```powershell
  docker compose up -d --wait postgres
  .\mvnw.cmd -B -ntp verify -Pintegration
  ```

Trên Linux/macOS/Codespaces thay `.\mvnw.cmd` bằng `bash ./mvnw`. CI (`.github/workflows/ci.yml`) chạy `bash ./mvnw -B -ntp -Pintegration verify` với service `postgres:17-alpine` (`qnu_events_test`). Lưu ý `pom.xml` hiện chưa cấu hình plugin `spotless`. JAR đóng gói tạo tại `target/qnu-events-0.0.1-SNAPSHOT.jar`.

## Cấu trúc

```text
src/main/java/vn/edu/qnu/events/
  config/         SecurityConfiguration, ClockConfiguration, OpenApiConfiguration
  common/         ApiError, ApiException, ErrorCode, CurrentUser, GlobalExceptionHandler
  auth/           Login/logout/me, JwtService, SessionJwtValidator, DemoDataSeeder (Google OAuth2 S3)
  student/        OrgClass, ClassController, HistoryController/HistoryService (lịch sử đầy đủ S2)
  importer/       Dự kiến Sprint 2 (QE-26/62): Import CSV/Excel Khoa/Ngành/Lớp/SV (SYSTEM_ADMIN)
  event/          Event CRUD, preview/publish workflow, lọc theo audience/search (event_pools S2)
  registration/   Đăng ký giữ suất nguyên tử (waiting list/hủy/no-show S2+)
  attendance/     QrTokenService, phát hành/check-in QR động, 3 attempts, idempotency (GPS/Offline S2+)
  audit/          AuditLog, AuditService (append-only cùng trigger DB)
  evidence/       Khung chờ Sprint 2+ (EvidenceStoragePort Local/S3, Proxy, BCS review)
  appeal/         Khung chờ Sprint 2+ (Khiếu nại 2 vòng: CVHT Vòng 1 -> EVENT_OWNER Vòng 2)
  outbox/         Dự kiến Sprint 2+ (Transactional Outbox poller & schedulers)
src/main/resources/db/migration/
  V1__sprint1_core_schema.sql
```

Tài liệu thiết kế & quy chuẩn quan trọng:
- [AGENTS.md](AGENTS.md) và [CONTRIBUTING.md](CONTRIBUTING.md)
- [docs/spec/QNU_Event_SRS.docx](docs/spec/QNU_Event_SRS.docx) (`v1.1`) và [docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx](docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx)
- [docs/api/sprint1-contract.md](docs/api/sprint1-contract.md) và [docs/api/error-codes.md](docs/api/error-codes.md)
- [docs/Design/erd.md](docs/Design/erd.md) (`V1`) và [docs/Design/v2-schema-blueprint.md](docs/Design/v2-schema-blueprint.md) (`V2` đóng băng)
- [docs/decisions.md](docs/decisions.md) và [docs/backend-foundation.md](docs/backend-foundation.md)

## Cấu hình triển khai

- Ngoài profile `dev`, ứng dụng yêu cầu `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; Swagger (`/v3/api-docs`, `/swagger-ui/**`) mặc định tắt và `SEED_DEMO_DATA` mặc định `false`.
- `DemoDataSeeder` chỉ chạy với profile `dev` hoặc `demo`.
- **Bảo mật Secret ngoài `dev`/`demo`/`test` (`R7`):** `JwtService`, `SecurityConfiguration` (`resolveJwtSecret`) và `QrTokenService` đã áp dụng kiểm tra **fail-fast** từ chối khởi động nếu thiếu hoặc yếu `JWT_SECRET` / `QR_SIGNING_SECRET` (`< 32` byte) khi chạy ở môi trường ngoài `dev`/`demo`/`test`; khi triển khai staging/production bắt buộc cấu hình hai khóa độc lập qua biến môi trường.
- Giới hạn upload mặc định `10MB`/`12MB` (`UPLOAD_MAX_FILE_SIZE`, `UPLOAD_MAX_REQUEST_SIZE`). File minh chứng được lưu qua `EvidenceStoragePort` (Local Private Filesystem ngoài public web root hoặc MinIO/S3 riêng tư), tuyệt đối không trả URL static công khai.
