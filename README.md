# QNU Events Backend

Bộ khung Spring Boot cho hệ thống quản lý đăng ký và xác thực tham gia hoạt động của sinh viên QNU, dựa trên [proposal v2](docs/QNU_Event_Participation_Management_Project_Proposal_v2.docx).

Đã có cấu hình môi trường, dependency, health check, Swagger, kiểm thử bảo mật và CI. Các API nghiệp vụ, đăng nhập, phát JWT, Google OAuth2 và lưu minh chứng sẽ được triển khai ở các sprint tiếp theo.

## Stack và dependency

| Thành phần | Mục đích |
| --- | --- |
| Java 21, Spring Boot 4.1.2, Maven Wrapper | Runtime và build thống nhất |
| Spring MVC, Validation | REST API và kiểm tra dữ liệu đầu vào |
| Spring Data JPA, PostgreSQL 17 | Giao dịch, khóa quota và ràng buộc dữ liệu |
| Spring Security, OAuth2 Client, Resource Server | Nền tảng RBAC, Google OAuth2 và JWT/JOSE |
| Flyway + PostgreSQL module | Quản lý migration SQL |
| springdoc 3.2.0 | OpenAPI và Swagger UI |
| Apache Commons CSV 1.14.1, Apache POI 6.0.0 | Import/export CSV và Excel |
| ZXing 3.5.4 | Tạo ảnh QR; token ký và kiểm tra thời gian cần code nghiệp vụ riêng |
| Actuator, DevTools | Health check và hỗ trợ phát triển |
| Spring Boot Test, Spring Security Test | JUnit, MockMvc và kiểm tra phân quyền |

JWT/JOSE đã có trong Resource Server nên chưa cần thêm JJWT. GPS được xử lý bằng dữ liệu tọa độ và logic backend, chưa cần SDK bản đồ. PostgreSQL là nguồn dữ liệu chính; chưa cần Redis, Kafka hoặc microservices cho MVP 3.000 sinh viên. Nhà cung cấp lưu file chưa được chốt nên chưa thêm SDK S3/Cloudinary.

Các phiên bản Spring, PostgreSQL JDBC, Hibernate, Flyway và thư viện test do Spring Boot BOM quản lý. Tương thích: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [springdoc](https://springdoc.org/faq.html).

## GitHub Codespaces

Cấu hình nằm trong [.devcontainer/devcontainer.json](.devcontainer/devcontainer.json).

1. Đưa các thay đổi này lên branch của repository GitHub.
2. Mở repository → **Code → Codespaces → Create codespace on branch**.
3. Container tự cài Java 21, kéo PostgreSQL, tải Maven và chạy build cùng integration test.
4. Trong terminal chạy:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Mở port **8080** từ tab **Ports**, rồi truy cập `/swagger-ui/index.html` hoặc `/actuator/health`. Giữ port ở chế độ **Private**.

Dev container cũng dùng được bằng VS Code → **Dev Containers: Reopen in Container** khi Docker đang chạy. Backend kết nối đến hostname `postgres` trong container, còn khi chạy trực tiếp trên máy thì dùng `localhost`. Compose truyền cùng cấu hình database/tài khoản cho hai container từ `.env` hoặc giá trị development mặc định. Không đưa thông tin thật vào file được commit.

Cấu hình trong repository chưa tạo một máy Codespace trên tài khoản GitHub; thao tác tạo máy thực hiện ở bước 2.

## Chạy local

Cần JDK 21 trở lên và Docker Desktop đang chạy. Maven được Wrapper tải tự động.

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --wait postgres
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Linux/macOS:

```bash
cp .env.example .env
docker compose up -d --wait postgres
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Hoặc mở [workspace VS Code](QNU-Events-BE.code-workspace), chọn **Terminal → Run Task → Spring Boot: Run (dev)** sau khi PostgreSQL sẵn sàng.

- Health: http://localhost:8080/actuator/health
- Swagger trong profile `dev`: http://localhost:8080/swagger-ui/index.html
- OpenAPI trong profile `dev`: http://localhost:8080/v3/api-docs
- Database: `localhost:5432/qnu_events`; tài khoản development nằm trong `.env.example`.

`.env` được Spring đọc như file Java properties: viết `KEY=value`, không đặt dấu nháy quanh giá trị. Biến môi trường hệ điều hành được ưu tiên hơn `.env`. Nếu đổi `DB_PORT`/`DB_NAME`, sửa cả `DB_URL`. Nếu volume PostgreSQL đã được khởi tạo, đổi thông tin trong `.env` không tự đổi user/password/database trong volume; cần cập nhật database tương ứng.

## Kiểm tra

```powershell
# Kiểm tra biên bảo mật/CORS và đóng gói, không cần PostgreSQL.
.\mvnw.cmd verify

# Kiểm tra khởi động toàn bộ ứng dụng, DB, Flyway, health và Swagger.
docker compose up -d --wait postgres
.\mvnw.cmd -Pintegration verify
```

Linux/Codespaces dùng `bash ./mvnw` thay cho `.\mvnw.cmd`. CI chạy cả hai nhóm test với PostgreSQL riêng. JAR được tạo tại `target/qnu-events-0.0.1-SNAPSHOT.jar`.

## Cấu trúc

```text
src/main/java/vn/edu/qnu/events/
  config/         Security, CORS, OpenAPI
  auth/           Đăng nhập, JWT, Google OAuth2
  student/        Tài khoản sinh viên, khoa/ngành/lớp, import
  event/          Event, đối tượng, trạng thái publish
  registration/   Quota, waiting list, hủy và no-show
  attendance/     QR/GPS, check-in, offline sync, idempotency
  evidence/       Minh chứng, proxy, phạm vi duyệt, retention
  appeal/         Khiếu nại và appeal lần hai
  audit/          Theo dõi thao tác nghiệp vụ
src/main/resources/db/migration/  Migration Flyway
```

Các package nghiệp vụ hiện là nơi đặt code cho từng tính năng. Xem [ghi chú triển khai từ proposal](docs/backend-foundation.md) trước khi thiết kế schema/API.

## Cấu hình triển khai

Ngoài profile `dev`, ứng dụng yêu cầu `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Swagger mặc định tắt, API nghiệp vụ mặc định từ chối truy cập; chưa có endpoint đăng nhập hay tài khoản seed. Health công khai chỉ trả trạng thái, không lộ chi tiết DB.

Khi triển khai auth cần thêm quy tắc endpoint/role cụ thể và kiểm tra quyền theo lớp, event, chủ sở hữu evidence. OAuth2 Google cần xác thực email đã verify, domain email trường được phê duyệt và tài khoản sinh viên đã được cấp; chưa chốt domain trong proposal.

Giới hạn upload 10 MB/file, 12 MB/request là mặc định kỹ thuật ban đầu, cần thống nhất với yêu cầu minh chứng. File evidence phải lưu riêng tư; không trả link public hoặc lưu vào thư mục static.

Dừng PostgreSQL mà giữ dữ liệu: `docker compose stop`.
