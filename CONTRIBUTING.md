# Quy chuẩn Đóng góp & Phát triển — QNU-Events

Chào mừng bạn tham gia phát triển dự án QNU-Events. Để đảm bảo chất lượng mã nguồn, tính minh bạch và tuân thủ quy trình Scrum theo rubric môn học, tất cả thành viên bắt buộc phải tuân theo quy chuẩn dưới đây.

---

## 1. Mô hình Phân nhánh (Git Branching Model)

Dự án áp dụng mô hình phân nhánh chuẩn:
- **`main`**: Nhánh phát hành chính thức (Production). Chỉ chứa code ổn định sau mỗi Sprint Review. Nghiêm cấm push code trực tiếp.
- **`develop`**: Nhánh tích hợp chính (Integration). Mọi tính năng sau khi hoàn thành sẽ merge vào đây qua Pull Request.
- **`feature/QE-<id>-<slug>`**: Nhánh tính năng mới tương ứng với một thẻ Story/Task trên Jira (ví dụ: `feature/QE-65-event-crud`). Rẽ nhánh từ `develop`.
- **`chore/QE-<id>-<slug>`**: Nhánh bảo trì, cấu hình, hạ tầng (ví dụ: `chore/QE-70-repo-hygiene`). Rẽ nhánh từ `develop`.
- **`bugfix/QE-<id>-<slug>`**: Nhánh sửa lỗi được QA phát hiện (ví dụ: `bugfix/QE-85-quota-race`).

---

## 2. Quy ước Thông điệp Commit (Conventional Commits)

Mỗi commit phải giải quyết một mục đích duy nhất và tuân theo định dạng:
```text
<type>(<scope>): <mô tả ngắn gọn bằng tiếng Anh hoặc tiếng Việt> [QE-<id>]
```

**Các `type` hợp lệ:**
- `feat`: Tính năng mới cho người dùng.
- `fix`: Sửa lỗi phát sinh.
- `test`: Thêm hoặc chỉnh sửa unit test, integration test.
- `docs`: Thêm hoặc cập nhật tài liệu (SRS, architecture, design docs).
- `chore`: Cập nhật cấu hình build, dependency, công cụ phát triển.
- `refactor`: Tái cấu trúc code nhưng không thay đổi hành vi nghiệp vụ.
- `ci`: Thay đổi cấu hình CI/CD (`.github/workflows`).

*Ví dụ:*
- `feat(event): implement event draft creation API [QE-65]`
- `test(registration): add concurrent quota race test [QE-71]`
- `chore(repo): configure CODEOWNERS and git workflow [QE-70]`

---

## 3. Quy trình Tạo & Duyệt Pull Request (PR)

1. **Trước khi mở PR:**
   - Đảm bảo nhánh đã được rebase mới nhất từ `develop`.
   - Chạy kiểm thử nội bộ trên máy:
     ```powershell
     .\mvnw.cmd -B -ntp verify -Pintegration
     ```
   - Xác nhận 100% test xanh (không dùng `-DskipTests`).

2. **Mở PR vào `develop`:**
   - Điền đầy đủ thông tin theo mẫu `.github/pull_request_template.md`.
   - Đính kèm mã Jira key, danh sách use case / requirement liên quan.
   - Ghi rõ các bước kiểm thử và bằng chứng (ảnh chụp màn hình, log test).

3. **Điều kiện để được Merge (Definition of Done - DoD):**
   - CI (`GitHub Actions`) chạy thành công (build và verify xanh).
   - Ít nhất **1 thành viên khác (Code Owner tương ứng)** phê duyệt (Approve).
   - Không có conflict với nhánh `develop`.

---

## 4. Báo cáo Bug & Lỗi

- Mọi lỗi được phát hiện trong quá trình kiểm thử phải được tạo ticket `Bug` trên Jira `QE`.
- Không âm thầm sửa lỗi không có ticket. Mã commit sửa lỗi phải gắn Jira Bug key tương ứng.
