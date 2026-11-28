# QNU-Events API Automation Test Suite (Bruno Collection)

Bộ kiểm thử API tự động viết bằng **Bruno** (`.bru` — Git-native API client & CLI `@usebruno/cli`), ánh xạ trực tiếp từ **6 tài liệu Test Cases** trong `docs/test/testcase/` (mục `4.1` → `4.6`) và bám sát hợp đồng API thực tế của `QNU-Events-BE`.

---

## 1. Cấu trúc Thư mục (7 Nhóm · 350 Requests / 359 tệp `.bru` gồm collection, folder & environment)

```text
src/test/AutomationTest/ApiTest/
├── bruno.json                       # Cấu hình Bruno Collection (ignore scripts/)
├── collection.bru                   # Header chung + script khởi tạo mốc thời gian ISO-8601 UTC, runSuffix, freshIdemKey
├── environments/
│   └── local.bru                    # Biến môi trường dev (baseUrl, tài khoản mẫu, UUID cố định, vars:secret)
├── 01-Auth/                         # 52 requests — 4.1 Xác thực, JWT HS256, Phiên, Logout, RBAC, CORS, TraceId
├── 02-Events-Management/            # 60 requests — 4.2 CRUD Sự kiện, Validation EP/BVA, Optimistic Locking, Preview-Publish, Ownership
├── 03-Event-List/                   # 39 requests — 4.3 Danh sách theo role/lớp, DISTINCT đa lớp, Tìm kiếm, Phân trang, Chi tiết, Anti-IDOR
├── 04-Registration/                 # 40 requests — 4.4 Đăng ký giữ chỗ, Idempotent lặp, Rollback CAPACITY_FULL, Anti-IDOR R9, RBAC
├── 05-Dynamic-QR-Attendance/        # 87 requests — 4.5 Phát QR HMAC, Grace Period (<=3s), Hết hạn (>=4s), 9 ca Malformed, BadSig, WrongEvt, Limit 3 lần, Replay
├── 06-History/                      # 42 requests — 4.6 DTO HistoryItem, @JsonInclude(NON_NULL), startAt DESC + tie-breaker, lọc QE-72, Anti-IDOR R9
├── 07-DB-Fixture-Cases/             # 30 requests — Các ca API cần trạng thái DB đặc thù (tag: db-fixture)
└── scripts/
    ├── prepare-bruno-data.sql       # Nạp fixture DB idempotent cho thư mục 07-DB-Fixture-Cases/
    ├── cleanup-bruno-data.sql       # Dọn sạch sự kiện [BRU-*] và user bruno_* (không đụng audit_logs)
    └── run-bruno.ps1                # Script PowerShell chạy 1-click + xuất báo cáo HTML/JUnit/JSON
```

---

## 2. Hướng dẫn Thực thi

### Tiền điều kiện
1. **PostgreSQL 17** đang chạy (`docker compose up -d` tại `QNU-Events-BE`).
2. **Backend** đang chạy ở profile `dev` (`http://localhost:8080`):
   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Cách 1: Chạy 1-click bằng PowerShell (`scripts/run-bruno.ps1`) — Khuyến nghị
Script tự kiểm tra `/actuator/health`, tự nạp `JWT_SECRET` / `QR_SIGNING_SECRET` từ `.env` (không hardcode bí mật theo **R7**), ẩn header `Authorization` trong báo cáo, và xuất báo cáo HTML/JUnit/JSON vào `target/bruno/`:

```powershell
cd d:\QNU-EVENT\QNU-Events-BE\src\test\AutomationTest\ApiTest\scripts

# 1. Chạy toàn bộ API test chuẩn (Thư mục 01 -> 06, không cần nạp SQL thêm)
.\run-bruno.ps1 -Mode core

# 2. Chạy NHANH (bỏ qua 2 ca chờ đồng hồ thật TC-QR-11 & TC-QR-12)
.\run-bruno.ps1 -Mode fast

# 3. Chạy FULL 100% (tự nạp prepare-bruno-data.sql -> chạy đủ 01 -> 07 -> dọn dẹp)
.\run-bruno.ps1 -Mode full -CleanupDb

# 4. Chạy riêng một phân hệ (tự chạy kèm 01-Auth để lấy token)
.\run-bruno.ps1 -Folder 04-Registration
.\run-bruno.ps1 -Folder 05-Dynamic-QR-Attendance
```

