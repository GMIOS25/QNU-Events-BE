# Kiểm thử Hiệu năng QNU-Events với Apache JMeter 5.6.3

Bộ kịch bản kiểm thử hiệu năng (Performance / Load / Burst / Stress Testing) cho Backend **QNU-Events** xây dựng bằng **Apache JMeter 5.6.3**, đối chiếu trực tiếp với đặc tả **Mục 4.1 — Performance Requirements** trong `docs/QNU_Event_SRS.docx` (`NFR-PERF-001` → `NFR-PERF-010`, `NFR-QUAL-001`).

---

## 1. Chỉ tiêu SLA mục tiêu (theo SRS §4.1)

| Mã yêu cầu | Hạng mục đo | Ngưỡng đạt (SLA) | Kịch bản JMeter phủ |
|---|---|---|---|
| **NFR-PERF-001** | Tỷ lệ lỗi kỹ thuật (`5xx`, timeout, `429`) dưới tải P1 | **≤ 1%** | `01`, `02`, `03`, `04` |
| **NFR-PERF-002** | Độ trễ API tương tác (tra cứu, đăng ký, điểm danh QR) | **p95 ≤ 2.000 ms**, **p99 ≤ 5.000 ms** | `01`, `02`, `03`, `04` |
| **NFR-PERF-008** | Xoay vòng mã Dynamic QR 10s + grace 3s theo đồng hồ server | Quét thành công khi mã xoay vòng liên tục | `01`, `03` |
| **NFR-PERF-010** | Điều tiết tải & Idempotency khi retry mạng | Retry cùng `Idempotency-Key` không tạo trùng bản ghi | `03`, `04` |
| **NFR-QUAL-001** | Bất biến dữ liệu dưới tranh chấp đồng thời (`INV-REG-01`, `INV-ATT-01`) | **0 overbooking** (`registeredCount ≤ quotaTotal`), **0 điểm danh trùng** | `02`, `03` |

---

## 2. Cấu trúc thư mục `JMeterTest/`

```text
src/test/AutomationTest/JMeterTest/
├── plans/
│   ├── 01-baseline-read-and-auth-load.jmx      # Tải nền: Health, Login, /auth/me, Events List/Search/Detail, History, Owner QR Poll
│   ├── 02-registration-quota-burst.jmx         # Đột biến tranh suất đăng ký (Synchronizing Timer + kiểm chứng 0 overbooking)
│   ├── 03-p1-dynamic-qr-checkin-load.jmx       # Hồ sơ P1 SRS: Owner QR Poller (5s) + Student QR Check-in + Idempotency + Concurrent Read (1 req/5s)
│   └── 04-stress-and-spike-test.jmx            # Stress & Spike vượt tải P1 (Constant Throughput Timer 100 req/s)
├── data/
│   ├── demo_users.csv                          # 3 SV mẫu có sẵn (sv001..sv003) — chạy ngay kịch bản 01 & 04
│   └── perf_students.csv                       # 30 SV giả lập (perf_sv_0001..0030), mở rộng tới 3.000 SV cho P1 đầy đủ
├── config/
│   └── user.properties                         # Cấu hình p50/p90/p95/p99, APDEX (500ms/2000ms), UTF-8, CSV full fields
└── scripts/
    ├── prepare-perf-data.sql                   # Seed nhanh N sinh viên perf_sv_* vào PostgreSQL 17 (< 1s)
    ├── cleanup-perf-data.sql                   # Dọn sạch dữ liệu perf_sv_* và sự kiện [JMETER-*]
    └── run-jmeter.ps1                          # Script chạy 1-click (JMeter CLI hoặc Maven jmeter-maven-plugin) + in bảng SLA
```

---

## 3. Hướng dẫn chuẩn bị & thực thi

