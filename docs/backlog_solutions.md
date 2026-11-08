# Giải pháp chi tiết sửa Product Backlog (project QE)

Mỗi mục dưới đây tương ứng một vấn đề đã nêu. Các con số (point, ngày) là **đề xuất** để nhóm duyệt, không phải quyết định đã chốt.

---

## 1. Chuẩn rubric "Quản lý yêu cầu và bug"

### 1.1 Story point cho 32 item chưa có
Thang chuẩn: **1, 2, 3, 5, 8** (bỏ 6 và 13). Đề xuất:

| Item | SP | Item | SP | Item | SP |
|---|---|---|---|---|---|
| QE-35 Cấp/thu hồi Admin | 3 | QE-47 Retention job | 5 | QE-62 Import preview | 5 |
| QE-36 Chốt login/session | 3 | QE-48 Backup/restore | 3 | QE-63 Export attendance | 3 |
| QE-37 Chi tiết event | 3 | QE-49 Rà soát bảo mật | 3 | QE-73 Cấp TK sinh viên | 5 |
| QE-38 Quy tắc quota | 2 | QE-50 Test phân quyền | 5 | QE-74 Google OAuth | 5 |
| QE-39 Đăng ký + waitlist | 5 | QE-51 E2E test | 5 | QE-75 Event Owner xem DS | 3 |
| QE-40 Hủy + auto-promote | 5 | QE-52 API contract tương lai | 3 | QE-76 Sync QR offline | 5 |
| QE-41 Chốt deadline appeal | 2 | QE-53 Release gate | 2 | QE-77 Sync GPS offline | 5 |
| QE-42 Gửi evidence | 5 | QE-54 3 attempts + chống trùng | 5 | QE-78 Evidence offline | 5 |
| QE-43 BCS duyệt evidence | 5 | QE-55 GPS radius | 5 | QE-58 Lịch sử đầy đủ | 3 |
| QE-44 Đặc tả token QR | 3 | QE-59 Nộp appeal | 5 | QE-60 Xử lý appeal | 5 |
| QE-45 CSV contract | 2 | QE-46 Load test | 5 | | |

Cách làm: họp **Planning Poker** 30–45 phút, lấy QE-71 (5 điểm) làm item tham chiếu. Chỉ estimate item lá (xem mục 2). Ghi người estimate vào comment.

### 1.2 Deadline
Jira dùng trường `Due date`. Quy tắc:
- Item thuộc sprint: due date = **ngày Sprint Review**.
- Item chưa vào sprint: gán due date theo mốc dự kiến của release, ghi rõ "dự kiến".
- Nhóm Sprint 1 (QE-28, 56, 57, 61, 64, 65, 66, 70, 71, 72; tổng 38 SP): due = ngày review Sprint 1.
- Nhóm decision (QE-36, 38, 41, 44, 45, 53): due = **trước** sprint dùng chúng, vì các story phụ thuộc vào đó.

Thứ tự sprint gợi ý, theo phụ thuộc:

| Sprint | Nội dung |
|---|---|
| 1 | Lát cắt dọc: login mẫu → tạo/publish → đăng ký → QR → lịch sử |
| 2 | QE-38/12/39/40 (quota, waitlist, hủy), QE-44, QE-54/55 (GPS), QE-45/62 (import), QE-73 |
| 3 | Evidence (QE-42/43/18/19), no-show (QE-14), retention (QE-47), appeal (QE-41/59/60) |
| 4 | Offline (QE-33/76/77/78), export, Google OAuth, load test, security, release |

Dùng Jira Automation hoặc `twg jira workitem update` để gán hàng loạt sau khi nhóm chốt ngày.

### 1.3 Checklist
Jira chuẩn không có checklist. Hai lựa chọn:
- **Khuyến nghị:** bật field *Checklist* nếu có (app Checklist for Jira). Mỗi dòng AC thành một mục.
- **Không cần app:** tạo sub-task cho từng nhóm AC. Với item 5 điểm trở lên, tạo 3–5 sub-task (API, UI, test, audit).

Mẫu cho QE-71:
- [ ] API đăng ký giữ suất nguyên tử
- [ ] Từ chối sai lớp, ngoài hạn, hết chỗ
- [ ] Unique constraint student/event
- [ ] Integration test tranh suất cuối
- [ ] UI hiển thị kết quả và lý do lỗi

