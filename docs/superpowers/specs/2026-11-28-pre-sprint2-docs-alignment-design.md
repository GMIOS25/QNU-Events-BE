# Design Spec: Chuẩn hóa & Đồng bộ Toàn diện Bộ Tài liệu Trước Sprint 2 (Pre-Sprint 2 Documentation Alignment & V2 Baseline)

> **Ngày lập:** 28/11/2026  
> **Phạm vi kho mã nguồn:** `QNU-Events-BE` (`docs/`, `README.md`, `AGENTS.md`, `.github/CODEOWNERS`) & `QNU-Events-FE` (`docs/api/sprint1-contract.md`)  
> **Trạng thái:** Ready for Approval — Đã qua Builder Check & bổ sung đầy đủ 3 quyết định rà soát (`R1`–`R3`)  
> **Quyết định nền tảng đã chốt qua `/grill-me` & Builder Check:** `D-1` → `D-10`, `A-EVT-01..04`, `TBD-EVT-03`, `CODEOWNERS` Sprint 2  
> **Phương án triển khai đã chọn:** **Phương án A** (Đồng bộ toàn bộ Markdown BE + FE, cập nhật trực tiếp 2 file Word `.docx` bảo toàn UML/style, và đóng băng thiết kế nền `V2` cho Sprint 2)

---

## 1. Mục tiêu & Lý do (Intent & Why)

Dự án **QNU-Events** vừa hoàn tất toàn bộ phạm vi Sprint 1 trên nhánh `main` (`26/11/2026`: `151` BE JUnit tests, `43` FE Vitest tests, `15/15` Jira Bugs `Done`) và đang đứng giữa hai cột mốc quan trọng:
1. **Báo cáo giữa kỳ Tuần 9 (`27/11`–`03/12`):** Giảng viên chấm điểm trực tiếp trên **Hồ sơ dự án** (Proposal, SRS, Kiến trúc, ERD, Test Plan/Report, Truy vết RTM, Quy trình Scrum) và vấn đáp đối chiếu với thực tế (Rubric *Tính trung thực* & *Phân tích & Thiết kế*).
2. **Mở đầu Sprint 2 (`QE-12, 19, 24, 26, 32, 39, 40, 42, 43, 55, 58, 59, 62...`):** Agent nền tảng `A0` cần đóng băng **01 file migration `V2__...` duy nhất** (`AGENTS.md` `R6`) làm nền tảng chung cho cả 5 thành viên làm song song.

Cuộc rà soát chéo toàn bộ tài liệu `FE/docs`, `BE/docs`, `Proposal v2`, `SRS v1.0`, `Figma` và mã nguồn `main` phát hiện **8 cụm mâu thuẫn nghiêm trọng** (4 vs 5 vai trò, thẩm quyền Evidence/Appeal/Import, 2 mô hình Quota/Waitlist đối lập giữa `QE-38` và `QE-8`, 2 mô hình Offline QR đối lập giữa `QE-44` và `QE-8`, lệch quy tắc hủy 12h & tái đăng ký, lệch hạn Appeal `5 ngày` vs `48 giờ`, lệch tên mã lỗi/HTTP status, và lệch số liệu test `94`/`145` vs `151` thực tế).

Thiết kế này đóng băng toàn bộ quyết định nghiệp vụ/kiến trúc đã được duyệt (`D-1` → `D-10`) và đặc tả chính xác từng thay đổi cần thực hiện trên toàn bộ hệ thống tài liệu BE, FE và Word `.docx` để đạt **một nguồn sự thật duy nhất (Single Source of Truth)** trước khi bước vào Sprint 2.

---

## 2. Mục tiêu, Phi mục tiêu & Điều cấm (Goals, Non-Goals, Anti-Goals)

### Goals
1. **Hợp nhất mọi quyết định nghiệp vụ & kiến trúc:** Đưa toàn bộ 10 quyết định đã chốt (`D-1` → `D-10`) cùng `A-EVT-01..04` thành trạng thái **`APPROVED`** trong `docs/decisions.md` và đóng toàn bộ các mục `TBD-*` thuộc thẩm quyền nhóm/sản phẩm.
2. **Xóa bỏ 100% mâu thuẫn trên Markdown (BE & FE):** Đồng bộ nhất quán 14+ tài liệu `.md` ở `QNU-Events-BE` và đồng bộ `QNU-Events-FE/docs/api/sprint1-contract.md` khớp `QNU-Events-BE/docs/api/sprint1-contract.md`.
3. **Cập nhật trực tiếp 2 tài liệu Word (`.docx`) bảo toàn định dạng & UML:**
   - `QNU_Event_Participation_Management_Project_Proposal_v2.docx` (bổ sung đủ 5 vai trò có `CVHT`, đồng bộ quy tắc đổi lớp/mục mở §18).
   - `QNU_Event_SRS.docx` (nâng lên `v1.1`, cập nhật `Revision History`, đồng bộ mã lỗi `409`, quy tắc rút `WAITING_LIST`, phân cấp 3 tầng Evidence/Appeal, và cập nhật trạng thái các `TBD-*` trong `Appendix C`).
4. **Đảm bảo tính trung thực số liệu (`R1`):** Cập nhật chính xác số liệu kiểm đếm trực tiếp trên `main` ngày `26/11/2026` (`151` BE JUnit = `40` unit + `111` IT; `43` FE Vitest; `7` Playwright mocked; `359` Bruno `.bru`; `35` Selenium; `4` JMeter; `15/15` Jira Bugs `Done`) vào các báo cáo và `README.md`.
5. **Dọn nợ vệ sinh tài liệu:** Sửa link gãy (`file:///c:/Users/fleya/...`, `docs/QNU_Event_SRS.docx` thiếu `/spec/`), bỏ file trùng `docs/plan/sprint1_rescue_plan.md`, sửa lỗi đánh máy chữ Nga `освоб` trong `quota-rules.md`, và giải quyết trùng mã `A-04..A-07` trong `dynamic-qr-specification.md`.
6. **Đóng băng đầu vào cho Sprint 2:** Khóa **Schema Blueprint `V2`**, **Bảng mã lỗi chuẩn Sprint 1–3**, **Ma trận phân quyền 5 vai trò**, và cập nhật phạm vi sở hữu module Sprint 2 trong `AGENTS.md` & `.github/CODEOWNERS`.

### Non-Goals
- **Không** viết mã nguồn Java/React của các tính năng Sprint 2 và **không** tạo file SQL `V2__*.sql` trong đợt chỉnh sửa tài liệu này (chỉ đóng băng bản thiết kế `V2` cho A0 thực thi ở task đầu Sprint 2).
- **Không** chỉnh sửa `src/main/resources/db/migration/V1__sprint1_core_schema.sql` (`R6`).
- **Không** chỉnh sửa trực tiếp file Figma (chỉ tổng hợp danh sách đầu mục cần cập nhật trên Figma để người phụ trách UI thực hiện).
- **Không** tự ý gọi lệnh ghi lên Jira qua `twg` trong phạm vi đợt này (`R10`).
- **Không** tự điền ngày họp hay điểm danh thành viên vào các ô `[Chờ thành viên xác nhận...]` trong `docs/scrum/sprint1-scrum-ceremonies-and-dod.md` (`R1`).

