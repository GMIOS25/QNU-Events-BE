# Kịch bản Demo Sprint 1 — QNU-Events

> **Thời lượng dự kiến:** 8 – 10 phút  
> **Ngày báo cáo:** 11/11/2026  
> **Người thực hiện:** Nhóm phát triển QNU-Events (Cường @GMIOS25, Hoàng @hoangkq157, Dương @YangKlee, Đan @Dan28-05, Khánh @KHANHSKY0808)

---

## 1. Chuẩn bị môi trường & Tài khoản Demo

### 1.1 Khởi động hệ thống
```bash
# 1. Khởi động PostgreSQL 17 (Docker WSL2)
docker start postgres

# 2. Khởi động Backend (Spring Boot 4.1.2 - Java 21)
cd "d:\Work\QNU-Events BE"
./mvnw spring-boot:run

# 3. Khởi động Frontend (React 19 + Vite)
cd "d:\Work\QNU-Events FE"
npm run dev
```

### 1.2 Danh sách tài khoản đã nạp sẵn (`DemoDataSeeder`)
| Vai trò | Tên đăng nhập | Mật khẩu | Lớp / Đơn vị | Mục đích demo |
|---|---|---|---|---|
| **Ban tổ chức (Owner)** | `owner01` | `Demo@12345` | Ban Chấp hành Đoàn | Tạo, xem trước, công bố sự kiện và chiếu mã QR động |
| **Sinh viên 1** | `sv001` | `Demo@12345` | `K49CNTT1` (K49) | Đăng ký thành công, quét QR hợp lệ, kiểm tra lịch sử |
| **Sinh viên 2** | `sv002` | `Demo@12345` | `K49CNTT1` (K49) | Đăng ký tranh suất cuối, quét QR |
| **Sinh viên 3** | `sv003` | `Demo@12345` | `K49CNTT1` (K49) | Thử đăng ký khi hết suất (nhận lỗi `CAPACITY_FULL`) |

---

## 2. Các bước trình diễn chi tiết (8 Bước)

### Bước 1: Đăng nhập & Tạo sự kiện (Tài khoản Owner)
1. Truy cập `http://localhost:5173/login`.
2. Bấm nút nạp nhanh **"Nạp mẫu: owner01"** hoặc nhập `owner01` / `Demo@12345`.
3. Bấm **"Tạo sự kiện mới"** (`/events/new`):
   - **Tiêu đề:** `Hội thảo Ứng dụng AI & Khởi nghiệp Công nghệ 2026`
   - **Địa điểm:** `Hội trường A - Đại học Quy Nhơn`
   - **Chỉ tiêu:** `2` suất (để dễ dàng demo tranh suất).
   - **Thời gian diễn ra:** Hôm nay đến ngày mai.
   - **Cửa sổ điểm danh:** Đang mở (bắt đầu từ 15 phút trước đến 2 giờ sau).
   - **Điểm rèn luyện:** `+10` điểm (Hoạt động học thuật).
   - **Lớp đối tượng:** Chọn `K49CNTT1`.
4. Bấm **"Tạo bản nháp sự kiện"** -> Sự kiện ở trạng thái `DRAFT`.

### Bước 2: Quy trình Xem trước & Công bố (Preview-then-Publish Wizard)
1. Tại trang chi tiết sự kiện nháp, bấm **"Xem trước & Công bố"**.
2. Modal hiện thông tin bản xem trước (`previewVersion = 0`).
3. Tích chọn: *"Tôi xác nhận thông tin sự kiện trên là hoàn toàn chính xác và muốn công bố mở đăng ký cho sinh viên."*
4. Bấm **"Xác nhận công bố"** -> Trạng thái chuyển sang `PUBLISHED` (Ghi nhận log kiểm toán `EVENT_PUBLISH`).

### Bước 3: Sinh viên khám phá & Đăng ký giữ chỗ
1. Đăng xuất `owner01`, đăng nhập tài khoản sinh viên `sv001` / `Demo@12345`.
2. Màn hình khám phá hiển thị sự kiện `Hội thảo Ứng dụng AI...`.
3. Bấm **"Chi tiết"** -> Bấm **"Đăng ký giữ chỗ"**:
   - Nút hiển thị trạng thái đang xử lý -> Thành công.
   - Thanh chỉ tiêu cập nhật: `1/2` chỗ đã giữ.
   - Huy hiệu `Đã đăng ký` hiển thị cùng thời gian giữ chỗ.

