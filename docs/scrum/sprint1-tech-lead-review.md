# Tech Lead & Scrum Master Review — Sprint 1 (`Sprint 41`) & Open PR Audit

> **Ngày rà soát:** 18/11/2026 · **Phạm vi kiểm tra:** `QNU-Events BE` (`origin/develop @ 4ac5da1`, `origin/main @ 667a54e`, các remote branch/PR mở), `QNU-Events FE` (`develop @ 0fe4a3c`), Jira project `QE` (`Sprint 41`, 27 items).  
> **Nguyên tắc R1 (`AGENTS.md`):** Toàn bộ số liệu bên dưới được đối chiếu trực tiếp từ git, mã nguồn, Jira và log chạy lệnh kiểm chứng thực tế; không suy đoán hay hợp thức hóa số liệu chưa kiểm chứng.

---

## 1. Tóm tắt Trạng thái Kiểm chứng Kỹ thuật Hiện tại

| Kho mã nguồn | Lệnh kiểm tra đã chạy | Kết quả thực tế | Ghi chú phạm vi |
|---|---|---|---|
| **Backend (`develop @ 4ac5da1`)** | `.\mvnw.cmd -B -ntp test` | **PASS (13/13 unit tests)** | `QrTokenServiceTest` (8) + `SecurityConfigurationTest` (5); chạy không cần PostgreSQL. |
| **Backend (`develop @ 4ac5da1`)** | `.\mvnw.cmd -B -ntp verify -Pintegration` | **CHƯA CHẠY TRÊN MÁY AUDIT** | Thiếu PostgreSQL 17 đang chạy (`localhost:5432` đóng, chưa cài Docker trên Windows host). Tổng `@Test` trên `develop` là **94** (`13` unit + `81` IT trong 9 file `*IT.java`). `pom.xml` chưa cấu hình `spotless`. |
| **Frontend (`develop @ 0fe4a3c`)** | `npm run lint`, `npm run typecheck`, `npm run test -- --run`, `npm run build` | **PASS toàn bộ** | Lint `0` lỗi/`0` cảnh báo (32 file); TypeScript `exit 0`; Vitest `5/5` file, `11/11` test (`RegistrationSlot.test.tsx` có 6 cảnh báo `act(...)` trên stderr); Vite build `2256` modules. |
| **Frontend E2E (`e2e/*.spec.ts`)** | Đọc mã nguồn kiểm thử | **7 specs dùng mock `page.route()`** | `auth-and-routes.spec.ts` (4) + `owner-flow.spec.ts` (3) chặn API bằng `page.route()`, không chạy với BE/PostgreSQL thật. |

---

## 2. Tình hình Sprint 1 (`Sprint 41`, gia hạn đến `20/11/2026`) — 27 Items

Theo nhật ký Jira (`docs/scrum/jira-change-log.md`):
- Sprint 41 ban đầu đặt mốc review `11/11/2026`, được đóng muộn lúc `17/11/2026 23:23 UTC+7` với 10 item lõi, sau đó được Leader mở lại và gia hạn đến **`20/11/2026`** (`endDate: 2026-11-20T10:00Z`), bổ sung thêm 17 item (`QE-6, 8, 15, 20, 25, 29, 31, 36, 37, 38, 41, 44, 45, 54, 81, 82, 83`) nâng tổng phạm vi lên **27 items**.

### 2.1 Thống kê Trạng thái trên Jira (`project = QE AND sprint = 41`)