### Cách 2: Chạy trực tiếp bằng Bruno CLI (`@usebruno/cli`)
```powershell
cd d:\QNU-EVENT\QNU-Events-BE\src\test\AutomationTest\ApiTest

# Chạy 01 -> 06 (không cần chuẩn bị DB fixture)
npx --yes @usebruno/cli run -r --env local --exclude-tags db-fixture --reporter-skip-headers Authorization

# Chạy FULL 01 -> 07 (sau khi đã chạy scripts/prepare-bruno-data.sql)
npx --yes @usebruno/cli run -r --env local `
  --env-var "jwtSecret=$env:JWT_SECRET" `
  --env-var "qrSigningSecret=$env:QR_SIGNING_SECRET" `
  --reporter-skip-headers Authorization
```

### Cách 3: Mở bằng giao diện Bruno Desktop
1. Mở **Bruno Desktop** → **Open Collection** → chọn thư mục `QNU-Events-BE/src/test/AutomationTest/ApiTest`.
2. Chọn môi trường **`local`** ở góc trên phải.
3. Nếu chạy cả thư mục `07-DB-Fixture-Cases`, hãy nạp `scripts/prepare-bruno-data.sql` trước và điền 2 biến secret `jwtSecret`, `qrSigningSecret` trong môi trường `local`.
4. Bấm **Run Collection** (hoặc chạy `01-Auth` trước để khởi tạo token cho các thư mục `02`–`06`).

---

## 3. Ma trận Bao phủ Toàn diện các Test Cases (`docs/test/testcase/`)

| Phân hệ & Tài liệu | Test Cases API phủ trực tiếp trong Bruno | Test Cases ngoài tầng HTTP API (đã phủ bởi bộ công cụ chuyên biệt) |
|---|---|---|
| **4.1 Xác thực & Phiên**<br>`4.1-xac-thuc-quan-ly-phien-dang-nhap.md` | **Core (`01-Auth/`):** `TC-AUTH-01`, `02`, `03`, `04`, `05`, `07`, `08`, `09`, `12`, `13`, `14`, `15`, `16`, `20`, `21`, `22`, `23`, `25`, `26`, `28`, `WB-01`, `WB-04`, `WB-09`, `WB-22`<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-AUTH-06`, `10`, `11`, `26` (exp skew), `27`, `WB-02`, `WB-12` | • **UI / Route Guard / Offline (`SeleniumTest/AuthSeleniumTest.java` & Vitest/Playwright):** `TC-AUTH-17`, `18`, `19`, `24`, `WB-23` → `WB-27`<br>• **Unit / DB / Spring Security Mock (`AuthIT`, `SecurityConfigurationTest`, `SharedKernelSecurityIT`):** `TC-AUTH-16` (BCrypt hash DB), `WB-03`, `WB-05` → `WB-08`, `WB-10`, `WB-11`, `WB-13` → `WB-21` |
| **4.2 Quản lý & Công bố Sự kiện**<br>`4.2-quan-ly-xem-truoc-cong-bo-su-kien.md` | **Core (`02-Events-Management/`):** `TC-EVT-001`, `003`, `004`, `005`, `006`, `007`, `008`, `009`, `010`, `011`, `012`, `013`, `014`, `016`, `017`, `018`, `019`, `020`, `021`, `022`, `023`, `024`, `025`, `026`, `027`, `028`, `029`, `030`, `031`, `032`, `033`, `WB-03`, `WB-04`, `WB-05`, `WB-07`, `WB-09`, `WB-14`<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-EVT-WB-06` (`CANCELLED`) | • **UI Form / Modal / Timezone / Responsive (`SeleniumTest/EventManagementSeleniumTest.java`):** `TC-EVT-002`, `015`, `035`, `WB-16` → `WB-21`<br>• **DB Trigger & White-box Service (`EventIT`, `AuditIT`, `MigrationIT`):** `TC-EVT-034` (`trg_audit_logs_immutable`), `WB-01`, `WB-02`, `WB-08`, `WB-10` → `WB-13`, `WB-15` |
| **4.3 Danh sách Sự kiện**<br>`4.3-danh-sach-su-kien.md` | **Core (`03-Event-List/` + `04-Registration/`):** `TC-4.3-01`, `02`, `03`, `04`, `05` (gồm lọc `activityType`), `06`, `07`, `08`, `09`, `10`, `11`, `12`, `13`, `14`, `15`, `16`, `21`, `22` (đa lớp DISTINCT), `23`, `WB-01`, `WB-05`, `WB-07`<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-4.3-21` (`bcs01`), `TC-4.3-22` (`CANCELLED`), `WB-02`, `WB-03` (`class_id=NULL`) | • **UI EmptyState / Offline / A11y / Responsive (`SeleniumTest/EventListSeleniumTest.java`):** `TC-4.3-17`, `18`, `19`, `20`, `24`, `WB-08` → `WB-14`<br>• **White-box Clock/JPQL (`EventIT`):** `WB-04`, `WB-06` |
| **4.4 Đăng ký Sự kiện**<br>`4.4-dang-ky-su-kien.md` | **Core (`04-Registration/`):** `TC-REG-04`, `07`, `08`, `09`, `10`, `11`, `12`, `13`, `14`, `16`, `17`, `23` (đa lớp + Anti-IDOR R9), `25` (lặp khi đầy), `WB-11`<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-REG-15` (đăng ký lại sau `CANCELLED`), `TC-REG-22` (`CANCELLED`), `TC-REG-23` (`class_id=NULL`) | • **UI RegistrationSlot / F5 / Offline / 320px (`SeleniumTest/RegistrationSeleniumTest.java`):** `TC-REG-01`, `02`, `03`, `05`, `06`, `24`, `WB-13` → `WB-15`<br>• **Concurrency Race 2/10/30 luồng & Audit DB (`RegistrationIT`, `AuditIT`, `JMeterTest/plans/02-*`):** `TC-REG-18`, `19`, `20`, `21`, `WB-01` → `WB-10`, `WB-12` |
| **4.5 Điểm danh Dynamic QR**<br>`4.5-diem-danh-dynamic-qr.md` | **Core (`05-Dynamic-QR-Attendance/`):** `TC-QR-01`, `02`, `03`, `04`, `05`, `08` (chưa đk + Anti-IDOR), `09`, `10`, `11` (Grace Period thời gian thực), `12` (Hết hạn thời gian thực), `13` (đủ 9 phân vùng Malformed), `14` (đủ 3 ca BadSig), `15`, `17`, `18` (Replay thành công & lỗi), `21`, `22` (JSON + key 1/100/101), `23`, `25`, `WB-04`, `WB-12`<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-QR-03`/`09` (`CANCELLED`), `TC-QR-08` (`WAITING_LIST`), `TC-QR-16` (`QR_FROM_FUTURE`), `TC-QR-26` (`rotation=5s, grace=0s`) | • **UI Camera / Fallback / Countdown / 320px (`SeleniumTest/DynamicQrAttendanceSeleniumTest.java`):** `TC-QR-06`, `07`, `WB-14` → `WB-16`<br>• **Concurrency Advisory Lock & Audit Immutability (`AttendanceIT`, `QrTokenServiceTest`, `JMeterTest/plans/03-*`):** `TC-QR-19`, `20`, `24`, `WB-01` → `WB-03`, `WB-05` → `WB-11`, `WB-13` |
| **4.6 Tra cứu Lịch sử**<br>`4.6-tra-cuu-lich-su-tham-gia-su-kien.md` | **Core (`06-History/`):** `TC-4.6-07`, `08`, `09`, `10`, `11` (Anti-IDOR R9), `12`, `14` (điểm 0 + tie-breaker), `16` (POST/DELETE denyAll), `WB-03`, `WB-06` + đầy đủ bộ lọc/phân trang `QE-72` (`attendanceStatus`, `activityType`, `upcoming`, `academicYear`, `semester`, `page`/`size`)<br>**DB Fixture (`07-DB-Fixture-Cases/`):** `TC-4.6-07` (SV sạch 0 đăng ký), `TC-4.6-15` (`PENDING`/`REJECTED` + `CANCELLED`/`REGISTERED`), `TC-4.6-16` (`bcs01`) | • **UI KPI / EmptyState / F5 / Offline / A11y 320px (`SeleniumTest/HistorySeleniumTest.java`):** `TC-4.6-01` → `06`, `13`, `17`, `WB-08` → `WB-11`<br>• **White-box Service / SQL / DB Constraints (`HistoryIT`, `MigrationIT`):** `WB-01`, `WB-02`, `WB-04`, `WB-05`, `WB-07` |

---

## 4. Lưu ý Bảo mật & Vận hành (Tuân thủ `AGENTS.md`)
- **R7 (Không commit bí mật):** Hai khóa `jwtSecret` và `qrSigningSecret` trong `environments/local.bru` được khai báo ở khối `vars:secret` (không lưu giá trị trong Git). Script `run-bruno.ps1` truyền khóa qua `--env-var` từ `.env` và gắn cờ `--reporter-skip-headers Authorization`.
- **An toàn dữ liệu:** Tất cả sự kiện tạo mới trong quá trình chạy đều gắn tiền tố `[BRU-<runSuffix>]`, cho phép dọn dẹp sạch bằng `scripts/cleanup-bruno-data.sql` mà không làm ảnh hưởng dữ liệu nghiệp vụ hay bảng `audit_logs` bất biến.