### Bước 4: Tranh suất cuối & Chặn khi hết chỗ (Race Condition Safe)
1. Mở cửa sổ ẩn danh, đăng nhập tài khoản sinh viên `sv002`.
2. Bấm **"Đăng ký giữ chỗ"** -> Thành công. Chỉ tiêu đạt `2/2` chỗ (Hết chỗ).
3. Đăng nhập tài khoản sinh viên `sv003` -> Bấm đăng ký:
   - Hệ thống hiển thị thông báo lỗi thân thiện: *"Sự kiện đã hết suất tham gia."* (Mã `CAPACITY_FULL`).
   - Quota trên hệ thống không bị vượt quá giới hạn 2 chỗ.

### Bước 5: Chiếu mã QR động tự đổi chu kỳ 10 giây (Owner)
1. Quay lại màn hình của `owner01`, mở trang chi tiết sự kiện -> Bấm **"Màn hình chiếu QR"** (`/events/:id/qr`).
2. Màn hình chiếu toàn cảnh với giao diện nền tối chuyên nghiệp:
   - Mã QR khổ lớn chất lượng cao (`QRCodeSVG`).
   - Đồng hồ đếm ngược `10s` tự động xoay vòng.
   - Thông tin Epoch và chuỗi token dự phòng (`qr1...`).
   - Khán giả có thể quan sát mã QR tự đổi sau mỗi 10 giây một cách mượt mà.

### Bước 6: Sinh viên quét mã QR điểm danh & Nhận phản hồi tức thì
1. Trên màn hình sinh viên `sv001`:
   - Bấm **"Quét mã QR bằng Camera"** hoặc dán chuỗi token hiển thị trên máy chiếu.
   - Bấm **"Gửi mã"**.
   - Nhận phản hồi **200 OK**: Huy hiệu xanh `Đã điểm danh` xuất hiện với thông báo *"Điểm danh thành công! Thời gian xác nhận..."*.
2. Thử điểm danh lại bằng mã cũ hoặc gửi lại:
   - Gửi lại cùng khóa Idempotency-Key -> Trả lại kết quả thành công mà không cộng trùng.
   - Thử gửi mã đã hết hạn (> 13 giây) -> Thông báo lỗi *"Mã QR đã hết hạn, vui lòng quét lại mã đang hiển thị."* (`QR_EXPIRED`).
   - Thử nhập sai quá 3 lần -> Bị chặn với lỗi `ATTEMPT_LIMIT_REACHED`.

### Bước 7: Kiểm tra Lịch sử & Điểm rèn luyện tích lũy
1. Sinh viên `sv001` bấm menu **"Lịch sử"** (`/history`):
   - Thẻ KPI: Đã đăng ký `1`, Đã điểm danh `1`, Điểm tích lũy `+10 đ`.
   - Danh sách sự kiện: Hiển thị sự kiện với huy hiệu `Đã điểm danh`, thời gian điểm danh chính xác.
   - Tải lại trang (F5) -> Dữ liệu vẫn được bảo toàn nguyên vẹn.

### Bước 8: Bằng chứng Kiểm toán Bất biến (Database Proof)
1. Mở terminal / DBeaver / psql truy vấn trực tiếp bảng `audit_logs`:
```sql
SELECT action, outcome, reason_code, target_type, occurred_at 
FROM audit_logs 
ORDER BY occurred_at DESC 
LIMIT 10;
```
2. Chứng minh trigger chống sửa xóa:
```sql
UPDATE audit_logs SET action = 'HACKED';
-- Kết quả: ERROR: audit_logs is append-only
```

---

## 3. Phương án dự phòng (Contingency Plans)
- **Nếu camera laptop không nhận:** Sử dụng ô *"Dán chuỗi token dự phòng"* đã được tích hợp sẵn ngay bên dưới khung quét camera.
- **Nếu cần reset lại dữ liệu ban đầu:** Chạy script nạp lại demo data hoặc khởi động lại backend với profile `dev`.

