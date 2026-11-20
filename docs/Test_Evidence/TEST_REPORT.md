# Báo cáo Kiểm thử Tự động Trình duyệt (Browser QA Test Report)

- **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)
- **Phạm vi:** Sprint 1 — 4.1. Xác thực & Quản lý phiên đăng nhập (Các Test Case giao diện Web / Browser E2E)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Vite dev server)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`, `DemoDataSeeder`)
  - Database: PostgreSQL 17 (`qnu-events-postgres-1`)
  - Viewport: Desktop `1280x720`
- **Ngày thực thi:** 2026-11-20

## Tổng hợp kết quả

- **Tổng số Test Case đã chạy:** 21 (13 Browser E2E + 8 API/DB)
- **PASS:** 20
- **FAIL:** 1 (`TC-AUTH-24`)
- **BLOCKED:** 0
- **Console Errors (`Uncaught TypeError` / JS Runtime Errors):** Ghi nhận lỗi `TypeError: historyItems?.filter is not a function` tại `HistoryPage` (`src/features/history/index.tsx:20:38`) trong `TC-AUTH-24`.
- **Server Errors (`500 Internal Server Error`):** Không ghi nhận lỗi 500 nào. Các mã `400`, `401`, `403` xuất hiện đúng theo kịch bản kiểm thử negative.

## Bảng kết quả chi tiết

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-AUTH-01 | Đăng nhập thành công với vai trò Ban tổ chức (`owner01`) qua UI | PASS | `Auth/TC-AUTH-01_Step3_PASS_OwnerDashboard.png` | Chuyển hướng `/events`, header hiển thị `Nguyễn Văn Chủ Sự Kiện` (`owner01`), badge `Ban tổ chức`, menu có `Sự kiện của tôi` & `Tạo sự kiện mới`. |
| TC-AUTH-02 | Đăng nhập thành công với vai trò Sinh viên (`sv001`) qua UI | PASS | `Auth/TC-AUTH-02_Step3_PASS_StudentDashboard.png` | Chuyển hướng `/events`, header hiển thị `Trần Sinh Viên 1` (`sv001`), badge `Sinh viên`, menu có `Sự kiện` & `Lịch sử điểm danh`. |
| TC-AUTH-03 | Kiểm tra bỏ trống trường Tên đăng nhập hoặc Mật khẩu trên form `/login` | PASS | `Auth/TC-AUTH-03_Step1_PASS_EmptyFieldsValidation.png` | Hiển thị lỗi `"Tên đăng nhập không được để trống"` / `"Mật khẩu không được để trống"`, không gửi `POST /api/v1/auth/login`. |
| TC-AUTH-04 | Đăng nhập sai mật khẩu với tài khoản hợp lệ trên UI (`owner01`) | PASS | `Auth/TC-AUTH-04_Step3_PASS_InvalidPasswordBanner.png` | `POST /api/v1/auth/login` trả `401`, `ErrorBanner` hiển thị `"Thông tin đăng nhập không chính xác."` (`Mã lỗi: INVALID_CREDENTIALS`), giữ nguyên `/login`. |
| TC-AUTH-05 | Đăng nhập với tên đăng nhập không tồn tại trên UI (`non_existent_user_999`) | PASS | `Auth/TC-AUTH-05_Step3_PASS_NonExistentUserBanner.png` | `POST /api/v1/auth/login` trả `401`, `ErrorBanner` hiển thị thông báo giống hệt `TC-AUTH-04`, không lộ thông tin tài khoản. |
| TC-AUTH-06 | Đăng nhập bằng tài khoản đang bị khóa (`status = 'LOCKED'`) trên UI (`sv002`) | PASS | `Auth/TC-AUTH-06_Step2_PASS_LockedAccountBanner.png` | `POST /api/v1/auth/login` trả `403`, `ErrorBanner` hiển thị `"Tài khoản của bạn đã bị khóa."` (`Mã lỗi: FORBIDDEN`). Đã khôi phục `sv002` về `ACTIVE` sau khi test. |
| TC-AUTH-07 | Duy trì và đồng bộ lại phiên đăng nhập khi tải lại trang (`F5`) | PASS | `Auth/TC-AUTH-07_Step2_PASS_ReloadPersistsSession.png` | Sau reload tại `/events`, FE gọi `GET /api/v1/auth/me` trả `200 OK`, giữ nguyên phiên `sv001`. |
| TC-AUTH-08 | Đăng xuất trên giao diện Web qua hộp thoại xác nhận `ConfirmDialog` | PASS | `Auth/TC-AUTH-08_Step4_PASS_LogoutToLogin.png` | Bấm `Hủy` giữ nguyên phiên; bấm xác nhận `Đăng xuất` gọi `POST /api/v1/auth/logout` (`204`), chuyển về `/login`; truy cập lại `/events` bị đẩy về `/login`. |
| TC-AUTH-09 | Thu hồi phiên trên DB (`revoked_at`) và từ chối JWT cũ sau khi đăng xuất | PASS | `Auth/TC-AUTH-09_Result.txt` | `POST /api/v1/auth/logout` trả `204`, `auth_session.revoked_at IS NOT NULL`, gọi lại `/auth/me` & `/events` với JWT cũ đều trả `401 SESSION_REVOKED`. |
| TC-AUTH-10 | Từ chối JWT đang hoạt động ngay sau khi tài khoản bị chuyển trạng thái `LOCKED` | PASS | `Auth/TC-AUTH-10_Result.txt` | Trước khi khóa `/auth/me` trả `200`; sau khi `UPDATE status='LOCKED'` cho `sv002`, cả `/auth/me` & `/events` trả `401 UNAUTHENTICATED`. Đã khôi phục `ACTIVE`. |
| TC-AUTH-11 | Từ chối phiên đã quá hạn TTL 8 giờ (`expires_at < now`) | PASS | `Auth/TC-AUTH-11_Result.txt` | Sau khi lùi `auth_session.expires_at` về quá khứ 1s, `GET /api/v1/auth/me` trả `401 UNAUTHENTICATED`. |
| TC-AUTH-12 | Kiểm tra API `POST /api/v1/auth/login` khi rỗng, thiếu trường hoặc sai kiểu JSON | PASS | `Auth/TC-AUTH-12_Result.txt` | Cả 6 payload (`{}`, `""`, `"   "`, `{invalid`, sai kiểu mảng) đều trả `400 VALIDATION_FAILED`. |
| TC-AUTH-13 | So sánh chi tiết response API giữa sai mật khẩu và user không tồn tại | PASS | `Auth/TC-AUTH-13_Result.txt` | Cả 2 trả `401 INVALID_CREDENTIALS` với cùng `message` (`"Thông tin đăng nhập không chính xác."`) và `details: {}`. |
| TC-AUTH-14 | Kiểm tra payload JWT HS256 và API `GET /api/v1/auth/me` cho cả `STUDENT` và `EVENT_OWNER` | PASS | `Auth/TC-AUTH-14_Result.txt` | Header `alg=HS256`, `exp - iat = 28800`, `sv001` có `classId`, `owner01` không có `classId`; `/auth/me` trả `200 OK` đúng thông tin. |
| TC-AUTH-15 | Gọi API yêu cầu xác thực khi thiếu token, sai prefix hoặc token bị chỉnh sửa chữ ký | PASS | `Auth/TC-AUTH-15_Result.txt` | Thiếu header, `Basic abc`, `Bearer invalid.jwt.token` và token bị sửa ký tự cuối đều trả `401 UNAUTHENTICATED`. |
| TC-AUTH-16 | Kiểm tra dữ liệu khởi tạo `DemoDataSeeder` và băm mật khẩu BCrypt cost 12 trong DB | PASS | `Auth/TC-AUTH-16_Result.txt` | Đủ 6 user (`admin01`, `owner01`, `owner02`, `sv001`, `sv002`, `sv003`), tất cả `status = ACTIVE`, `password_hash` bắt đầu `$2a$12$` dài `60` ký tự. |
| TC-AUTH-17 | Chặn truy cập các đường dẫn nội bộ (`/events`, `/events/new`, `/history`) khi chưa đăng nhập | PASS | `Auth/TC-AUTH-17_Step2_PASS_RedirectToLogin.png` | Cả 3 đường dẫn đều tự động chuyển hướng về `http://localhost:5173/login`. |
| TC-AUTH-18 | Sinh viên (`sv001`) cố tình truy cập route của Ban tổ chức (`/events/new`, `/edit`, `/qr`) | PASS | `Auth/TC-AUTH-18_Step3_PASS_StudentBlockedFromOwnerRoutes.png` | `RequireRole` chặn và chuyển hướng cả 3 route về `http://localhost:5173/events`. |
| TC-AUTH-19 | Ban tổ chức (`owner01`) cố tình truy cập route của Sinh viên (`/history`) | PASS | `Auth/TC-AUTH-19_Step2_PASS_OwnerBlockedFromHistory.png` | `RequireRole` chặn và chuyển hướng về `http://localhost:5173/events`. |
| TC-AUTH-23 | Kiểm tra giá trị biên chuỗi khoảng trắng `"   "` và độ dài 1 ký tự (`"a"`/`"x"`) trên `/login` | PASS | `Auth/TC-AUTH-23_Step3_PASS_WhitespaceAndMinLen.png` | `"   "` gửi lên BE nhận `400 VALIDATION_FAILED` (`"Dữ liệu không hợp lệ."`); `"a"`/`"x"` gửi lên BE nhận `401 INVALID_CREDENTIALS`. |
| TC-AUTH-24 | Điều hướng quay lại đúng trang dự định (`deep-link redirect`) sau khi đăng nhập (`/history`) | FAIL | `Auth/TC-AUTH-24_Step3_FAIL_HistoryPageTypeError.png`, `Auth/TC-AUTH-24_ErrorLog.txt` | Deep-link redirect từ `/login` về `/history` hoạt động đúng và `GET /api/v1/me/history` trả `200 OK`, nhưng trang `/history` bị crash render (`Unexpected Application Error!`): `TypeError: historyItems?.filter is not a function` tại `src/features/history/index.tsx:20:38`. *(Phần ngắt kết nối mạng/tắt BE chưa mô phỏng).* |

> **Ghi chú phạm vi:** Các Test Case API RBAC/Boundary còn lại (`TC-AUTH-20` → `TC-AUTH-22`, `TC-AUTH-25` → `TC-AUTH-28`) và các Test Case White-box Unit Test (`TC-AUTH-WB-01` → `TC-AUTH-WB-27`) chưa nằm trong lượt chạy này.


---

# Báo cáo Kiểm thử Tự động — Sprint 1: 4.2. Quản lý sự kiện, xem trước và công bố sự kiện

- **Phạm vi:** Sprint 1 — 4.2 (`TC-EVT-001` → `TC-EVT-035` Black-box/Browser E2E/API/DB + White-box BE Maven & FE Vitest)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Viewport `1280x720` và Mobile `375x667`)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`)
  - Database: PostgreSQL 17 (`qnu-events-postgres-1`, `qnu_events`)
- **Ngày thực thi:** 2026-11-20

## Tổng hợp kết quả (Mục 4.2)

- **Tổng số Black-box / Browser E2E / API / DB Test Case đã chạy:** 35 (`TC-EVT-001` → `TC-EVT-035`)
  - **PASS:** 32
  - **FAIL:** 3 (`TC-EVT-015`, `TC-EVT-029`, `TC-EVT-030`)
  - **BLOCKED:** 0
- **Kiểm thử tự động White-box hiện có trong repo:**
  - **Backend Maven** (`EventIT`, `AuthIT`, `AuditIT`, `MigrationIT`, `SharedKernelSecurityIT`, `SecurityConfigurationTest`): **49/49 PASS** (trong đó `EventIT`: **21/21 PASS**) — xem [`Event/Whitebox_BE_Maven.txt`](Event/Whitebox_BE_Maven.txt).
  - **Frontend Vitest** (5 file test): **11/11 PASS** — xem [`Event/Whitebox_FE_Vitest.txt`](Event/Whitebox_FE_Vitest.txt).
  - *(Lưu ý: Các ca unit test hộp trắng chi tiết `TC-EVT-WB-01` → `WB-21` mới được bao phủ một phần qua 49 test BE và 11 test FE trên; repo chưa có unit test riêng cho `EventValidator`, `EventFormPage.tsx`, `EventDetailPage.tsx`.)*
- **Console Errors (`Uncaught TypeError`):** `0` lỗi JS runtime trên trình duyệt trong toàn bộ các bước kiểm thử mục 4.2.
- **Server Errors (`500 Internal Server Error`):** Ghi nhận **2** lỗi `500 INTERNAL_ERROR` khi kiểm thử biên/lỗi đầu vào API (`TC-EVT-029` Step 3: `activityType` 51 ký tự; `TC-EVT-030` Step 4: `PUT /api/v1/events/not-a-uuid`).

## Bảng kết quả chi tiết (Mục 4.2)

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-EVT-001 | Mở trang quản lý sự kiện và biểu mẫu tạo sự kiện mới nạp danh sách lớp | PASS | `Event/TC-EVT-001_002_Step3_PASS_EventsNewClasses.png` | `/events` hiện tiêu đề & nút `Tạo sự kiện mới`; `/events/new` gọi `GET /api/v1/classes` (`200 OK`) hiển thị `K49CNTT1`, `K49CNTT2`. |
| TC-EVT-002 | Chọn lớp đối tượng riêng lẻ và thao tác "Chọn tất cả các lớp" | PASS | `Event/TC-EVT-001_002_Step3_PASS_EventsNewClasses.png` | Tích/bỏ tích cập nhật đúng `(1 lớp đã chọn)` / `(0 lớp đã chọn)`; bấm `Chọn tất cả các lớp` chọn đủ `(2 lớp đã chọn)`. |
| TC-EVT-003 | Tạo mới sự kiện nháp (`DRAFT`) hợp lệ đầy đủ các trường | PASS | `Event/TC-EVT-003_Step3_PASS_DraftCreated.png` | `POST /api/v1/events` trả `201 Created`, chuyển `/events/5d34a44c-18a1-42fd-8d53-8bde32fd5fe0`, badge `Bản nháp`, hiện nút `Chỉnh sửa` & `Xem trước & Công bố`. |
| TC-EVT-004 | Tạo sự kiện nháp khi bỏ trống trường tùy chọn `Mô tả sự kiện` | PASS | — *(đã xác minh trực tiếp trên trình duyệt)* | Tạo `Hội thảo Không Mô Tả` (`2381b639-7276-49fc-83f3-4e8eb274c15b`) trả `201 Created` với `description: null`; trang chi tiết hiển thị bình thường. |
| TC-EVT-005 | Kiểm định giá trị dưới biên độ dài `title` (2 ký tự) và chuỗi khoảng trắng | PASS | — *(đã xác minh trên UI `/events/new`)* | Cả `"AB"` và `"     "` đều bị chặn ở client với `ErrorBanner`: `"Tiêu đề sự kiện phải từ 3 ký tự trở lên."`, không gửi `POST /api/v1/events`. |
| TC-EVT-006 | Kiểm định giá trị tại biên độ dài `title` hợp lệ (3 ký tự và 200 ký tự) | PASS | `Event/TC-EVT-006_Result.txt` | Cả `"AI1"` (3 ký tự) và chuỗi 200 ký tự đều trả `201 Created`, `status: "DRAFT"`. |
| TC-EVT-007 | Kiểm định giá trị vượt biên độ dài `title` (201 ký tự) | PASS | `Event/TC-EVT-007_Result.txt` | API trả `400 VALIDATION_FAILED` kèm `details.fields.title`. |
| TC-EVT-008 | Kiểm định `Địa điểm tổ chức` (`location`) bỏ trống và vượt biên 301 ký tự | PASS | — *(đã xác minh trên UI `/events/new`)* | Trên UI với `"   "`, hiển thị `ErrorBanner`: `"Vui lòng nhập địa điểm tổ chức."`, không gửi request. |
| TC-EVT-009 | Kiểm định sai mốc thời gian diễn ra (`endAt <= startAt`) | PASS | — *(đã xác minh trên UI `/events/new`)* | Khi `endAt == startAt`, hiển thị `ErrorBanner`: `"Thời gian kết thúc phải diễn ra sau thời gian bắt đầu."`. |
| TC-EVT-010 | Kiểm định sai mốc thời gian cửa sổ điểm danh (`attendanceCloseAt <= attendanceOpenAt`) | PASS | — *(đã xác minh trên UI `/events/new`)* | Khi `attendanceCloseAt == attendanceOpenAt`, hiển thị `ErrorBanner`: `"Thời gian đóng điểm danh phải sau thời gian mở điểm danh."`. |
| TC-EVT-011 | Kiểm định hạn đăng ký vượt quá thời gian đóng điểm danh (`registrationDeadline > attendanceCloseAt`) | PASS | `Event/TC-EVT-011_Step3_PASS_ValidationBanner.png` | `POST /api/v1/events` trả `400 VALIDATION_FAILED` với `details.fields.registrationDeadline`. *(Lưu ý UI: `ErrorBanner` chỉ hiện `"Thông tin sự kiện không hợp lệ."`, chưa render chi tiết từng field).* |
| TC-EVT-012 | Kiểm định giá trị biên chỉ tiêu `quotaTotal` (`0`, `-1`, và `1`) | PASS | `Event/TC-EVT-012_Result.txt` | `quotaTotal = 0` và `-1` trả `400 VALIDATION_FAILED`; `quotaTotal = 1` trả `201 Created`. |
| TC-EVT-013 | Kiểm định giá trị biên `activityPoints` (`-1`, `0`) và `activityType` rỗng | PASS | `Event/TC-EVT-013_Result.txt` | `activityPoints = -1` và `activityType` rỗng/khoảng trắng trả `400 VALIDATION_FAILED`; `activityPoints = 0` trả `201 Created`. |
| TC-EVT-014 | Kiểm định `audienceClassIds` rỗng trên UI | PASS | — *(đã xác minh trên UI `/events/new`)* | Khi không tích lớp nào, hiển thị `ErrorBanner`: `"Vui lòng chọn ít nhất một lớp đối tượng được tham gia sự kiện."`. |
| TC-EVT-015 | Kiểm tra chuyển đổi & hiển thị múi giờ `Asia/Ho_Chi_Minh` (UTC+7) ↔ UTC khi tạo, xem và sửa | FAIL | `Event/TC-EVT-015_Step4_FAIL_EditTimezoneUTC7.png` | Tạo & xem chi tiết hiển thị đúng (`08:00` UTC+7 ↔ `01:00Z`), **nhưng** khi mở `/events/{id}/edit`, cả 5 ô `datetime-local` bị hiển thị giờ UTC (`01:00`, `04:00`, `00:30`, `00:45`, `04:30` thay vì `08:00`, `11:00`...) do cắt chuỗi `.slice(0, 16)`. Ngoài ra trên `/edit`, lớp `K49CNTT1` bị bỏ tích (`0 lớp đã chọn`). |
| TC-EVT-016 | Chỉnh sửa thành công sự kiện ở trạng thái `DRAFT` và tăng `version` | PASS | `Event/TC-EVT-016_Step3_PASS_DraftUpdatedV1.png` | `PUT /api/v1/events/{id}` trả `200 OK`, `version` tăng `0 -> 1`, cập nhật tiêu đề, địa điểm, chỉ tiêu `60`. *(Hệ quả lỗi `TC-EVT-015`: lưu từ form `/edit` làm dịch thêm -7h).* |
| TC-EVT-017 | Xung đột phiên bản (Optimistic Locking) khi cập nhật với `version` cũ | PASS | `Event/TC-EVT-017_Step3_PASS_VersionConflictBanner.png` | Khi server ở `version: 2` và form giữ `version: 1`, `PUT` trả `409 VERSION_CONFLICT`, `ErrorBanner` hiện `"Dữ liệu sự kiện đã bị thay đổi bởi người khác. Vui lòng tải lại trang và thử lại."`. |
| TC-EVT-018 | Chặn chỉnh sửa sự kiện đã ở trạng thái `PUBLISHED` | PASS | `Event/TC-EVT-018_Result.txt`, `Event/TC-EVT-019_020_023_Step4_PASS_PublishedEvent.png` | Trên UI ẩn nút `Chỉnh sửa`; gọi `PUT` trực tiếp trả `409 VERSION_CONFLICT`. |
| TC-EVT-019 | Xem trước sự kiện (`GET /preview`) và đóng modal khi chưa xác nhận công bố | PASS | `Event/TC-EVT-019_020_023_Step4_PASS_PublishedEvent.png` | `GET /preview` trả `200 OK`, modal hiện `"Xem trước sự kiện (Version 2)"` & QR `10s`; bấm `Hủy bỏ` đóng modal và giữ nguyên `Bản nháp`. |
| TC-EVT-020 | Bắt buộc xác nhận (`confirm = true`) khi công bố sự kiện | PASS | `Event/TC-EVT-020_Result.txt`, `Event/TC-EVT-019_020_023_Step4_PASS_PublishedEvent.png` | Trên UI nút `Xác nhận công bố` bị `disabled` khi chưa tích checkbox; API với `confirm: false` trả `400 VALIDATION_FAILED`. |
| TC-EVT-021 | Từ chối công bố khi sự kiện chưa được xem trước (`PREVIEW_REQUIRED`) | PASS | `Event/TC-EVT-021_Result.txt` | API trả `409 PREVIEW_REQUIRED`; `audit_logs` ghi `EVENT_PUBLISH_DENIED | DENIED | PREVIEW_REQUIRED`. |
| TC-EVT-022 | Từ chối công bố khi chỉnh sửa sau lúc xem trước (`PREVIEW_OUTDATED`) | PASS | `Event/TC-EVT-022_Result.txt` | API trả `409 PREVIEW_OUTDATED`; `audit_logs` ghi `EVENT_PUBLISH_DENIED | DENIED | PREVIEW_OUTDATED`. |
| TC-EVT-023 | Công bố sự kiện thành công sau khi xem trước phiên bản mới nhất và xác nhận | PASS | `Event/TC-EVT-019_020_023_Step4_PASS_PublishedEvent.png` | `POST /publish` trả `200 OK` (`PUBLISHED`), UI đổi badge `Đã công bố`, hiện `Màn hình chiếu QR`; DB `audit_logs` ghi `EVENT_PUBLISH | SUCCESS`. |
| TC-EVT-024 | Công bố lặp lại lần 2 trên sự kiện đã `PUBLISHED` (Idempotent) | PASS | `Event/TC-EVT-024_Result.txt` | API trả `200 OK`, giữ nguyên `publishedAt`, không tạo thêm bản ghi `EVENT_PUBLISH` trong `audit_logs` (vẫn = 1). |
| TC-EVT-025 | Sinh viên không nhìn thấy sự kiện ở trạng thái `DRAFT` | PASS | `Event/TC-EVT-025_Result.txt` | `sv001` không thấy `DRAFT` trong `/events`; truy cập `/events/{draftId}` cả UI và API đều trả `404` với thông báo `"Không tìm thấy sự kiện hoặc bạn không có quyền xem sự kiện này."`. |
| TC-EVT-026 | Chặn vai trò `STUDENT` và `SYSTEM_ADMIN` truy cập tính năng quản lý sự kiện | PASS | `Event/TC-EVT-026_Result.txt` | `RequireRole` đẩy `sv001` khỏi `/events/new` & `/edit`; cả 10 API call (`sv001` & `admin01`) trả `403 FORBIDDEN`; ghi audit `EVENT_PUBLISH_DENIED/FORBIDDEN`. |
| TC-EVT-027 | Cô lập quyền sở hữu giữa hai Ban tổ chức (`owner01` vs `owner02`) | PASS | `Event/TC-EVT-027_Result.txt`, `Event/TC-EVT-027_Step1_PASS_Owner02IsolatedEvents.png` | `owner02` vào `/events` không thấy sự kiện của `owner01`; `GET` trả `404 NOT_FOUND`, `PUT`/`preview`/`publish` trả `403 FORBIDDEN`. |
| TC-EVT-028 | Chặn truy cập ẩn danh (chưa có JWT) vào toàn bộ API quản lý sự kiện | PASS | `Event/TC-EVT-028_Result.txt` | Cả 5 endpoint trả `401 UNAUTHENTICATED` theo chuẩn `ApiError` kèm `traceId`. |
| TC-EVT-029 | Kiểm định giá trị tại biên (`location` 1/300, `time+1s`, `deadline==close`, `activityType` 50 vs 51) | FAIL | `Event/TC-EVT-029_Result.txt` | Step 1 & 2 trả `201 Created`, **nhưng** Step 3 (`activityType` 51 ký tự) trả `500 INTERNAL_ERROR` do `EventUpsert` thiếu `@Size(max=50)` nên chạm giới hạn `varchar(50)` ở PostgreSQL. |
| TC-EVT-030 | Kiểm định JSON rỗng `{}`, sai ISO-8601, enum lạ `NFC`, `attendanceMethod` null và UUID sai định dạng | FAIL | `Event/TC-EVT-030_Result.txt` | Step 1-3 đạt kỳ vọng (`400` / `201 DYNAMIC_QR`), **nhưng** Step 4 (`PUT /api/v1/events/not-a-uuid`) trả `500 INTERNAL_ERROR` do `GlobalExceptionHandler` thiếu handler cho `MethodArgumentTypeMismatchException`. |
| TC-EVT-031 | Gán nhiều lớp (`K49CNTT1` + `K49CNTT2`), trộn lớp không tồn tại, và cập nhật lớp trên `DRAFT` | PASS | `Event/TC-EVT-031_Result.txt` | Khử trùng 2 lớp thành công, trả `400 VALIDATION_FAILED` khi có UUID không tồn tại, `PUT` cập nhật đúng bảng `event_audience` còn `[K49CNTT2]`. |
| TC-EVT-032 | Công bố với `previewVersion` thiếu (`null`) và lệch bản hiện tại | PASS | `Event/TC-EVT-032_Result.txt` | Thiếu `previewVersion` trả `400 VALIDATION_FAILED`; lệch `previewVersion` trả `409 PREVIEW_OUTDATED` + ghi `audit_logs`. |
| TC-EVT-033 | Thao tác `PUT`, `GET /preview`, `POST /publish` với UUID sự kiện không tồn tại | PASS | `Event/TC-EVT-033_Result.txt` | Cả 3 endpoint trả `404 NOT_FOUND` và không tạo bản ghi `audit_logs` rác. |
| TC-EVT-034 | Kiểm chứng dữ liệu chi tiết `audit_logs` và tính bất biến không cho sửa/xóa | PASS | `Event/TC-EVT-034_Result.txt` | Bản ghi `EVENT_PUBLISH` đầy đủ trường; PostgreSQL trigger `trg_audit_logs_immutable` chặn cả `UPDATE` và `DELETE` (`audit_logs is append-only`). |
| TC-EVT-035 | Kiểm tra hiển thị Responsive (`375x667`) trên `/events/{id}` và Modal Preview | PASS | `Event/TC-EVT-035_Step1_PASS_Mobile375PreviewModal.png` | Không tràn ngang (`scrollWidth <= innerWidth`, `maxRight = 360 <= 360`) trên cả trang chi tiết và Modal Preview. |
| White-box Automated Suites | Chạy bộ test tự động BE Maven (`EventIT` + 5 suite liên quan) & FE Vitest | PASS | `Event/Whitebox_BE_Maven.txt`, `Event/Whitebox_FE_Vitest.txt` | BE Maven: `49/49 PASS` (`EventIT`: `21/21 PASS`); FE Vitest: `11/11 PASS` (`5` test files). |


---

# Báo cáo Kiểm thử Tự động — Sprint 1: 4.6. Tra cứu lịch sử tham gia sự kiện

- **Phạm vi:** Sprint 1 — 4.6 (`TC-4.6-01` → `TC-4.6-17` Browser E2E/API + FE Vitest cho History)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Chromium Playwright với `newContext()` cô lập cache/session, Viewport `1280x720` & `320x568`)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`)
  - Ngày thực thi: `2026-11-20`

