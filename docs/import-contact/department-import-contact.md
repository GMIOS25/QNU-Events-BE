# Hợp đồng Import CSV/Excel: Thực thể Khoa / Đơn vị Tổ chức (`Department` / `Faculty`) — UC026

> **Mã tài liệu:** `CONTRACT-IMP-DEPARTMENT-v1.0`
> **Liên kết Jira:** `QE-45`, `QE-26`, `QE-62`
> **Liên kết SRS:** `UC026` (UC6.2 — Nhập dữ liệu khoa ngành và lớp), `REQ-DATA-001` → `REQ-DATA-003`, `REQ-DATA-007` → `REQ-DATA-010`, `TBD-DATA-01`
> **Tệp dữ liệu nguồn QNU:** `khoa.csv` (52 đơn vị, trong đó 22 Khoa đào tạo và 12 Khoa đang quản lý trực tiếp các lớp chính quy K46–K49)

---

## 1. Mục tiêu & Ánh xạ Hệ thống

Thực thể **Khoa / Đơn vị (`Department` / `Faculty`)** là cấp cao nhất trong cây phân cấp tổ chức học vụ của Trường ĐH Quy Nhơn:

$$\text{Khoa (Department)} \longrightarrow \text{Ngành (Specialization)} \longrightarrow \text{Lớp (OrgClass)} \longrightarrow \text{Sinh viên (AppUser)}$$

