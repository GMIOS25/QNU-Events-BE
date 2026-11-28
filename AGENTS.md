# Hướng dẫn & Quy tắc dành cho AI-Agent — QNU-Events

> Tài liệu này là nguồn sự thật (Single Source of Truth) ràng buộc mọi AI-Agent khi tham gia phát triển dự án QNU-Events (Sprint 1, Sprint 2 và các Sprint tiếp theo).
> Chi tiết kế hoạch Sprint 1: [`docs/agent/sprint1-rescue-plan.md`](docs/agent/sprint1-rescue-plan.md) · Lược đồ `V2` đóng băng cho Sprint 2: [`docs/Design/v2-schema-blueprint.md`](docs/Design/v2-schema-blueprint.md) · Nhật ký quyết định: [`docs/decisions.md`](docs/decisions.md).

---

## 1. Bộ 12 Quy tắc Bắt buộc (Rules of Engagement)

| # | Quy tắc | Chi tiết |
|---|---|---|
| **R1** | **Không bịa số liệu / bằng chứng** | Tuyệt đối không tạo biên bản họp, kết quả test, bug, commit hay PR giả. Mọi số liệu báo cáo, khai bảng điểm phải lấy từ dữ liệu chạy thật trên git, Jira và kết quả test tự động. |
| **R2** | **Không làm yếu test** | Cấm dùng `@Disabled`, cấm xóa assertion, hạ thấp ngưỡng kiểm thử hoặc thêm `-DskipTests` để "xanh" build. Nếu test fail 3 lần liên tiếp, ghi rõ vào `BLOCKERS.md`, dừng lại và xin ý kiến con người. |
| **R3** | **Một nhánh = Một Jira key** | Đặt tên nhánh theo cú pháp: `feature/QE-xx-<ten-ngan-gon>` hoặc `chore/QE-xx-<ten>`. Không bao giờ push trực tiếp vào `main` hay `develop`; mọi thay đổi phải qua Pull Request. |
| **R4** | **Quy ước Commit (Conventional Commits)** | Cú pháp: `type(scope): mô tả ngắn [QE-xx]` với `type` ∈ {`feat`, `fix`, `test`, `docs`, `chore`, `refactor`, `ci`}. Mỗi commit giải quyết một mục đích rõ ràng. |
| **R5** | **Kiểm chứng trước khi commit** | Chạy lệnh kiểm tra bắt buộc trước mỗi commit: <br>• BE: `bash ./mvnw -B -ntp verify` (hoặc `.\mvnw.cmd -B -ntp verify`)<br>• FE: `npm run lint && npm run test && npm run build` |
| **R6** | **Không sửa migration đã merge** | Schema Flyway một khi đã merge vào `develop` là bất biến. Mọi thay đổi cấu trúc DB phải tạo file mới `V2__...`, `V3__...`. Chỉ Agent phụ trách nền tảng (A0) mới được tạo migration. |
| **R7** | **Không commit bí mật** | Tất cả secrets (JWT secret, QR signing key, password) phải đọc qua biến môi trường. File `.env.example` chỉ chứa giá trị mẫu cho môi trường dev. Tuyệt đối không in mật khẩu, JWT hay token QR đầy đủ ra log. |
| **R8** | **Tuân thủ phạm vi phân công** | Mỗi agent chỉ được sửa các file nằm trong phạm vi của mình (xem Bảng phạm vi bên dưới). Nếu cần sửa đổi thành phần dùng chung hoặc của module khác, phải mở thảo luận trong PR, không tự ý sửa. |
| **R9** | **Server là nguồn sự thật** | Nghiệp vụ về thời gian, quyền hạn, quota giữ chỗ phải do backend xác thực. Tuyệt đối không tin tưởng dữ liệu gửi lên từ client (role, studentId, client timestamp). |
| **R10** | **Quản trị Jira (TWG)** | Luôn chạy `help describe` trước khi gọi lệnh → đọc trạng thái hiện tại → cập nhật theo lô nhỏ (≤ 10 item) → đọc lại xác minh → ghi nhật ký vào `docs/scrum/jira-change-log.md`. Không dùng lệnh xóa hoặc cờ hủy diệt. |
| **R11** | **Quản lý giả định** | Mọi quyết định kỹ thuật chưa có trong SRS phải được ghi vào `docs/decisions.md` dưới dạng **Assumption (chưa duyệt)**, không tự ý coi là đã chốt. |
| **R12** | **Dừng và hỏi khi thiếu thông tin** | Khi gặp điểm chặn (thiếu quyền, thiếu link thiết kế, thiếu remote), phải dừng lại và yêu cầu con người hỗ trợ, không tự ý đoán hay đi đường vòng qua cơ chế bảo mật. |

