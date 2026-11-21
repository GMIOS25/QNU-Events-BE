# Hợp đồng Import CSV/Excel: Thực thể Ngành / Chuyên ngành (`Specialization`) — UC026

> **Mã tài liệu:** `CONTRACT-IMP-SPECIALIZATION-v1.0`
> **Liên kết Jira:** `QE-45`, `QE-26`, `QE-62`
> **Liên kết SRS:** `UC026` (UC6.2), `REQ-DATA-001` → `REQ-DATA-003`, `REQ-DATA-007` → `REQ-DATA-010`, `TBD-DATA-01`
> **Tệp dữ liệu nguồn QNU:** `nganh.csv` (116 bản ghi ngành & chuyên ngành đào tạo)

---

## 1. Mục tiêu & Đặc thù Dữ liệu Ngành Đào tạo QNU

Thực thể **Ngành / Chuyên ngành (`Specialization` / `Major`)** nằm giữa **Khoa (`Department`)** và **Lớp (`OrgClass`)** / **Chương trình khóa học (`IntakeProgram`)**.

Qua khảo sát tệp `nganh.csv` (116 dòng), `lop.csv` (367 dòng) và `student/sv_k46..k49.csv` (6.435 dòng), dữ liệu ngành của QNU có 3 đặc điểm quan trọng cần chuẩn hóa trong contract:
1. **Phân cấp 2 tầng trong cùng bảng `nganh.csv`**:
   - **Ngành đào tạo gốc (Mã 3 chữ số — `^\d{3}$`)**: Ví dụ `101` (Sư phạm Toán học), `105` (Công nghệ thông tin), `119` (Kỹ thuật phần mềm), `410` (Trí tuệ nhân tạo). Đây là mã xuất hiện ở vị trí chữ số thứ 4–6 của MSSV (`4651050001` → `105`).
   - **Chuyên ngành hẹp / Định hướng (Mã 4 chữ số — `^\d{4}$`)**: Ví dụ `1051` (KHOA HỌC MÁY TÍNH), `1052` (HỆ THỐNG THÔNG TIN), `1053` (CÔNG NGHỆ PHẦN MỀM) có `ParentSpecializationId` trỏ về ngành gốc `105` (`0d03377f-f79f-4f20-840e-ef1390b4dd40`).
2. **Tham chiếu Khoa (`DepartmentId` & `DepartmentCode`)**:
   - Mỗi ngành bắt buộc thuộc về một Khoa hợp lệ đã tồn tại trong `khoa.csv` (`REQ-DATA-008`).
3. **Phát hiện 3 mã ngành mới ở Khóa 49 chưa có trong `nganh.csv` gốc**:
   - Trong `lop.csv` và `sv_k49.csv`, có 3 lớp K49 mang mã ngành suy từ MSSV gồm: `412` (*Kinh tế số*, Khoa `114`), `502` (*Kế toán CLC / Định hướng ACCA*, Khoa `114`), `503` (*Tài chính - Ngân hàng Tiếng Anh*, Khoa `115`). Contract này cung cấp cơ chế bổ sung các ngành mới này vào danh mục trước khi import Lớp và Sinh viên K49.

---

## 2. Cấu trúc Cột CSV/Excel (`nganh.csv`)

Hệ thống hỗ trợ đọc đầy đủ **19 cột gốc của `nganh.csv`** hoặc **Mẫu rút gọn 7 cột**:

| # | Tên cột chuẩn (Header) | Alias chấp nhận | Bắt buộc? | Kiểu & Độ dài | Định dạng / Quy tắc kiểm tra | Ví dụ (`nganh.csv`) |
|---:|---|---|:---:|---|---|---|
| 1 | `SpecializationCode` | `code`, `major_code`, `ma_nganh` | **Bắt buộc** | Text (3–10) | Chuỗi 3 hoặc 4 chữ số (`^\d{3,4}$`). Duy nhất toàn hệ thống. | `105` |
| 2 | `SpecializationName` | `name`, `major_name`, `ten_nganh` | **Bắt buộc** | Text (2–150) | Tiếng Việt có dấu, chuẩn hóa Unicode NFC. | `Công nghệ thông tin` |
| 3 | `DepartmentCode` | `department_code`, `ma_khoa` | **Bắt buộc theo ĐK** (1 trong 2: `DepartmentCode` hoặc `DepartmentId`) | Text (2–30) | Mã khoa quản lý ngành. Phải tồn tại trong danh mục Khoa (`org_department`). | `105` |
| 4 | `DepartmentId` | `department_id` | **Bắt buộc theo ĐK** (nếu không có `DepartmentCode`) | UUID (36) | UUID của Khoa quản lý ngành. Nếu truyền cả `DepartmentCode` và `DepartmentId` thì phải khớp cùng 1 Khoa. | `24d2865f-ad8b-4c86-8a70-745045a98fbc` |
| 5 | `SpecializationId` | `id`, `major_id` | Tùy chọn | UUID (36) | UUID định danh ngành. Nếu trống: sinh UUID v5 từ `"qnu:specialization:" + SpecializationCode`. | `0d03377f-f79f-4f20-840e-ef1390b4dd40` |
| 6 | `ParentSpecializationId` | `parent_id`, `parent_code` | Tùy chọn | UUID / Code | UUID (hoặc mã 3 số) của ngành cha. Nếu là mã 4 số (`1051`), khuyến nghị trỏ về ngành 3 số (`105`). | `8b7fc10e-2f45-4137-ae37-0b3137e66c82` |
| 7 | `Abbreviation` | `abbreviation`, `ten_viet_tat` | Tùy chọn | Text (≤ 30) | Chữ viết tắt của ngành (dùng để gợi ý sinh mã lớp ngắn gọn, ví dụ `CNTT`, `SPT`, `KTPM`). | `CNTT` |
| 8 | `EnglishName` | `english_name` | Tùy chọn | Text (≤ 150) | Tên tiếng Anh của ngành. | `Information Technology` |
| 9 | `IsActive` | `is_active`, `active` | Tùy chọn | Boolean | `true` hoặc `false` (không phân biệt hoa thường). Mặc định `true`. | `true` |

