# Hợp đồng Import CSV/Excel: Thực thể Khóa học & Chương trình Đào tạo (`IntakeProgram`) — UC026

> **Mã tài liệu:** `CONTRACT-IMP-INTAKE-PROGRAM-v1.0`
> **Liên kết Jira:** `QE-45`, `QE-26`, `QE-62`
> **Liên kết SRS:** `UC026` (UC6.2), `REQ-DATA-001` → `REQ-DATA-003`, `REQ-DATA-007` → `REQ-DATA-009`, `TBD-DATA-01`
> **Tệp dữ liệu nguồn QNU:** `khoahoc.csv` (202 chương trình đào tạo theo khóa từ K45 đến K49)

---

## 1. Mục tiêu & Vai trò trong Hệ thống

Thực thể **Khóa học & Chương trình đào tạo (`IntakeProgram` / `Cohort`)** từ tệp `khoahoc.csv` xác định mối liên kết giữa **Khóa tuyển sinh (`IntakeNo` / `StartYear`)**, **Ngành đào tạo (`SpecializationCode`)** và **Khoa (`DepartmentCode`)**:
- Cung cấp ánh xạ chuẩn từ `IntakeNo` (`45, 46, 47, 48, 49`) sang năm nhập học `StartYear` (`2022, 2023, 2024, 2025, 2026` — lưu tại cột `org_class.cohort_year` trong Schema V1).
- Chuẩn hóa mã chương trình khóa học `IntakeProgramCode` theo công thức: `<SpecializationCode><ProgramLevelCode><TrainingModeCode>K<IntakeNo>` (ví dụ: `105DHCQK48` = Ngành `105` Công nghệ thông tin, bậc `DH` Đại học, hệ `CQ` Chính quy, khóa `K48`).

---

## 2. Cấu trúc Cột CSV/Excel (`khoahoc.csv`)

Hỗ trợ đọc trực tiếp **24 cột gốc của `khoahoc.csv`** hoặc **Mẫu rút gọn 10 cột**:

| # | Tên cột chuẩn (Header) | Alias chấp nhận | Bắt buộc? | Kiểu & Độ dài | Định dạng / Quy tắc kiểm tra | Ví dụ (`khoahoc.csv`) |
|---:|---|---|:---:|---|---|---|
| 1 | `IntakeProgramCode` | `program_code`, `ma_ctdt_khoa` | **Bắt buộc** | Text (6–30) | Khớp mẫu `^\d{3,4}[A-Z]{2,4}K\d{2}$` (ví dụ `105DHCQK48`, `752DHCQK49`). Duy nhất toàn hệ thống. | `105DHCQK48` |
| 2 | `IntakeProgramName` | `program_name`, `ten_ctdt` | **Bắt buộc** | Text (2–150) | Tên chương trình đào tạo (tiếng Việt có dấu). | `Công nghệ thông tin` |
| 3 | `IntakeNo` | `intake_no`, `cohort_no`, `so_khoa` | **Bắt buộc** | SmallInt | Số nguyên dương trong khoảng `40`..`60`. Khớp với hậu tố `K<IntakeNo>` trong `IntakeProgramCode`. | `48` |
| 4 | `StartYear` | `start_year`, `cohort_year`, `nam_nhap_hoc` | **Bắt buộc*** | SmallInt | Năm bắt đầu khóa học (`2017`..`2037`). *Nếu để trống: tự tính theo quy tắc QNU `StartYear = 1977 + IntakeNo` (ví dụ K46 → 2023, K48 → 2025, K49 → 2026). | `2025` |
| 5 | `SpecializationCode` | `specialization_code`, `ma_nganh` | **Bắt buộc** | Text (3–10) | Mã ngành (`^\d{3,4}$`). Phải tồn tại trong danh mục Ngành và khớp với tiền tố của `IntakeProgramCode`. | `105` |
| 6 | `DepartmentCode` | `department_code`, `ma_khoa` | **Bắt buộc** | Text (2–30) | Mã khoa quản lý. Phải tồn tại trong danh mục Khoa và khớp với Khoa của Ngành. | `105` |
| 7 | `ProgramLevelCode` | `graduate_level_code`, `ma_bac` | Tùy chọn | Text (2–10) | Mã bậc đào tạo (`DH` = Đại học, `THS` = Thạc sĩ, `TS` = Tiến sĩ). Mặc định `DH`. | `DH` |
| 8 | `TrainingModeCode` | `study_type_id`, `ma_he` | Tùy chọn | Text (2–10) | Mã loại hình đào tạo (`CQ` = Chính quy, `VHVL` = Vừa học vừa làm, `LT` = Liên thông). Mặc định `CQ`. | `CQ` |
| 9 | `IntakeProgramId` | `id` | Tùy chọn | UUID (36) | UUID của chương trình khóa học. Nếu trống: sinh UUID v5 từ `IntakeProgramCode`. | `ec806e76-83f0-44d6-b97e-4460e746dd76` |
| 10 | `DepartmentName` | `faculty_name`, `ten_khoa` | Tùy chọn | Text (≤ 150) | Tên khoa (dùng để kiểm tra chéo nếu có). | `Khoa Công nghệ thông tin` |