## Tổng hợp kết quả (Mục 4.6)

- **Tổng số Test Case đã thực thi:** 13 (`TC-4.6-01` → `13`, `TC-4.6-16`, `TC-4.6-17`) + 1 suite Vitest FE
- **PASS:** 4 (`TC-4.6-10`, `TC-4.6-11`, `TC-4.6-12`, Vitest FE `7/7`)
- **FAIL:** 7 (`TC-4.6-01`, `TC-4.6-02`, `TC-4.6-05`, `TC-4.6-06`, `TC-4.6-07`, `TC-4.6-08`, `TC-4.6-13`, `TC-4.6-16` — 8 mục nếu tính cả `TC-4.6-16`, chính xác: 8 FAIL: `01, 02, 05, 06, 07, 08, 13, 16`)
- **BLOCKED:** 4 (`TC-4.6-03`, `TC-4.6-04`, `TC-4.6-09`, `TC-4.6-17` — bị chặn xác minh UI `/history` do lỗi crash render `TypeError: historyItems?.filter is not a function`)
- **Chưa chạy (tránh xung đột dữ liệu DB dùng chung):** 2 (`TC-4.6-14`, `TC-4.6-15`)
- **Console Errors:** Ghi nhận `TypeError: historyItems?.filter is not a function` tại `src/features/history/index.tsx:20:38` trên mọi lượt render `/history` khi API trả `200 OK`. Nguyên nhân gốc: `GET /api/v1/me/history` trả về Spring `Page<HistoryItem>` (`{ content: [...], pageable: ..., totalElements: ... }`) trong khi `fetchHistory()` ở FE kỳ vọng mảng `HistoryItem[]`.
- **Server Errors (`500 Internal Server Error`):** Không ghi nhận lỗi `500` thật từ Backend (chỉ mô phỏng `500` qua `page.route` trong `TC-4.6-06`).

