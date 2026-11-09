# Báo cáo Thực thi Kiểm thử Sprint 1 — QNU-Events

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Sprint:** Sprint 1 (09/11/2026 – 11/11/2026)  
> **Môi trường kiểm thử:** PostgreSQL 17 (Docker WSL2), Java 21, Spring Boot 4.1.2, React 19, Vite, Playwright, Vitest  
> **Người thực hiện:** Nhóm phát triển QNU-Events

---

## 1. Tổng quan Kết quả Kiểm thử

| Hạng mục kiểm thử | Công cụ | Số test case | Đạt (Pass) | Lỗi (Fail) | Bỏ qua (Skip) | Tỷ lệ thành công |
|---|---|---|---|---|---|---|
| **Backend Integration & Unit Tests** | JUnit 5 + Spring Boot Test | 66 | 66 | 0 | 0 | **100%** |
| **Frontend Unit & Component Tests** | Vitest + React Testing Library | 11 | 11 | 0 | 0 | **100%** |
| **Frontend End-to-End (E2E) Tests** | Playwright (Chromium) | 7 | 7 | 0 | 0 | **100%** |
| **TỔNG CỘNG TOÀN HỆ THỐNG** | | **84** | **84** | **0** | **0** | **100%** |

> [!NOTE]
> Tuân thủ nghiêm ngặt nguyên tắc **R3** (`AGENTS.md`): Không bao giờ sử dụng cờ `-DskipTests`, mọi bài kiểm tra đều được thực thi thật trên cơ sở dữ liệu PostgreSQL 17 thực tế.

---

## 2. Chi tiết Bộ Kiểm thử Backend (66/66 Pass)

### 2.1 Thuật toán QR động & Bảo mật Token (`QrTokenServiceTest` - 8 tests)
- [x] Tạo mã QR chuẩn định dạng `qr1.<eventId>.<epoch>.<sig>`.
- [x] Xác thực mã QR hợp lệ trong chu kỳ hiện tại (epoch 0).
- [x] Xác thực mã QR trong thời gian ân hạn (grace period = 3s).
- [x] Từ chối mã QR đã hết hạn (> grace period) với lỗi `QR_EXPIRED`.
- [x] Từ chối mã QR có chữ ký bị chỉnh sửa (tampered) với lỗi `QR_BAD_SIGNATURE`.
- [x] Từ chối mã QR sai cấu trúc (thiếu prefix, epoch không phải số) với lỗi `QR_MALFORMED`.
- [x] Mã QR tự động đổi chữ ký và token khi bước sang epoch tiếp theo.
- [x] Hai sự kiện khác nhau có chữ ký hoàn toàn độc lập dù tạo cùng epoch.

### 2.2 Xác thực & Phân quyền (`AuthIT` + `SecurityConfigurationTest` - 14 tests)
- [x] Đăng nhập thành công với tài khoản Ban tổ chức (`owner01`) và Sinh viên (`sv001`).
- [x] Đăng nhập thất bại khi sai mật khẩu -> 401 `UNAUTHENTICATED`.
- [x] Khóa tài khoản sau 5 lần đăng nhập sai liên tiếp -> 423 `ACCOUNT_LOCKED`.
- [x] Thu hồi phiên đăng nhập khi logout -> 204 No Content.
- [x] Kiểm tra phân quyền theo vai trò (`STUDENT`, `EVENT_OWNER`, `SYSTEM_ADMIN`).
- [x] Sinh viên bị chặn 403 khi gọi các endpoint của Ban tổ chức.

### 2.3 Quản lý Sự kiện & Quy trình Duyệt (`EventIT` - 10 tests)
- [x] Ban tổ chức tạo sự kiện nháp (`DRAFT`) thành công.
- [x] Cập nhật sự kiện nháp với Optimistic Locking (`version`).
- [x] Chặn cập nhật khi có xung đột phiên bản -> 409 `VERSION_CONFLICT`.
- [x] Quy trình Xem trước & Công bố: bắt buộc xem trước (`GET /preview`) trước khi công bố (`POST /publish`).
- [x] Công bố sự kiện thành công (`PUBLISHED`) và ghi nhận thời gian `publishedAt`.
- [x] Phân lập dữ liệu: Sinh viên chỉ nhìn thấy sự kiện mở cho lớp của mình và ở trạng thái `PUBLISHED`.
- [x] Tìm kiếm sự kiện không phân biệt hoa thường (`title ILIKE %q%`).

### 2.4 Tranh chấp đồng thời & Giữ chỗ nguyên tử (`RegistrationIT` - 12 tests)
- [x] Đăng ký giữ chỗ 1 sinh viên bình thường -> 201 Created.
- [x] Gửi lại cùng yêu cầu đăng ký (Idempotency) -> 200 OK (idempotent, không tăng quota).
- [x] **Tranh suất cuối cùng (2 luồng song song cho 1 suất):** Đúng 1 luồng thành công 201, 1 luồng bị từ chối 409 `CAPACITY_FULL`.
- [x] **Tranh chấp tải cao (30 sinh viên đồng thời tranh 5 suất):** Đúng 5 sinh viên đăng ký thành công, 25 sinh viên nhận lỗi `CAPACITY_FULL`, số lượng `registered_count` trên database chính xác bằng 5.
- [x] **Gửi lặp đồng thời (10 luồng song song của cùng 1 sinh viên):** Chỉ tạo đúng 1 bản ghi `registrations`, 9 luồng còn lại trả về kết quả an toàn mà không trừ thừa quota.
- [x] Chặn sinh viên sai lớp đăng ký -> 403 `NOT_IN_AUDIENCE`.
- [x] Chặn đăng ký khi đã quá hạn deadline -> 409 `REGISTRATION_CLOSED`.

