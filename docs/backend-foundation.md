# Ghi chú nền tảng backend

Nguồn: proposal v2 trong thư mục docs. Phạm vi hiện tại là một khoa, khoảng 3.000 sinh viên. Backend được tổ chức thành một ứng dụng với package theo tính năng để giữ giao dịch và việc triển khai đơn giản.

## Các yêu cầu cần giữ khi phát triển

- Đăng ký phải cập nhật quota nguyên tử bằng transaction và khóa/conditional update trong PostgreSQL; kiểm tra quota bằng một SELECT riêng rồi INSERT không đủ.
- Cần unique constraint cho đăng ký theo sinh viên/event và attendance hợp lệ theo sinh viên/event; schema chi tiết cần theo SRS.
- Offline sync phải idempotent. Client gửi lại request không được tạo thêm attendance hoặc tính thêm check-in attempt chỉ vì retry truyền tải.
- QR xoay mặc định 10 giây, grace khoảng 3 giây. ZXing chỉ dựng ảnh; backend cần ký token chứa event/epoch và kiểm tra token khi sync, không tin thời gian client. Cửa sổ sync offline cần được chốt riêng.
- GPS dùng bán kính theo event, tham chiếu ban đầu 40–50 m; vị trí đáng ngờ được flag, không tự reject trong MVP.
- Evidence luôn chờ BCS duyệt; BCS chỉ xem sinh viên thuộc lớp được phân công. Proxy attendance luôn qua evidence và cần audit.
- Mỗi check-in tối đa 3 attempts. Không cho hủy đăng ký từ mốc 12 giờ trước event.
- No-show bị hạn chế đăng ký 7 ngày sau khi đóng quy trình attendance/verification; có khiếu nại để Admin xem xét gỡ.
- Role dự kiến: STUDENT, CLASS_REPRESENTATIVE, EVENT_OWNER, SYSTEM_ADMIN. Role không thay thế kiểm tra phạm vi dữ liệu.
- Tác vụ audit, no-show và retention có thể dùng sự kiện nội bộ/scheduler; chỉ chạy side effect sau commit và thiết kế retry. Tác vụ cần bền vững nên dùng DB outbox/job khi hiện thực.
- Retention evidence khoảng một học kỳ (4–5 tháng); thời hạn chính thức và backup cần chốt trước khi triển khai.
- Không xây tính tổng điểm rèn luyện, AI, nhận diện khuôn mặt hoặc chữ ký số trong MVP.

## Migration

Đặt SQL ở `src/main/resources/db/migration/V1__description.sql`, tiếp theo là V2, V3... Không sửa migration đã chạy ở môi trường dùng chung. Hibernate dùng `ddl-auto=validate`, không tự tạo/sửa schema.

Bộ khung chưa tạo các bảng nghiệp vụ để tránh cố định schema trước bước thiết kế CSDL. Flyway sẽ khởi tạo lịch sử migration khi ứng dụng kết nối database trống.

## Các quyết định còn cần SRS

Thời hạn khiếu nại/appeal; quota nhiều tầng; format và validation CSV/Excel; quy tắc sửa/xóa event đã có đăng ký; cửa sổ sync offline; domain email trường; nơi lưu file, retention và backup; load test profile và mục tiêu latency.

Các dependency đã được cài để hỗ trợ triển khai. Chưa có cơ chế xác thực JWT/Google, điểm danh, queue offline, scheduler xử phạt hoặc kho evidence hoạt động. Khi thêm Google OAuth2 cần thiết kế quản lý state/callback và CSRF phù hợp trước khi bật cookie/session.