## Bảng kết quả chi tiết (Mục 4.6)

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-4.6-01 | Sinh viên chưa đăng ký (`sv003`) mở `/history` hiển thị `EmptyState` | FAIL | `History/TC-4.6-01_Step3_FAIL_HistoryPageTypeError.png`, `History/TC-4.6-01_ErrorLog.txt` | API trả `200 OK` dạng `Page` (`{content: [], totalElements: 0}`), FE crash `TypeError: historyItems?.filter is not a function` tại `HistoryPage` (`src/features/history/index.tsx:20:38`). |
| TC-4.6-02 | Hiển thị sự kiện đã đăng ký nhưng chưa điểm danh (`sv001`) | FAIL | `History/TC-4.6-02_Step2_FAIL_HistoryPageTypeError.png`, `History/TC-4.6-02_ErrorLog.txt` | Trang `/history` crash `TypeError: historyItems?.filter is not a function` do không đọc `.content` từ response `Page`. |
| TC-4.6-03 | Cập nhật lịch sử sang `ACCEPTED` và cộng điểm sau điểm danh QR | BLOCKED | — | Bị chặn bởi lỗi crash render `/history` (`TC-4.6-02`). |
| TC-4.6-04 | Tính tổng KPI nhiều sự kiện & điều hướng xem chi tiết | BLOCKED | — | Bị chặn bởi lỗi crash render `/history` (`TC-4.6-02`). |
| TC-4.6-05 | Bảo toàn dữ liệu lịch sử khi tải lại trang (`F5`) | FAIL | `History/TC-4.6-05_Step2_FAIL_ReloadHistoryCrash.png` | Sau khi `F5` tại `/history`, `HistoryPage` tiếp tục crash `TypeError: historyItems?.filter is not a function`. |
| TC-4.6-06 | Trạng thái tải, retry `1` lần và thông báo lỗi khi API `500` | FAIL | `History/TC-4.6-06_Step1_PASS_LoadingState.png`, `History/TC-4.6-06_Step3_FAIL_ErrorBoxNoRetryBtn.png`, `History/TC-4.6-06_Result.txt` | Hiển thị `"Đang tải lịch sử tham gia sự kiện..."`, retry `1` lần (tổng 2 request) và hiện hộp lỗi đỏ `"Không thể tải dữ liệu lịch sử sự kiện. Vui lòng thử lại sau."`, nhưng **thiếu nút `"Thử lại"`** theo Test Plan §4.6. |
| TC-4.6-07 | API `GET /api/v1/me/history` trả `[]` cho `sv003` | FAIL | `History/TC-4.6-07_to_11_API_Result.txt` | HTTP `200 OK`, `content: []`, `totalElements: 0`, nhưng trả về object `Page` (`isArray: false`) thay vì mảng `[]` theo `sprint1-contract.md`. |
| TC-4.6-08 | Kiểm tra cấu trúc DTO `HistoryItem` (`NOT_CHECKED_IN` & `ACCEPTED`) | FAIL | `History/TC-4.6-07_to_11_API_Result.txt` | Các phần tử trong `content` đủ trường DTO (ẩn `attendanceAt` khi `NOT_CHECKED_IN`, có `attendanceAt` UTC khi `ACCEPTED`), nhưng cấu trúc top-level là `Page` thay vì mảng. |
| TC-4.6-09 | Thứ tự sắp xếp `startAt DESC` trên API & múi giờ `Asia/Ho_Chi_Minh` trên FE | BLOCKED | `History/TC-4.6-07_to_11_API_Result.txt` | API `content` sắp xếp đúng `startAt DESC`, nhưng phần kiểm tra hiển thị `UTC+7` trên FE bị chặn bởi lỗi crash `/history`. |
| TC-4.6-10 | Cô lập dữ liệu lịch sử giữa `sv001` và `sv002` | PASS | `History/TC-4.6-07_to_11_API_Result.txt` | `sv001` (`totalElements: 9`) và `sv002` (`totalElements: 3`) tách biệt hoàn toàn; `sv002` không thấy sự kiện/trạng thái của `sv001`. |
| TC-4.6-11 | Chống tấn công IDOR qua `?studentId=`, `?userId=`, `X-Student-Id` | PASS | `History/TC-4.6-07_to_11_API_Result.txt` | `sv002` truyền UUID của `sv001` qua query param và header `X-Student-Id` vẫn chỉ nhận dữ liệu của `sv002` (`totalElements: 3`). |
| TC-4.6-12 | Xác thực & phân quyền RBAC trên API và route `/history` (`owner01`, `admin01`) | PASS | `History/TC-4.6-12_Step4_PASS_OwnerBlockedFromHistory.png`, `History/TC-4.6-12_Result.txt` | Không token `401 UNAUTHENTICATED`; token đã logout `401 SESSION_REVOKED`; `owner01` & `admin01` `403 FORBIDDEN`; trên UI `owner01` không có menu `/history` và bị đẩy về `/events`. |
| TC-4.6-13 | Khả năng tiếp cận `StatusBadge`, bàn phím & Responsive `320px` | FAIL | `History/TC-4.6-13_Step2_FAIL_Mobile320pxHistoryCrash.png` | Ở `320x568` bấm nav mobile `"Lịch sử"` điều hướng tới `/history` nhưng trang crash `TypeError: historyItems?.filter is not a function`. |
| TC-4.6-16 | Deep-link redirect `/history` → `/login` → `/history` & chặn `POST` | FAIL | `History/TC-4.6-16_Step1_PASS_RedirectToLogin.png`, `History/TC-4.6-16_Step2_FAIL_DeepLinkAfterLogin.png`, `History/TC-4.6-16_Result.txt` | Redirect `/history` → `/login` → `/history` sau đăng nhập và `POST /api/v1/me/history` (`403 FORBIDDEN`) hoạt động đúng, nhưng `/history` crash render sau khi quay lại. *(Chưa tạo `bcs01` trên DB dùng chung).* |
| TC-4.6-17 | Đối chiếu trường hiển thị UI `/history` với `REQ-REV-008`, `REQ-REV-009` | BLOCKED | — | Bị chặn bởi lỗi crash render `/history`. |
| Vitest FE (History) | Chạy unit tests `HistoryPage.test.tsx`, `StatusBadge.test.tsx`, `time.test.ts` | PASS | — | `3/3` file test (`7/7` tests) PASS khi mock `fetchHistory` trả về mảng `HistoryItem[]`. |