### 1.4 Chuẩn hóa mô tả QE-6→29
Dùng một template cho mọi item:

```
User story: Là <role>, tôi muốn <mục tiêu> để <giá trị>.
Acceptance Criteria: (mỗi dòng kiểm chứng được, dạng Given/When/Then)
Dependencies: QE-x, QE-y
Out of scope:
Mã FR (SRS): FR-xx
```
Task kỹ thuật/thiết kế dùng: Mục tiêu / Deliverables / Acceptance Criteria (đang đúng ở QE-38, 44, 45).

### 1.5 Mã chức năng ↔ SRS
- Đặt mã `FR-<module>-<nn>`, ví dụ `FR-REG-03`, và `NFR-xx` cho phi chức năng.
- Mỗi mã chức năng trong SRS ghi key Jira (`QE-39`), và ngược lại, mỗi issue ghi mã FR trong mô tả.
- Tạo **bảng truy vết** (traceability matrix) trong Confluence: FR → QE → test case → PR.
- Dùng BR-ATT-01..10 của proposal làm mã nghiệp vụ cho attendance (QE-20, 54, 56).

### 1.6 Quản lý bug
- Tạo workflow Bug: `Open → In Progress → Fixed → Verified → Closed` (+ `Reopened`).
- Field bắt buộc: Steps to reproduce, Expected/Actual, Severity (Critical/Major/Minor), Environment, Link tới story hoặc test case.
- Label: `bug`, `found-by-qa`, `regression`.
- Quy tắc: lỗi phát hiện ở QE-28, 46, 50, 51 phải có Bug issue, theo yêu cầu có sẵn trong các description đó.
- Điểm bug được tính trong công thức Sprint 1 (0,05/bug), nên QA phải report đúng và có link.

---

## 2. Trùng lặp và cấu trúc

### 2.1 Gỡ trùng point
Hiện nhiều cặp "cha – lát cắt" cùng có point. Giải pháp: **chỉ point ở item lá**.

| Item cha (đổi thành Epic con/Feature, bỏ point) | Item lá |
|---|---|
| QE-11 Quản lý event | QE-65, QE-32, QE-37 |
| QE-13 Đăng ký/hủy/waitlist | QE-71, QE-39, QE-40 |
| QE-16 Dynamic QR | QE-56, QE-57, QE-76 |
| QE-21 Lịch sử | QE-72, QE-58 |
| QE-22 Appeal | QE-59, QE-60 |
| QE-23 Audit | QE-61 + phần audit trong từng story |
| QE-26 Import | QE-62 (QE-26 giữ làm backend, QE-62 làm UI/preview) |
| QE-34 Export | QE-63 |
| QE-17 GPS / QE-18 Evidence | QE-55 / QE-42, QE-43 |

Cách làm gọn nhất trong Jira: giữ 5 epic hiện có, thêm **mức Feature bằng label hoặc Epic con** (ví dụ `feature:registration`), đổi item cha thành **Epic** hoặc đóng và chuyển AC sang item lá. Tránh làm việc kép bằng cách:
1. Copy AC còn thiếu từ item cha sang item lá.
2. Đặt SP của cha = 0, hoặc đóng với lý do "Superseded by QE-x".
3. Cập nhật tham chiếu chéo.

Các phần "Phân chia triển khai" đã có trong description chính là nội dung để chuyển sang item lá.

### 2.2 Epic mới cho Auth và User/Role
Tạo **EPIC 6 — Authentication, Accounts & Roles**, chuyển vào: QE-30, 35, 36, 64, 73, 74, 7 (RBAC), 50. EPIC 1 chỉ còn kiến trúc, NFR, bảo mật, hạ tầng. Việc này khớp PROD-4 và PROD-7.

### 2.3 Đổi loại issue
QE-19 (Proxy Attendance) đổi **Task → Story**, viết lại theo "Là sinh viên chưa có thiết bị, tôi muốn nhờ bạn điểm danh hộ qua evidence…". Thêm AC: chọn sinh viên B, evidence chứa mặt cả A và B, đánh dấu `PROXY`, BCS duyệt, audit.

### 2.4 Sửa tên role
Trong QE-30 thay "Admin/Student Affairs" bằng "Admin/Event Owner" cho đúng proposal §2.