### Anti-Goals
- **Tuyệt đối không** làm mất hoặc hỏng bất kỳ hình nào trong **54 ảnh UML nhúng** (`word/media/*`), bảng biểu hay style của `QNU_Event_SRS.docx` và `Proposal_v2.docx`.
- **Tuyệt đối không** sửa một nơi quên nơi khác tạo ra mâu thuẫn thứ cấp giữa BE docs, FE docs và Word `.docx`.
- **Tuyệt đối không** push trực tiếp lên `main` hoặc `develop` (`R3` — mọi thay đổi thực hiện trên nhánh `docs/...` riêng cho từng repo).

---

## 3. Ràng buộc Dự án (Constraints)

- **Quy tắc `AGENTS.md`:** `R1` (100% số liệu thật từ mã nguồn/git/Jira), `R3` (nhánh `docs/QE-xx-...` + PR), `R4` (Conventional Commits có `[QE-xx]`), `R5` (kiểm tra build/test trước commit), `R6` (chỉ A0 tạo migration `V2`), `R7` (không commit secret), `R11` (chỉ đánh dấu `APPROVED` cho các mục đã được người dùng chốt trong `D-1`–`D-10` & `A-EVT-01..04`; giữ nguyên trạng thái `OPEN — chờ hạ tầng/nhà trường` cho các mục thực sự phụ thuộc bên ngoài như `TBD-NFR-01..05`, `TBD-SEC-03`, `TBD-SEC-04`, thông tin Google Cloud Console `Q-AUTH-03`).
- **Hai kho độc lập:** `c:\Workspaces\QNU-Events-BE` và `c:\Workspaces\QNU-Events-FE` là 2 repository Git riêng biệt, cần 2 nhánh và 2 PR độc lập.
- **Tương thích ngược Sprint 1:** Không thay đổi hành vi API Sprint 1 đang chạy pass 151 BE tests / 43 FE tests; các thay đổi hành vi mới (như vá thứ tự check `LOCKED` `A-AUTH-03`, thêm Refresh Cookie `HttpOnly`, thêm `CVHT`, `event_pools`) áp dụng từ Sprint 2–3 theo lộ trình rõ ràng.

---

## 4. Phương án Đã Cân nhắc & Phương án Được Chọn (Approaches)

| Phương án | Mô tả | Đánh đổi | Quyết định |
|---|---|---|---|
| **Phương án A** | Đồng bộ toàn bộ Markdown (BE + FE), dọn số liệu/link gãy/file trùng, cập nhật trực tiếp 2 file Word `.docx` (giữ nguyên UML/style), và đóng băng Design Baseline `V2`. | Phạm vi chỉnh sửa rộng nhất (~18 file trên 2 repo), đòi hỏi kỹ thuật chỉnh sửa XML `.docx` cẩn trọng và bước xác minh toàn vẹn media. Đổi lại **100% Hồ sơ dự án nhất quán hoàn toàn** cho Báo cáo giữa kỳ Tuần 9 và mở khóa ngay Sprint 2. | **ĐƯỢC CHỌN** |
| **Phương án B** | Đồng bộ toàn bộ Markdown (BE + FE) và xuất file Phụ lục Đính chính (`spec-errata-v1.1.md`) thay vì sửa trực tiếp `.docx`. | An toàn tuyệt đối với định dạng Word, nhưng nhóm phải tự sửa tay 2 file `.docx` (mất thời gian và dễ sót trước Tuần 9). | Loại |
| **Phương án C** | Chỉ tạo Master Baseline và vá các file chặn trực tiếp Sprint 2 (`FE contract`, `decisions.md`, `quota-rules.md`). | Nhanh nhất để vào code Sprint 2, nhưng để lại mâu thuẫn trong các tài liệu kiểm thử/kiến trúc/Word → rủi ro mất điểm Rubric Tuần 9. | Loại |

---

## 5. Bộ Quyết định Chuẩn hóa Chính thức (`D-1` → `D-10` & `A-EVT-01..04`)

### 5.1 Ma trận 5 Vai trò & Phân quyền Dữ liệu (`D-1`, `D-2`, `D-3`)

| Vai trò (DB `user_role`) | Tên hiển thị (SRS / Figma) | Thẩm quyền chính | Phạm vi Dữ liệu Bắt buộc (Server-Side — R9) | Giới hạn cấm |
|---|---|---|---|---|
| `STUDENT` | Sinh viên | Xem/tìm sự kiện; đăng ký/hủy/waitlist; điểm danh QR/GPS/Evidence/Proxy/Offline; xem lịch sử; gửi Appeal 2 vòng | Chỉ thấy sự kiện `PUBLISHED` thuộc lớp mình (`org_class`); chỉ thao tác trên dữ liệu của chính `userId` | Không truy cập dữ liệu SV khác, sự kiện `DRAFT`/ngoài lớp (`404`) |
| `CLASS_REPRESENTATIVE` | Ban cán sự (BCS) | **Duyệt / từ chối ảnh minh chứng (`Evidence` & `Proxy Evidence`, `UC020`/`QE-43`)**; theo dõi trạng thái điểm danh lớp | Chỉ xem/duyệt sinh viên và ảnh Evidence thuộc **các lớp được phân công** (`bcs_class_assignment`) | Không tạo/sửa sự kiện; không cộng/trừ điểm; **không** xử lý Appeal quyết định của chính mình |
| `EVENT_OWNER` | Admin / Event Owner | Tạo/sửa/preview/publish/hủy sự kiện; cấu hình `event_pools` & GPS; chiếu QR động; xem danh sách lớp (read-only `/api/v1/classes`); **xử lý Phúc khảo Appeal Vòng 2 (quyết định cuối, `UC023–024`) & gỡ chế tài No-show**; xuất báo cáo sự kiện mình quản lý | Chỉ thao tác trên sự kiện thuộc `owner_id` (hoặc `unit_id` được gán); chỉ xem Evidence trong ngữ cảnh sự kiện/khiếu nại thuộc thẩm quyền | **Không** CRUD/Import dữ liệu nền (Khoa/Ngành/Lớp/Học kỳ/SV); không mặc định xem toàn bộ ảnh Evidence ngoài thẩm quyền |
| `CVHT` *(mới ở `V2`)* | Cố vấn học tập (CVHT) | Xem lịch sự kiện tuần/tháng theo lớp (`UC007`); xem Evidence liên quan khi giải quyết khiếu nại; **tiếp nhận & xử lý Khiếu nại Appeal Vòng 1 (`UC022`/`QE-59`)** (Chấp nhận / Từ chối / Yêu cầu bổ sung) | Chỉ xem/xử lý sinh viên, lớp và hồ sơ khiếu nại thuộc **các lớp được phân công cố vấn** (`advisor_class_assignment`) | Không tạo/sửa sự kiện; không quyết định Phúc khảo Vòng 2; không quản trị tài khoản hệ thống |
| `SYSTEM_ADMIN` | Quản trị hệ thống | **Duy nhất có quyền CRUD & Import CSV/Excel Khoa, Ngành, Khóa/CTĐT, Lớp, Học kỳ & Sinh viên (`UC025–026`, `QE-26/62/73`)**; cấp/khóa/mở khóa/thu hồi vai trò (`US1.1`, `QE-35`); phân công lớp cho BCS & CVHT; tra cứu `audit_logs` (`UC028`); cấu hình tham số & vận hành backup/retention (`UC029`) | Toàn quyền kỹ thuật & dữ liệu nền trên toàn hệ thống | **Không phải business reviewer** trong luồng duyệt Evidence/Appeal thường nhật; bị chặn ở endpoint nghiệp vụ của `STUDENT`/`EVENT_OWNER` |