| Trạng thái Jira | Số lượng | Danh sách Jira Key |
|---|---:|---|
| **Done** | **10** | `QE-28`, `QE-56`, `QE-57`, `QE-61`, `QE-64`, `QE-65`, `QE-66`, `QE-70`, `QE-71`, `QE-72` |
| **In Review** | **1** | `QE-54` (`Huỳnh Hữu Đan` — đang mở BE PR #9 `feature/QE-54-attendance-attempts`) |
| **In Progress** | **2** | `QE-31` (`Nguyễn Nhật Khánh` — BE PR #8 đã merge vào `develop`, còn FE filter & Jira), `QE-41` (`Nguyễn Nhật Khánh`) |
| **To Do** | **14** | `QE-6`, `QE-8`, `QE-36` (`Trần Thanh Cường` — đã xong tài liệu & qua review, chờ merge/chuyển trạng thái), `QE-15`, `QE-37`, `QE-38` (`Nguyễn Huy Hoàng`), `QE-20`, `QE-44` (`Huỳnh Hữu Đan`), `QE-25`, `QE-83` (`Nguyễn Nhật Khánh`), `QE-29`, `QE-45`, `QE-81`, `QE-82` (`Khánh Dương Nguyễn`) |
| **Tổng cộng** | **27** | **10 Done · 1 In Review · 2 In Progress · 14 To Do** |

### 2.2 Đánh giá các Hạng mục Nền tảng A0 (`Trần Thanh Cường / @GMIOS25`)

| Jira Key | Vai trò A0 | Tình trạng thực tế & Điểm cần lưu ý |
|---|---|---|
| **`QE-64`** (`Done`) | Login mẫu, JWT HS256, thu hồi phiên `auth_session`, RBAC `SecurityConfiguration`, `DemoDataSeeder` | Hoàn thành luồng cốt lõi Sprint 1 (`AuthIT` 8 tests + `SecurityConfigurationTest` 5 tests). Nợ kỹ thuật Sprint 2 đã ghi vào [docs/decisions.md](../decisions.md): loại fallback secret trong `JwtService`/`SecurityConfiguration` (`A-AUTH-01`), bỏ TTL 8h hardcode ở `AuthService.java:60`, đảo thứ tự `LOCKED` check sau BCrypt (`A-AUTH-03`), và bổ sung khóa brute-force `REQ-AUTH-015` (`A-AUTH-06`). |
| **`QE-70`** (`Done` + cleanup) | Flyway `V1`, Compose PostgreSQL 17, CI, FE scaffold, Frozen Contract | Đã hoàn tất khung nền BE+FE. Đợt rà soát phát hiện 4 sai lệch vận hành đã xử lý trên nhánh `chore/QE-70-a0-cleanup-and-review`: (1) `README.md` lỗi thời báo chưa làm auth/API, (2) `scripts/reset-demo-data.{bat,sh}` hardcode `docker exec -i postgres psql -U postgres` sai so với `compose.yaml`, (3) `.env.example` & `application.yml` đặt `QR_EPOCH_SECONDS=15` lệch `10s` trong `Event.java`/`V1`/FE, (4) tổng hợp đầy đủ `docs/decisions.md`. Phía FE có nhánh `chore/QE-70-figma-shell-alignment` đồng bộ token & shell theo Figma. |
| **`QE-6`** (`To Do` → chờ merge/chốt) | Thiết kế kiến trúc MVP & boundaries | Đã hoàn thiện & xác minh [docs/architecture/mvp-architecture-and-boundaries.md](../architecture/mvp-architecture-and-boundaries.md) (trên nhánh `chore/QE-6-mvp-architecture`), đóng góp `ARCH-A01..ARCH-A07`. |
| **`QE-8`** (`To Do` → chờ merge/chốt) | Thiết kế attendance consistency, atomic quota & idempotency | Đã hoàn thiện & xác minh [docs/architecture/attendance-consistency-design.md](../architecture/attendance-consistency-design.md) (trên nhánh `chore/QE-8-attendance-consistency`), đóng góp `A-QE8-01..A-QE8-11`. |
| **`QE-36`** (`To Do` → chờ merge/chốt) | Đặc tả phương thức đăng nhập & vòng đời phiên | Đã hoàn thiện & xác minh [docs/architecture/auth-and-session-lifecycle.md](../architecture/auth-and-session-lifecycle.md) (trên nhánh `chore/QE-36-auth-session-lifecycle`), đóng góp `A-AUTH-01..A-AUTH-08` và `Q-AUTH-01..07` chờ stakeholder. |

---

## 3. Rà soát Branch Governance & Các Pull Request Đang Mở

### 3.1 Phân kỳ nghiêm trọng giữa `origin/main` và `origin/develop` (`ahead=5, behind=13`)
- **Bằng chứng git:** `git rev-list --left-right --count origin/main...origin/develop` trả về **`5` commits chỉ có trên `main` và `13` commits chỉ có trên `develop`**.
- **Nguyên nhân:** **PR #4** (`fix/QE-56-dynamic-qr-hardening`, merge commit `667a54e` gồm `fcef234`, `5fd4ec4`, `c497416`, `63be9e8`) đã được merge thẳng vào `main` mà **chưa đưa vào `develop`**. Ngược lại, các PR #1 (`QE-65`), #3 (`QE-64`), #5 (`QE-72`), #6 (`QE-66`), #8 (`QE-31`) chỉ nằm trên `develop @ 4ac5da1`.
- **Hành động bắt buộc:** Cherry-pick / đồng bộ các commit hardening của PR #4 từ `main` vào `develop` trước khi duyệt các PR attendance tiếp theo, và thiết lập rule mọi PR feature/fix phải mở vào `develop` theo [CONTRIBUTING.md](../../CONTRIBUTING.md).

### 3.2 Review chi tiết `QE-54` (`In Review` — PR #9 `feature/QE-54-attendance-attempts`, `@Dan28-05`)

Nhánh `origin/feature/QE-54-attendance-attempts` có phần net diff `QE-54` (`3076f54`, `27a62a5`, `731f3f2`) trên nền nhánh PR #4 (`5fb74e9`), tách `EVENT_NOT_PUBLISHED`, bổ sung metadata attempt (`method`, `attemptCount`, `maxAttempts: 3`, `remainingAttempts`, `canRetry`) vào `ApiError.details` và thêm 5 test trong `AttendanceIT` (`#18–#22`). Tuy nhiên **CHƯA ĐẠT ĐIỀU KIỆN MERGE** vì 4 vấn đề sau:

1. **[BLOCKER] Sai nhánh đích (`base=main`) & kéo theo phân kỳ:** PR #9 đang trỏ vào `main` (chứa merge `fix/QE-56-dynamic-qr-hardening`, khác lịch sử với `develop @ 4ac5da1` nên thiếu `PR #8` `QE-31` và gây xung đột lịch sử `14` commits, `+1027/-66`). **Yêu cầu:** sau khi `develop` tiếp nhận PR #4, rebase `feature/QE-54-attendance-attempts` lên `origin/develop` và đổi base PR #9 về `develop`.
2. **Code thừa/chưa dọn sạch sau commit `731f3f2`:**
   - `AttendanceService.processCheckIn(UUID eventId, String method, ...)` nhận tham số `method` nhưng vẫn gọi thẳng `qrTokenService.verify(...)` mà chưa guard/từ chối method khác.
   - Overload `reject(...)` 9 tham số cũ trong `AttendanceService` trở thành dead code.
   - `AttendanceAttemptService` vẫn giữ wrapper overload 8 tham số dư thừa.
3. **Thiếu test phủ đủ AC của `QE-54`:**
   - Chưa có integration test cho nhánh mới `EVENT_NOT_PUBLISHED` (kỳ vọng `409 EVENT_NOT_PUBLISHED`, `counted = false`).
   - Chưa có test tranh chấp đồng thời ở biên 3 lần thử (ví dụ 5 request QR sai song song khi đã có 2 lần sai: chỉ tối đa 1 request được tính lên 3, các request còn lại nhận `409 ATTEMPT_LIMIT_REACHED`, `counted = false`).
   - Yêu cầu chạy xanh toàn bộ `.\mvnw.cmd -B -ntp verify -Pintegration` trên PostgreSQL 17 sau rebase.
4. **Thiếu đồng bộ Contract & Frontend:**
   - Các trường `ApiError.details` (`method`, `attemptCount`, `maxAttempts`, `remainingAttempts`, `canRetry`) chưa được cập nhật vào [docs/api/sprint1-contract.md](../api/sprint1-contract.md).
   - **Chưa có nhánh/PR nào bên `QNU-Events FE`** cho `QE-54` (`AttendanceSlot` hiện chưa hiển thị bộ đếm lượt thử `x/3`).

### 3.3 PR #7 (`docs/import-contact` → `develop` — `QE-45`, `@YangKlee`)
- Chứa 4 commits (`QE45-*`) với 16 file tài liệu và CSV mẫu dưới thư mục `docs/import-contact/`.
- **Lỗi đặt tên (Typo Governance):** Tên nhánh và thư mục/tên file dùng **`import-contact`** thay vì **`import-contract`**. Cần đổi tên thư mục/liên kết về `docs/import-contract/` (và đối chiếu các mục mở `TBD-DATA-01`) trước khi hợp nhất chính thức vào bộ tài liệu.

### 3.4 PR #10, #11, #12 (`QE-12`, `QE-39`, `QE-40` → `develop`) — BLOCKER Quy tắc R6
- Các nhánh `origin/feature/QE-12-audience-quota-pools`, `origin/feature/QE-39-register-pool-allocation`, và `origin/feature/QE-40-cancel-promote-waitlist` **cả 3 đều tự tạo file `src/main/resources/db/migration/V2__event_pools.sql`** giống nhau và cùng sửa `docs/decisions.md` (thêm `A-003`, bỏ trống `A-002`).
- **Vi phạm `AGENTS.md` R6:** *"Chỉ Agent phụ trách nền tảng (A0) mới được tạo migration."* Ngoài xung đột merge giữa 3 PR, việc đóng băng migration `V2__...` cần tổng hợp đồng bộ schema Sprint 2 (pool quota, `waiting_at`, `payload_hash` của `QE-8`/`QE-54`, `email`/`google_sub` của `QE-36`/`QE-73`/`QE-74`, và role `CVHT` nếu được duyệt).
- **Khuyến nghị xử lý:** Giữ chưa merge PR #10/#11/#12; để A0 rà soát, tách migration `V2` chuẩn hóa vào `develop` trước rồi rebase code Java của `QE-12`/`QE-39`/`QE-40` lên trên.

### 3.5 Nhánh tài liệu QA (`docs/QE-82-Test-plan-manual-testcase`)
- Nhánh `origin/docs/QE-82-Test-plan-manual-testcase` hiện trỏ đúng vào `667a54e` (`origin/main`), **chưa có commit mới nào** (0 file test plan / manual test case mới được đẩy lên).

---

## 4. Lỗi Tích hợp Nghiêm trọng FE ↔ BE (`QE-72` & Contract Drift)

1. **[BREAKING] `GET /api/v1/me/history` (`HistoryController` vs FE `fetchHistory()`):**
   - PR #5 (`2fd6e69`, `QE-72`) trên BE `develop` đã nâng cấp `HistoryController.getHistory` sang trả về **`Page<HistoryItem>`** (phân trang kèm bộ lọc `academicYear`, `semester`, `attendanceStatus`, `upcoming`, `activityType`).
   - Phía `QNU-Events FE` (`develop @ 0fe4a3c`: `src/api/events.ts:86-88`, `src/features/history/index.tsx:17-22, 76-80`) vẫn khai báo kiểu trả về `HistoryItem[]` và gọi trực tiếp `.length`, `.filter()`, `.map()` trên response — gây lỗi runtime khi chạy nối với BE `develop`. Cả `HistoryPage.test.tsx` và `e2e/auth-and-routes.spec.ts` đều mock `HistoryItem[]` nên chưa phát hiện được khi chạy CI FE.
   - **Cách khắc phục:** Cập nhật FE `fetchHistory()`, `HistoryPage.tsx`, Vitest và Playwright mocks sang `Page<HistoryItem>` (`data.content`).
2. **Sai lệch DTO `RegistrationResponse`:** BE (`RegistrationResponse.java`) trả trường `alreadyRegistered`, trong khi FE (`src/features/events/types.ts:79`) vẫn định nghĩa `cached: boolean`.
3. **Sai lệch màn hình QR (`EventQrPage.tsx`):** Hardcode chu kỳ `10s` / refetch `9500ms` thay vì đọc `qrData.rotationSeconds`, và chưa có bộ đếm xác nhận trực tiếp như Figma.

---

## 5. Các Điểm Sai lệch Cần Sửa trong Tài liệu Sprint 1 của A2 (Tuân thủ `AGENTS.md` R1)

### 5.1 Trong `docs/test/sprint1-test-execution-report.md`
1. **§2.2 Số lượng test Auth sai:** Báo cáo ghi `AuthIT + SecurityConfigurationTest - 14 tests`; số test thực tế trong mã nguồn là **`13` tests** (`AuthIT`: 8 + `SecurityConfigurationTest`: 5).
2. **§2.2 Bịa đặt tính năng khóa brute-force & sai mã lỗi:** Báo cáo ghi *"Khóa tài khoản sau 5 lần đăng nhập sai liên tiếp -> 423 `ACCOUNT_LOCKED`"* và *"sai mật khẩu -> 401 `UNAUTHENTICATED`"*. **Thực tế trong `AuthService.java` và `AuthIT.java`:**
   - **Chưa có** logic đếm 5 lần sai / mã lỗi `423 ACCOUNT_LOCKED` trong Sprint 1 (`REQ-AUTH-015` thuộc Sprint 2+).
   - Tài khoản trạng thái `LOCKED` bị từ chối với **`403 FORBIDDEN`** (`AuthIT#loginWithLockedUserReturnsForbidden`).
   - Sai mật khẩu (hoặc không tồn tại tài khoản) trả **`401 INVALID_CREDENTIALS`**, không phải `UNAUTHENTICATED`.
3. **§1 & §2 Tổng số BE tests không khớp:** Báo cáo ghi `66` tests (tổng các tiểu mục 2.1–2.6 chỉ cộng ra `64`: `8+14+10+12+11+9`, bỏ sót `ApplicationIT`, `MigrationIT`, `SharedKernelSecurityIT`). Kiểm tra lịch sử git cho thấy số `@Test` thực tế tại commit `99c507f` (thời điểm hoàn tất tài liệu Sprint 1) là **`79`**, và trên `develop @ 4ac5da1` hiện tại là **`94`** (`13` unit + `81` IT).
4. **§3.2 Phạm vi Playwright E2E:** Ghi rõ cả `7` test Playwright chạy với **`page.route()` mock**, không chạy trên PostgreSQL 17 thật (tránh mâu thuẫn với ghi chú *"mọi bài kiểm tra đều được thực thi thật trên PostgreSQL 17"* ở §1).

### 5.2 Trong `docs/scrum/sprint1-demo-walkthrough.md`
1. **Bước 1 — Tên nút quick-fill:** Kịch bản ghi nút `"Nạp mẫu: owner01"`; nhãn nút thực tế trong `LoginPage.tsx:124` là **`"Chủ sự kiện"`** (và `"Sinh viên (K49)"` cho `sv001`).
2. **Mục 1.2 & Bước 4 — Lớp của `sv003`:** Kịch bản liệt kê `sv003` thuộc `K49CNTT1` để demo lỗi hết chỗ `409 CAPACITY_FULL` khi `sv001` & `sv002` đã giữ 2/2 suất; nhưng `DemoDataSeeder.java:72` seed `sv003` thuộc lớp **`K49CNTT2`** (`class2`), nên nếu sự kiện chỉ chọn `K49CNTT1` thì `sv003` sẽ gặp **`403 NOT_IN_AUDIENCE`** thay vì `CAPACITY_FULL`.
3. **Bước 7 (`/history`):** Kịch bản xem trang `/history` sẽ lỗi trên BE `develop` hiện tại cho tới khi FE đồng bộ `Page<HistoryItem>`.
4. **Mục 1.1 — Lệnh khởi động DB & BE:** Kịch bản dùng `docker start postgres` và `./mvnw spring-boot:run`; cần đồng bộ sang `docker compose up -d --wait postgres` và profile `dev` (`-Dspring-boot.run.profiles=dev`).

---

## 6. Nhắc nhở Scrum Master cho `QE-81` (Biên bản Scrum & DoD)

- **`QE-81`** (*"Biên bản Scrum Sprint 1 và Definition of Done"*, đang `To Do`, giao cho `Khánh Dương Nguyễn`):
  - **Tuyệt đối tuân thủ R1:** chỉ tổng hợp từ dữ kiện thật (các commit trong `git log`, lịch sử chuyển trạng thái/gia hạn trong `docs/scrum/jira-change-log.md`, kết quả chạy test thật, tình trạng PR thật).
  - Các phần nội dung họp (Planning, Daily Standup, Sprint Review, Retrospective, người tham dự, vướng mắc thực tế và xác nhận) phải do thành viên thật ghi nhận/xác nhận, **không tạo ngày họp giả hay lời thoại họp giả**.

---

## 7. Danh sách Việc Ưu tiên Ngay Tiếp theo

1. **A0 (`@GMIOS25`):** Đồng bộ PR #4 (`main` → `develop`); merge nhánh cleanup `QE-70`, FE Figma shell (`chore/QE-70-figma-shell-alignment`) và 3 tài liệu thiết kế đã verify (`QE-6`, `QE-8`, `QE-36`), sau đó cập nhật trạng thái Jira `QE-6`/`QE-8`/`QE-36` đúng thực tế (tuân thủ R10). Điều phối thiết kế migration `V2__...` thống nhất trước khi mở khóa PR #10/#11/#12.
2. **A3 (`@Dan28-05`):** Rebase PR #9 (`QE-54`) lên `develop`, dọn code thừa (`method`/`reject`), bổ sung test `EVENT_NOT_PUBLISHED` + concurrent 3-attempt, cập nhật `sprint1-contract.md`, chạy `.\mvnw.cmd -B -ntp verify -Pintegration` trên PostgreSQL 17, và bổ sung nhánh FE cho `QE-54`.
3. **A4 (`@KHANHSKY0808`) & FE:** Khắc phục lỗi breaking `Page<HistoryItem>` trên FE (`QE-72`) + cập nhật Jira `QE-31` sau khi hoàn tất phần FE/filter nếu cần.
4. **A2 (`@YangKlee`):** Đính chính số liệu R1 trong `sprint1-test-execution-report.md` và `sprint1-demo-walkthrough.md`; đổi tên `docs/import-contact` → `docs/import-contract` ở PR #7; hoàn thiện `QE-81` (chỉ dữ kiện thật) và `QE-82`.