---

## 2. Bảng Phân công & Phạm vi Thư mục / File (Scope Table — Cập nhật Sprint 2)

| Agent | Thành viên (Owner) | Nhiệm vụ chính (Sprint 1 → Sprint 2+) | Phạm vi file được phép sửa đổi |
|---|---|---|---|
| **A0** | **Trần Thanh Cường** (`@GMIOS25`) | Duy nhất tạo migration `V2__sprint2_core_extensions.sql` (`R6`), `Auth` hardening & Google OAuth (`QE-30`, `QE-74`), `Outbox`, hạ tầng `EvidenceStoragePort`, CI/CD | `config/`, `auth/` (`QE-30`, `QE-74`), `common/`, `outbox/`, `db/migration/`, root FE scaffold & `src/features/auth/`, `.github/`, `docs/agent/` |
| **A1** | **Nguyễn Huy Hoàng** (`@hoangkq157`) | Quản lý Sự kiện (`QE-65`, `QE-66`, `QE-12`, `QE-32`), quản trị tài khoản & cấp TK SV (`QE-35`, `QE-73` phối hợp A0), phối hợp A2 (`QE-39`, `QE-40`) | `event/`, phối hợp `auth/`/`admin/` & `registration/`, `FE src/features/events/` |
| **A2** | **Khánh Dương Nguyễn** (`@YangKlee`) | Giữ suất, Waiting List & Hủy (`QE-71`, `QE-39`, `QE-40`), Chế tài No-show (`QE-14`), CSV/Excel Import (`QE-26`, `QE-62`) & QA (`QE-28`) | `registration/`, `importer/`, `FE src/features/registration/` & FE import, `src/test/**/e2e/`, `docs/test/` |
| **A3** | **Huỳnh Hữu Đan** (`@Dan28-05`) | Dynamic QR & Điểm danh (`QE-56`, `QE-57`, `QE-54`), GPS (`QE-55`), Offline Sync (`QE-33`, `QE-76..78`), Upload/Proxy Evidence & BCS duyệt (`QE-19`, `QE-42`, `QE-43`) | `attendance/`, `evidence/` *(đồng sở hữu với A4)*, `FE src/features/attendance/`, `FE src/features/evidence/` |
| **A4** | **Nguyễn Nhật Khánh** (`@KHANHSKY0808`) | Lịch sử (`QE-72`, `QE-58`), Audit log (`QE-61`), Khiếu nại 2 vòng (`QE-59`, `QE-60`), Evidence visibility/retention (`QE-24`, `QE-47`) & Tài liệu (`QE-80`) | `audit/`, `student/` (history), `appeal/`, `evidence/` *(đồng sở hữu với A3)*, `FE src/features/history/`, `FE src/features/appeal/`, `docs/` |

---

## 3. Lệnh Build, Test & Kiểm tra

### Backend (Spring Boot 4.1.2 - Java 21)
```powershell
# Chạy toàn bộ Unit test
.\mvnw.cmd -B -ntp test

# Chạy toàn bộ Unit test + Integration test (cần PostgreSQL 17 đang chạy)
.\mvnw.cmd -B -ntp verify -Pintegration

# Format code và kiểm tra style
.\mvnw.cmd spotless:check
```

### Frontend (React 19 + TypeScript + Vite)
```powershell
# Cài đặt thư viện
npm ci

# Kiểm tra kiểu & lint
npm run lint
npm run typecheck

# Chạy unit test giao diện
npm run test

# Build production
npm run build
```