### 5.2 Quy tắc Nghiệp vụ Chuẩn hóa (`D-4` → `D-10` & `A-EVT-01..04`)

1. **Vòng đời Sự kiện & Mốc thời gian (`A-EVT-01..04`, đóng `TBD-EVT-02` & `TBD-EVT-03`):**
   - `A-EVT-01` (**Approved — có ngoại lệ quota theo `quota-rules.md §7`**): Sự kiện đã `PUBLISHED` + đã có registration chỉ được sửa trường an toàn (`title`, `description`) **VÀ được điều chỉnh `quotaTotal` / `event_pools.capacity`** theo đúng `BR-QTA-10` (`quota-rules.md §7`: không được giảm nhỏ hơn `registered_count`/`used`; $\sum \text{capacity} \le \text{quotaTotal}$; khi tăng sức chứa phải tự động kích hoạt auto-promote `WAITING_LIST` theo FIFO trong cùng giao dịch), kèm `@Version`. Cấm đổi thời gian, địa điểm hoặc đối tượng lớp (`409 VERSION_CONFLICT`). Nếu `PUBLISHED` nhưng **chưa có registration nào**, cho sửa toàn bộ nhưng buộc re-preview trước khi có hiệu lực.
   - `A-EVT-02` (**Approved**): "Xóa" sự kiện `DRAFT` chưa có đăng ký có thể xóa cứng sau xác nhận (`REQ-EVT-015`), còn sự kiện đã `PUBLISHED` tuyệt đối không xóa cứng mà chuyển sang `CANCELLED` (giữ nguyên registrations/attendances phục vụ lịch sử, ẩn khỏi SV `404`, không phạt No-show).
   - `A-EVT-03` (**Approved**): Owner được đóng đăng ký sớm (`registration_closed_early = true`) và mở lại nếu chưa quá `registration_deadline` gốc; quá deadline gốc thì không mở lại.
   - `A-EVT-04` (**Approved**): `attendanceOpenAt`/`attendanceCloseAt` không đóng/mở tay tùy tiện sau khi publish; mọi thay đổi đi qua luồng sửa sự kiện có kiểm soát (`A-EVT-01`).
   - **Mốc thời gian & Lịch (`TBD-EVT-03` — Approved):** Giữ ràng buộc `A-03` (`startAt < endAt`, `attendanceOpenAt < attendanceCloseAt`, `registrationDeadline <= attendanceCloseAt`). Với sự kiện bật phương thức `EVIDENCE`, bắt buộc `evidence_deadline >= attendance_close_at` (mặc định `+24 giờ` sau `attendance_close_at`). Lịch sự kiện tuần/tháng (`UC007`) lọc nghiêm ngặt theo phạm vi vai trò: `STUDENT` chỉ thấy sự kiện `PUBLISHED` thuộc lớp mình; `EVENT_OWNER` thấy sự kiện mình quản lý; `CVHT` thấy sự kiện liên quan các lớp phụ trách.

2. **Quota nhiều tầng & Waiting List (`D-4`, đóng `TBD-EVT-01` & `TBD-REG-01`):**
   - Dùng mô hình **`event_pools`**: một sự kiện có `quota_total` là trần tuyệt đối và danh sách các pool (`scope_type ∈ {'LOP','KHOA','KHOI','CLB','ALL'}`, `scope_ref`, `capacity >= 1`, `used`, `priority` nhỏ hơn ưu tiên hơn, `allow_overflow`, `is_mandatory`, `reserved_until`). Ràng buộc: $\sum \text{capacity} \le \text{quota\_total}$, `is_mandatory = true` luôn đi kèm `allow_overflow = false`.
   - Mỗi đăng ký `REGISTERED` thuộc **đúng 01 pool** (`registrations.pool_id`); thuật toán duyệt các pool khớp scope của SV theo `priority ASC`: còn chỗ & không bị `reserved_until` khóa thì `claimSeat` nguyên tử; nếu pool đầy và `allow_overflow = true` thì thử pool kế tiếp; nếu `allow_overflow = false` thì dừng và đưa vào `WAITING_LIST` của đúng pool đó. Nếu claim pool thành công nhưng `events.registered_count == quota_total` thì đưa vào `WAITING_LIST` (không trả `409 CAPACITY_FULL` khi sự kiện bật Waiting List ở Sprint 2).
   - Thứ tự hàng chờ dùng bộ đếm tuần tự **`events.next_wait_order` → `registrations.wait_order`** (loại bỏ hòa timestamp `TBD-REG-01`) kèm `waiting_at`.
   - Khi có suất giải phóng ở pool `X`: hoàn trả `used - 1` về đúng `pool_id = X` và `registered_count - 1`, sau đó promote trong cùng giao dịch bằng **`SELECT ... FROM registrations WHERE event_id = :eid AND pool_id = :pid AND status = 'WAITING_LIST' ORDER BY wait_order ASC FOR UPDATE SKIP LOCKED`**. Tái xác thực điều kiện ứng viên (`ACTIVE`, không bị `NO_SHOW` ban, còn thuộc scope); nếu `#01` không đủ điều kiện thì ghi audit skip và xét tiếp `#02`.

3. **Hủy đăng ký 12 giờ, Tái đăng ký & Auto-promote (`D-5`, đóng `TBD-REG-02`):**
   - Mốc chặn hủy **`server_time >= start_at - 12h`** chỉ áp dụng cho trạng thái **`REGISTERED`** → vi phạm trả **`409 CANCELLATION_CUTOFF_PASSED`**.
   - Sinh viên ở trạng thái **`WAITING_LIST` được miễn mốc 12 giờ** khi tự rút khỏi hàng chờ (không chạm quota, không kích hoạt promote).
   - Sinh viên đã **`CANCELLED` ĐƯỢC PHÉP đăng ký lại** nếu sự kiện vẫn `PUBLISHED`, còn hạn đăng ký và không bị chế tài `NO_SHOW` (khớp `V1` `uq_registrations_active` và `MigrationIT`).
   - **Auto-promote sau `registration_deadline`:** Vẫn tiếp tục auto-promote cho người đã nằm sẵn trong `WAITING_LIST` nếu có suất trống phát sinh trước mốc `start_at - 12h` và sự kiện chưa `CANCELLED`.