> **Các cột phụ trong `khoahoc.csv` (24 cột) được phép giữ nguyên**:
> `IntakeId`, `ProgramId`, `ApplyFromSemesterId`, `Notes`, `ProgramLevelId`, `ProgramLevelName`, `TrainingModeId`, `TrainingModeName`, `IntakeName`, `SpecializationId`, `SpecializationName`, `DepartmentId`, `ProgramInputInfo`, `FilePath`.

---

## 3. Quy tắc Kiểm tra Chéo & Xử lý Lỗi

1. **Tính nhất quán của Mã chương trình (`IntakeProgramCode`)**:
   - Tiền tố mã ngành trong `IntakeProgramCode` (ví dụ `105` trong `105DHCQK48`) phải khớp với cột `SpecializationCode` (`105`). Nếu lệch → `ROW_PROGRAM_CODE_SPEC_MISMATCH`.
   - Hậu tố khóa học trong `IntakeProgramCode` (ví dụ `K48` trong `105DHCQK48`) phải khớp với cột `IntakeNo` (`48`). Nếu lệch → `ROW_PROGRAM_CODE_INTAKE_MISMATCH`.
2. **Tính nhất quán giữa `IntakeNo` và `StartYear`**:
   - Với hệ Đại học chính quy QNU, `StartYear` chuẩn là `1977 + IntakeNo`. Nếu `StartYear` lệch quá ±1 năm so với công thức này → Báo lỗi `ROW_INVALID_START_YEAR`.
3. **Kiểm tra tham chiếu `Khoa ↔ Ngành`**:
   - `SpecializationCode` phải tồn tại trong danh mục Ngành (`ROW_SPECIALIZATION_NOT_FOUND`).
   - `DepartmentCode` phải tồn tại trong danh mục Khoa (`ROW_DEPARTMENT_NOT_FOUND`) và phải đúng là Khoa quản lý ngành đó (`ROW_DEPARTMENT_SPEC_MISMATCH`).

---

## 4. Bảng Mã Lỗi Cấp Dòng cho Thực thể Khóa học & CTĐT (`IntakeProgram`)

| Mã lỗi (`errorCode`) | Trường (`field`) | Điều kiện kích hoạt |
|---|---|---|
| `ROW_MISSING_REQUIRED_FIELD` | `IntakeProgramCode`, `IntakeProgramName`, `IntakeNo`, `SpecializationCode`, `DepartmentCode` | Thiếu bất kỳ trường bắt buộc nào. |
| `ROW_INVALID_PROGRAM_CODE_FORMAT` | `IntakeProgramCode` | Không khớp mẫu `^\d{3,4}[A-Z]{2,4}K\d{2}$`. |
| `ROW_DUPLICATE_CODE_IN_FILE` | `IntakeProgramCode` | Trùng mã `IntakeProgramCode` trong cùng tệp. |
| `ROW_PROGRAM_CODE_SPEC_MISMATCH` | `IntakeProgramCode` | Mã ngành trong `IntakeProgramCode` khác với cột `SpecializationCode`. |
| `ROW_PROGRAM_CODE_INTAKE_MISMATCH` | `IntakeProgramCode` | Số khóa trong `IntakeProgramCode` khác với cột `IntakeNo`. |
| `ROW_INVALID_INTAKE_NO` | `IntakeNo` | Không phải số nguyên hoặc nằm ngoài khoảng `40..60`. |
| `ROW_INVALID_START_YEAR` | `StartYear` | Không phải năm hợp lệ hoặc lệch với `1977 + IntakeNo`. |
| `ROW_SPECIALIZATION_NOT_FOUND` | `SpecializationCode` | Ngành chưa tồn tại trong hệ thống. |
| `ROW_DEPARTMENT_NOT_FOUND` | `DepartmentCode` | Khoa chưa tồn tại trong hệ thống. |
| `ROW_DEPARTMENT_SPEC_MISMATCH` | `DepartmentCode` | Ngành thuộc khoa khác với `DepartmentCode` khai báo trên dòng. |

---

## 5. File Mẫu Hợp lệ và File Mẫu Lỗi

- **File mẫu hợp lệ**: [`samples/intake-program-valid-sample.csv`](./samples/intake-program-valid-sample.csv) (8 chương trình đào tạo K46–K49 trích từ `khoahoc.csv`).
- **File mẫu lỗi**: [`samples/intake-program-invalid-sample.csv`](./samples/intake-program-invalid-sample.csv) (Bao gồm dòng sai định dạng mã, lệch mã ngành giữa `IntakeProgramCode` và `SpecializationCode`, lệch `IntakeNo` vs `StartYear`, ngành không tồn tại, và trùng mã).
