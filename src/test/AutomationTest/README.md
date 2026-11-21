# QNU-Events Automation Test Suite (Bruno API + Java Selenium 4 + Apache JMeter 5.6.3)

Bộ kiểm thử tự động toàn diện ánh xạ trực tiếp từ 6 tài liệu Test Cases trong `docs/test/testcase/` (mục `4.1` → `4.6`) và các yêu cầu phi chức năng trong `docs/QNU_Event_SRS.docx`.

## 1. Cấu trúc thư mục

```text
src/test/AutomationTest/
├── ApiTest/                                # Bruno API Automation Collection (350 requests, 7 thư mục)
│   ├── bruno.json                          # Khai báo Bruno Collection
│   ├── collection.bru                      # Pre-request động (ISO-8601 UTC, academicYear, freshIdemKey, runSuffix)
│   ├── environments/local.bru              # Biến môi trường dev & vars:secret (jwtSecret, qrSigningSecret)
│   ├── 01-Auth/                            # 4.1 Xác thực, JWT HS256, Phiên, Logout, RBAC, CORS, TraceId (52 req)
│   ├── 02-Events-Management/               # 4.2 CRUD Sự kiện, Validation EP/BVA, Locking, Preview-Publish (60 req)
│   ├── 03-Event-List/                      # 4.3 Danh sách theo role/lớp, DISTINCT đa lớp, Tìm kiếm, Phân trang (39 req)
│   ├── 04-Registration/                    # 4.4 Đăng ký giữ suất, Idempotent, Rollback Quota, Anti-IDOR (40 req)
│   ├── 05-Dynamic-QR-Attendance/           # 4.5 Dynamic QR, Grace Period, Expired, 9 Malformed, Limit 3, Replay (87 req)
│   ├── 06-History/                         # 4.6 Lịch sử DTO, @JsonInclude, Sort/Tie-breaker, Lọc QE-72, IDOR (42 req)
│   ├── 07-DB-Fixture-Cases/                # Các ca API cần fixture DB đặc thù (LOCKED, bcs01, CANCELLED, ...) (30 req)
│   ├── scripts/                            # prepare-bruno-data.sql, cleanup-bruno-data.sql, run-bruno.ps1
│   └── README.md                           # Hướng dẫn chạy Bruno & Ma trận truy vết 100% Test Cases
├── SeleniumTest/                           # Java + Selenium 4 (POM) + JUnit Jupiter
│   ├── config/
│   │   ├── SeleniumConfig.java             # Cấu hình FE/BE URL, browser (chrome/edge), headless, timeout
│   │   ├── BaseSeleniumTest.java           # WebDriver (Selenium Manager), CDP viewport 320px/375px & offline
│   │   ├── SeleniumDbHelper.java           # JDBC PostgreSQL helper cho tiền điều kiện UI
│   │   └── SeleniumTestDataFactory.java    # Factory tạo dữ liệu mẫu cho luồng E2E
│   ├── pages/                              # Page Object Model (POM) bám sát QNU-Events-FE
│   │   ├── LoginPage.java
│   │   ├── LayoutComponent.java
│   │   ├── EventsPage.java
│   │   ├── EventFormPage.java
│   │   ├── EventDetailPage.java
│   │   ├── EventQrPage.java
│   │   └── HistoryPage.java
│   ├── AuthSeleniumTest.java               # 4.1 UI Đăng nhập, F5, Logout ConfirmDialog, Deep-link & Route RBAC
│   ├── EventManagementSeleniumTest.java    # 4.2 UI Tạo/Sửa DRAFT, Validation, Optimistic Locking 2 tab, Preview-Publish
│   ├── EventListSeleniumTest.java          # 4.3 UI Danh sách theo role/lớp, Search debounce, 404 Banner, A11y & Responsive
│   ├── RegistrationSeleniumTest.java       # 4.4 UI Đăng ký giữ chỗ, F5, Stale-tab CAPACITY_FULL/CLOSED, Owner mode, 320px
│   ├── DynamicQrAttendanceSeleniumTest.java# 4.5 UI Chiếu QR 10s, Chép mã, Camera fallback, Đếm lượt 0/3 -> 3/3 khóa UI
│   └── HistorySeleniumTest.java            # 4.6 UI Lịch sử, EmptyState, KPI, cập nhật sau điểm danh QR, F5 & 320px
└── JMeterTest/                             # Apache JMeter 5.6.3 Performance / Load / Burst / Stress Suite
    ├── plans/                              # 4 kịch bản .jmx (Baseline, Quota Burst, P1 Dynamic QR, Stress & Spike)
    ├── data/                               # demo_users.csv & perf_students.csv
    ├── config/user.properties              # p50/p90/p95/p99, APDEX (500ms/2000ms), UTF-8
    └── scripts/                            # prepare-perf-data.sql, cleanup-perf-data.sql, run-jmeter.ps1
```

## 2. Điều kiện tiên quyết để chạy

1. **PostgreSQL 17** đang chạy (ví dụ `docker compose up -d` trong `QNU-Events-BE`).
2. **Backend** đang chạy ở profile `dev` (`http://localhost:8080`):
   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
   ```
3. **Node.js (v18+)** để chạy Bruno CLI (`npx --yes @usebruno/cli`) hoặc cài **Bruno Desktop** (cho `ApiTest`).
4. **Frontend** đang chạy (`http://localhost:5173`) — chỉ cần cho `SeleniumTest`:
   ```powershell
   npm run dev
   ```
