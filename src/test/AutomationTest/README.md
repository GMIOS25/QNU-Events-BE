# QNU-Events Automation Test Suite (Java Selenium + REST Assured + JUnit 5)

Bộ script kiểm thử tự động ánh xạ trực tiếp từ 6 tài liệu Test Cases trong `docs/test/testcase/` (mục `4.1` → `4.6`).

## 1. Cấu trúc thư mục

```text
src/test/AutomationTest/
├── ApiTest/                                # Java + REST Assured 6 + JUnit Jupiter
│   ├── config/
│   │   ├── ApiTestConfig.java              # Cấu hình URL, tài khoản mẫu, DB, JWT/QR secrets
│   │   ├── ApiTestDataFactory.java         # Builder payload EventUpsert, giả mạo/ký QR & JWT
│   │   ├── BaseApiTest.java                # Base class @Tag("api"), RestAssured & cleanup fixture
│   │   └── TestDbHelper.java               # JDBC PostgreSQL helper (tạo user tạm, kiểm tra audit_logs)
│   ├── AuthApiTest.java                    # 4.1 Xác thực, phiên, JWT, RBAC & CORS
│   ├── EventManagementApiTest.java         # 4.2 CRUD Sự kiện, Validation EP/BVA, Preview-Publish, Audit
│   ├── EventListApiTest.java               # 4.3 Danh sách, Tìm kiếm, Phân trang, Chi tiết & Anti-IDOR
│   ├── RegistrationApiTest.java            # 4.4 Đăng ký giữ suất, Rollback Quota & Tương tranh đồng thời
│   ├── DynamicQrAttendanceApiTest.java     # 4.5 Dynamic QR, Grace Period, Giới hạn 3 lần & Idempotency
│   └── HistoryApiTest.java                 # 4.6 Lịch sử tham gia, DTO, Sắp xếp, Lọc, Anti-IDOR & RBAC
└── SeleniumTest/                           # Java + Selenium 4 (POM) + JUnit Jupiter
    ├── config/
    │   ├── SeleniumConfig.java             # Cấu hình FE/BE URL, browser (chrome/edge), headless, timeout
    │   └── BaseSeleniumTest.java           # WebDriver (Selenium Manager), CDP viewport 320px/375px & offline
    ├── pages/                              # Page Object Model (POM) bám sát QNU-Events-FE
    │   ├── LoginPage.java
    │   ├── LayoutComponent.java
    │   ├── EventsPage.java
    │   ├── EventFormPage.java
    │   ├── EventDetailPage.java
    │   ├── EventQrPage.java
    │   └── HistoryPage.java
    ├── AuthSeleniumTest.java               # 4.1 UI Đăng nhập, F5, Logout ConfirmDialog, Deep-link & Route RBAC
    ├── EventManagementSeleniumTest.java    # 4.2 UI Tạo/Sửa DRAFT, Validation, Optimistic Locking 2 tab, Preview-Publish
    ├── EventListSeleniumTest.java          # 4.3 UI Danh sách theo role/lớp, Search debounce, 404 Banner, A11y & Responsive
    ├── RegistrationSeleniumTest.java       # 4.4 UI Đăng ký giữ chỗ, F5, Stale-tab CAPACITY_FULL/CLOSED, Owner mode, 320px
    ├── DynamicQrAttendanceSeleniumTest.java# 4.5 UI Chiếu QR 10s, Chép mã, Camera fallback, Đếm lượt 0/3 -> 3/3 khóa UI
    └── HistorySeleniumTest.java            # 4.6 UI Lịch sử, EmptyState, KPI, cập nhật sau điểm danh QR, F5 & 320px
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
3. **Frontend** đang chạy (`http://localhost:5173`) — chỉ cần cho `SeleniumTest`:
   ```powershell
   npm run dev
   ```
4. **Trình duyệt Google Chrome hoặc Microsoft Edge** đã cài trên máy (Selenium 4 tích hợp sẵn *Selenium Manager* tự tải driver khớp phiên bản trình duyệt).

## 3. Lệnh chạy bằng Maven (`QNU-Events-BE`)

```powershell
# 1. Biên dịch toàn bộ source & automation tests + chạy unit tests mặc định (không cần server)
.\mvnw.cmd -B -ntp test

# 2. Chạy toàn bộ API Tests (Java + REST Assured + JUnit)
.\mvnw.cmd -B -ntp test -Papi-test

# 3. Chạy toàn bộ Selenium UI Tests (mặc định headless Chrome)
.\mvnw.cmd -B -ntp test -Pselenium-test

# 4. Chạy Selenium có giao diện trình duyệt (quan sát trực tiếp)
.\mvnw.cmd -B -ntp test -Pselenium-test -Dselenium.headless=false

# 5. Chạy Selenium bằng Microsoft Edge
.\mvnw.cmd -B -ntp test -Pselenium-test -Dselenium.browser=edge

# 6. Chạy toàn bộ Automation Test (cả API + Selenium)
.\mvnw.cmd -B -ntp test -Pautomation-test

# 7. Chạy riêng một lớp test cụ thể
.\mvnw.cmd -B -ntp test -Papi-test -Dtest=AuthApiTest
.\mvnw.cmd -B -ntp test -Pselenium-test -Dtest=RegistrationSeleniumTest
```

### Tùy chọn cấu hình qua System Properties (`-D...`)

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

| Tài liệu Test Case | API Test Suite (`ApiTest/`) | Selenium UI Test Suite (`SeleniumTest/`) |
|---|---|---|
| `4.1-xac-thuc-quan-ly-phien-dang-nhap.md` | `AuthApiTest.java` | `AuthSeleniumTest.java` |
| `4.2-quan-ly-xem-truoc-cong-bo-su-kien.md` | `EventManagementApiTest.java` | `EventManagementSeleniumTest.java` |
| `4.3-danh-sach-su-kien.md` | `EventListApiTest.java` | `EventListSeleniumTest.java` |
| `4.4-dang-ky-su-kien.md` | `RegistrationApiTest.java` | `RegistrationSeleniumTest.java` |
| `4.5-diem-danh-dynamic-qr.md` | `DynamicQrAttendanceApiTest.java` | `DynamicQrAttendanceSeleniumTest.java` |
| `4.6-tra-cuu-lich-su-tham-gia-su-kien.md` | `HistoryApiTest.java` | `HistorySeleniumTest.java` |