### Bước 1: Khởi động PostgreSQL 17 và Backend (profile `dev`)
```powershell
cd d:\QNU-EVENT\QNU-Events-BE
docker compose up -d
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Bước 2: (Cho kịch bản `02` và `03`) Seed sinh viên giả lập
- Mặc định `perf_students.csv` có sẵn 30 sinh viên (`perf_sv_0001` → `perf_sv_0030`).
- Nạp tương ứng vào PostgreSQL (thay `30` bằng `100` hoặc `3000` cho tải P1 đầy đủ):
```powershell
docker compose exec -T postgres psql -U qnu_events -d qnu_events -v student_count=30 < src\test\AutomationTest\JMeterTest\scripts\prepare-perf-data.sql
```
- Nếu muốn tăng lên `100` hoặc `3000` sinh viên, dùng cờ `-GenerateCsvCount 100` của `run-jmeter.ps1` để đồng bộ file `perf_students.csv`.

### Bước 3: Chạy kiểm thử hiệu năng (3 cách linh hoạt)

#### Cách 1: Chạy trực tiếp qua Maven (không cần cài sẵn JMeter trên máy!)
Maven profile `-Pjmeter-test` tự tải **Apache JMeter 5.6.3**, chạy kịch bản và xuất **HTML Dashboard Report** tại `target/jmeter/reports/`:
```powershell
# Chạy riêng 1 kịch bản
.\mvnw.cmd -B -ntp verify -Pjmeter-test -Djmeter.testFile=01-baseline-read-and-auth-load.jmx -DskipTests=true

# Chạy kịch bản tranh suất đăng ký (02)
.\mvnw.cmd -B -ntp verify -Pjmeter-test -Djmeter.testFile=02-registration-quota-burst.jmx -DskipTests=true

# Chạy kịch bản hồ sơ P1 Dynamic QR (03)
.\mvnw.cmd -B -ntp verify -Pjmeter-test -Djmeter.testFile=03-p1-dynamic-qr-checkin-load.jmx -DskipTests=true

# Chạy toàn bộ 4 kịch bản (.jmx)
.\mvnw.cmd -B -ntp verify -Pjmeter-test -DskipTests=true
```

#### Cách 2: Chạy bằng PowerShell Runner kèm tự động chấm điểm SLA (`run-jmeter.ps1`)
```powershell
cd d:\QNU-EVENT\QNU-Events-BE\src\test\AutomationTest\JMeterTest\scripts

# Kịch bản 01: Baseline 20 VU trong 60s
.\run-jmeter.ps1 -Plan 01 -Threads 20 -Duration 60

# Kịch bản 02: 30 SV đồng loạt tranh 5 suất
.\run-jmeter.ps1 -Plan 02 -BurstUsers 30 -QuotaTotal 5

# Kịch bản 03: P1 Dynamic QR (30 SV điểm danh + 20 phiên đọc song song)
.\run-jmeter.ps1 -Plan 03 -P1CheckInUsers 30 -P1ReadUsers 20 -Duration 60

# Kịch bản 04: Stress & Spike 100 req/s (6000 req/phút)
.\run-jmeter.ps1 -Plan 04 -Threads 60 -TargetRpm 6000 -Duration 45
```

#### Cách 3: Mở & chạy bằng Apache JMeter CLI / GUI
- **Chạy non-GUI (khuyến nghị khi đo hiệu năng chuẩn):**
  ```powershell
  jmeter -n -t src\test\AutomationTest\JMeterTest\plans\03-p1-dynamic-qr-checkin-load.jmx `
         -q src\test\AutomationTest\JMeterTest\config\user.properties `
         -l target\jmeter\results\p1.csv `
         -e -o target\jmeter\reports\p1-dashboard `
         -Jhost=localhost -Jport=8080 -Jp1CheckInUsers=100 -Jp1ReadUsers=100 -Jduration=60
  ```
- **Mở giao diện GUI để chỉnh sửa/xem cây kịch bản:** Mở trực tiếp bất kỳ tệp `.jmx` nào trong `plans/` bằng Apache JMeter 5.6.3.

### Bước 4: Dọn dẹp dữ liệu giả lập sau khi đo
```powershell
docker compose exec -T postgres psql -U qnu_events -d qnu_events < src\test\AutomationTest\JMeterTest\scripts\cleanup-perf-data.sql
```