5. **Trình duyệt Google Chrome hoặc Microsoft Edge** đã cài trên máy (Selenium 4 tích hợp sẵn *Selenium Manager* tự tải driver khớp phiên bản trình duyệt).

## 3. Lệnh chạy nhanh

### A. API Automation Tests (Bruno — `ApiTest/`)
```powershell
# 1. Chạy toàn bộ API Tests chuẩn (Thư mục 01 -> 06, không cần nạp DB fixture)
.\src\test\AutomationTest\ApiTest\scripts\run-bruno.ps1 -Mode core

# 2. Chạy FULL 100% (tự nạp DB fixture + chạy 01 -> 07 + dọn dẹp)
.\src\test\AutomationTest\ApiTest\scripts\run-bruno.ps1 -Mode full -CleanupDb

# 3. Chạy trực tiếp bằng Bruno CLI (tại thư mục ApiTest/)
npx --yes @usebruno/cli run -r --env local --exclude-tags db-fixture --reporter-skip-headers Authorization
```
Chi tiết xem thêm tại [`ApiTest/README.md`](ApiTest/README.md).

### B. Unit, Integration & Selenium UI Tests (Maven — `QNU-Events-BE`)
```powershell
# 1. Biên dịch & chạy Unit tests mặc định (không cần server)
.\mvnw.cmd -B -ntp test

# 2. Chạy toàn bộ Unit + Spring Boot Integration Tests (cần PostgreSQL 17)
.\mvnw.cmd -B -ntp verify -Pintegration

# 3. Chạy toàn bộ Selenium UI Tests (mặc định headless Chrome)
.\mvnw.cmd -B -ntp test -Pselenium-test

# 4. Chạy Selenium có giao diện trình duyệt (quan sát trực tiếp)
.\mvnw.cmd -B -ntp test -Pselenium-test -Dselenium.headless=false

# 5. Chạy Selenium bằng Microsoft Edge hoặc chạy riêng 1 lớp test
.\mvnw.cmd -B -ntp test -Pselenium-test -Dselenium.browser=edge
.\mvnw.cmd -B -ntp test -Pselenium-test -Dtest=RegistrationSeleniumTest
```

### C. Performance / Load / Stress Tests (JMeter — `JMeterTest/`)
```powershell
.\mvnw.cmd -B -ntp verify -Pjmeter-test -Djmeter.testFile=01-baseline-read-and-auth-load.jmx -DskipTests=true
```
Chi tiết xem thêm tại [`JMeterTest/README.md`](JMeterTest/README.md).

### Tùy chọn cấu hình Selenium qua System Properties (`-D...`)

| Tham số | Mặc định | Ý nghĩa |
|---|---|---|
| `-Dapi.baseUrl` | `http://localhost:8080` | Base URL của Backend API |
| `-Dfe.baseUrl` | `http://localhost:5173` | Base URL của Frontend |
| `-Dselenium.browser` | `chrome` | Trình duyệt (`chrome` hoặc `edge`) |
| `-Dselenium.headless` | `true` | Chạy không hiện cửa sổ (`true` / `false`) |
| `-Dselenium.timeoutSeconds` | `15` | Thời gian chờ tường minh `WebDriverWait` (giây) |
| `-Ddb.enabled` | `true` | Cho phép kết nối PostgreSQL JDBC cho các ca cần tạo dữ liệu DB / kiểm tra `audit_logs` |
| `-Ddb.url` / `-Ddb.username` / `-Ddb.password` | `jdbc:postgresql://localhost:5432/qnu_events` | Chuỗi kết nối PostgreSQL (tự động đọc từ `.env` nếu có) |

## 4. Ma trận Ánh xạ Test Cases (`docs/test/testcase/`)

| Tài liệu Test Case | Bruno API Test Suite (`ApiTest/`) | Selenium UI Test Suite (`SeleniumTest/`) | JUnit / JMeter Bổ trợ |
|---|---|---|---|
| `4.1-xac-thuc-quan-ly-phien-dang-nhap.md` | `01-Auth/` + `07-DB-Fixture-Cases/` | `AuthSeleniumTest.java` | `AuthIT`, `SecurityConfigurationTest`, `SharedKernelSecurityIT` |
| `4.2-quan-ly-xem-truoc-cong-bo-su-kien.md` | `02-Events-Management/` + `07-DB-Fixture-Cases/` | `EventManagementSeleniumTest.java` | `EventIT`, `AuditIT`, `MigrationIT` |
| `4.3-danh-sach-su-kien.md` | `03-Event-List/` + `07-DB-Fixture-Cases/` | `EventListSeleniumTest.java` | `EventIT`, `JMeter 01` |
| `4.4-dang-ky-su-kien.md` | `04-Registration/` + `07-DB-Fixture-Cases/` | `RegistrationSeleniumTest.java` | `RegistrationIT`, `AuditIT`, `JMeter 02` |
| `4.5-diem-danh-dynamic-qr.md` | `05-Dynamic-QR-Attendance/` + `07-DB-Fixture-Cases/` | `DynamicQrAttendanceSeleniumTest.java` | `AttendanceIT`, `QrTokenServiceTest`, `JMeter 03/04` |
| `4.6-tra-cuu-lich-su-tham-gia-su-kien.md` | `06-History/` + `07-DB-Fixture-Cases/` | `HistorySeleniumTest.java` | `HistoryIT`, `MigrationIT` |