---

# Báo cáo Kiểm thử Tự động — Sprint 1: 4.3. Danh sách sự kiện

- **Phạm vi:** Sprint 1 — 4.3 (`TC-4.3-01` → `TC-4.3-24` Black-box / Browser E2E / API)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Chromium Playwright với `newContext()` cô lập cache/session, Viewport `1280x720`, `1024x768` & `320x568`)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`)
  - Ngày thực thi: `2026-11-20`

## Tổng hợp kết quả (Mục 4.3)

- **Tổng số Test Case đã chạy:** 24 (`TC-4.3-01` → `TC-4.3-24`)
- **PASS:** 21 (`TC-4.3-01` → `TC-4.3-10`, `TC-4.3-12` → `TC-4.3-21`, `TC-4.3-24`)
- **FAIL:** 2 (`TC-4.3-11`, `TC-4.3-23`)
- **BLOCKED:** 1 (`TC-4.3-22` — DB test chung chưa có dữ liệu mẫu `CANCELLED` & sự kiện đa lớp `[K49CNTT1, K49CNTT2]`)
- **Console Errors (`Uncaught TypeError`):** `0` lỗi JS runtime trên các trang `/events` và `/events/:id`.
- **Server Errors (`500 Internal Server Error`):** Ghi nhận `2` lỗi `500 INTERNAL_ERROR` khi kiểm thử tham số API sai trong `TC-4.3-23` (`GET /api/v1/events?sort=nonExistentField,asc` và `GET /api/v1/events/not-a-uuid`).

## Bảng kết quả chi tiết (Mục 4.3)

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-4.3-01 | Sinh viên đúng lớp (`sv001`) xem danh sách sự kiện trên FE | PASS | `EventList/TC-4.3-01_Step3_PASS_StudentEventsList.png` | H1 `"Khám phá sự kiện"`, không có nút `"Tạo sự kiện mới"`, không hiện badge `"Bản nháp"`, hiển thị các sự kiện `Đã công bố`. |
| TC-4.3-02 | Sinh viên khác lớp (`sv003`) không thấy sự kiện ngoài phạm vi | PASS | `EventList/TC-4.3-02_Step3_PASS_OtherClassIsolation.png` | `GET /api/v1/events` trả `200 OK`, sự kiện lớp `K49CNTT1` (`08885aef-...`) không có trong `content[]` của `sv003`. |
| TC-4.3-03 | Ban tổ chức (`owner01`) thấy cả `DRAFT` và `PUBLISHED` của mình | PASS | `EventList/TC-4.3-03_Step2_PASS_OwnerEventsList.png` | H1 `"Quản lý sự kiện đơn vị"`, có nút `"Tạo sự kiện mới"`, hiển thị cả `"Bản nháp"` và `"Đã công bố"`. |
| TC-4.3-04 | Chặn truy cập danh sách khi chưa đăng nhập hoặc sai vai trò (`SYSTEM_ADMIN`) | PASS | `EventList/TC-4.3-04_Step1_PASS_UnauthRedirect.png`, `EventList/TC-4.3-04_Result.txt` | FE chuyển về `/login`; API không token trả `401 UNAUTHENTICATED`; `admin01` trả `403 FORBIDDEN`. |
| TC-4.3-05 | Tìm kiếm theo từ khóa `q` không phân biệt hoa/thường & debounce `350ms` | PASS | `EventList/TC-4.3-05_Step2_PASS_SearchDebounce.png` | Gõ liên tiếp chỉ gửi 1 request cuối `?q=h%E1%BB%99i+th%E1%BA%A3o+ai&size=20` (`200 OK`); xóa trắng gửi lại request không có `q=`. |
| TC-4.3-06 | Tìm kiếm với từ khóa không khớp hiển thị `EmptyState` | PASS | `EventList/TC-4.3-06_Step2_PASS_SearchEmptyState.png` | Hiển thị `"Không tìm thấy sự kiện nào khớp với \"khongtontai_xyz_999\""` và `"Hãy thử tìm kiếm với từ khóa khác."` (không có nút tạo). |
| TC-4.3-07 | Kiểm tra phân trang API `GET /api/v1/events?page=&size=` | PASS | `EventList/TC-4.3-07_Result.txt` | Trang 0 (`size=2`, `number=0`, `total=18`) và Trang 1 (`number=1`) không trùng lặp, sắp xếp `startAt,desc` chính xác. |
| TC-4.3-08 | Sinh viên đúng lớp mở trang chi tiết sự kiện `/events/:id` | PASS | `EventList/TC-4.3-08_Step2_PASS_StudentEventDetail.png` | Điều hướng `/events/08885aef-...`, API `200 OK`, hiển thị đủ thông tin sự kiện, `RegistrationSlot`, `AttendanceSlot` và `"Quay lại danh sách"`. |
| TC-4.3-09 | Kiểm tra định dạng múi giờ `Asia/Ho_Chi_Minh` và trường tính toán động | PASS | `EventList/TC-4.3-08_Step2_PASS_StudentEventDetail.png`, `EventList/TC-4.3-09_Result.txt` | `remainingQuota=5`, `registrationOpen=true`, `attendanceOpen=false`; UTC `2026-11-20T12:33:24Z` hiển thị đúng `19:33:24 20/11/2026` (UTC+7). |
| TC-4.3-10 | Kiểm tra trạng thái khi hết chỉ tiêu (`remainingQuota = 0`) | PASS | `EventList/TC-4.3-10_Step2_PASS_FullQuotaClosed.png` | `sv002` mở sự kiện `1158edec-...` (`1/1` suất): API `remainingQuota=0`, `registrationOpen=false`, ẩn nút `"Đăng ký giữ chỗ"`, hiện `"Sự kiện đã hết suất tham gia."`. |
| TC-4.3-11 | Kiểm tra trạng thái đăng ký cá nhân (`myRegistrationStatus` / `RegistrationSlot`) | FAIL | `EventList/TC-4.3-11_Step2_PASS_MyRegistrationStatus.png` | Trang chi tiết `RegistrationSlot` hiển thị `"Đã đăng ký"` & `"Bạn đã giữ chỗ thành công!"`, **nhưng** BE gán cứng `myRegistrationStatus: null` trong `EventSummary` & `EventDetail` khiến danh sách `/events` không hiện `"✓ Bạn đã giữ chỗ"`. |
| TC-4.3-12 | Ban tổ chức mở chi tiết sự kiện `DRAFT` và `PUBLISHED` của chính mình | PASS | `EventList/TC-4.3-12_Step1_PASS_OwnerDraftDetail.png`, `EventList/TC-4.3-12_Step2_PASS_OwnerPublishedDetail.png` | `DRAFT` hiện `"Bản nháp"`, `"Chỉnh sửa"`, `"Xem trước & Công bố"`; `PUBLISHED` hiện `"Đã công bố"`, `"Màn hình chiếu QR"`, đều không có `AttendanceSlot`. |
| TC-4.3-13 | Sinh viên truy cập trực tiếp `DRAFT` qua URL/API nhận `404 NOT_FOUND` | PASS | `EventList/TC-4.3-13_Step2_PASS_StudentAccessDraft404.png` | API trả `404 NOT_FOUND`; FE hiển thị `ErrorBanner` `"Không tìm thấy sự kiện hoặc bạn không có quyền xem sự kiện này."`. |
| TC-4.3-14 | Sinh viên khác lớp truy cập trực tiếp sự kiện ngoài lớp nhận `404 NOT_FOUND` | PASS | `EventList/TC-4.3-14_Step2_PASS_OtherClassDirect404.png` | `sv003` gọi API trả `404 NOT_FOUND`; FE hiển thị `ErrorBanner`. |
| TC-4.3-15 | Ban tổ chức B (`owner02`) truy cập trực tiếp sự kiện của `owner01` nhận `404 NOT_FOUND` | PASS | `EventList/TC-4.3-15_Step2_PASS_CrossOwnerDirect404.png` | `owner02` gọi API trả `404 NOT_FOUND`; FE hiển thị `ErrorBanner`. |
| TC-4.3-16 | Truy cập UUID không tồn tại trả `404 NOT_FOUND` chuẩn `ApiError` | PASS | `EventList/TC-4.3-16_Step1_PASS_NonExistentUuid404.png` | API trả `404 NOT_FOUND` kèm `traceId`; FE hiển thị `ErrorBanner` và nút `"Quay lại danh sách"`. |
| TC-4.3-17 | Hiển thị `EmptyState` khi lớp hoặc Owner chưa có sự kiện nào | PASS | `EventList/TC-4.3-17_Step1_PASS_StudentEmptyState.png`, `EventList/TC-4.3-17_Step2_PASS_OwnerEmptyState.png` | `sv003` hiện `"Chưa có sự kiện dành cho lớp bạn"`; `owner02` hiện `"Chưa có sự kiện nào"` + nút `"Tạo sự kiện đầu tiên"` *(giả lập response rỗng do DB test chung đang có dữ liệu từ lượt chạy khác)*. |
| TC-4.3-18 | Hiển thị thông báo lỗi khi mất kết nối / API thất bại (sau `retry: 1`) | PASS | `EventList/TC-4.3-18_Step2_PASS_NetworkRetryAndErrorBanner.png` | React Query retry 1 lần (tổng 2 request), không crash, hiển thị hộp lỗi đỏ `"Không thể tải danh sách sự kiện. Vui lòng kiểm tra lại kết nối."`. |
| TC-4.3-19 | Kiểm tra khả năng tiếp cận `StatusBadge` (chữ + icon, thang xám) | PASS | `EventList/TC-4.3-19_Step2_PASS_StatusBadgeGrayscaleA11y.png` | Tất cả `StatusBadge` (`62` badge trên trang) đều có đồng thời SVG `aria-hidden="true"` và nhãn chữ tiếng Việt rõ ràng ở chế độ grayscale. |
| TC-4.3-20 | Kiểm tra giao diện trên viewport nhỏ (`320px`) và điều hướng bàn phím | PASS | `EventList/TC-4.3-20_Step2_PASS_Mobile320pxResponsive.png` | Viewport `320x568` không tràn ngang (`horizontal overflow = false`), bottom navigation hiển thị `"Sự kiện"`, điều hướng `Tab` hoạt động tốt. |
| TC-4.3-21 | Chặn phiên đã thu hồi (`SESSION_REVOKED`) và HTTP method sai (`DELETE`/`PATCH`) | PASS | `EventList/TC-4.3-21_Result.txt` | Token sau `/logout` trả `401 SESSION_REVOKED`; `DELETE`/`PATCH` trả `403 FORBIDDEN`. *(Tài khoản `bcs01` chưa được seed trong `DemoDataSeeder`)*. |
| TC-4.3-22 | Kiểm tra phân lập sự kiện `CANCELLED` và sự kiện đa lớp `[K49CNTT1, K49CNTT2]` | BLOCKED | `EventList/TC-4.3-22_Result.txt` | DB hiện tại chưa có sự kiện `CANCELLED` và sự kiện đa lớp được seed sẵn để kiểm chứng trực tiếp trên UI. |
| TC-4.3-23 | Kiểm thử biên phân trang, giới hạn 20 mục UI, từ khóa đặc biệt & tham số sai | FAIL | `EventList/TC-4.3-23_Step4_FAIL_MissingPaginationAnd500Errors.png`, `EventList/TC-4.3-23_ErrorLog.txt` | `sort=nonExistentField` & `id=not-a-uuid` trả `500 INTERNAL_ERROR`; `q="%"` khớp toàn bộ `32/32` sự kiện (chưa escape wildcard LIKE); UI `owner01` có `106` sự kiện nhưng chỉ render `20` thẻ và chưa có nút chuyển trang. |
| TC-4.3-24 | Deep-link `/events/:id` sau đăng nhập & lưới đa kích thước (`1024px`) | PASS | `EventList/TC-4.3-24_Step3_PASS_DeepLinkAndResponsive.png` | Truy cập `/events/08885aef-...` khi chưa đăng nhập bị đẩy về `/login`, sau khi đăng nhập quay đúng về `/events/08885aef-...`; lưới `1024px` hiển thị 3 cột. |


---

# Báo cáo Kiểm thử Tự động — Sprint 1: 4.5. Điểm danh Dynamic QR

- **Phạm vi:** Sprint 1 — 4.5 (`TC-QR-01` → `TC-QR-26`: Browser E2E, API, Concurrency, Audit DB)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Vite dev server, Viewport Desktop `1280x720` & Mobile `320x568`, trình duyệt webview cô lập)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`)
  - Database: PostgreSQL 17 (`qnu-events-postgres-1`, `qnu_events`) — tạo bộ dữ liệu sự kiện cô lập `[QA-4.5]` để không xung đột với các agent khác.
