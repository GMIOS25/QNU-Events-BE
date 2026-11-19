# QNU Events Backend

Backend Spring Boot cho hệ thống quản lý đăng ký và xác thực tham gia hoạt động sinh viên Đại học Quy Nhơn, xây dựng theo [proposal v2](docs/QNU_Event_Participation_Management_Project_Proposal_v2.docx) và [đặc tả SRS](docs/QNU_Event_SRS.docx).

Trên nhánh `develop` hiện tại đã triển khai hoàn chỉnh lát cắt dọc Sprint 1 (`V1__sprint1_core_schema.sql`, xác thực JWT HS256 + thu hồi phiên `auth_session`, phân quyền `SecurityConfiguration`, CRUD/preview/publish/lọc sự kiện, giữ suất nguyên tử, Dynamic QR HMAC-SHA256 + `Idempotency-Key` + giới hạn 3 lần thử, lịch sử tham gia phân trang và `audit_logs` bất biến). Các tính năng mở rộng (Google OAuth2/OIDC, khóa đăng nhập 5 lần sai, quota nhiều tầng & waiting list, GPS, minh chứng, đồng bộ offline, khiếu nại, import/export CSV/Excel và API quản trị `SYSTEM_ADMIN`) thuộc phạm vi Sprint 2+.

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

JWT/JOSE dùng Spring Security Resource Server (`NimbusJwtEncoder`/`NimbusJwtDecoder`), không thêm JJWT. PostgreSQL 17 là nguồn dữ liệu chính; chưa dùng Redis, Kafka hay microservices cho MVP 3.000 sinh viên. Nhà cung cấp lưu trữ file minh chứng (`evidence/`) chưa chốt nên chưa thêm SDK object storage.

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
| `STUDENT` | `POST /api/v1/events/{id}/attendance/qr` | Điểm danh QR động (header `Idempotency-Key`, grace 3s, tối đa 3 lần thử sai, chống trùng `ACCEPTED`) |
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

Trên nhánh `develop` hiện có tổng cộng **94 phương thức `@Test`** trong `src/test/java` gồm **13 unit `@Test`** (2 file `*Test.java`) và **81 integration `@Test`** (9 file `*IT.java`):

- **Unit test (13/13 pass, không cần PostgreSQL):** `QrTokenServiceTest` (8) và `SecurityConfigurationTest` (5).

  ```powershell
  .\mvnw.cmd -B -ntp test
  ```

- **Unit + Integration test (94 tests, yêu cầu PostgreSQL 17 đang chạy):** `ApplicationIT` (4), `MigrationIT` (7), `SharedKernelSecurityIT` (5), `AuthIT` (8), `EventIT` (20), `RegistrationIT` (12), `AttendanceIT` (11), `HistoryIT` (11), `AuditIT` (3) cùng 13 unit tests.

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
  auth/           Login/logout/me, JwtService, SessionJwtValidator, DemoDataSeeder
  student/        OrgClass, ClassController, HistoryController/HistoryService (import CSV ở Sprint 2+)
  event/          Event CRUD, preview/publish workflow, lọc theo audience/search
  registration/   Đăng ký giữ suất nguyên tử (waiting list/hủy ở Sprint 2+)
  attendance/     QrTokenService, phát hành/check-in QR động, 3 attempts, idempotency
  audit/          AuditLog, AuditService (append-only cùng trigger DB)
  evidence/       Khung chờ Sprint 2+ (chỉ có package-info.java)
  appeal/         Khung chờ Sprint 2+ (chỉ có package-info.java)
src/main/resources/db/migration/
  V1__sprint1_core_schema.sql
```

Tài liệu thiết kế & quy chuẩn quan trọng:
- [AGENTS.md](AGENTS.md) và [CONTRIBUTING.md](CONTRIBUTING.md)
- [docs/api/sprint1-contract.md](docs/api/sprint1-contract.md)
- [docs/Design/erd.md](docs/Design/erd.md)
- [docs/decisions.md](docs/decisions.md)
- [docs/backend-foundation.md](docs/backend-foundation.md)

## Cấu hình triển khai

- Ngoài profile `dev`, ứng dụng yêu cầu `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; Swagger (`/v3/api-docs`, `/swagger-ui/**`) mặc định tắt và `SEED_DEMO_DATA` mặc định `false`.
- `DemoDataSeeder` chỉ chạy với profile `dev` hoặc `demo`.
- **Lưu ý bảo mật Sprint 1 (`docs/decisions.md`):** Hiện tại `JwtService`, `SecurityConfiguration.jwtDecoder` và `QrTokenService` vẫn giữ chuỗi fallback cho dev khi biến môi trường `JWT_SECRET`/`QR_SIGNING_SECRET` trống để phục vụ test unit/CI hiện hành. Khi triển khai staging/production **bắt buộc** truyền `JWT_SECRET` và `QR_SIGNING_SECRET` độc lập (≥ 32 byte) qua biến môi trường; việc ép fail-fast ngoài `dev`/`test` sẽ thực hiện ở Sprint 2.
- Giới hạn upload mặc định `10MB`/`12MB` (`UPLOAD_MAX_FILE_SIZE`, `UPLOAD_MAX_REQUEST_SIZE`). File minh chứng khi triển khai không được lưu công khai hay trả URL static.
