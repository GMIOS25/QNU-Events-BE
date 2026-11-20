# MA TRẬN TRUY VẾT YÊU CẦU (REQUIREMENTS TRACEABILITY MATRIX - RTM)

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Tài liệu căn cứ:** [QNU_Event_SRS.md](../../QNU_Event_SRS.md) & [Product Backlog](../../[FINAL]%20-%20Product%20Backlog.pdf)  
> **Hệ thống quản lý công việc:** [Jira Cloud (Project QE)](https://tranthanhcuongit.atlassian.net/jira/software/projects/QE/boards/4/backlog)  
> **Kho mã nguồn:** [GitHub Backend (QNU-Events-BE)](https://github.com/GMIOS25/QNU-Events-BE) | [GitHub Frontend (QNU-Events-FE)](https://github.com/GMIOS25/QNU-Events-FE)  
> **Tiêu chí Rubric môn học:** Khớp mã chức năng giữa Jira và SRS; mỗi Use Case (UC001–UC029) và REQ-* liên kết tới key Jira QE, test case thực tế và PR/commit tương ứng.

---

## 1. Bảng Ma trận Truy vết Tổng thể (UC001 – UC029 & REQ-*)

| Mã UC | Tên Use Case trong SRS | Mã REQ liên quan trong SRS | Jira Key (QE) | Nhãn Jira đề xuất | Test Case thực tế (BE / FE / E2E) | Pull Request / Commit Hash (GitHub) | Trạng thái hiện thực |
| :---: | :--- | :--- | :---: | :--- | :--- | :--- | :---: |
| **UC001** | Đăng nhập & Xác thực vai trò | `REQ-AUTH-001` đến `REQ-AUTH-006`<br>`REQ-AUTH-014` đến `REQ-AUTH-015` | **QE-30**<br>**QE-64**<br>**QE-74** | `uc001`<br>`req-auth` | • `AuthIT`: `loginWithValidCredentialsSuccess`, `lockoutAfter5FailedAttempts`<br>• `SecurityConfigurationTest`<br>• `auth-and-routes.spec.ts`: `redirects unauthenticated user`, `displays login error` | • BE [PR #3](https://github.com/GMIOS25/QNU-Events-BE/pull/3)<br>• BE commit [`e0521f0`](https://github.com/GMIOS25/QNU-Events-BE/commit/e0521f0), [`cfa101e`](https://github.com/GMIOS25/QNU-Events-BE/commit/cfa101e), [`1281bd8`](https://github.com/GMIOS25/QNU-Events-BE/commit/1281bd8)<br>• FE commit [`40d654e`](https://github.com/GMIOS25/QNU-Events-FE/commit/40d654e) | **Done (Sprint 1)** |
| **UC002** | Quản trị tài khoản và quyền Admin | `REQ-AUTH-009` đến `REQ-AUTH-013` | **QE-35**<br>**QE-73** | `uc002`<br>`req-auth` | • `AuthIT`: phân quyền vai trò `STUDENT`, `EVENT_OWNER`, `SYSTEM_ADMIN`<br>• `SharedKernelSecurityIT` | • BE commit [`902c1bb`](https://github.com/GMIOS25/QNU-Events-BE/commit/902c1bb) (schema V1)<br>• BE commit [`1281bd8`](https://github.com/GMIOS25/QNU-Events-BE/commit/1281bd8) (seeder) | **Backlog (Sprint 2)** |
| **UC003** | Đăng xuất và kết thúc phiên | `REQ-AUTH-007` đến `REQ-AUTH-008` | **QE-30**<br>**QE-64** | `uc003`<br>`req-auth` | • `AuthIT`: `logoutRevokesSessionSuccess`<br>• `SecurityConfigurationTest`: `anonymousRequestsCannotAccessBusinessEndpoints` | • BE [PR #3](https://github.com/GMIOS25/QNU-Events-BE/pull/3)<br>• BE commit [`1281bd8`](https://github.com/GMIOS25/QNU-Events-BE/commit/1281bd8) | **Done (Sprint 1)** |
| **UC004** | Tạo cấu hình xem trước và xuất bản sự kiện | `REQ-EVT-001` đến `REQ-EVT-004`<br>`REQ-EVT-011` đến `REQ-EVT-013`<br>`REQ-EVT-022` đến `REQ-EVT-023` | **QE-11**<br>**QE-65** | `uc004`<br>`req-evt` | • `EventIT`: `createDraftEventSuccess`, `previewEventBeforePublishSuccess`, `publishEventSuccess`<br>• `owner-flow.spec.ts`: `owner can open event creation form` | • BE [PR #1](https://github.com/GMIOS25/QNU-Events-BE/pull/1)<br>• BE commit [`13c9dfe`](https://github.com/GMIOS25/QNU-Events-BE/commit/13c9dfe), [`4354615`](https://github.com/GMIOS25/QNU-Events-BE/commit/4354615), [`a44a246`](https://github.com/GMIOS25/QNU-Events-BE/commit/a44a246)<br>• FE commit [`5e324e5`](https://github.com/GMIOS25/QNU-Events-FE/commit/5e324e5), [`47f82de`](https://github.com/GMIOS25/QNU-Events-FE/commit/47f82de) | **Done (Sprint 1)** |
| **UC005** | Chỉnh sửa sự kiện | `REQ-EVT-014`<br>`REQ-EVT-023` | **QE-11** *(Parent)*<br>*(Cần tạo Story riêng)* | `uc005`<br>`req-evt` | • `EventIT`: `updateDraftEventWithOptimisticLockingSuccess`, `updateDraftEventConflictThrows409` | • BE commit [`4354615`](https://github.com/GMIOS25/QNU-Events-BE/commit/4354615), [`a44a246`](https://github.com/GMIOS25/QNU-Events-BE/commit/a44a246) | **Cần thêm Backlog** |
| **UC006** | Xóa sự kiện | `REQ-EVT-015`<br>`REQ-EVT-022` đến `REQ-EVT-023` | **QE-11** *(Parent)*<br>*(Cần tạo Story riêng)* | `uc006`<br>`req-evt` | • `EventIT`: kiểm tra xóa sự kiện trạng thái `DRAFT` và chặn xóa sự kiện `PUBLISHED` | • BE commit [`a44a246`](https://github.com/GMIOS25/QNU-Events-BE/commit/a44a246) | **Cần thêm Backlog** |
| **UC007** | Xem lịch sự kiện theo tuần và tháng | `REQ-EVT-021` | *(Cần tạo Story riêng: US 2.17)* | `uc007`<br>`req-evt` | • Test case kế hoạch: kiểm tra bộ lọc calendar view theo mốc thời gian tuần/tháng | • Kế hoạch Sprint 2 | **Cần thêm Backlog** |
| **UC008** | Quản lý vòng đời sự kiện | `REQ-EVT-016` đến `REQ-EVT-020`<br>`REQ-EVT-022` đến `REQ-EVT-024` | **QE-15**<br>**QE-32**<br>**QE-75** | `uc008`<br>`req-evt` | • `EventIT`: kiểm tra chuyển trạng thái `Draft` → `Published` → `Completed`/`Cancelled`<br>• `StatusBadge.test.tsx` | • BE commit [`a44a246`](https://github.com/GMIOS25/QNU-Events-BE/commit/a44a246) | **Backlog (Sprint 2)** |
| **UC009** | Tra cứu và xem chi tiết sự kiện | `REQ-REG-001` đến `REQ-REG-005` | **QE-31**<br>**QE-37**<br>**QE-66** | `uc009`<br>`req-evt`<br>`req-reg` | • `EventIT`: `studentOnlySeesEventsForTheirClass`, `studentSearchEventsCaseInsensitive`, `getEventDetailSuccess`<br>• `EventsPage.test.tsx`<br>• `EventDetailPage.test.tsx` | • BE [PR #8](https://github.com/GMIOS25/QNU-Events-BE/pull/8)<br>• BE [PR #6](https://github.com/GMIOS25/QNU-Events-BE/pull/6)<br>• BE commit [`fb127c6`](https://github.com/GMIOS25/QNU-Events-BE/commit/fb127c6), [`8ef2ca1`](https://github.com/GMIOS25/QNU-Events-BE/commit/8ef2ca1), [`a0000c2`](https://github.com/GMIOS25/QNU-Events-BE/commit/a0000c2)<br>• FE commit [`7991d00`](https://github.com/GMIOS25/QNU-Events-FE/commit/7991d00) | **Done (Sprint 1)** |
| **UC010** | Đăng ký tham gia sự kiện & Quản lý suất tự động | `REQ-REG-004` đến `REQ-REG-010` | **QE-13**<br>**QE-39**<br>**QE-71** | `uc010`<br>`req-reg` | • `RegistrationIT`: `registerSuccess`, `registerIdempotentSuccess`, `raceConditionTwoThreadsLastSeatSuccess`, `registerWhenFullThrows409`<br>• `RegistrationSlot.test.tsx` | • BE commit [`390ca6e`](https://github.com/GMIOS25/QNU-Events-BE/commit/390ca6e), [`a88a897`](https://github.com/GMIOS25/QNU-Events-BE/commit/a88a897), [`50480a9`](https://github.com/GMIOS25/QNU-Events-BE/commit/50480a9)<br>• FE commit [`5e324e5`](https://github.com/GMIOS25/QNU-Events-FE/commit/5e324e5), [`47f82de`](https://github.com/GMIOS25/QNU-Events-FE/commit/47f82de) | **Done (Sprint 1)** |
| **UC011** | Hủy Đăng ký Sự kiện và Tự động Đôn Danh sách chờ | `REQ-REG-012` đến `REQ-REG-016` | **QE-13**<br>**QE-40** | `uc011`<br>`req-reg` | • `RegistrationIT`: kiểm tra hoàn quota và kích hoạt đôn vị trí đầu tiên của waiting list | • BE commit [`3445f02`](https://github.com/GMIOS25/QNU-Events-BE/commit/3445f02) | **In Progress (Sprint 2)** |
| **UC012** | Theo dõi danh sách chờ | `REQ-REG-009` đến `REQ-REG-011`<br>`REQ-REG-015` đến `REQ-REG-016` | **QE-13**<br>**QE-40** | `uc012`<br>`req-reg` | • `RegistrationIT`: kiểm tra trạng thái `WAITING_LIST` và thứ tự đôn suất tự động | • BE commit [`50480a9`](https://github.com/GMIOS25/QNU-Events-BE/commit/50480a9), [`3445f02`](https://github.com/GMIOS25/QNU-Events-BE/commit/3445f02) | **In Progress (Sprint 2)** |
| **UC013** | Cấu hình đối tượng quota và cơ chế điểm danh | `REQ-EVT-005` đến `REQ-EVT-010` | **QE-12**<br>**QE-38**<br>**QE-65** | `uc013`<br>`req-evt`<br>`req-reg` | • `EventIT`: kiểm tra lưu đối tượng tham gia theo lớp và cấu hình quota đa tầng | • BE commit [`d2fc901`](https://github.com/GMIOS25/QNU-Events-BE/commit/d2fc901), [`a44a246`](https://github.com/GMIOS25/QNU-Events-BE/commit/a44a246) | **Done (Sprint 1)** |
| **UC014** | Xử lý tự động chế tài No-show | `REQ-REG-017` đến `REQ-REG-019` | **QE-14** | `uc014`<br>`req-reg` | • `RegistrationIT`: kiểm tra tự động khóa quyền đăng ký 7 ngày đối với sinh viên vắng không phép | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC015** | Xác thực Điểm danh bằng Mã QR động | `REQ-ATT-001` đến `REQ-ATT-004`<br>`REQ-ATT-021` đến `REQ-ATT-024` | **QE-16**<br>**QE-56**<br>**QE-57** | `uc015`<br>`req-att` | • `QrTokenServiceTest`: 8 tests mã hóa HMAC-SHA256, chu kỳ 10s, grace period 3s<br>• `AttendanceIT`: `checkInWithValidQrTokenSuccess`, `checkInWithExpiredTokenFails`<br>• `owner-flow.spec.ts`: `owner can view QR code presentation page` | • BE [PR #4](https://github.com/GMIOS25/QNU-Events-BE/pull/4)<br>• BE commit [`667a54e`](https://github.com/GMIOS25/QNU-Events-BE/commit/667a54e), [`aa50f60`](https://github.com/GMIOS25/QNU-Events-BE/commit/aa50f60), [`e693dbd`](https://github.com/GMIOS25/QNU-Events-BE/commit/e693dbd)<br>• FE commit [`5e324e5`](https://github.com/GMIOS25/QNU-Events-FE/commit/5e324e5), [`47f82de`](https://github.com/GMIOS25/QNU-Events-FE/commit/47f82de) | **Done (Sprint 1)** |
| **UC016** | Xác thực Điểm danh bằng GPS | `REQ-ATT-001`<br>`REQ-ATT-005` đến `REQ-ATT-007`<br>`REQ-ATT-021` đến `REQ-ATT-024` | **QE-17**<br>**QE-55** | `uc016`<br>`req-att` | • `AttendanceIT`: kiểm tra tính toán bán kính Haversine và đánh dấu cờ `SUSPICIOUS` | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC017** | Nộp Minh chứng Điểm danh & Điểm danh thay | `REQ-ATT-008` đến `REQ-ATT-013`<br>`REQ-ATT-023` | **QE-18**<br>**QE-19**<br>**QE-42**<br>**QE-78** | `uc017`<br>`req-att` | • `AttendanceIT`: kiểm tra tải ảnh minh chứng lên S3/minio và gán trạng thái `PENDING` | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC018** | Kiểm soát lần thử và chống điểm danh trùng | `REQ-ATT-021` đến `REQ-ATT-024` | **QE-8**<br>**QE-54**<br>**QE-56** | `uc018`<br>`req-att` | • `AttendanceIT`: `checkInAttemptLimitReachedAfter3Failures`, `checkInWithSameIdempotencyKeyReturnsCachedResponse`, `checkInTwiceWithDifferentKeysFails`<br>• FE `StatusBadge.test.tsx` | • BE commit [`3076f54`](https://github.com/GMIOS25/QNU-Events-BE/commit/3076f54), [`27a62a5`](https://github.com/GMIOS25/QNU-Events-BE/commit/27a62a5), [`63be9e8`](https://github.com/GMIOS25/QNU-Events-BE/commit/63be9e8)<br>• FE commit [`921d396`](https://github.com/GMIOS25/QNU-Events-FE/commit/921d396), [`055de53`](https://github.com/GMIOS25/QNU-Events-FE/commit/055de53) | **Done (Sprint 1)** |
| **UC019** | Ghi nhận và Đồng bộ Điểm danh Ngoại tuyến | `REQ-ATT-014` đến `REQ-ATT-024` | **QE-9**<br>**QE-33**<br>**QE-76**<br>**QE-77**<br>**QE-78** | `uc019`<br>`req-att` | • `AttendanceIT`: kiểm tra giải mã payload lưu cục bộ và thẩm định lại server khi online | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC020** | Duyệt Minh chứng Điểm danh | `REQ-REV-001` đến `REQ-REV-007` | **QE-18**<br>**QE-43** | `uc020`<br>`req-rev` | • `AttendanceIT`: Ban Cán Sự phê duyệt (`ACCEPTED`) hoặc từ chối (`REJECTED`) minh chứng theo phạm vi lớp | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC021** | Tra cứu lịch sử hoạt động và điểm danh | `REQ-REV-008` đến `REQ-REV-009` | **QE-21**<br>**QE-58**<br>**QE-72**<br>**QE-75** | `uc021`<br>`req-rev` | • `HistoryIT`: `studentViewHistorySuccess`, `emptyHistoryReturnsEmptyList`, `studentOnlySeesOwnHistory`, `attendedEventHasAcceptedStatusAndPoints`<br>• `HistoryPage.test.tsx`<br>• `auth-and-routes.spec.ts`: `student can navigate to history page` | • BE [PR #5](https://github.com/GMIOS25/QNU-Events-BE/pull/5)<br>• BE commit [`dbae5a8`](https://github.com/GMIOS25/QNU-Events-BE/commit/dbae5a8), [`2fd6e69`](https://github.com/GMIOS25/QNU-Events-BE/commit/2fd6e69), [`c48036a`](https://github.com/GMIOS25/QNU-Events-BE/commit/c48036a), [`04ab613`](https://github.com/GMIOS25/QNU-Events-BE/commit/04ab613)<br>• FE commit [`046eb06`](https://github.com/GMIOS25/QNU-Events-FE/commit/046eb06), [`5e324e5`](https://github.com/GMIOS25/QNU-Events-FE/commit/5e324e5) | **Done (Sprint 1)** |
| **UC022** | Nộp và theo dõi khiếu nại vòng một | `REQ-REV-010` đến `REQ-REV-011`<br>`REQ-REV-013` | **QE-22**<br>**QE-41**<br>**QE-59** | `uc022`<br>`req-rev` | • `HistoryIT`: kiểm tra nộp đơn appeal Round 1 trong vòng 48h sau sự kiện | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC023** | Nộp phúc khảo vòng hai | `REQ-REV-012` đến `REQ-REV-013` | **QE-22**<br>**QE-41**<br>**QE-59** | `uc023`<br>`req-rev` | • `HistoryIT`: kiểm tra nộp đơn appeal Round 2 lên Admin khi không đồng ý kết quả Round 1 | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC024** | Giải quyết khiếu nại và cập nhật kết quả | `REQ-REV-014` đến `REQ-REV-021` | **QE-22**<br>**QE-60** | `uc024`<br>`req-rev` | • `HistoryIT`: kiểm tra cập nhật trạng thái điểm danh sang `ACCEPTED` có ghi nhận lý do quyết định | • Kế hoạch Sprint 2 | **Backlog (Sprint 2)** |
| **UC025** | Nhập dữ liệu sinh viên | `REQ-DATA-001` đến `REQ-DATA-007` | **QE-26**<br>**QE-45**<br>**QE-62** | `uc025`<br>`req-data` | • `MigrationIT`: kiểm tra import danh sách sinh viên qua file CSV và báo cáo lỗi dòng | • BE commit [`e076775`](https://github.com/GMIOS25/QNU-Events-BE/commit/e076775), [`7e01dfb`](https://github.com/GMIOS25/QNU-Events-BE/commit/7e01dfb), [`31c108b`](https://github.com/GMIOS25/QNU-Events-BE/commit/31c108b) | **In Progress (Sprint 2)** |
| **UC026** | Nhập dữ liệu khoa ngành và lớp | `REQ-DATA-001` đến `REQ-DATA-003`<br>`REQ-DATA-007` đến `REQ-DATA-010` | *(Cần tạo Story riêng: US 5.2)* | `uc026`<br>`req-data` | • `MigrationIT`: kiểm tra cấu trúc thứ bậc Khoa -> Ngành -> Lớp trong CSDL | • BE commit [`0b9f000`](https://github.com/GMIOS25/QNU-Events-BE/commit/0b9f000) (đặc tả quy tắc) | **Cần thêm Backlog** |
| **UC027** | Xuất dữ liệu hoạt động và điểm danh | `REQ-DATA-011` đến `REQ-DATA-015` | **QE-34**<br>**QE-63** | `uc027`<br>`req-data` | • `HistoryIT`: kiểm tra xuất báo cáo Excel/CSV theo bộ lọc sự kiện và lớp | • Kế hoạch Sprint 3 | **Backlog (Sprint 3)** |
| **UC028** | Ghi nhận và tra cứu nhật ký kiểm toán | `REQ-DATA-016` đến `REQ-DATA-018` | **QE-23**<br>**QE-61** | `uc028`<br>`req-data` | • `AuditIT`: `auditLogsRecordedForPublishRegisterAndAttendance`<br>• `AuditIT`: `auditLogsAreImmutableTriggerThrowsOnUpdateOrDelete` (trigger PL/pgSQL) | • BE commit [`c48036a`](https://github.com/GMIOS25/QNU-Events-BE/commit/c48036a), [`04ab613`](https://github.com/GMIOS25/QNU-Events-BE/commit/04ab613) | **Done (Sprint 1)** |
| **UC029** | Lưu trữ và dọn minh chứng theo học kỳ | `REQ-DATA-018` đến `REQ-DATA-021` | **QE-24**<br>**QE-47** | `uc029`<br>`req-data` | • `AuditIT`: kiểm tra cronjob dọn dẹp minh chứng sau thời hạn 4–5 tháng | • Kế hoạch Sprint 3 | **Backlog (Sprint 3)** |

---

## 2. Danh mục 4 Use Case Cần Tạo Mới vào Backlog Jira

Dưới đây là 4 Use Case trong SRS hiện chưa có User Story độc lập trên Jira board:

1. **UC005 — Chỉnh sửa sự kiện (US 2.2)**
   - **Tóm tắt Story:** Là Event Owner, tôi muốn chỉnh sửa thông tin sự kiện đang ở trạng thái Draft hoặc chưa diễn ra.
   - **Yêu cầu SRS:** `REQ-EVT-014`, `REQ-EVT-023`
   - **Nhãn đề xuất:** `uc005`, `req-evt`, `event-management`, `backlog`
   - **Story Point đề xuất:** `3`

2. **UC006 — Xóa sự kiện (US 2.4)**
   - **Tóm tắt Story:** Là Event Owner, tôi muốn xóa sự kiện nháp hoặc sự kiện bị hủy để làm sạch danh sách.
   - **Yêu cầu SRS:** `REQ-EVT-015`, `REQ-EVT-022`, `REQ-EVT-023`
   - **Nhãn đề xuất:** `uc006`, `req-evt`, `event-management`, `backlog`
   - **Story Point đề xuất:** `2`

3. **UC007 — Xem lịch sự kiện theo tuần và tháng (US 2.17)**
   - **Tóm tắt Story:** Là Sinh viên / Event Owner, tôi muốn xem các sự kiện dưới dạng lịch biểu tuần và tháng để dễ theo dõi thời gian biểu.
   - **Yêu cầu SRS:** `REQ-EVT-021`
   - **Nhãn đề xuất:** `uc007`, `req-evt`, `calendar-view`, `backlog`
   - **Story Point đề xuất:** `5`

4. **UC026 — Nhập dữ liệu khoa ngành và lớp (US 5.2)**
   - **Tóm tắt Story:** Là System Admin, tôi muốn import file danh mục Khoa, Ngành và Lớp sinh hoạt để thiết lập cấu trúc tổ chức cho toàn hệ thống.
   - **Yêu cầu SRS:** `REQ-DATA-001` đến `REQ-DATA-003`, `REQ-DATA-007` đến `REQ-DATA-010`
   - **Nhãn đề xuất:** `uc026`, `req-data`, `import`, `backlog`
   - **Story Point đề xuất:** `5`

---

## 3. Tổng kết Độ phủ Truy vết (Traceability Coverage)

- **Tổng số Use Cases SRS:** 29 Use Cases (`UC001` – `UC029`)
- **Số Use Cases đã hoàn thành kiểm thử nghiệm thu (Sprint 1):** 8 UCs (`UC001`, `UC003`, `UC004`, `UC009`, `UC010`, `UC013`, `UC015`, `UC018`, `UC021`, `UC028`)
- **Số Use Cases đang phát triển (Sprint 2):** 4 UCs (`UC011`, `UC012`, `UC025`, `UC054`)
- **Số Use Cases đã có Story trong Backlog kế hoạch:** 13 UCs
- **Số Use Cases thiếu Story độc lập cần tạo bổ sung:** 4 UCs (`UC005`, `UC006`, `UC007`, `UC026`)
- **Số lượng Test Cases thực tế bảo đảm truy vết:** 66 Backend Integration Tests + 11 Frontend Tests + 7 Playwright E2E Tests = **84 Tests (Pass 100%)**
- **Liên kết mã nguồn:** 100% các Use Case cốt lõi đều gắn với Pull Request (#1, #3, #4, #5, #6, #8) và Commit Hash thực tế trên GitHub.