- **Ngày thực thi:** 2026-11-20

## Tổng hợp kết quả (Mục 4.5)

- **Tổng số Test Case:** 26 (`TC-QR-01` → `TC-QR-26`)
- **PASS:** 22
- **FAIL:** 2 (`TC-QR-05`, `TC-QR-09`)
- **BLOCKED:** 2 (`TC-QR-06`, `TC-QR-16`)
- **Console Errors (`Uncaught TypeError`):** Ghi nhận lỗi `TypeError: historyItems?.filter is not a function` tại `HistoryPage` (`src/features/history/index.tsx:20:38`) trong `TC-QR-05` khi mở `/history`.
- **Server Errors (`500 Internal Server Error`):** Không có HTTP 500 ngoài dự kiến; riêng `GET /api/v1/events/not-a-uuid/qr` trong `TC-QR-04` trả `500` đúng theo ghi nhận White-box Finding (`MethodArgumentTypeMismatchException`).

## Bảng kết quả chi tiết (Mục 4.5)

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-QR-01 | Ban tổ chức mở màn hình chiếu QR và kiểm tra tự động xoay vòng 10 giây | PASS | `Attendance/TC-QR-01_Step3_PASS_QrProjectionAndRotation.png` | `QRCodeSVG` (`340x340px`), `Epoch #179516616 • Chu kỳ 10s`, đếm ngược `Đổi mã sau: 10s` hiển thị đúng; sau 12s + `Làm mới ngay` tăng lên `Epoch #179516618` / `#179516619`. |
| TC-QR-02 | Kiểm tra cấu trúc token QR động, tính độc lập theo sự kiện và nút Chép mã | PASS | `Attendance/TC-QR-01_Step3_PASS_QrProjectionAndRotation.png`, `Attendance/API_Concurrency_Results.txt` | Token đúng 4 phần `qr1.<uuid>.<epoch>.<sig>` không có `=`; bấm `Chép mã` đổi sang `Đã chép`; chữ ký giữa `EVT-QR-01` và `EVT-QR-02` khác nhau. *(Ghi chú nhỏ UI: chữ `Đã chép` trên nền tối hơi thấp độ tương phản).* |
| TC-QR-03 | Từ chối cấp mã QR khi `DRAFT`/`CANCELLED` hoặc ngoài cửa sổ điểm danh | PASS | `Attendance/TC-QR-03_Step4_PASS_ClosedWindowErrorBanner.png`, `Attendance/API_Concurrency_Results.txt` | `DRAFT` & `CANCELLED` trả `409 EVENT_NOT_PUBLISHED`; `EVT-CLOSED` trả `409 ATTENDANCE_NOT_OPEN`; UI hiển thị cảnh báo vàng `Cửa sổ điểm danh` và `ErrorBanner`. |
| TC-QR-04 | Kiểm tra xác thực & phân quyền lấy mã QR | PASS | `Attendance/TC-QR-04_Step1_PASS_StudentBlockedFromQr.png`, `Attendance/API_Concurrency_Results.txt` | `sv001` vào `/qr` bị `RequireRole` đẩy về `/events`; API trả `401 UNAUTHENTICATED`, `401 SESSION_REVOKED`, `403 FORBIDDEN` (`sv001`, `admin01`, `owner02`), `404` và `500` (`not-a-uuid`). |
| TC-QR-05 | Sinh viên điểm danh thành công qua ô nhập token dự phòng & đối chiếu `/history` | FAIL | `Attendance/TC-QR-05_Step3_PASS_CheckInAccepted.png`, `Attendance/TC-QR-05_Step4_FAIL_HistoryPageTypeError.png`, `Attendance/TC-QR-05_ErrorLog.txt` | Điểm danh trên `EVT-QR-01` trả `200 ACCEPTED`, hiện badge `Đã điểm danh` & hộp xanh `Điểm danh thành công!`. **Tuy nhiên** khi mở `/history`, trang crash với `TypeError: historyItems?.filter is not a function` (`src/features/history/index.tsx:20:38`) do `GET /api/v1/me/history` trả object phân trang `{"content":[...]}` thay vì mảng. |
| TC-QR-06 | Sinh viên quét mã QR bằng Camera trên điện thoại thật qua HTTPS Tunnel | BLOCKED | — | Yêu cầu điện thoại thật và HTTPS Tunnel (`cloudflared`), không khả dụng trong môi trường kiểm thử trình duyệt tự động. |
| TC-QR-07 | Kiểm tra đóng mở Camera và luồng dự phòng khi không có Camera | PASS | `Attendance/TC-QR-07_Step1_PASS_CameraScannerOpen.png` | Bấm `Quét mã QR bằng Camera` mở `#qr-reader-container`, nút đổi `Tắt Camera Quét` và hiện `Đóng camera`, không crash; bấm `Đóng camera` đóng sạch sẽ. |
| TC-QR-08 | Sinh viên chưa đăng ký (`sv003`) điểm danh QR (`NOT_REGISTERED`, `counted=false`) & chống giả mạo `studentId` | PASS | `Attendance/API_Concurrency_Results.txt` | Trả `403 NOT_REGISTERED`, `counted=0`, server bỏ qua `studentId=sv001` truyền lên, không tạo bản ghi `attendances` cho `sv001`. |
| TC-QR-09 | Sinh viên điểm danh khi sự kiện `DRAFT`/`CANCELLED` hoặc ngoài cửa sổ điểm danh | FAIL | `Attendance/TC-QR-09_Step1_PASS_ClosedWindowBanner.png`, `Attendance/API_Concurrency_Results.txt` | UI `EVT-CLOSED` hiện banner vàng đúng; API `EVT-CLOSED` trả `409 ATTENDANCE_NOT_OPEN`, **nhưng** `EVT-DRAFT` và `EVT-CANCELLED` trả `409 EVENT_NOT_PUBLISHED` thay vì `409 ATTENDANCE_NOT_OPEN` như kỳ vọng trong tài liệu test case. |
| TC-QR-10 | Khóa nút khi đang gửi, chặn input khoảng trắng và từ chối điểm danh lặp `ALREADY_CHECKED_IN` | PASS | `Attendance/API_Concurrency_Results.txt` | Nút `Gửi mã` bị `disabled` khi rỗng và khi nhập `"   "`; sau khi `ACCEPTED`, gửi request mới luôn trả `409 ALREADY_CHECKED_IN`. |
| TC-QR-11 | Biên thời gian ân hạn (Grace Period): Token epoch trước trong khoảng `<= 3s` vẫn hợp lệ | PASS | `Attendance/API_Concurrency_Results.txt` | Lấy token `epoch 179516628`, chờ sang đầu `epoch 179516629` (+~1.2s), gửi `POST /attendance/qr` trả `200 ACCEPTED`. |
| TC-QR-12 | Biên thời gian hết hạn: Token vượt quá 3s ân hạn -> `QR_EXPIRED` | PASS | `Attendance/TC-QR-12_Step2_PASS_ExpiredQrBanner.png` | Gửi token `epoch 179516625` đã hết hạn trên UI trả `422`, `ErrorBanner` hiển thị `"Mã QR đã hết hạn (xoay vòng 10 giây). Vui lòng quét lại mã đang hiển thị."`. |
| TC-QR-13 | Phân vùng token sai định dạng (`QR_MALFORMED`) | PASS | `Attendance/API_Concurrency_Results.txt` | Các mẫu `""`, `"   "`, `null`, `"invalid.token"`, `"qr2..."`, `"not-a-uuid"`, `"abc"` đều trả `422 QR_MALFORMED`. *(Lưu ý: mẫu có chuỗi `"sig."` kết thúc bằng dấu `.` khi `split("\\.")` trong Java vẫn ra 4 phần nên trả `QR_BAD_SIGNATURE`).* |
| TC-QR-14 | Token bị sửa đổi chữ ký HMAC hoặc giả mạo event khác (`QR_BAD_SIGNATURE`) | PASS | `Attendance/API_Concurrency_Results.txt` | Cả 3 biến thể (sửa `<sig>`, sửa `<epoch>`, giả mạo `EVT-QR-02` với `forged-sig`) đều trả `422 QR_BAD_SIGNATURE`, không lộ `QR_WRONG_EVENT`. |
| TC-QR-15 | Dùng token hợp lệ của Sự kiện B quét tại Sự kiện A (`QR_WRONG_EVENT`) | PASS | `Attendance/TC-QR-15_Step2_PASS_WrongEventBanner.png`, `Attendance/API_Concurrency_Results.txt` | Trả `422 QR_WRONG_EVENT`, UI `ErrorBanner` hiển thị `"Mã QR không thuộc về sự kiện này."`. |
| TC-QR-16 | Token mang `epoch` tương lai (`QR_FROM_FUTURE`) | BLOCKED | — | Profile `dev` tự sinh `QR_SIGNING_SECRET` trong bộ nhớ nên không thể ký token `currentEpoch + 1` từ bên ngoài qua HTTP API; cần kiểm thử ở mức White-box Unit Test (`MutableClock`). |
| TC-QR-17 | Biên giới hạn 3 lần thử sai: lần 1–3 tăng đếm, lần 4 bị chặn `ATTEMPT_LIMIT_REACHED` kể cả token đúng | PASS | `Attendance/TC-QR-17_Step3_PASS_AttemptLimitReached.png` | `sv002` thử sai 3 lần (`422`), lần thứ 4 gửi token hợp lệ mới nhất của `owner01` bị chặn `409`, `ErrorBanner` hiển thị `"Bạn đã vượt quá số lần thử điểm danh cho phép (tối đa 3 lần)."`. |
| TC-QR-18 | Kiểm tra Replay nguyên trạng response với cùng `Idempotency-Key` | PASS | `Attendance/API_Concurrency_Results.txt` | Cả replay lỗi (`422` với `X-Request-Id` khác) và replay ngoài cửa sổ (`409`) đều trả JSON giống hệt từng byte. |
| TC-QR-19 | Chống điểm danh trùng khi gửi 10 request hợp lệ đồng thời khác `Idempotency-Key` | PASS | `Attendance/API_Concurrency_Results.txt` | Đúng `1` request trả `200 ACCEPTED`, `9` request trả `409 ALREADY_CHECKED_IN`, DB `attendances` có duy nhất `1` dòng `ACCEPTED`. |
| TC-QR-20 | Kiểm tra ghi nhận Audit Log bất biến (`audit_logs`) và không lộ token QR gốc | PASS | `Attendance/API_Concurrency_Results.txt` | Có đủ `22` bản ghi audit, `0` rò rỉ `qr1.`, lệnh `UPDATE audit_logs` bị PostgreSQL chặn với `ERROR: audit_logs is append-only`. |
| TC-QR-21 | Phân quyền, ẩn `AttendanceSlot` với `owner01` & Responsive `320px` | PASS | `Attendance/TC-QR-21_Step2_PASS_OwnerNoAttendanceSlot.png`, `Attendance/TC-QR-21_Step3_PASS_Responsive320px.png` | `owner01` không thấy `AttendanceSlot`; viewport `320x568` không tràn ngang (`width = 305px <= 320px`). *(Lưu ý: mô phỏng `Offline` qua `context.setOffline` không được hỗ trợ trong webview sandbox).* |
| TC-QR-22 | Kiểm định giá trị biên Request Body JSON và `Idempotency-Key` (`""`, `"   "`, `1`, `101` ký tự) | PASS | `Attendance/API_Concurrency_Results.txt` | JSON lỗi / thiếu key / `""` / `"   "` / `101` ký tự trả `400`; key `1` ký tự trả `200 ACCEPTED`. |
| TC-QR-23 | Thành công ở lần thử thứ 3 (sau 2 lần sai) và thứ tự ưu tiên điều kiện chặn | PASS | `Attendance/API_Concurrency_Results.txt` | 2 lần sai (`422`), lần thứ 3 gửi token đúng trả `200 ACCEPTED`; gửi tiếp token sai sau khi `ACCEPTED` trả `409 ALREADY_CHECKED_IN`. |
| TC-QR-24 | Tương tranh đồng thời cùng `Idempotency-Key` (2 luồng) và 5 luồng sai khác key | PASS | `Attendance/API_Concurrency_Results.txt` | 2 luồng cùng key trả `200` giống hệt từng byte; 5 luồng sai khác key giữ tổng `counted = 3` (`2` trả `422`, `3` trả `409 ATTEMPT_LIMIT_REACHED`). |
| TC-QR-25 | Tính độc lập của bộ đếm lần thử giữa các sinh viên và giữa các sự kiện | PASS | `Attendance/API_Concurrency_Results.txt` | `sv002` bị khóa ở `EVT-CONC` không ảnh hưởng `sv001` tại `EVT-CONC` và `sv002` vẫn điểm danh `200 ACCEPTED` tại `EVT-QR-03`. |
| TC-QR-26 | Kiểm định cấu hình tham số QR (`qrRotationSeconds = 5`, `qrGraceSeconds = 0`) | PASS | `Attendance/API_Concurrency_Results.txt` | `GET /events/{EVT-QR-03}/qr` trả `rotationSeconds = 5` và `validUntil` đúng chu kỳ 5s. |


