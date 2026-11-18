# Hợp đồng Import CSV/Excel: Thực thể Sinh viên (`Student` / `AppUser`) — QE-45 & QE-26

> **Mã tài liệu:** `CONTRACT-IMP-STUDENT-v1.0`
> **Liên kết Jira:** `QE-45` (Xác định CSV import contract và báo cáo lỗi), `QE-26` (Backend Import Sinh viên), `QE-62` (UI Import Preview), `QE-73` (Cấp tài khoản sinh viên)
> **Liên kết SRS:** `UC025` (UC6.1), `REQ-DATA-001` → `REQ-DATA-007`, `REQ-DATA-010`, `NFR-PERF-005`, `NFR-SEC-009`, `NFR-QUAL-012`, `OTH-DATA-001` → `OTH-DATA-003`, `OTH-I18N-001`, `TBD-DATA-01`
> **Trạng thái:** Đề xuất chuẩn hóa cho `QE-45` (Assumption `A-IMP-01` chờ Stakeholder phê duyệt để đóng `TBD-DATA-01`)

---

## 1. Mục tiêu & Phạm vi

Tài liệu này định nghĩa **Hợp đồng kỹ thuật (Data & API Contract)** cho chức năng nhập (import) danh sách sinh viên từ tệp CSV/Excel vào hệ thống **QNU-Events**, dựa trên:
1. **Lược đồ CSDL hiện hành** ([`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql)) và Model Java ([`AppUser.java`](../../src/main/java/vn/edu/qnu/events/auth/AppUser.java), [`OrgClass.java`](../../src/main/java/vn/edu/qnu/events/student/OrgClass.java)).
2. **Tập dữ liệu thực tế của Trường Đại học Quy Nhơn (QNU)** gồm 6.435 bản ghi sinh viên thuộc 4 khóa (`student/sv_k46.csv`, `sv_k47.csv`, `sv_k48.csv`, `sv_k49.csv`) và danh mục tổ chức (`khoa.csv`, `nganh.csv`, `khoahoc.csv`, `lop.csv`).
3. **Đặc tả yêu cầu phần mềm (SRS v1.0)** mục 3.6 (`UC025`, `REQ-DATA-001..007`).

Mọi kịch bản kiểm thử tự động của `QE-26` và `QE-62` phải bám sát cấu trúc cột, mã lỗi và ngữ nghĩa giao dịch trong tài liệu này — **không tự giả định cấu trúc tệp**.

---

## 2. Ánh xạ Mô hình Dữ liệu & Cơ sở Dữ liệu (Model & DB Mapping)

### 2.1. Ánh xạ vào Schema V1 hiện hành (`app_user` & `user_role`)

Trong lược đồ [`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql), mỗi sinh viên được lưu trữ dưới dạng một tài khoản trong bảng `app_user` đi kèm vai trò `STUDENT` trong bảng `user_role`:

| Cột trong DB (`V1`) | Thuộc tính `AppUser` | Kiểu dữ liệu DB | Ràng buộc DB | Quy tắc sinh / Ánh xạ từ CSV |
|---|---|---|---|---|
| `id` | `id` | `uuid` | `PK DEFAULT gen_random_uuid()` | Giữ nguyên `id` nếu đã tồn tại theo `student_code`; sinh `UUID.randomUUID()` nếu tạo mới. |
| `login_id` | `loginId` | `varchar(50)` | `NOT NULL UNIQUE` | Gán bằng chính `StudentID` (MSSV 10 chữ số, đã `trim()`). |
| `password_hash` | `passwordHash` | `varchar(100)` | `NOT NULL` | Khi **tạo mới**: băm `BCrypt(12)` mật khẩu khởi tạo theo cấu hình hệ thống (không lưu plaintext). Khi **cập nhật**: giữ nguyên `password_hash` hiện có. |
| `full_name` | `fullName` | `varchar(150)` | `NOT NULL` | Lấy từ cột `StudentName` (hoặc ghép `LastName.trim() + " " + FirstName.trim()` nếu `StudentName` trống nhưng có họ/tên tách rời). Chuẩn hóa Unicode NFC. |
| `student_code` | `studentCode` | `varchar(30)` | `UNIQUE` | Lấy từ cột `StudentID` (chuỗi 10 chữ số `^\d{10}$`). |
| `class_id` | `classId` | `uuid` | ` FK REFERENCES org_class(id)` | Phân giải từ `ClassId` hoặc `ClassCode` / `ClassStudentName` sang `org_class.id` (xem Mục 5). |
| `status` | `status` | `varchar(20)` | `CHECK IN ('ACTIVE','LOCKED')` | Ánh xạ từ `StudyStatusID` / `StudyStatusName`: `1` (`Còn học`) → `ACTIVE`; `2, 3, 4, 7` (`Nghỉ học tạm thời`, `Buộc thôi học`, `Thôi học`, `Chuyển trường`) → `LOCKED`. Mặc định `ACTIVE` nếu cột trạng thái để trống. |
| `user_role.role` | `roles` | `varchar(30)` | `PK (user_id, role)` | Đảm bảo có `'STUDENT'` trong tập `roles` (không xóa các role bổ sung như `'CLASS_REPRESENTATIVE'` nếu sinh viên đã được gán BCS trước đó). |

### 2.2. Đề xuất mở rộng Schema V2 (`student_profile` / mở rộng `app_user`) cho `QE-26`

> [!NOTE]
> Theo Quy tắc **R6** ([`AGENTS.md`](../../AGENTS.md)), file `V1__sprint1_core_schema.sql` đã đóng băng. Khi triển khai `QE-26` ở Sprint 2, nếu cần lưu thêm thông tin hồ sơ sinh viên từ CSV nhà trường để phục vụ Google OAuth (`QE-74`) và tra cứu nâng cao, Agent A0 sẽ tạo migration `V2__student_profile_and_catalog.sql` bổ sung các cột/bảng phụ:
> - `email varchar(150)` (email cá nhân từ CSV; **không đặt UNIQUE cứng** vì dữ liệu thật QNU có các trường hợp thí sinh thi lại khóa sau dùng chung email cá nhân).
> - `qnu_email varchar(150) UNIQUE` (email định danh trường cấp dạng `<student_code>@st.qnu.edu.vn` phục vụ `QE-74`).
> - `birth_date date`, `gender varchar(10)`, `id_card varchar(20)`, `study_status_code smallint`, `intake_no smallint`, `specialization_code varchar(10)`, `department_code varchar(10)`.

---

## 3. Đặc tả Định dạng Tệp, Encoding & Cấu trúc Cột (Template Specification)

### 3.1. Quy chuẩn Tệp vật lý & Encoding

| Thuộc tính | Quy định bắt buộc | Mã lỗi khi vi phạm |
|---|---|---|
| **Định dạng hỗ trợ** | `.csv` (`text/csv`, `application/csv`, `text/plain`) hoặc `.xlsx` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) | `FILE_UNSUPPORTED_MEDIA_TYPE` (HTTP 415) |
| **Bảng mã (Encoding)** | **UTF-8** (chấp nhận cả `UTF-8 without BOM` và `UTF-8 with BOM` `\uFEFF` do Excel xuất ra). Tệp chứa byte lỗi UTF-8 (`\uFFFD`) hoặc mã hóa `Windows-1258`/TCVN3/VNI bị từ chối. | `FILE_INVALID_ENCODING` (HTTP 400) |
| **Dấu phân cách (CSV)** | Dấu phẩy `,` (RFC 4180). Trường chứa dấu `,`, dấu `"` hoặc xuống dòng phải bọc trong cặp nháy kép `"..."` (ví dụ: `"Số 08, Huỳnh Thúc Kháng, thôn 3"`). | `FILE_MALFORMED_CSV` (HTTP 400) |
| **Dòng tiêu đề (Header)** | Dòng 1 bắt buộc là dòng Header. Tên cột **không phân biệt hoa thường** (`case-insensitive`), tự động cắt khoảng trắng đầu/cuối và bỏ ký tự BOM `\uFEFF` ở cột đầu tiên. | `FILE_MISSING_REQUIRED_COLUMNS` (HTTP 400) |
| **Excel (`.xlsx`)** | Đọc Sheet đầu tiên (index `0`) hoặc Sheet có tên `Students` / `SinhVien`. Các cột mã (`StudentID`, `IDCard`, `DepartmentCode`, `SpecializationCode`) phải được đọc dưới dạng chuỗi (`DataFormatter` / Text) để **bảo toàn số `0` ở đầu** (`NFR-QUAL-012`). | `FILE_CORRUPTED` (HTTP 400) |
| **Giới hạn dung lượng** | Tối đa **10 MB** / tệp; số dòng dữ liệu từ **1 đến 10.000 dòng** (không tính dòng header và dòng trống hoàn toàn ở cuối tệp). | `FILE_TOO_LARGE` (HTTP 413) / `FILE_ROW_LIMIT_EXCEEDED` (HTTP 422) |
| **Chống Formula Injection** | Mọi giá trị chuỗi bắt đầu bằng `=`, `+`, `-`, `@`, `\t`, `\r` trong các trường văn bản (`StudentName`, `ClassStudentName`, `Email`, ...) bị coi là không hợp lệ hoặc phải bị từ chối ở bước validate (`NFR-SEC-009`). | `ROW_FORMULA_INJECTION_DETECTED` |