---

## 3. Item còn thiếu: đề xuất tạo mới

### 3.1 Chức năng và nghiệp vụ

| Item mới | Loại | SP | AC chính |
|---|---|---|---|
| **Phân công BCS ↔ lớp** | Story | 5 | System Admin gán/gỡ BCS cho lớp. Có lịch sử thay đổi và audit. Hiệu lực ngay ở request tiếp theo. Là phụ thuộc của QE-18, 43. |
| **BCS xem lịch sử xử lý của lớp** | Story | 3 | Chỉ thấy quyết định của lớp mình. Có lọc theo event, trạng thái. Có test truy cập sai lớp. |
| **Sinh viên xem trạng thái hạn chế đăng ký** | Story | 3 | Hiển thị lý do, ngày hết hạn, nút khiếu nại. Chặn đăng ký ở server, không chỉ ẩn nút. |
| **Admin gỡ hạn chế no-show** | Story | 3 | Chỉ Admin được gỡ, bắt buộc có lý do, ghi audit, gỡ xong sinh viên đăng ký lại được ngay. |
| **Appeal cho no-show restriction** | Story | 3 | Sinh viên khiếu nại hạn chế. Bổ sung vào QE-59 các loại §9.1: sai thông tin event, sync offline lỗi. |
| **Quản lý danh mục** (loại hoạt động, điểm liên quan, khoa/ngành/lớp) | Story | 5 | CRUD cho System Admin. Event chọn từ danh mục. Không xóa danh mục đang được dùng, chỉ vô hiệu hóa. |
| **Field tùy chọn của event** (banner, đơn vị tổ chức, điều kiện, phí) | Task | 3 | Bổ sung vào QE-11/65. Có validate kích thước ảnh và định dạng. |
| **Sửa/xóa event đã publish** | Story | 5 | Dựa vào quyết định QE-15. Gợi ý: event đã có đăng ký chỉ cho sửa field an toàn, đổi thời gian/địa điểm phải thông báo, xóa thành `Cancelled`. |
| **Cấu hình tham số hệ thống** | Task | 3 | Cấu hình chu kỳ QR, grace, bán kính mặc định, số attempt, hạn evidence, hạn appeal. Có giá trị mặc định, mọi thay đổi ghi audit. |
| **Admin xem flag GPS đáng ngờ** | Story | 2 | Danh sách attendance bị flag theo event, chỉ đọc. |

**Việc bắt buộc làm trước:** chốt các quyết định đã có sẵn trong item decision (QE-15, 36, 38, 41, 44, 45, 53). Đây là chốt chặn thật của sprint 2–3.

### 3.2 Kỹ thuật

| Item mới | SP | Nội dung |
|---|---|---|
| **Thiết kế CSDL/ERD** | 5 | Bảng user, role, class, student, event, registration (unique student+event), attendance (unique hợp lệ), attempt, evidence, appeal, audit, restriction, outbox. Review xong mới viết migration V2+. Tạo **trước** các story cần schema. |
| **Hạ tầng DB outbox + scheduler** | 5 | Bảng outbox, worker poll, retry với backoff, idempotent consumer, chỉ chạy side effect sau commit. Dùng chung cho no-show (QE-14), retention (QE-47), audit. |
| **Quyết định lưu trữ evidence** | 2 | Chọn đĩa local, MinIO hoặc S3. Giới hạn dung lượng, định dạng, đường dẫn không đoán được, truy cập qua URL ký hạn ngắn. Là đầu vào của QE-42, 47, 48. |
| **Chọn công nghệ client offline** | 3 | Spike: PWA + service worker + IndexedDB. Đánh giá camera, GPS, background sync trên iOS/Android. Kết quả cập nhật QE-33, 57. |

### 3.3 Đưa vào Epic 5 hoặc epic "Delivery & Quality" mới

---

## 4. Hạng mục theo rubric

Đề xuất tạo **EPIC 7 — Project Deliverables & Quality** (sở hữu bởi Scrum Master/QA):