4. **Điểm danh Online/Offline QR, GPS & Giới hạn 3 lượt thử (`D-6`, `D-7`, đóng `TBD-ATT-01` & `TBD-ATT-02`):**
   - **Offline QR (`D-6`):** Tuyệt đối **cấm Offline Prefetch** (không tải trước danh sách token tương lai về máy SV). Hỗ trợ **Offline Store-and-Forward**: Owner vẫn chiếu QR động 10s bình thường; SV mất sóng quét mã đang chiếu và lưu cục bộ `PENDING_SYNC` (IndexedDB) kèm `idempotencyKey` (UUID v4); khi có mạng đồng bộ trong cửa sổ **`OFFLINE_SYNC_MAX_WINDOW = 24 giờ` sau `attendance_close_at`** (và trước khi job `NO_SHOW` chạy). Server xác thực chữ ký HMAC-SHA256 + `epoch` đã ký nằm trong `[attendance_open_at, attendance_close_at]` (không tin đồng hồ client), kiểm tra SV đã `REGISTERED` trước đó, hỗ trợ cờ `events.allow_offline_sync` để Owner bật/tắt theo sự kiện, và gắn cờ hậu kiểm `is_offline_late_sync = true` nếu nhận sau `attendance_close_at`.
   - **GPS (`D-7`):** Cấu hình tâm tọa độ và bán kính theo từng sự kiện (`allowed_radius_meters ∈ [10, 500]`), giá trị mặc định **`50m`** (khớp `SRS REQ-EVT-009`; `60m` trên Figma là giá trị cấu hình ví dụ cho một sự kiện cụ thể). Tính Haversine trên server; ngoài bán kính trả **`422 GPS_OUT_OF_RANGE`** (`counted = true`); trong bán kính nhưng tín hiệu đáng ngờ (`isMocked` hoặc `accuracy` bất thường) vẫn ghi `ACCEPTED` kèm `suspicious_flag = true`.
   - **Phạm vi 3 lượt thử (`TBD-ATT-02`):** Giới hạn 3 lượt (`counted = true`) dùng chung cho nhóm xác thực tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`) trên mỗi `(event_id, student_id)`. **`EVIDENCE` (chờ BCS duyệt) là kênh dự phòng riêng**, không bị chặn bởi 3 lượt lỗi tự động. Lần thử tự động thứ 4 trả **`409 ATTEMPT_LIMIT_REACHED`** (`counted = false`, giữ `attemptsUsed <= 3`).
   - **Tương tác `PENDING` Evidence ↔ QR/GPS (`D-7`):** Nếu SV đã nộp `EVIDENCE` (`PENDING`) rồi quét thành công QR/GPS trong giờ, hệ thống **chấp nhận `ACCEPTED`** và tự động chuyển các bản ghi `PENDING` thừa sang `REJECTED` (`reason = SUPERSEDED_BY_ACCEPTED`).
   - **Idempotency & Concurrency:** Giữ `pg_advisory_xact_lock` theo `(event_id, student_id)` + `V2` bổ sung `attendance_attempts.payload_hash` (`SHA-256`) trả `400 VALIDATION_FAILED` (`IDEMPOTENCY_PAYLOAD_MISMATCH`) nếu tái sử dụng key với payload/method khác.

5. **Lưu trữ Minh chứng & Retention (`D-8`, đóng `TBD-SEC-02` & `TBD-DATA-03`):**
   - Dùng `EvidenceStoragePort` với adapter mặc định **Local Private Filesystem** (lưu ngoài web public root, tên file UUID ngẫu nhiên, kiểm tra magic bytes JPEG/PNG & dung lượng `< 5 MB` = `5 * 1024 * 1024` bytes, phục vụ xem ảnh qua endpoint BE có kiểm tra RBAC + scope lớp với `Cache-Control: private, no-store`), giữ cổng sẵn sàng cắm **MinIO/S3** ở môi trường staging/production.
   - **Retention = `150 ngày` (5 tháng)** tính từ `events.end_at`: loại trừ tuyệt đối hồ sơ đang có Appeal chưa đóng; bắt buộc sao lưu sang kho tách biệt và **xác minh đọc lại thành công trước khi xóa bản online** (`NFR-SAFE-005`); giữ nguyên kết quả điểm danh & `audit_logs`.

6. **Khiếu nại (Appeal) & Chế tài No-show (`D-9`, đóng `TBD-REV-01..03` & `TBD-REG-03`):**
   - **Thời hạn Appeal (`A-REV-01`):** Vòng 1 tối đa **01 đơn trong vòng 5 ngày lịch** từ `T0`; Vòng 2 (Phúc khảo) tối đa **01 đơn trong vòng 3 ngày lịch** từ thời điểm có kết quả Vòng 1; trần tổng thời gian xử lý tuyệt đối **30 ngày lịch từ `T0`**. Hạn tính theo giờ máy chủ **GMT+7**, kết thúc `23:59:59` ngày cuối. SLA xử lý chỉ cảnh báo nội bộ, **không** auto-approve/auto-reject.
   - **Mốc `T0` chính thức:**
     - Khiếu nại Evidence bị BCS từ chối: `T0 = reviewed_at` (thời điểm BCS ra quyết định `REJECTED`).
     - Khiếu nại lỗi QR/GPS hoặc khiếu nại No-show: `T0 = attendance_close_at` (hoặc thời điểm ghi nhận chốt `NO_SHOW`).
   - **Job `NO_SHOW` (7 ngày):** Chỉ chạy sau **`MAX(attendance_close_at + 24 giờ, COALESCE(evidence_deadline, attendance_close_at))`** (đảm bảo đã qua cả cửa sổ offline 24h lẫn hạn nộp Evidence nếu có); **miễn phạt** nếu SV đang có `EVIDENCE` trạng thái `PENDING` (chỉ xét lại sau quyết định cuối `REJECTED`) hoặc đang có Appeal Vòng 1 hợp lệ (tạm dừng bộ đếm phạt đến khi có kết quả); bỏ qua `WAITING_LIST`, `CANCELLED` và sự kiện `CANCELLED`. Khi nhiều án phạt No-show chồng nhau: **`banned_until = MAX(current_banned_until, now + 7 days)`**.

7. **Xác thực, Bảo mật Phiên & Import CSV (`D-10`, đóng `TBD-AUTH-01..02`, chốt nguyên tắc `TBD-AUTH-03` & `TBD-DATA-01`):**
   - **Chống User Enumeration (`A-AUTH-03`):** Ở `QE-30` (Sprint 2–3), luôn chạy `BCrypt` match trước kiểm tra `UserStatus.LOCKED` (sai pass trên tài khoản khóa trả `401 INVALID_CREDENTIALS`, đúng pass mới trả `403 FORBIDDEN`) kèm cập nhật `AuthIT` & Bruno test.
   - **Cơ chế phiên & `"Lưu đăng nhập"` (`A-AUTH-07`):** Ở `QE-30`, Access Token ngắn hạn (mặc định 8h khi không tích hoặc ngắn hơn 15–60p khi bật refresh) + khi tích `"Lưu đăng nhập"` cấp **Refresh Token trong cookie `HttpOnly; Secure; SameSite=Lax` (TTL 7 ngày)** lưu hash trong DB, kèm bảo vệ CSRF cho endpoint refresh và giới hạn tối đa **5 phiên active đồng thời/user** (thu hồi FIFO phiên cũ nhất, `A-AUTH-04`). Thu hồi toàn bộ phiên tức thì khi `LOCK`, `GRANT_ROLE`/`REVOKE_ROLE`, đổi lớp/đơn vị, hoặc đổi/reset mật khẩu (`A-AUTH-02`).
   - **Brute-force (`REQ-AUTH-015`):** Sai mật khẩu **5 lần liên tiếp → khóa tạm 15 phút**, lưu bộ đếm tập trung trên PostgreSQL + rate-limit theo IP (`A-AUTH-06`).
   - **Google OAuth2 (`QE-74`, `A-AUTH-05`):** Authorization Code + PKCE (hoặc verify `id_token` qua Google JWKS tại BE), bắt buộc `email_verified = true`, kiểm tra khớp chính xác domain từ biến môi trường `APP_GOOGLE_ALLOWED_DOMAINS` (mặc định cấu hình cho QNU: `st.qnu.edu.vn,qnu.edu.vn`, cấm `endsWith`), **chỉ liên kết `app_user` đã import/cấp trước** (không tự tạo tài khoản), bind nguyên tử `google_sub`, **mở cho mọi vai trò đã được cấp tài khoản có email QNU hợp lệ**. *(Lưu ý: chỉ giữ mở phần thông tin vận hành Google Cloud Project/Client ID thực tế `Q-AUTH-03` khi triển khai staging/prod.)*
   - **Cấp tài khoản từ CSV (`QE-73`, `A-AUTH-08`):** Sinh mật khẩu tạm ngẫu nhiên mạnh một lần hoặc activation token một lần + cờ `must_change_password = true`; không dùng mật khẩu mặc định chung, không ghi credential vào log/report/audit (`R7`); re-import tuân thủ *No Silent Overwrite*.
   - **Đổi lớp khi re-import CSV (`Proposal §11.2` vs `student-import-contract.md`):** Mặc định **`allowClassTransfer = false`** (từ chối đổi lớp ngầm để khớp nguyên tắc dữ liệu lớp ổn định của MVP); chỉ cho phép `SYSTEM_ADMIN` chủ động bật cờ `allowClassTransfer = true` khi cần đính chính dữ liệu nhập sai ban đầu, bắt buộc ghi `audit_logs` (`STUDENT_CLASS_TRANSFER`).

---

### 5.3 Bảng Mã Lỗi Chuẩn Thống Nhất Toàn Dự Án (Canonical Error Code Catalog)

Mọi tài liệu contract, kiến trúc, kiểm thử và SRS phải sử dụng **đúng một bộ mã lỗi duy nhất** sau (chấm dứt các biến thể `400`, `429`, `CANCELLATION_DEADLINE_PASSED`, `OUT_OF_GPS_RADIUS`, `MISSING_HEADER`):

| HTTP | Mã lỗi chuẩn (`ErrorCode`) | Giai đoạn | `counted` (3-attempt) | Thay thế mã cũ bị lệch |
|---:|---|---|:---:|---|
| `400` | `VALIDATION_FAILED` | Sprint 1+ | `false` | Thay `MISSING_HEADER` trong `QE-44`; dùng kèm `details` cho `IDEMPOTENCY_PAYLOAD_MISMATCH` |
| `401` | `UNAUTHENTICATED` / `INVALID_CREDENTIALS` / `SESSION_REVOKED` / `SESSION_EXPIRED` *(S2+)* | Sprint 1+ | `false` | — |
| `403` | `FORBIDDEN` / `NOT_IN_AUDIENCE` / `NOT_REGISTERED` | Sprint 1+ | `false` | Thay `400` sai đối tượng trong `SRS UC010` |
| `403` | `STUDENT_PENALIZED` *(No-show 7 ngày)* | Sprint 3 | `false` | — |
| `403` | `OAUTH_DOMAIN_NOT_ALLOWED` / `OAUTH_ACCOUNT_NOT_PROVISIONED` / `OAUTH_IDENTITY_MISMATCH` | Sprint 3 | `false` | — |
| `404` | `NOT_FOUND` | Sprint 1+ | `false` | — |
| `409` | `EVENT_NOT_PUBLISHED` / `REGISTRATION_CLOSED` / `CAPACITY_FULL` / `PREVIEW_REQUIRED` / `PREVIEW_OUTDATED` / `VERSION_CONFLICT` / `ATTENDANCE_NOT_OPEN` / `ALREADY_CHECKED_IN` | Sprint 1+ | `false` | — |
| `409` | `ATTEMPT_LIMIT_REACHED` | Sprint 1+ | `false` | Thay `429 Too Many Requests` trong `SRS UC015` |
| `409` | `CANCELLATION_CUTOFF_PASSED` | Sprint 2 | `false` | Thay `400` (`SRS UC011`) và `CANCELLATION_DEADLINE_PASSED` (`QE-25`) |
| `422` | `QR_MALFORMED` / `QR_BAD_SIGNATURE` / `QR_WRONG_EVENT` / `QR_EXPIRED` / `QR_FROM_FUTURE` | Sprint 1+ | `true` | — |
| `422` | `GPS_OUT_OF_RANGE` | Sprint 2 | `true` | Thay `OUT_OF_GPS_RADIUS` trong `test-matrix-attendance-br-att.md` |
| `422` | `INVALID_EVIDENCE_FILE` | Sprint 2 | `false` | Thay `INVALID_EVIDENCE` trong `test-matrix-attendance-br-att.md` §4 |
| `422` | `OFFLINE_SYNC_WINDOW_EXPIRED` | Sprint 3 | `true` | — |
| `500` | `INTERNAL_ERROR` | Sprint 1+ | `false` | — |

---

### 5.4 Blueprint Schema `V2` Thống nhất (Đầu vào đóng băng cho `A0` — `R6`)

Toàn bộ yêu cầu lược đồ Sprint 2–3 được `A0` tổng hợp vào **01 migration `V2__sprint2_core_extensions.sql` duy nhất** (thay thế các file `V2__event_pools.sql` rời rạc trên các PR draft `#10/#11/#12`):