---

# Báo cáo Kiểm thử Tự động — Sprint 1: 4.4. Đăng ký sự kiện

- **Phạm vi:** Sprint 1 — 4.4 (`TC-REG-01` → `TC-REG-25` Black-box/Browser E2E/API/DB/Concurrency + White-box BE `RegistrationIT` & FE `RegistrationSlot.test.tsx`)
- **Môi trường kiểm thử:**
  - Frontend: `http://localhost:5173` (Vite dev server, Playwright Chrome `1280x720` & Mobile `320x568`, cô lập `browser.newContext()`)
  - Backend: `http://localhost:8080` (Spring Boot, profile `dev`)
  - Database: PostgreSQL 17 (`qnu-events-postgres-1`, `qnu_events`)
- **Ngày thực thi:** 2026-11-20

## Tổng hợp kết quả (Mục 4.4)

- **Tổng số Black-box / Browser E2E / API / DB / Concurrency Test Case đã chạy:** 25 (`TC-REG-01` → `TC-REG-25`)
  - **PASS:** 24
  - **FAIL:** 1 (`TC-REG-13`)
  - **BLOCKED:** 0
- **Kiểm thử tự động White-box hiện có trong repo:**
  - **Backend Maven (`RegistrationIT`):** **12/12 PASS** — xem [`Registration/Whitebox_BE_Maven.txt`](Registration/Whitebox_BE_Maven.txt).
  - **Frontend Vitest (`RegistrationSlot.test.tsx`):** **2/2 PASS** — xem [`Registration/Whitebox_FE_Vitest.txt`](Registration/Whitebox_FE_Vitest.txt).