### 3.2. Hai Profile Cột Hỗ trợ (Tương thích Dữ liệu Gốc QNU & Template Chuẩn)

Bộ parser của `QE-26` phải hỗ trợ đồng thời **2 profile cột** thông qua cơ chế ánh xạ tên cột (Header Alias Mapping):
- **Profile A (QNU Raw Export — 45 cột)**: Đọc trực tiếp các tệp xuất từ hệ thống đào tạo QNU (`student/sv_k46.csv` → `sv_k49.csv`) mà không bắt Admin phải xóa cột thừa.
- **Profile B (Standard Template — 12 cột chuẩn)**: Mẫu rút gọn dùng để tải xuống từ giao diện Import (`GET /api/v1/admin/imports/students/template`).

#### Bảng đặc tả cột chuẩn (Áp dụng cho cả Profile A & Profile B)

| # | Tên cột chuẩn (Header) | Alias chấp nhận | Bắt buộc? | Kiểu & Độ dài | Định dạng / Quy tắc kiểm tra (Validation Rule) | Ví dụ hợp lệ |
|---:|---|---|:---:|---|---|---|
| 1 | `StudentID` | `student_code`, `mssv` | **Bắt buộc** | Text (10) | Chuỗi đúng **10 chữ số** (`^\d{10}$`). Xem chi tiết cấu trúc MSSV tại Mục 3.3. | `4651050001` |
| 2 | `StudentName` | `full_name`, `ho_ten` | **Bắt buộc*** | Text (2–150) | Tiếng Việt có dấu (Unicode NFC), không chứa chữ số hoặc ký tự điều khiển. *Nếu trống nhưng có cả `LastName` và `FirstName` thì tự ghép `LastName.trim() + " " + FirstName.trim()`. | `Võ Hoài An` |
| 3 | `ClassId` | `class_id` | **Bắt buộc theo ĐK** (1 trong 2: `ClassId` hoặc `ClassStudentName`) | UUID (36) | Định dạng UUID chuẩn (`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`). Phải tồn tại trong bảng `org_class`. | `915d2d89-8562-500a-a0ab-7a4d0bc27601` |
| 4 | `ClassStudentName` | `class_name`, `class_code`, `ten_lop`, `ma_lop` | **Bắt buộc theo ĐK** (nếu không có `ClassId`) | Text (2–150) | Tên lớp hoặc mã lớp sinh hoạt. Phải khớp (sau chuẩn hóa khoảng trắng/NFC) với `org_class.name` hoặc `org_class.code`. | `Công nghệ thông tin 46E` |
| 5 | `DepartmentCode` | `department_code`, `ma_khoa` | Tùy chọn (Khuyến nghị) | Text (2–30) | Mã khoa (`101`..`203`). Nếu có cung cấp, phải khớp với khoa của Lớp được tham chiếu. | `105` |
| 6 | `DepartmentName` | `department_name`, `faculty_name`, `ten_khoa` | Tùy chọn | Text (2–150) | Tên khoa (chấp nhận có hoặc không có tiền tố `[105] `). Nếu có, phải khớp với `org_class.faculty_name`. | `Khoa Công nghệ thông tin` |
| 7 | `SpecializationCode` | `specialization_code`, `ma_nganh` | Tùy chọn (Khuyến nghị) | Text (3–10) | Mã ngành (`^\d{3,4}$`, ví dụ `105`, `119`, `1051`). Nếu có, kiểm tra đối chiếu với ngành của Lớp và 3 chữ số ngành trong MSSV. | `105` |
| 8 | `IntakeNo` | `intake_no`, `cohort`, `khoa_hoc` | Tùy chọn | Integer (2) | Số hiệu khóa học (`40`..`60`). Nếu có, phải khớp với 2 chữ số đầu của `StudentID` và khóa của Lớp. | `46` |
| 9 | `Email` | `email` | Tùy chọn | Text (≤ 150) | Nếu khác rỗng: phải đúng định dạng email RFC 5322. **Lưu ý dữ liệu thực tế QNU**: Giá trị `"0"` hoặc `"N/A"` hoặc `"-"` trong dữ liệu nguồn phải bị từ chối với mã `ROW_INVALID_EMAIL` (hoặc cảnh báo nếu chạy chế độ sanitize). | `anhoaivo101@gmail.com` |
| 10 | `BirthDay` | `birth_day`, `birth_date`, `ngay_sinh` | Tùy chọn | Date (10) | Định dạng `dd/MM/yyyy` (chuẩn xuất của QNU, ví dụ `01/03/2005`) hoặc ISO `yyyy-MM-dd` (`2005-03-01`). Năm sinh hợp lệ trong khoảng `1970`..`2015`. | `01/03/2005` |
| 11 | `Genders` | `gender_text`, `gioi_tinh` | Tùy chọn | Enum Text | Chấp nhận `Nam`, `Nữ`, `Khác` (hoặc cột boolean `Gender`: `true` = Nam, `false` = Nữ). | `Nữ` |
| 12 | `StudyStatusID` | `study_status_id`, `status` | Tùy chọn | Enum / Int | Chấp nhận mã số QNU (`1`=Còn học, `2`=Nghỉ học tạm thời, `3`=Buộc thôi học, `4`=Thôi học, `7`=Chuyển trường) hoặc trạng thái hệ thống (`ACTIVE`, `LOCKED`). Mặc định `1` (`ACTIVE`). | `1` |