1. **Auth & Vai trò (`D-1`, `D-10`):**
   - `user_role`: cập nhật `CHECK (role IN ('STUDENT','CLASS_REPRESENTATIVE','EVENT_OWNER','CVHT','SYSTEM_ADMIN'))`.
   - `app_user`: thêm `email varchar(255)` + `UNIQUE INDEX uq_app_user_email_lower ON app_user (LOWER(email)) WHERE email IS NOT NULL`; `google_sub varchar(128) UNIQUE`; `must_change_password boolean NOT NULL DEFAULT false`; `failed_login_attempts smallint NOT NULL DEFAULT 0`; `locked_until timestamptz`; `auth_version int NOT NULL DEFAULT 0`; `unit_id uuid`.
   - `auth_session`: thêm `refresh_token_hash varchar(64)` + `remember_me boolean NOT NULL DEFAULT false` (phục vụ `HttpOnly` Refresh Cookie ở `QE-30`).
2. **Tổ chức, Phân công Lớp & Hồ sơ SV (`D-1`, `D-3`, `D-10`):**
   - Tạo `org_department`, `org_specialization`, `intake_program` (khớp `docs/import-contract/`); bổ sung `department_id`, `specialization_id` trên `org_class`.
   - Tạo `student_profile` (`user_id PK/FK`, `personal_email` không unique, `birth_date`, `gender`, `id_card`, `study_status_code`, `intake_no`).
   - Tạo `bcs_class_assignment(user_id, class_id, assigned_by, assigned_at, revoked_at)` và `advisor_class_assignment(user_id, class_id, assigned_by, assigned_at, revoked_at)` (phân công lớp cho BCS & CVHT).