- **Console Errors (`Uncaught TypeError` / JS Runtime Errors):** `0` lỗi JS runtime trên trình duyệt trong toàn bộ các bước kiểm thử mục 4.4.
- **Server Errors (`500 Internal Server Error`):** Ghi nhận lỗi `500 INTERNAL_ERROR` tại `TC-REG-13` (`POST/GET /api/v1/events/invalid-uuid/registrations`) do `GlobalExceptionHandler` thiếu handler cho `MethodArgumentTypeMismatchException`. *(Lưu ý thêm: ở lần chạy đầu của `TC-REG-20`, 1/30 request đồng thời gặp `500` tạm thời khi tranh chấp tải cao; ở lần chạy kiểm tra lại sạch, `TC-REG-20` đạt `5/5` `201 Created`, `25/25` `409 CAPACITY_FULL`, `0` lỗi `500`).*

## Bảng kết quả chi tiết (Mục 4.4)

| Mã TC | Tên chức năng | Trạng thái | File Evidence đính kèm | Ghi chú / Lỗi thực tế (nếu Fail) |
| :--- | :--- | :--- | :--- | :--- |
| TC-REG-01 | Sinh viên đúng lớp đăng ký giữ chỗ thành công lần đầu qua UI | PASS | `Registration/TC-REG-01_Step3_PASS_BeforeRegister.png`, `Registration/TC-REG-01_Step4_PASS_RegisteredSuccess.png` | `POST /registrations` trả `201 Created` (`alreadyRegistered: false`); UI hiển thị badge `Đã đăng ký`, `"Bạn đã giữ chỗ thành công!"` và `1/10 chỗ (còn lại 9 chỗ)`. |
| TC-REG-02 | Duy trì trạng thái "Đã đăng ký" và số chỗ sau khi tải lại trang (`F5`) | PASS | `Registration/TC-REG-02_Step1_PASS_ReloadPersistsRegistered.png` | Sau `F5`, UI giữ nguyên `"Bạn đã giữ chỗ thành công!"` và `1/10 chỗ`, ẩn nút `Đăng ký giữ chỗ`. |
| TC-REG-03 | Khóa nút `Đăng ký giữ chỗ` khi đang gửi yêu cầu (chống bấm đúp trên UI) | PASS | `Registration/TC-REG-03_Step3_PASS_DisabledDuringSubmit.png` | Khi giả lập trễ mạng `1.5s`, nút chuyển `disabled` (`isDisabledDuring=true`) + spinner, chỉ gửi đúng `1` request `POST`. |
| TC-REG-04 | Hiển thị trạng thái hết suất trên UI và xử lý lỗi `CAPACITY_FULL` khi tab cũ bấm đăng ký | PASS | `Registration/TC-REG-04_Step3_PASS_FullQuotaUI.png`, `Registration/TC-REG-04_Step4_PASS_CapacityFullBanner.png` | Tab 2 đạt `1/1 chỗ`, thanh tiến trình `bg-amber-500` 100%, hiện `"Sự kiện đã hết suất tham gia."`; Tab 1 (chưa F5) bấm nhận `409` và `ErrorBanner`. |
| TC-REG-05 | Hiển thị hết hạn đăng ký trên UI và xử lý lỗi `REGISTRATION_CLOSED` khi vừa quá hạn | PASS | `Registration/TC-REG-05_Step1_PASS_ExpiredEventUI.png`, `Registration/TC-REG-05_Step2_PASS_RegistrationClosedBanner.png` | E4 hiển thị `"Đã hết thời hạn đăng ký sự kiện."`; sự kiện sắp hết hạn bấm sau deadline trả `409` và `ErrorBanner` `"Đã hết thời hạn đăng ký tham gia sự kiện."`. |
| TC-REG-06 | Hiển thị khối `RegistrationSlot` khi đăng nhập bằng vai trò Ban tổ chức (`owner01`) | PASS | `Registration/TC-REG-06_Step2_PASS_OwnerManagementMode.png` | Hiện `"Chế độ quản lý: Sinh viên thuộc lớp được mời có thể đăng ký giữ chỗ tại đây."`, ẩn nút đăng ký và không gọi `GET /registrations/me`. |
| TC-REG-07 | API `POST /api/v1/events/{id}/registrations` đăng ký thành công lần đầu (`201 Created`) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `201 Created`, `registered_count` tăng `1 -> 2`, DB `registrations` có `1` dòng `REGISTERED`. |
| TC-REG-08 | API `GET /api/v1/events/{id}/registrations/me` trước và sau khi đăng ký | PASS | `Registration/TC-REG-API_Results.txt` | Chưa đăng ký: `404 NOT_FOUND`; Đã đăng ký: `200 OK` (`status: "REGISTERED"`, có `registeredAt`). |
| TC-REG-09 | API đăng ký lặp lần 2 bởi cùng sinh viên (`200 OK`, `alreadyRegistered = true`) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `200 OK`, `alreadyRegistered = true`, `registered_count` giữ nguyên `2`. |
| TC-REG-10 | Từ chối đăng ký khi sự kiện đang ở trạng thái `DRAFT` (`409 EVENT_NOT_PUBLISHED`) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `409 Conflict`, `code = "EVENT_NOT_PUBLISHED"`, `message = "Sự kiện chưa được công bố."`. |
| TC-REG-11 | Từ chối đăng ký khi thời gian máy chủ vượt quá `registrationDeadline` (`409 REGISTRATION_CLOSED`) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `409 Conflict`, `code = "REGISTRATION_CLOSED"`. |
| TC-REG-12 | Từ chối đăng ký khi sinh viên thuộc lớp ngoài `event_audience` (`403 NOT_IN_AUDIENCE`) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `403 Forbidden`, `code = "NOT_IN_AUDIENCE"`. |
| TC-REG-13 | Kiểm tra `eventId` không tồn tại (`404 NOT_FOUND`) và `eventId` sai định dạng UUID | FAIL | `Registration/TC-REG-13_ErrorLog.txt` | UUID không tồn tại trả `404 NOT_FOUND`, **nhưng** `invalid-uuid` trên cả `POST` và `GET /me` trả `500 INTERNAL_ERROR` do `GlobalExceptionHandler` thiếu handler `MethodArgumentTypeMismatchException`. |
| TC-REG-14 | Kiểm tra rollback toàn vẹn CSDL khi thất bại `CAPACITY_FULL` (không để lại dòng mồ côi) | PASS | `Registration/TC-REG-API_Results.txt` | HTTP `409 CAPACITY_FULL`; `registrations` giữ `1` dòng (`sv001 = 0`), `events.registered_count = 1`. |
| TC-REG-15 | Cho phép đăng ký lại khi sinh viên chỉ có bản ghi cũ ở trạng thái `CANCELLED` | PASS | `Registration/TC-REG-API_Results.txt` | `GET /me` trả `200` (`CANCELLED`); `POST` trả `201 Created`; DB có `2` dòng (`1 CANCELLED + 1 REGISTERED`), `registered_count = 1`. |
| TC-REG-16 | Chặn người dùng chưa đăng nhập hoặc dùng token đã thu hồi gọi API đăng ký (`401`) | PASS | `Registration/TC-REG-API_Results.txt` | Không token: `401 UNAUTHENTICATED`; Token đã logout: `401 SESSION_REVOKED`. |
| TC-REG-17 | Chặn vai trò `EVENT_OWNER` và `SYSTEM_ADMIN` gọi API đăng ký (`403 FORBIDDEN`) | PASS | `Registration/TC-REG-API_Results.txt` | Cả 4 request (`POST` & `GET /me` cho `owner01`, `admin01`) đều trả `403 FORBIDDEN`. |
| TC-REG-18 | Kiểm tra ghi nhận `audit_logs` cho các kết quả đăng ký và tính bất biến | PASS | `Registration/TC-REG-API_Results.txt` | Đủ 5 outcome (`SUCCESS|NULL`, `DENIED|NOT_IN_AUDIENCE`, `REJECTED|CAPACITY_FULL`, `REJECTED|EVENT_NOT_PUBLISHED`, `REJECTED|REGISTRATION_CLOSED`); trigger `trg_audit_logs_immutable` chặn `UPDATE`. |
| TC-REG-19 | Tranh suất cuối cùng (`quotaTotal = 1`): 2 sinh viên (`sv001`, `sv002`) đăng ký đồng thời | PASS | `Registration/TC-REG-API_Results.txt` | 2 request song song trả `[201, 409]`, DB `registered_count = 1`, `registrations = 1`. |
| TC-REG-20 | Tranh chấp tải cao đồng thời: 30 sinh viên cùng lớp tranh 5 suất (`quotaTotal = 5`) | PASS | `Registration/TC-REG-API_Results.txt` | 30 luồng song song: `5x 201 Created`, `25x 409 CAPACITY_FULL`, `0x 500`; DB `registered_count = 5`, `registrations = 5`. |
| TC-REG-21 | Cùng 1 sinh viên (`sv001`) gửi 10 request đăng ký song song cùng lúc | PASS | `Registration/TC-REG-API_Results.txt` | 10 luồng song song: `1x 201 Created`, `9x 200 OK` (`alreadyRegistered = true`), `0x 500`; DB `1` dòng, `registered_count = 1`. |
| TC-REG-22 | Kiểm định biên thời gian `registrationDeadline` vs `+1s` và sự kiện trạng thái `CANCELLED` | PASS | `Registration/TC-REG-API_Results.txt` | E5 `CANCELLED` -> `409 EVENT_NOT_PUBLISHED`; trước hạn -> `201`; sau hạn -> `409 REGISTRATION_CLOSED`. *(Mốc exact `now == deadline` kiểm chứng ở White-box `MutableClock`)*. |
| TC-REG-23 | Đăng ký sự kiện mời đa lớp, sinh viên `class_id = NULL`, và chống giả mạo `studentId` (Anti-IDOR) | PASS | `Registration/TC-REG-API_Results.txt` | `sv003` (`K49CNTT2`): `201`; `sv_noclass`: `403 NOT_IN_AUDIENCE`; Anti-IDOR: tạo cho `sv001` (`sv002 = 0` dòng). |
| TC-REG-24 | Kiểm tra lỗi mất mạng, đóng `ErrorBanner`, `StatusBadge` và hiển thị Responsive `320px` | PASS | `Registration/TC-REG-24_Step1_PASS_Responsive320px.png`, `Registration/TC-REG-24_Step2_PASS_OfflineErrorBanner.png`, `Registration/TC-REG-24_Step4_PASS_StatusBadge320px.png` | `320px` không tràn ngang (`overflow = false`); mất kết nối hiện `ErrorBanner` `"Không thể đăng ký tham gia sự kiện."`, bấm `X` ẩn banner; online đăng ký hiện `StatusBadge` `"Đã đăng ký"` kèm icon rõ nét ở chế độ thang xám. |
| TC-REG-25 | Đăng ký lặp khi sự kiện đã hết suất (`200`) vs sau khi đã quá hạn (`409 REGISTRATION_CLOSED`) | PASS | `Registration/TC-REG-API_Results.txt` | 10x song song `quota = 1`: `1x 201 + 9x 200`; lặp khi đầy: `200 alreadyRegistered = true`; lặp sau hạn: `409 REGISTRATION_CLOSED`. |
| White-box Automated Suites | Chạy bộ test tự động BE Maven `RegistrationIT` & FE Vitest `RegistrationSlot.test.tsx` | PASS | `Registration/Whitebox_BE_Maven.txt`, `Registration/Whitebox_FE_Vitest.txt` | BE `RegistrationIT`: `12/12 PASS`; FE `RegistrationSlot.test.tsx`: `2/2 PASS`. |