> **Các cột phụ trong Profile A (45 cột từ `student/sv_k46..k49.csv`) được phép có mặt và không gây lỗi cấu trúc**:
> `LastName`, `FirstName`, `BirthPlace`, `Gender`, `EthnicName`, `ReligionName`, `PermanentResidence`, `StudentNameWithID`, `IDCard` (`^\d{9}|\d{12}$` bảo toàn số `0` đầu), `GraduateLevelName`, `GraduateLevelID`, `StudyTypeID`, `StudentTypeName`, `StudyStatusName`, `CourseID`, `CourseName`, `CourseTime`, `DepartmentId`, `SpecializationId`, `SpecializationName`, `IntakeId`, `IntakeProgramId`, `IntakeProgramCode`, `ProgramId`, `ProgramLevelCode`, `TrainingModeCode`, `InferredSpecializationCode`, `IntakeMatchMethod`, `SpecializationMatchMethod`, `NeedsReview`, `DataIssues`, `SourceFile`, `SourceFiles`.

### 3.3. Quy định Định dạng Mã Sinh viên (MSSV — `StudentID`)

Dựa trên phân tích toàn bộ **6.435 sinh viên** từ K46 đến K49 của Trường ĐH Quy Nhơn, mã sinh viên (`StudentID` / `student_code`) tuân theo cấu trúc cố định **10 chữ số**:

$$\underbrace{\text{KK}}_{\text{Khóa (2 số)}}\ \underbrace{\text{B}}_{\text{Hệ/Bậc (1 số)}}\ \underbrace{\text{MMM}}_{\text{Mã ngành gốc (3 số)}}\ \underbrace{\text{SSSS}}_{\text{Số thứ tự (4 số)}}$$

- **Biểu thức chính quy (Regex) bắt buộc**: `^[0-9]{10}$` (hoặc chặt hơn cho hệ ĐH chính quy: `^(4[5-9]|5[0-5])[0-9][0-9]{3}[0-9]{4}$`).
- **Giải nghĩa từng phần**:
  - `KK` (Vị trí 1–2): Số hiệu khóa học (`IntakeNo`), ví dụ `46` (K46 - nhập học 2023), `47` (K47 - 2024), `48` (K48 - 2025), `49` (K49 - 2026). Công thức quy đổi năm nhập học (`cohort_year`): `cohort_year = 1977 + KK`.
  - `B` (Vị trí 3): Mã bậc/hệ đào tạo (trong toàn bộ 6.435 SV chính quy K46–K49, chữ số thứ 3 luôn là `5`).
  - `MMM` (Vị trí 4–6): Mã ngành đào tạo 3 chữ số (`InferredSpecializationCode`), ví dụ `101` (Sư phạm Toán học), `105` (Công nghệ thông tin), `119` (Kỹ thuật phần mềm), `410` (Trí tuệ nhân tạo), `752` (Ngôn ngữ Trung Quốc).
  - `SSSS` (Vị trí 7–10): Số thứ tự sinh viên trong khóa-ngành (`0001` đến `9999`, luôn giữ các chữ số `0` ở đầu).
- **Quy tắc xử lý chuỗi**:
  - Tự động `trim()` khoảng trắng hai đầu trước khi kiểm tra regex.
  - Tuyệt đối không ép kiểu sang số nguyên 32-bit hay số thực dấu phẩy động (`4.65105E+09` từ Excel làm hỏng dữ liệu sẽ bị từ chối với lỗi `ROW_INVALID_STUDENT_ID_FORMAT`).

---