| Item | SP | Rubric | Deliverable |
|---|---|---|---|
| Viết SRS (đầy đủ FR/NFR/BR, có mã khớp Jira) | 8 | Phân tích & Thiết kế | File SRS + bảng truy vết |
| Vẽ UML (use case, sequence cho đăng ký/QR/offline sync, state cho registration/attendance, ERD) | 5 | Phân tích & Thiết kế | Bộ sơ đồ |
| Tài liệu kiến trúc | 3 | Phân tích & Thiết kế | Mở rộng QE-6 |
| Wireframe/UI-UX (Event Owner, Student, BCS, Admin) | 8 | Phân tích & Thiết kế, UI/UX | Figma + style guide |
| Git workflow và quy ước commit | 2 | Quản lý mã nguồn | `CONTRIBUTING.md`: nhánh `main/develop/feature/*`, tên nhánh gắn key Jira, Conventional Commits, PR bắt buộc có review |
| CI/CD pipeline (build, test, lint, deploy) | 5 | Quản lý mã nguồn, Triển khai | GitHub Actions hoặc tương đương cho cả 2 repo |
| Test plan | 3 | Kiểm thử | Phạm vi, môi trường, chiến lược, tiêu chí vào/ra |
| Manual test case | 5 mỗi sprint | Kiểm thử | Test case gắn FR, lưu trong Jira/Xray hoặc bảng tính |
| Automation test (API + một luồng UI) | 5 mỗi sprint | Kiểm thử | Chạy được trong CI |
| Unit test (ghi trong AC của từng story) | — | Coding | Ngưỡng coverage tối thiểu, ví dụ 70% cho service |
| Test report | 2 mỗi sprint | Kiểm thử | Kết quả chạy, bug, độ phủ |
| Triển khai môi trường thử nghiệm | 5 | Triển khai (hệ số 0,5) | Staging chạy được 24/7 với seed data |
| Báo cáo cuối kỳ và demo script | 5 | Báo cáo và thuyết trình | Tài liệu + kịch bản demo |

Quy ước để điểm cá nhân công bằng, vì công thức Sprint 1 tính riêng điểm code và điểm test: gán assignee rõ cho từng item test, mỗi thành viên có item ở đúng cột của mình, và PR/test run gắn vào Jira (nhóm đã yêu cầu ở QE-28).

---

## 5. Project PROD

1. **PROD-3** (khiếu nại): hoàn tất nghiên cứu, chuyển user story sang QE (QE-22, 59, 60 và các item no-show mới ở mục 3.1) rồi đóng, hoặc dời due date nếu chưa xong. Hiện đang quá hạn (05/10/2026).
2. **Description của PROD-1→7:** ghi kết quả nghiệp vụ, danh sách user story sinh ra và link sang item QE. Nếu việc đã hoàn thành thì thêm một dòng "Output: QE-xx, QE-yy".
3. **Quan hệ PROD ↔ QE:** dùng link `relates to`. PROD-5 (danh mục) đang không có item QE tương ứng, đã bù ở mục 3.1. PROD-6 (theo dõi hoạt động) tương ứng QE-21, 58, 72, 75.
4. Xác định rõ PROD là **backlog nghiên cứu**, QE là **product backlog**. Ghi vào mô tả project để người chấm không nhầm.

---

## Thứ tự thực hiện (khoảng 1–2 ngày công)

| Bước | Việc | Ai |
|---|---|---|
| 1 | Chốt các item decision (QE-15, 36, 38, 41, 44, 45, 53) với stakeholder | PO/Leader |
| 2 | Tạo Epic 6 và 7, chuyển item sang đúng epic, sửa tên role | Scrum Master |
| 3 | Gỡ trùng point (mục 2.1), đổi QE-19 thành Story | Scrum Master + dev |
| 4 | Tạo item mới ở mục 3 và 4 | PO + Leader |
| 5 | Planning Poker toàn bộ item chưa có point | Cả nhóm |
| 6 | Viết lại QE-6→29 theo template, thêm checklist và mã FR | Mỗi người sửa item mình sở hữu |
| 7 | Gán sprint và due date | Scrum Master |
| 8 | Thiết lập workflow Bug và bảng truy vết | QA |
| 9 | Dọn PROD | Người sở hữu PROD |

> [!NOTE]
> Tôi có thể thực hiện bước 2, 3, 5 (phần ghi point sau khi bạn duyệt bảng 1.1) và 7 bằng TWG. Các bước tạo item mới (3, 4) nên duyệt từng nhóm trước khi tạo, vì một khi tạo sẽ sinh nhiều key mới.