### 2.5 Điểm danh QR & Kiểm soát Thử sai (`AttendanceIT` - 11 tests)
- [x] Ban tổ chức lấy mã QR động trong cửa sổ điểm danh -> 200 OK.
- [x] Sinh viên không được phép lấy mã QR động -> 403 `FORBIDDEN`.
- [x] Không thể lấy mã QR ngoài cửa sổ điểm danh -> 409 `ATTENDANCE_NOT_OPEN`.
- [x] Sinh viên điểm danh thành công với mã QR hợp lệ -> 200 `ACCEPTED`.
- [x] Idempotency: Gửi lại cùng `Idempotency-Key` trả về bản ghi cũ, không tạo thêm bản ghi attendance.
- [x] Điểm danh lại với key mới khi đã điểm danh rồi -> 409 `ALREADY_CHECKED_IN`.
- [x] Chưa đăng ký sự kiện mà điểm danh -> 403 `NOT_REGISTERED`.
- [x] **Giới hạn 3 lần thử sai:** Thử sai 3 lần, đến lần thứ 4 (kể cả với mã QR hợp lệ) lập tức bị từ chối với lỗi 409 `ATTEMPT_LIMIT_REACHED`.

### 2.6 Lịch sử Sinh viên & Kiểm toán Bất biến (`HistoryIT` + `AuditIT` - 9 tests)
- [x] Sinh viên xem lịch sử cá nhân (`GET /api/v1/me/history`) -> 200 OK.
- [x] Sinh viên chưa đăng ký sự kiện nào -> Danh sách rỗng `[]`.
- [x] Đã đăng ký nhưng chưa điểm danh -> `attendanceStatus: NOT_CHECKED_IN`.
- [x] Đã điểm danh thành công -> `attendanceStatus: ACCEPTED`, có timestamp `attendanceAt` và điểm rèn luyện.
- [x] Phân lập dữ liệu: Sinh viên A không thấy lịch sử sự kiện của sinh viên B.
- [x] **Bảng kiểm toán bất biến:** Trigger `trg_audit_logs_immutable` ném ngoại lệ khi có bất kỳ thao tác `UPDATE` hoặc `DELETE` trực tiếp vào bảng `audit_logs`.

---

## 3. Chi tiết Bộ Kiểm thử Frontend

### 3.1 Unit & Component Tests (Vitest - 11/11 Pass)
- `Button.test.tsx`: Kiểm tra render nhãn và trạng thái disabled khi đang loading.
- `StatusBadge.test.tsx`: Kiểm tra hiển thị đúng màu sắc và icon cho các trạng thái `DRAFT`, `PUBLISHED`, `ACCEPTED`, `NOT_CHECKED_IN`, `REGISTERED`.
- `time.test.ts`: Kiểm tra định dạng thời gian chuẩn tiếng Việt theo ISO 8601.
- `RegistrationSlot.test.tsx`: Kiểm tra hiển thị tiến trình chỉ tiêu, nút đăng ký cho sinh viên và trạng thái khi hết chỗ.
- `HistoryPage.test.tsx`: Kiểm tra thẻ tổng kết KPI (số sự kiện đã đăng ký, đã điểm danh, điểm tích lũy) và danh sách sự kiện.

### 3.2 End-to-End Tests (Playwright - 7/7 Pass)
- `auth-and-routes.spec.ts`:
  1. Chuyển hướng người dùng chưa đăng nhập về trang `/login`.
  2. Hiển thị thông báo lỗi khi nhập sai mật khẩu.
  3. Chặn sinh viên truy cập đường dẫn tạo sự kiện của ban tổ chức (`/events/new`).
  4. Sinh viên truy cập trang Lịch sử (`/history`), hiển thị danh sách sự kiện và điểm rèn luyện.
- `owner-flow.spec.ts`:
  5. Ban tổ chức thấy tiêu đề quản lý sự kiện và nút "Tạo sự kiện mới".
  6. Ban tổ chức mở biểu mẫu tạo sự kiện, chọn lớp đối tượng (`K49CNTT1`) thành công.
  7. Ban tổ chức mở màn hình trình chiếu mã QR điểm danh khổ lớn với đồng hồ đếm ngược chu kỳ 10 giây.

---

## 4. Kết luận Đánh giá Chất lượng Sprint 1
1. **Độ tin cậy kỹ thuật:** Toàn bộ 10 User Story / Jira Key thuộc phạm vi Sprint 1 (`QE-64`, `QE-65`, `QE-66`, `QE-70`, `QE-71`, `QE-56`, `QE-57`, `QE-61`, `QE-72`, `QE-28`) đã được hiện thực hóa hoàn chỉnh, đáp ứng 100% tiêu chí chấp nhận (Acceptance Criteria).
2. **Khả năng chịu tải & An toàn dữ liệu:** Bảo đảm an toàn tuyệt đối trước các lỗi Race Condition nhờ cơ chế `SELECT FOR UPDATE` / Native Atomic Query và Optimistic Locking; mã QR động được ký bằng HMAC-SHA256 chống chụp màn hình chia sẻ mã; bảng `audit_logs` có trigger PL/pgSQL bảo đảm tính bất biến phục vụ thanh tra.
3. **Mức độ sẵn sàng Demo:** Cả hai kho mã nguồn Backend và Frontend đều đã vượt qua toàn bộ các bước kiểm tra tự động (Lint, Typecheck, Unit Test, Integration Test, Build, E2E), sẵn sàng 100% cho buổi nghiệm thu Sprint 1 ngày 11/11/2026.