3. **Sự kiện, Quota Pools & Waiting List (`D-4`, `D-5`, `D-6`, `D-7`, `A-EVT-03`):**
   - `events`: thêm `next_wait_order bigint NOT NULL DEFAULT 0`, `registration_closed_early boolean NOT NULL DEFAULT false`, `allow_offline_sync boolean NOT NULL DEFAULT true`, `gps_lat double precision`, `gps_lng double precision`, `allowed_radius_meters int DEFAULT 50 CHECK (allowed_radius_meters BETWEEN 10 AND 500)`, `evidence_deadline timestamptz`, `cancel_reason varchar(500)`, `CONSTRAINT ck_events_evidence_deadline CHECK (evidence_deadline IS NULL OR evidence_deadline >= attendance_close_at)`.
   - Tạo `event_pools` (`id`, `event_id FK`, `scope_type IN ('LOP','KHOA','KHOI','CLB','ALL')`, `scope_ref`, `capacity >= 1`, `used >= 0 AND used <= capacity`, `priority`, `allow_overflow`, `is_mandatory`, `reserved_until`, `created_at`, `UNIQUE (event_id, scope_type, scope_ref)`, `CHECK (NOT is_mandatory OR NOT allow_overflow)`).
   - `registrations`: thêm `pool_id uuid REFERENCES event_pools(id)`, `waiting_at timestamptz`, `wait_order bigint` + index `ix_registrations_waitlist ON registrations(event_id, pool_id, wait_order ASC) WHERE status = 'WAITING_LIST'`.
4. **Điểm danh, GPS, Evidence, Appeal, No-show & Outbox (`D-2`, `D-6`, `D-7`, `D-8`, `D-9`):**
   - `attendance_attempts`: thêm `payload_hash varchar(64)`.
   - `attendances`: thêm `gps_lat double precision`, `gps_lng double precision`, `gps_accuracy double precision`, `suspicious_flag boolean NOT NULL DEFAULT false`, `suspicious_reason varchar(200)`, `is_offline_late_sync boolean NOT NULL DEFAULT false`.
   - Tạo `attendance_evidences` (`id`, `attendance_id FK`, `event_id FK`, `student_id FK`, `submitted_by FK`, `is_proxy boolean`, `storage_key varchar(255) NOT NULL`, `mime_type varchar(50) NOT NULL`, `file_size_bytes int NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes < 5242880)`, `reviewed_by FK`, `reviewed_at`, `rejection_reason varchar(255)`, `archived_at`, `backup_verified_at`, `deleted_at`, `created_at`).
   - Tạo `appeals` (`id`, `event_id FK`, `student_id FK`, `attendance_id FK`, `round smallint CHECK (round IN (1, 2))`, `parent_appeal_id FK`, `category varchar(40)`, `status varchar(30)`, `reason text`, `attachments jsonb`, `t0_anchor_at timestamptz`, `deadline_at timestamptz`, `decided_by FK`, `decided_role varchar(30)`, `decision_note text`, `decided_at timestamptz`, `created_at`, `UNIQUE (event_id, student_id, round)`).
   - Tạo `student_restrictions` (`id`, `student_id FK`, `source_event_id FK`, `banned_until timestamptz NOT NULL`, `paused_by_appeal_id FK`, `lifted_by FK`, `lifted_reason varchar(500)`, `lifted_at timestamptz`, `created_at`).
   - Tạo `outbox_events` (`id`, `event_type varchar(80)`, `payload jsonb`, `status varchar(20) CHECK IN ('PENDING','PROCESSED','DEAD_LETTER')`, `retry_count smallint DEFAULT 0`, `next_attempt_at timestamptz`, `created_at`, `processed_at`).

---

### 5.5 Cập nhật Phạm vi Sở hữu Sprint 2 (`AGENTS.md` & `.github/CODEOWNERS` — Đã chốt khớp 100% Jira)

*(Tên package nhập CSV dùng **`importer/`** thay vì `import/` do `import` là từ khóa trong Java — `[Builder Call]`.)*

| Agent | Thành viên | Phạm vi đầy đủ từ Sprint 2 (BE + FE + Docs) |
|---|---|---|
| **A0** | Trần Thanh Cường (`@GMIOS25`) | Duy nhất tạo migration `V2__...` (`R6`), `config/`, `common/`, `auth/` (`QE-30`, `QE-74`), `outbox/`, hạ tầng `admin/` & `EvidenceStoragePort` core, root FE scaffold, `.github/` |
| **A1** | Nguyễn Huy Hoàng (`@hoangkq157`) | `event/` (`QE-12`, `QE-32`), quản trị tài khoản & cấp TK SV (`QE-35`, `QE-73`, phối hợp A0 trên `auth/`/`admin/`), phối hợp A2 trên `registration/` (`QE-39`, `QE-40`), `FE src/features/events/` |
| **A2** | Khánh Dương Nguyễn (`@YangKlee`) | `registration/`, `importer/` (`QE-26`, `QE-62` — BE + FE import), `FE src/features/registration/`, `src/test/**/e2e/`, `docs/test/` |
| **A3** | Huỳnh Hữu Đan (`@Dan28-05`) | `attendance/` (`QE-55`, `QE-33`, `QE-76`, `QE-77`), `evidence/` (upload, proxy, BCS review `QE-19`, `QE-42`, `QE-43`), `FE src/features/attendance/`, `FE src/features/evidence/` |
| **A4** | Nguyễn Nhật Khánh (`@KHANHSKY0808`) | `audit/`, `student/` (history `QE-58`), `appeal/` (`QE-59`, `QE-60`), `evidence/` (visibility/retention `QE-24`, `QE-47` — đồng sở hữu `CODEOWNERS` cùng `@Dan28-05`), `FE src/features/history/`, `FE src/features/appeal/`, `docs/` |

---

## 6. Danh mục Chỉnh sửa Từng Tệp (File-by-File Reconciliation Manifest)

### 6.1 Kho `QNU-Events-FE` (nhánh: `docs/QE-70-sync-sprint1-contract`, rẽ từ `develop` → PR vào `develop`)
1. `docs/api/sprint1-contract.md`: Đồng bộ chính xác 2 dòng `GET /preview` và `POST /publish` (bổ sung quy tắc chặn `CANCELLED` → `409 VERSION_CONFLICT` và `PUBLISHED` idempotent `200`) khớp 100% với `QNU-Events-BE/docs/api/sprint1-contract.md`, kèm liên kết tham chiếu tới bảng mã lỗi mở rộng Sprint 2–3 (`QNU-Events-BE/docs/api/error-codes.md`).