> **Các cột metadata phụ trong `nganh.csv` (19 cột) được phép giữ nguyên khi import**:
> `OldSpecializationId`, `BeginYear`, `EndYear`, `DisplayOrder`, `SpecializationTypeId`, `TestingOlogyId`, `CreateDate`, `UpdateDate`, `CreateBy`, `UpdateBy`.

---

## 3. Quy tắc Kiểm tra Quan hệ & Xử lý Dữ liệu (`REQ-DATA-008` → `REQ-DATA-010`)

1. **Ràng buộc Khoa cha (`REQ-DATA-008`)**:
   - Mọi dòng Ngành phải tham chiếu tới một Khoa hợp lệ qua `DepartmentCode` hoặc `DepartmentId`.
   - Nếu Khoa không tồn tại trong hệ thống → Bỏ qua dòng lỗi và ghi mã `ROW_DEPARTMENT_NOT_FOUND` tại trường `DepartmentCode`/`DepartmentId`.
2. **Quan hệ Ngành cha - Chuyên ngành con trong cùng tệp (`REQ-DATA-009`)**:
   - Thứ tự dòng trong tệp không làm ảnh hưởng kết quả: nếu dòng chuyên ngành con `1051` xuất hiện trước dòng ngành cha `105` trong cùng tệp, hệ thống vẫn phân giải đúng qua cơ chế 2-pass.
   - Lưu ý dữ liệu thực tế `nganh.csv`: Một số ngành 3 chữ số có `ParentSpecializationId` trỏ tới UUID nhóm lĩnh vực cấp trường (ví dụ `3a55eaf5-24a2-4940-ad32-2f86f766cb60` cho khối Sư phạm, không nằm trong `nganh.csv`). Vì vậy:
     - Nếu `ParentSpecializationId` khớp một ngành trong `nganh.csv`/DB → Gắn FK `parent_id`.
     - Nếu `ParentSpecializationId` là UUID nhóm lĩnh vực ngoài bảng → Lưu vào cột `external_group_id` (kèm cảnh báo `WARN_EXTERNAL_PARENT_GROUP`) thay vì chặn lỗi dòng.
3. **Bảo vệ khi thay đổi Khoa quản lý Ngành (`REQ-DATA-010`)**:
   - Nếu một ngành đã có Lớp (`org_class`) và Sinh viên trực thuộc, việc đổi `DepartmentCode` của ngành sang Khoa khác sẽ bị chặn với lỗi `ROW_ORG_HIERARCHY_CHANGE_BLOCKED` trừ khi Admin bật cờ xác nhận điều chỉnh tổ chức (`allowHierarchyChange=true`).

---

## 4. Bảng Mã Lỗi Cấp Dòng cho Thực thể Ngành (`Specialization`)

| Mã lỗi (`errorCode`) | Trường (`field`) | Điều kiện kích hoạt |
|---|---|---|
| `ROW_MISSING_REQUIRED_FIELD` | `SpecializationCode`, `SpecializationName`, `DepartmentCode` | Thiếu mã ngành, tên ngành hoặc thông tin khoa quản lý. |
| `ROW_INVALID_SPEC_CODE_FORMAT` | `SpecializationCode` | Mã ngành không khớp định dạng 3–4 chữ số (`^\d{3,4}$`). |
| `ROW_DUPLICATE_CODE_IN_FILE` | `SpecializationCode` | Mã ngành bị lặp lại từ lần thứ 2 trong cùng tệp. |
| `ROW_DEPARTMENT_NOT_FOUND` | `DepartmentCode` / `DepartmentId` | Khoa được tham chiếu không tồn tại trong hệ thống. |
| `ROW_DEPARTMENT_ID_CODE_MISMATCH` | `DepartmentId` | Truyền cả `DepartmentCode` và `DepartmentId` nhưng không thuộc cùng một Khoa. |
| `ROW_CIRCULAR_PARENT_REFERENCE` | `ParentSpecializationId` | Ngành tự trỏ làm ngành cha của chính nó. |
| `ROW_ORG_HIERARCHY_CHANGE_BLOCKED` | `DepartmentCode` | Đổi khoa quản lý của ngành đang có lớp/sinh viên hoạt động khi `allowHierarchyChange=false`. |
| `ROW_FORMULA_INJECTION_DETECTED` | `SpecializationName`, `EnglishName` | Chứa ký tự bắt đầu công thức `=`, `+`, `-`, `@`. |

---

## 5. File Mẫu Hợp lệ và File Mẫu Lỗi

- **File mẫu hợp lệ**: [`samples/specialization-valid-sample.csv`](./samples/specialization-valid-sample.csv) (Bao gồm các ngành tiêu biểu `101`, `105`, `1051`, `119`, `409`, `410`, `752` và bổ sung 3 ngành mới K49 `412`, `502`, `503`).
- **File mẫu lỗi**: [`samples/specialization-invalid-sample.csv`](./samples/specialization-invalid-sample.csv) (Bao gồm dòng thiếu tên ngành, mã ngành chứa chữ, trùng mã `105`, mã khoa `999` không tồn tại, lệch `DepartmentCode` và `DepartmentId`, và Formula Injection).