### Ánh xạ Cơ sở Dữ liệu
- **Trong Schema V1 hiện hành ([`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql))**:
  - Thông tin Khoa được lưu trực tiếp tại cột `org_class.faculty_name` (`varchar(150) NOT NULL`).
- **Trong Schema V2 mở rộng cho `QE-26` (`org_department`)**:
  - Bảng `org_department`:
    - `id uuid PRIMARY KEY DEFAULT gen_random_uuid()` (ưu tiên giữ nguyên `DepartmentId` từ `khoa.csv` nếu có, hoặc sinh UUID v5 tất định theo `DepartmentCode`).
    - `code varchar(30) NOT NULL UNIQUE` (`DepartmentCode`).
    - `name varchar(150) NOT NULL` (`DepartmentName` đã làm sạch tiền tố `[code]`).
    - `raw_name varchar(180)` (tên gốc trong CSV, ví dụ `[105] Khoa Công nghệ thông tin`).
    - `parent_id uuid REFERENCES org_department(id)` (`ParentID` / phân giải từ `Parent`).
    - `is_academic_faculty boolean NOT NULL DEFAULT true`.

---

## 2. Cấu trúc Cột CSV/Excel (`khoa.csv`)

| # | Tên cột chuẩn (Header) | Alias chấp nhận | Bắt buộc? | Kiểu & Độ dài | Định dạng / Quy tắc kiểm tra | Ví dụ (`khoa.csv`) |
|---:|---|---|:---:|---|---|---|
| 1 | `DepartmentCode` | `code`, `faculty_code`, `ma_khoa` | **Bắt buộc** | Text (2–30) | Chuỗi chữ-số in hoa hoặc số (`^[A-Z0-9ĐÐ_-]{2,30}$`). Duy nhất toàn hệ thống. Bảo toàn số `0` đầu (ví dụ `000`, `001`). | `105` |
| 2 | `DepartmentId` | `id`, `faculty_id` | Tùy chọn | UUID (36) | Nếu có giá trị: phải đúng định dạng UUID chuẩn. Nếu để trống (như 40/52 dòng trong `khoa.csv`): hệ thống tự sinh UUID v5 từ namespace `"qnu:department:" + DepartmentCode`. | `24d2865f-ad8b-4c86-8a70-745045a98fbc` |
| 3 | `DepartmentName` | `name`, `faculty_name`, `ten_khoa` | **Bắt buộc** | Text (2–150) | Tiếng Việt có dấu. Hệ thống tự động chuẩn hóa bỏ tiền tố `[DepartmentCode] ` ở đầu nếu có (xem Mục 3.1). | `[105] Khoa Công nghệ thông tin` |
| 4 | `Parent` | `parent_code`, `ma_don_vi_cha` | Tùy chọn | Text (2–30) | Mã `DepartmentCode` của đơn vị cha (nếu là bộ môn/trung tâm trực thuộc khoa/trường). Phải tồn tại trong DB hoặc trong cùng tệp (`REQ-DATA-009`). | `000` |
| 5 | `ParentID` | `parent_id` | Tùy chọn | UUID (36) | UUID của đơn vị cha. Nếu có cả `Parent` và `ParentID` thì phải trỏ tới cùng một đơn vị cha. | `` |

---

## 3. Quy tắc Chuẩn hóa, Quan hệ Cha-Con & Xử lý Trùng mã

### 3.1. Quy tắc Chuẩn hóa Tên Khoa (`DepartmentName` Normalization)
Trong dữ liệu thực tế của QNU:
- Tệp `khoa.csv` lưu `DepartmentName` kèm mã ở đầu: `[105] Khoa Công nghệ thông tin`, `[201] Khoa Sư phạm`.
- Trong khi đó, tệp `khoahoc.csv`, `student/sv_k46..k49.csv` và bảng `org_class.faculty_name` lưu tên thuần: `Khoa Công nghệ thông tin`, `Khoa Sư phạm`.
- **Quy tắc bắt buộc khi Import**:
  1. Cắt khoảng trắng thừa đầu/cuối và áp dụng regex `^\[[^\]]+\]\s*` để tách bỏ tiền tố `[mã] ` khi lưu vào `name` / `faculty_name`.
  2. Khi so khớp tên Khoa giữa các tệp (`lop.csv`, `student/*.csv`), hệ thống so khớp trên **tên đã bỏ tiền tố `[mã]`** và chuẩn hóa Unicode NFC (không phân biệt hoa thường).

### 3.2. Quy tắc Xác định Quan hệ Cha-Con Không Phụ thuộc Thứ tự Dòng (`REQ-DATA-009`)
- Nếu tệp chứa cả đơn vị cha (`000` - Trường ĐH Quy Nhơn) và đơn vị con (`105` - Khoa CNTT có `Parent=000`), dù dòng con nằm ở dòng 2 và dòng cha nằm ở dòng 50:
  - Hệ thống thực hiện **2-pass parsing**: Pass 1 thu thập toàn bộ `DepartmentCode` hợp lệ trong tệp + DB; Pass 2 kiểm tra và gắn khóa ngoại `parent_id`.
  - Chặn chu trình tự tham chiếu (`DepartmentCode == Parent` hoặc vòng lặp `A → B → A`) với mã lỗi `ROW_CIRCULAR_PARENT_REFERENCE`.

### 3.3. Quy tắc Trùng mã & Bảo vệ Quan hệ Tổ chức (`REQ-DATA-007`, `REQ-DATA-010`)
- **Trùng `DepartmentCode` trong cùng tệp**: Giữ dòng đầu tiên, từ chối các dòng trùng phía sau với lỗi `ROW_DUPLICATE_CODE_IN_FILE`.
- **Trùng `DepartmentId` (UUID) giữa 2 `DepartmentCode` khác nhau**: Từ chối với lỗi `ROW_DUPLICATE_UUID_CONFLICT`.
- **Đã tồn tại trong DB (`UPSERT`)**:
  - Nếu tên và đơn vị cha không đổi → `UNCHANGED`.
  - Nếu đổi tên Khoa → Cập nhật tên Khoa và đồng bộ `org_class.faculty_name` của các lớp trực thuộc (`UPDATED`).

---

## 4. Bảng Mã Lỗi Cấp Dòng cho Thực thể Khoa (`Department`)

| Mã lỗi (`errorCode`) | Trường (`field`) | Điều kiện kích hoạt |
|---|---|---|
| `ROW_MISSING_REQUIRED_FIELD` | `DepartmentCode`, `DepartmentName` | Để trống mã khoa hoặc tên khoa (hoặc tên khoa sau khi cắt tiền tố `[mã]` bị rỗng). |
| `ROW_INVALID_CODE_FORMAT` | `DepartmentCode` | Mã khoa vượt quá 30 ký tự hoặc chứa ký tự đặc biệt ngoài danh mục cho phép. |
| `ROW_INVALID_UUID_FORMAT` | `DepartmentId`, `ParentID` | Không đúng chuẩn UUID 36 ký tự khi có giá trị. |
| `ROW_DUPLICATE_CODE_IN_FILE` | `DepartmentCode` | Lặp lại mã khoa trong cùng tệp CSV. |
| `ROW_DUPLICATE_UUID_CONFLICT` | `DepartmentId` | UUID trùng với một đơn vị khác mã trong tệp hoặc trong DB. |
| `ROW_PARENT_NOT_FOUND` | `Parent`, `ParentID` | Mã/UUID đơn vị cha không tồn tại trong DB và không có trong các dòng hợp lệ của tệp (`REQ-DATA-008`). |
| `ROW_CIRCULAR_PARENT_REFERENCE` | `Parent` | Đơn vị tự trỏ làm cha của chính mình hoặc tạo vòng lặp phân cấp. |
| `ROW_FORMULA_INJECTION_DETECTED` | `DepartmentName` | Bắt đầu bằng `=`, `+`, `-`, `@` (`NFR-SEC-009`). |

---

## 5. File Mẫu Hợp lệ và File Mẫu Lỗi

- **File mẫu hợp lệ**: [`samples/department-valid-sample.csv`](./samples/department-valid-sample.csv) (12 Khoa đào tạo chính quy có UUID thực tế từ `khoa.csv`).
- **File mẫu lỗi**: [`samples/department-invalid-sample.csv`](./samples/department-invalid-sample.csv) (Bao gồm dòng thiếu mã, trùng mã `105`, sai định dạng UUID, `Parent` không tồn tại, tự tham chiếu cha-con, và Formula Injection).