### 6.2 Kho `QNU-Events-BE` — Nhóm Tài liệu Markdown (nhánh: `docs/QE-81-pre-sprint2-docs-alignment`, rẽ từ `origin/main` để kế thừa `eaff6f1` `docs/spec/*.docx` → PR vào `develop` rồi release `develop` → `main` `[Builder Call]`)
1. **`docs/Design/v2-schema-blueprint.md` *(tạo mới `[Builder Call]`)*:** Lưu trữ cố định toàn bộ Blueprint Schema `V2` (§5.4) làm đầu vào đóng băng duy nhất cho A0 (`R6`), kèm ghi chú liên kết từ `docs/Design/erd.md` và `README.md`.
2. **`docs/api/error-codes.md` *(tạo mới `[Builder Call]`)*:** Lưu trữ cố định Bảng Mã Lỗi Chuẩn Sprint 1–3 (§5.3), gắn liên kết từ `docs/api/sprint1-contract.md` và `docs/decisions.md`.
3. `docs/decisions.md`: Chuyển trạng thái toàn bộ `A-001`, `A-01..A-06`, `FE-A01`, `ARCH-A01..A07`, `A-QE8-01..11`, `A-AUTH-01..08`, `A-EVT-01..04`, `A-REV-01` thành **`APPROVED (28/11/2026)`** có điều chỉnh nội dung theo đúng `D-1` → `D-10`, ngoại lệ sửa quota trong `A-EVT-01`, và quy tắc `TBD-EVT-03` (`evidence_deadline >= attendance_close_at`, `NO_SHOW` chờ sau `MAX(attendance_close_at + 24h, evidence_deadline)`, `UC007` theo scope); rút gọn mục `Điểm mở` chỉ giữ các mục vận hành/bên thứ ba thật sự còn mở (`Q-AUTH-03` Google Console, `TBD-NFR-01..05` môi trường đo tải, `TBD-SEC-03..04` pháp lý/pentest).
4. `docs/Design/quota-rules.md`: Chuyển trạng thái từ `⏳ CHỜ DUYỆT` sang `✅ APPROVED (28/11/2026)`; sửa lỗi đánh máy `освоб` → `giải phóng`; bổ sung `wait_order` tuần tự (`events.next_wait_order`), quy tắc tăng quota/capacity tự động kích hoạt auto-promote `WAITING_LIST` (đồng bộ ngoại lệ `A-EVT-01`), quy tắc auto-promote trước `startAt - 12h` kể cả sau `registration_deadline`, và mã lỗi `409 CANCELLATION_CUTOFF_PASSED`.
5. `docs/architecture/attendance-consistency-design.md`: Đồng bộ §2.2 sang mô hình **`event_pools` + `wait_order` + `FOR UPDATE SKIP LOCKED`** (`D-4`, bỏ bảng trung gian đa pool giao nhau `registration_pool_allocations`); đồng bộ ngoại lệ tăng/giảm quota `A-EVT-01` + `NO_SHOW` chờ sau `MAX(attendance_close_at + 24h, evidence_deadline)`; chuẩn hóa GPS mặc định `50m` (`D-7`), `150 ngày` retention, và bảng §7.2 sang trạng thái `APPROVED`.
6. `docs/architecture/dynamic-qr-specification.md`: Sửa §4 (`ADR-03`) phân biệt rõ **cấm Offline Prefetch** nhưng **hỗ trợ SV Offline Store-and-Forward trên mã QR đang chiếu trong cửa sổ `24h`** (`D-6`); đổi tên mã `A-04..A-07` thành `QR-ADR-01..04` để hết trùng mã với `decisions.md`; sửa `TC-16` từ `MISSING_HEADER` → `VALIDATION_FAILED` (`D-7`); sửa bán kính GPS ở §7.1 từ `100m` → cấu hình `10–500m` mặc định `50m`; thay thế toàn bộ đường dẫn cá nhân gãy `file:///c:/Users/fleya/...` bằng đường dẫn tương đối hợp lệ.
7. `docs/architecture/auth-and-session-lifecycle.md`: Cập nhật §7 đánh dấu các quyết định `D-1`, `D-3`, `D-10` đã chốt (`5 roles` có `CVHT`, Import chỉ thuộc `SYSTEM_ADMIN`, `HttpOnly` Refresh Cookie 7 ngày cho `"Lưu đăng nhập"`, Google SSO allowlist env cho mọi role đã cấp trước, check `LOCKED` sau BCrypt ở `QE-30`), chỉ giữ mở `Q-AUTH-03` (Google Cloud credentials thực tế).
8. `docs/architecture/mvp-architecture-and-boundaries.md`: Đồng bộ bảng 5 vai trò (`CVHT` là chính thức ở `V2`), **đồng bộ toàn bộ tên bảng Planned `V2+` ở §2.1 khớp 100% với `docs/Design/v2-schema-blueprint.md` (§5.4)** (thay thế các tên cũ `event_quota_pool`, `org_faculty`, `org_major`, `login_attempt_tracker`, `offline_sync_batches`, `proxy_attendance_items`, `appeal_histories`), cập nhật bảng hiện trạng Sprint 1 đã hoàn tất (đã fix `/history` `Page<HistoryItem>`, `alreadyRegistered`, `QR_EPOCH_SECONDS=10`, fail-fast secret), và bổ sung danh sách đầu mục cần chỉnh trên Figma.
9. `docs/test/business-state-transition-matrix.md`: Sửa 5 mâu thuẫn: (a) dòng 36 cho phép rút `WAITING_LIST` miễn mốc 12h (`D-5`); (b) dòng 37 cho phép đăng ký lại sau `CANCELLED` nếu còn hạn (`D-5`); (c) dòng 61 & bảng Appeal sửa thành 3 tầng BCS → CVHT (Vòng 1) → EVENT_OWNER (Vòng 2) (`D-2`); (d) dòng 72 cho phép quét QR/GPS ghi đè `PENDING` Evidence thành `ACCEPTED` (`D-7`); (e) dòng 46/117/123 sửa `CANCELLATION_DEADLINE_PASSED` → `CANCELLATION_CUTOFF_PASSED` và `48h` → `5 ngày lịch từ T0` (`D-5`, `D-9`); sửa link `../QNU_Event_SRS.docx` → `../spec/QNU_Event_SRS.docx`.
10. `docs/test/test-matrix-attendance-br-att.md`: Sửa dòng 70 & 192–193 từ `Event Owner` → `CLASS_REPRESENTATIVE (BCS)` duyệt Evidence (`D-2`); sửa `OUT_OF_GPS_RADIUS` → `GPS_OUT_OF_RANGE` và `INVALID_EVIDENCE` → `INVALID_EVIDENCE_FILE` (`D-7`); cập nhật link `docs/spec/...`.
11. `docs/import-contract/student-import-contract.md` & `docs/import-contract/README.md`: Cập nhật trạng thái từ `ASSUMPTION chưa duyệt` sang `APPROVED (D-3, D-10)` cho quy tắc phân tách `QE-26`/`QE-73`, quyền `SYSTEM_ADMIN`, `allowClassTransfer = false` mặc định + audit khi Admin bật cờ tường minh, và allowlist domain env.
12. `docs/traceability-matrix.md`: Sửa `48h` ở dòng `UC022` → `5 ngày lịch từ T0` (`D-9`); sửa link `QNU_Event_SRS.docx` → `spec/QNU_Event_SRS.docx`; cập nhật số liệu kiểm đếm thực tế nhánh `main` ngày `26/11/2026` (`151` BE JUnit = `40` unit + `111` IT; `43` FE Vitest; `7` Playwright mocked; `359` Bruno `.bru`; `35` Selenium; `4` JMeter; `15/15` Jira Bugs `Done`; PR `#49–#52` BE & `#14–#19` FE).
13. `docs/scrum/sprint1-scrum-ceremonies-and-dod.md`: Bổ sung mốc `25–26/11/2026` (merge PR `#49–#52` BE & `#14–#19` FE vào `main`, `15/15` Jira Bugs `Done`, cập nhật số test thực tế `151` BE / `43` FE), **giữ nguyên** các ô `[Chờ thành viên xác nhận...]` theo `R1`.
14. `README.md` (BE) & `docs/Design/erd.md`: Cập nhật `README.md` số liệu thực tế từ `94` tests (`13` unit + `81` IT) → **`151` tests (`40` unit + `111` IT)**; bổ sung endpoint `GET /api/v1/events/{id}/attendance/me`; sửa ghi chú fail-fast secret (đã triển khai `resolveJwtSecret` fail-fast ngoài `dev`/`demo`/`test`); sửa link `docs/spec/...` và gắn link `docs/Design/v2-schema-blueprint.md` & `docs/api/error-codes.md`. Trong `docs/Design/erd.md`, bổ sung ghi chú dẫn chiếu sang `v2-schema-blueprint.md` cho phần mở rộng Sprint 2.
15. `AGENTS.md` & `.github/CODEOWNERS`: Cập nhật bảng phân công Sprint 2 khớp 100% Jira theo §5.5 (dùng package Java hợp lệ `importer/`, đồng sở hữu `/evidence/` cho `@Dan28-05 @KHANHSKY0808`, `/appeal/` cho `@KHANHSKY0808`, `/outbox/` cho `@GMIOS25`).
16. **Xóa file trùng lặp:** Xóa `docs/plan/sprint1_rescue_plan.md` (bản trùng hoàn toàn với `docs/agent/sprint1-rescue-plan.md`, giữ lại `docs/agent/sprint1-rescue-plan.md` vì `AGENTS.md` đang trỏ tới đó).
17. **Commit bản Design Spec này (`docs/superpowers/specs/2026-11-28-pre-sprint2-docs-alignment-design.md`)** cùng nhánh tài liệu BE (`[Builder Call]`).

