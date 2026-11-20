# Hợp đồng Import CSV/Excel: Thực thể Lớp Sinh hoạt (`OrgClass` / `org_class`) — UC026

> **Mã tài liệu:** `CONTRACT-IMP-CLASS-v1.0`
> **Liên kết Jira:** `QE-45`, `QE-26`, `QE-62`
> **Liên kết SRS:** `UC026` (UC6.2 — Nhập dữ liệu khoa ngành và lớp), `REQ-DATA-001` → `REQ-DATA-003`, `REQ-DATA-007` → `REQ-DATA-010`, `TBD-DATA-01`
> **Liên kết Codebase:** Entity [`OrgClass.java`](../../src/main/java/vn/edu/qnu/events/student/OrgClass.java), Repository [`OrgClassRepository.java`](../../src/main/java/vn/edu/qnu/events/student/OrgClassRepository.java), Bảng `org_class` trong [`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql)
> **Tệp dữ liệu nguồn QNU:** `lop.csv` (367 lớp sinh hoạt chính quy từ K46 đến K49)

---

## 1. Mục tiêu & Ánh xạ Trực tiếp vào Bảng `org_class` (Schema V1)

Thực thể **Lớp sinh hoạt (`OrgClass`)** là hạt nhân phân quyền dữ liệu của hệ thống **QNU-Events**:
- Quyết định sinh viên nào thuộc đối tượng tham gia sự kiện (`event_audience(event_id, class_id)` — `QE-65`, `QE-66`, `QE-71`).
- Quyết định phạm vi duyệt minh chứng của Ban cán sự lớp (`CLASS_REPRESENTATIVE` — `QE-43`).

### Bảng Ánh xạ Cột CSV (`lop.csv`) sang Entity `OrgClass` (`V1__sprint1_core_schema.sql`)

| Cột trong DB `org_class` | Thuộc tính `OrgClass.java` | Kiểu dữ liệu & Ràng buộc V1 | Quy tắc Ánh xạ từ `lop.csv` / Template Chuẩn |
|---|---|---|---|
| `id` | `id` | `uuid PK` | Lấy trực tiếp từ cột `ClassId` (UUID v5 tất định trong `lop.csv`). Nếu trống, sinh UUID v5 từ `"qnu:class:" + code`. |
| `code` | `code` | `varchar(30) NOT NULL UNIQUE` | **Lưu ý quan trọng về độ dài**: Trong `lop.csv`, cột `ClassStudentName` dài tới **40 ký tự** (ví dụ `Logistics và quản lý chuỗi cung ứng K49A`), vượt quá `varchar(30)` của `org_class.code`. Do đó:<br>• Nếu CSV có cột `ClassCode` (≤ 30 ký tự, ví dụ `K46CNTT_E` hoặc `K49CNTT1`): dùng `ClassCode`.<br>• Nếu import trực tiếp `lop.csv` (không có cột `ClassCode`): hệ thống tự sinh `code` tất định theo công thức chuẩn: **`K<IntakeNo>_<SpecOrInferredCode>[_<ClassSection>]`** (ví dụ: `K46_105_E` cho lớp *Công nghệ thông tin 46E*, `K49_752_A` cho *Ngôn ngữ Trung Quốc K49A* — luôn ≤ 15 ký tự và duy nhất 100% trên cả 367 lớp!). |
| `name` | `name` | `varchar(150) NOT NULL` | Lấy từ cột `ClassStudentName` (ví dụ: `Công nghệ thông tin 46E`, `Sư phạm Toán học K49B`). Chuẩn hóa Unicode NFC. |
| `faculty_name` | `facultyName` | `varchar(150) NOT NULL` | Phân giải từ `DepartmentCode` / `DepartmentId` sang tên Khoa chuẩn đã bỏ tiền tố `[mã]` (ví dụ `105` → `Khoa Công nghệ thông tin`), hoặc lấy từ cột `FacultyName` / `DepartmentName` nếu được truyền trực tiếp. |
| `cohort_year` | `cohortYear` | `smallint` | Nếu có cột `CohortYear`: lấy trực tiếp. Nếu có `IntakeNo` (`46..49`): tính **`cohort_year = 1977 + IntakeNo`** (`46 → 2023`, `47 → 2024`, `48 → 2025`, `49 → 2026`). |
| `created_at` | `createdAt` | `timestamptz NOT NULL` | Thời điểm tạo bản ghi trên server (`Instant.now()`). |

---

## 2. Cấu trúc Cột CSV/Excel (`lop.csv` & Template Chuẩn)

Hệ thống hỗ trợ đọc trực tiếp **18 cột của `lop.csv`** hoặc **Mẫu chuẩn 8 cột**:

| # | Tên cột chuẩn (Header) | Alias chấp nhận | Bắt buộc? | Kiểu & Độ dài | Định dạng / Quy tắc kiểm tra | Ví dụ (`lop.csv`) |
|---:|---|---|:---:|---|---|---|
| 1 | `ClassStudentName` | `name`, `class_name`, `ten_lop` | **Bắt buộc** | Text (2–150) | Tên lớp đầy đủ bằng tiếng Việt có dấu (Unicode NFC). Duy nhất toàn hệ thống. | `Công nghệ thông tin 46E` |
| 2 | `ClassCode` | `code`, `class_code`, `ma_lop` | Tùy chọn* | Text (2–30) | Mã lớp ngắn gọn (`^[A-Za-z0-9_-]{2,30}$`). *Nếu không truyền (như trong `lop.csv` gốc), hệ thống tự sinh `K<IntakeNo>_<SpecializationCode>[_<ClassSection>]`. | `K46_105_E` |
| 3 | `ClassId` | `id`, `class_id` | Tùy chọn | UUID (36) | UUID định danh lớp. Khuyến nghị giữ nguyên từ `lop.csv` để đồng bộ với cột `ClassId` trong `student/sv_k46..k49.csv`. | `915d2d89-8562-500a-a0ab-7a4d0bc27601` |
| 4 | `DepartmentCode` | `department_code`, `ma_khoa` | **Bắt buộc theo ĐK** (1 trong 2: `DepartmentCode` hoặc `FacultyName`) | Text (2–30) | Mã khoa quản lý lớp (`101`..`203`). Phải tồn tại trong danh mục Khoa. | `105` |
| 5 | `FacultyName` | `DepartmentName`, `faculty_name`, `ten_khoa` | **Bắt buộc theo ĐK** (nếu không có `DepartmentCode`) | Text (2–150) | Tên khoa (ví dụ `Khoa Công nghệ thông tin`). | `Khoa Công nghệ thông tin` |
| 6 | `SpecializationCode` | `specialization_code`, `InferredSpecializationCode`, `ma_nganh` | **Bắt buộc** | Text (3–10) | Mã ngành 3–4 chữ số (`^\d{3,4}$`). Phải tồn tại trong danh mục Ngành và thuộc đúng Khoa ở cột 4/5 (`REQ-DATA-008`). | `105` |
| 7 | `IntakeNo` | `intake_no`, `so_khoa` | **Bắt buộc** | SmallInt | Số khóa (`40`..`60`). Dùng để tính `cohort_year = 1977 + IntakeNo` và kiểm tra hậu tố tên lớp (`46`, `K48`, `K49`). | `46` |
| 8 | `ClassSection` | `class_section`, `phan_ban` | Tùy chọn | Text (≤ 10) | Hậu tố phân lớp (`A`, `B`, `C`, `D`, `E` hoặc để trống nếu khóa-ngành chỉ có 1 lớp). | `E` |

> **Các cột phụ trong `lop.csv` (18 cột) được phép giữ nguyên**:
> `SpecializationId`, `SpecializationName`, `DepartmentId`, `GraduateLevelCode`, `TrainingModeCode`, `CourseCode`, `ObservedStudentCount`, `InferredSpecializationCode`, `MatchMethod`, `NeedsReview`, `SourceFiles`, `Notes`.

---

## 3. Quy tắc Xử lý Các Trường hợp Đặc biệt Thực tế từ `lop.csv`

Qua phân tích 367 dòng thực tế của `lop.csv`, có **3 tình huống thực tế** được định nghĩa rõ trong contract:

1. **Khác biệt dấu tiếng Việt trong tên lớp (`unaccented_name_department_student_code` — 2 lớp)**:
   - Trong `lop.csv`, hai lớp `Kỹ thuật cơ khi động lực K48` và `Kỹ thuật cơ khi động lực K49` bị gõ thiếu dấu sắc ở chữ `"khi"` so với tên ngành `"Kỹ thuật cơ khí động lực"` (`SpecializationCode = 121`).
   - **Quy tắc xử lý**: Khi đã có `SpecializationCode = 121` hợp lệ, hệ thống chấp nhận dòng lớp đó và phát sinh cảnh báo `WARN_CLASS_NAME_SPELLING_DIFF` (không chặn import để không làm mất `ClassId` mà các dòng sinh viên đang tham chiếu).
2. **Ba lớp K49 có `NeedsReview = true` do ngành mới (`412`, `502`, `503`)**:
   - `Kinh tế số K49` (`InferredSpecializationCode=412`, `SpecializationCode=""`)
   - `Kế toán K49 (Định hướng ACCA)` (`InferredSpecializationCode=502`, `SpecializationCode=""`)
   - `Tài chính - Ngân hàng K49 (Tiếng Anh)` (`InferredSpecializationCode=503`, `SpecializationCode=""`)
   - **Quy tắc xử lý**:
     - Nếu `SpecializationCode` trống nhưng `InferredSpecializationCode` có giá trị VÀ `NeedsReview != true`: dùng `InferredSpecializationCode`.
     - Nếu cả `SpecializationCode` đều chưa được xác nhận trong danh mục `nganh.csv` (hoặc `SpecializationCode` rỗng và ngành chưa có trong DB): hệ thống **từ chối dòng lỗi** với mã **`ROW_UNRESOLVED_SPECIALIZATION`** (`REQ-DATA-008`: *"lớp không có ngành hợp lệ: bỏ dòng lỗi, ghi trường tham chiếu"*).
     - Sau khi System Admin import bổ sung 3 ngành `412`, `502`, `503` (như trong [`specialization-valid-sample.csv`](./samples/specialization-valid-sample.csv)) và điền `SpecializationCode`, 3 lớp này sẽ được import thành công.
3. **Bảo vệ quan hệ khi Lớp đã gắn Sự kiện hoặc Sinh viên (`REQ-DATA-010`)**:
   - Nếu lớp đã tồn tại trong `org_class` và đang được tham chiếu bởi `event_audience` hoặc `app_user`:
     - Không cho phép đổi `DepartmentCode` / `faculty_name` hoặc `SpecializationCode` của lớp nếu `allowHierarchyChange=false` → Báo lỗi `ROW_CLASS_HIERARCHY_LOCKED`.

---

## 4. Bảng Mã Lỗi Cấp Dòng cho Thực thể Lớp (`OrgClass`)

| Mã lỗi (`errorCode`) | Trường (`field`) | Điều kiện kích hoạt |
|---|---|---|
| `ROW_MISSING_REQUIRED_FIELD` | `ClassStudentName`, `IntakeNo`, `DepartmentCode` | Thiếu tên lớp, khóa học hoặc khoa quản lý. |
| `ROW_CLASS_CODE_TOO_LONG` | `ClassCode` | `ClassCode` truyền vào dài quá 30 ký tự (`org_class.code varchar(30)`). |
| `ROW_DUPLICATE_CLASS_IN_FILE` | `ClassStudentName` / `ClassCode` | Trùng tên lớp hoặc mã lớp trong cùng tệp import. |
| `ROW_UNRESOLVED_SPECIALIZATION` | `SpecializationCode` | Lớp thiếu mã ngành hoặc mã ngành (`412`, `503`...) chưa có trong danh mục Ngành (`REQ-DATA-008`). |
| `ROW_DEPARTMENT_NOT_FOUND` | `DepartmentCode` | Mã khoa không tồn tại trong danh mục Khoa. |
| `ROW_DEPARTMENT_SPEC_MISMATCH` | `SpecializationCode` | Ngành khai báo của lớp không thuộc Khoa khai báo trên cùng dòng. |
| `ROW_INVALID_INTAKE_NO` | `IntakeNo` | Khóa học không phải số nguyên hợp lệ (`40..60`). |
| `ROW_CLASS_HIERARCHY_LOCKED` | `DepartmentCode` / `SpecializationCode` | Thay đổi Khoa/Ngành của lớp đang có sinh viên hoặc đang nằm trong `event_audience` khi chưa bật `allowHierarchyChange=true` (`REQ-DATA-010`). |
| `ROW_FORMULA_INJECTION_DETECTED` | `ClassStudentName`, `ClassCode` | Chứa ký tự bắt đầu công thức `=`, `+`, `-`, `@`. |

---

## 5. File Mẫu Hợp lệ và File Mẫu Lỗi

- **File mẫu hợp lệ**: [`samples/class-valid-sample.csv`](./samples/class-valid-sample.csv) (10 lớp thực tế từ `lop.csv` gồm các lớp CNTT 46E, K48A, K49A, Sư phạm Toán học 46, K48A, K48B, K49A, K49B, Ngôn ngữ Trung Quốc K49, Logistics K49).
- **File mẫu lỗi**: [`samples/class-invalid-sample.csv`](./samples/class-invalid-sample.csv) (Bao gồm 3 lớp `unresolved` thực tế từ `lop.csv` khi chưa có ngành `412`/`502`/`503`, dòng trùng tên lớp, dòng sai mã khoa, dòng lệch Khoa-Ngành, và `ClassCode` vượt quá 30 ký tự).