### 6.3 Kho `QNU-Events-BE` — Cập nhật 2 Tài liệu Word (`.docx`)
1. **`docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx`:**
   - Mục 2 (Bảng vai trò `[TBL#25]`): bổ sung hàng **Cố vấn học tập (CVHT)** (tiếp nhận & xử lý Khiếu nại Vòng 1, xem lịch lớp; phạm vi lớp được phân công) và cập nhật hàng **Admin / Event Owner** (xử lý Phúc khảo Vòng 2 & gỡ chế tài No-show; không mặc định CRUD/Import dữ liệu nền).
   - Mục 10.2 (`[TBL#125]`): bổ sung hàng **CVHT** (chỉ xem Evidence khi xử lý khiếu nại thuộc lớp được phân công).
   - Mục 11.2 (`[P#143]`): ghi rõ mặc định không chuyển lớp/chuyển khoa ở MVP; chỉ cho `SYSTEM_ADMIN` đính chính dữ liệu sai qua cờ tường minh kèm audit (`D-10`).
   - Mục 14 (`[TBL#149]`): bổ sung hàng **CVHT** trong bảng Actor – Use Cases.
   - Mục 18: ghi chú bổ sung rằng các điểm mở đã được chuẩn hóa và đóng trong `SRS v1.1` & `docs/decisions.md`.
2. **`docs/spec/QNU_Event_SRS.docx` (cập nhật lên `Version 1.1`):**
   - Trang bìa / Header `[P#5]`, `[P#23]` và bảng `Revision History` (`[P#15]`): ghi bản `1.1 (28/11/2026)` tóm tắt nội dung chuẩn hóa trước Sprint 2.
   - `UC005` & `UC007`: cập nhật ngoại lệ sửa `quotaTotal`/`pool.capacity` (`>= used`, tự động promote) trên sự kiện `PUBLISHED` đã có đăng ký (`A-EVT-01`), mốc `evidence_deadline >= attendance_close_at` và phạm vi hiển thị lịch `UC007` theo vai trò (`TBD-EVT-02`, `TBD-EVT-03`).
   - `REQ-REG-012` / `UC011`: sửa quy tắc rút `WAITING_LIST` được miễn mốc 12 giờ; đổi mã lỗi quá hạn 12h từ `400 Bad Request` → **`409 Conflict (CANCELLATION_CUTOFF_PASSED)`**; đổi lỗi sai đối tượng ở `UC010` từ `400 Bad Request` → **`403 Forbidden (NOT_IN_AUDIENCE)`**.
   - `UC015 5a`: đổi lỗi lần thử thứ 4 từ `429 Too Many Requests` → **`409 Conflict (ATTEMPT_LIMIT_REACHED)`**.
   - `REQ-REV-014` / `UC022–UC024`: làm rõ phân cấp 3 tầng (**BCS** duyệt Evidence → **CVHT** xử lý Vòng 1 → **EVENT_OWNER** xử lý Phúc khảo Vòng 2 & gỡ No-show) cùng thời hạn **5 ngày / 3 ngày / trần 30 ngày** từ `T0`.
   - `Appendix C Issues List` (`[TBL#1297]`): cập nhật rõ kết quả đã chốt (`Closed — 28/11/2026`) cho toàn bộ các mục `TBD-AUTH-01..03` (trừ credential Google Console), `TBD-EVT-01..03`, `TBD-REG-01..03`, `TBD-ATT-01..02`, `TBD-REV-01..03`, `TBD-DATA-01`, `TBD-DATA-03`, `TBD-SEC-02`; giữ nguyên `Open` đối với các mục vận hành/pháp lý ngoài phạm vi quyết định hiện tại (`TBD-NFR-01..05`, `TBD-SEC-01`, `TBD-SEC-03..04`, `TBD-ATT-03` ngưỡng vận tốc/accuracy GPS chi tiết, `TBD-DATA-02` ngưỡng đo export).

---

## 7. Phần Dành Cho Người Triển khai (Left to the Builder)

1. **Kỹ thuật chỉnh sửa `.docx` an toàn:** Dùng script Python chỉnh sửa trực tiếp trên cây XML (`python-docx` hoặc `lxml` trên `word/document.xml`) mà **không tạo mới file Word từ đầu** để bảo toàn 100% style, header/footer, TOC và **54 ảnh UML** (`word/media/*`). Sau khi ghi file, chạy script xác minh:
   - Mở lại hợp lệ không lỗi ZIP/XML;
   - Đếm số file trong `word/media/` phải bằng đúng `54` (SRS) và `0` (Proposal);
   - Trích xuất lại văn bản để xác nhận mọi cụm từ mâu thuẫn cũ (`429 Too Many Requests`, `400 Bad Request` ở UC010/UC011, thiếu CVHT ở Proposal) đã được thay thế chính xác.
2. **Kiểm chứng tổng thể sau khi sửa toàn bộ file:**
   - Chạy grep trên toàn bộ tài liệu đang hiệu lực ở BE & FE (**loại trừ `docs/superpowers/`** vì bản spec này liệt kê các chuỗi cũ cần thay thế, và giữ nguyên các mục chụp lịch sử có gắn banner `Bản chụp lịch sử` ở `docs/scrum/sprint1-tech-lead-review.md`, `docs/Test_Evidence/TEST_REPORT.md` `[Builder Call]`) để xác nhận **0** xuất hiện sót của các chuỗi mâu thuẫn (`file:///c:/Users/fleya`, `освоб`, `OUT_OF_GPS_RADIUS`, `CANCELLATION_DEADLINE_PASSED`, `MISSING_HEADER`, `trong vòng 48h`, `94 phương thức`), và **0** link Markdown nội bộ bị gãy.
   - Chạy lệnh kiểm chứng theo `R5`: BE `.\mvnw.cmd -B -ntp test` (xác nhận `40/40` unit tests xanh, không ảnh hưởng code); FE `npm run lint && npm run typecheck && npm run test && npm run build`.
   - Kiểm tra `git diff` trên cả 2 repo (đảm bảo chỉ sửa tài liệu/CODEOWNERS, không đụng file code/migration `V1`).
3. **Ghi chú bàn giao Figma & Jira:** Đính kèm trong PR (hoặc báo cáo kết thúc) danh sách 3 điểm cần cập nhật tay trên Figma và nhắc trạng thái Jira Sprint 1 (`41/41 Done` đang chờ Complete).
