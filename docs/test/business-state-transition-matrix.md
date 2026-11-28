# ĐẶC TẢ MA TRẬN CHUYỂN TRẠNG THÁI NGHIỆP VỤ & KỊCH BẢN KIỂM THỬ
## (BUSINESS STATUS & STATE TRANSITION TEST MATRIX)

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Jira Task:** [QE-25] Business status/state transition test  
> **Nhãn (Labels):** `mvp`, `phase-1`, `req-evt`, `req-reg`, `req-qa`  
> **Tài liệu căn cứ:** [Proposal v2](../spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx), [QNU_Event_SRS.docx](../spec/QNU_Event_SRS.docx) (`v1.1`), [quota-rules.md](../Design/quota-rules.md), [error-codes.md](../api/error-codes.md), [decisions.md](../decisions.md)  
> **Phân định phạm vi:**
> - **Phạm vi Sprint 1 (Đã hiện thực & Kiểm thử tự động trên API):** Chuyển trạng thái Đăng ký cơ bản (`NONE` $\rightarrow$ `REGISTERED`, từ chối `CAPACITY_FULL` theo `A-02`, Idempotent 200 OK, Cưỡng chế Unique Partial Index `uq_registrations_active`) và Điểm danh Dynamic QR (`NOT_CHECKED_IN` $\rightarrow$ `ACCEPTED`, từ chối 422 `QR_EXPIRED` / `QR_BAD_SIGNATURE`, khóa sau 3 lần sai $\rightarrow$ 409 `ATTEMPT_LIMIT_REACHED`, từ chối 409 `ALREADY_CHECKED_IN`, 403 `NOT_REGISTERED`, 409 `ATTENDANCE_NOT_OPEN`, Cưỡng chế Unique Partial Index `uq_attendances_valid`).
> - **Phạm vi Sprint 2–3 (Đã chốt chuẩn hóa `D-1..D-10` — Roadmap triển khai):** Hủy đăng ký trước ngưỡng cắt 12h (`startAt - 12h`) (`QE-40`), Danh sách chờ FIFO `event_pools` + `wait_order` & Auto-promote (`QE-39`), Chế tài No-show 7 ngày (`QE-14`), Nộp & Duyệt minh chứng Evidence (`QE-42`, `QE-43`) và Khiếu nại 2 vòng CVHT → EVENT_OWNER (`QE-59`, `QE-60`).

---

## PHẦN 1: MÁY TRẠNG THÁI ĐĂNG KÝ (REGISTRATION STATE MACHINE)

### 1.1. Định nghĩa các Trạng thái
| Mã trạng thái | Tên tiếng Việt | Phạm vi | Ý nghĩa nghiệp vụ | Quyền lợi & Ràng buộc |
|---|---|---|---|---|
| **NONE** | Chưa đăng ký | Sprint 1 | Trạng thái ban đầu của sinh viên đối với sự kiện. | Chưa có bản ghi trong bảng `registrations`. |
| **REGISTERED** | Đã giữ suất chính thức | Sprint 1 | Sinh viên đăng ký thành công và giữ 1 suất trong quota sự kiện. | Được phép tham gia điểm danh (Check-in); giữ suất hợp lệ. |
| **WAITING_LIST** | Danh sách chờ | Sprint 2 (`QE-39`) | Sinh viên đăng ký khi quota sự kiện đã đầy; xếp hàng theo thứ tự FIFO. | Không được phép điểm danh; không bị phạt No-show; tự động đôn lên `REGISTERED` khi có suất trống. |
| **CANCELLED** | Đã hủy đăng ký | Sprint 2 (`QE-40`) | Sinh viên chủ động hủy đăng ký trước mốc cắt 12 giờ (`startAt - 12h`). | Suất được hoàn trả lại quota hoặc chuyển giao cho sinh viên đầu tiên trong Waiting List; không bị phạt. |
| **NO_SHOW** | Vắng mặt không lý do | Sprint 2 (`QE-14`) | Sự kiện kết thúc mà sinh viên có trạng thái `REGISTERED` nhưng không có điểm danh `ACCEPTED`. | Tự động kích hoạt chế tài **cấm đăng ký sự kiện trong 07 ngày**; có thể khiếu nại (Appeal). |
| **EVENT_CANCELLED** | Sự kiện bị hủy | Sprint 2 (`QE-15`) | Sự kiện bị Ban tổ chức hủy bỏ trước hoặc trong khi diễn ra. | Toàn bộ đăng ký đóng lại; sinh viên không bị xử phạt chế tài No-show. |

---

### 1.2. Ma trận Chuyển trạng thái Đăng ký (Registration State Transition Matrix)

Bảng đối chiếu 2 chiều: **[Trạng thái hiện tại]** $\times$ **[Sự kiện kích hoạt / Hành động]**:

| Trạng thái hiện tại | E1: Đăng ký (Còn quota) *(Sprint 1)* | E2: Đăng ký (Hết quota) | E3: Bấm Hủy trước mốc 12h trước startAt *(Sprint 2)* | E4: Bấm Hủy trong vòng 12h trước startAt *(Sprint 2)* | E5: Tự động đôn suất (Waiting List Promotion) *(Sprint 2)* | E6: Sự kiện kết thúc (Có điểm danh ACCEPTED) | E7: Sự kiện kết thúc (Không điểm danh) *(Sprint 2)* | E8: BTC hủy sự kiện *(Sprint 2)* |
|---|---|---|---|---|---|---|---|---|
| **NONE (Chưa ĐK)** | $\rightarrow$ **REGISTERED** *(Hợp lệ - S1)*<br>Tăng `registered_count` (+1)<br>HTTP 201 Created | **Sprint 1:** ❌ Chặn (409 CAPACITY_FULL)<br>**Sprint 2:** $\rightarrow$ **WAITING_LIST** | ❌ Bất hợp lệ<br>(404 NOT_FOUND) | ❌ Bất hợp lệ<br>(404 NOT_FOUND) | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |
| **REGISTERED** | ❌ Bất hợp lệ / Lũy biến<br>(200 OK Idempotent, giữ nguyên) | ❌ Bất hợp lệ / Lũy biến<br>(200 OK Idempotent, giữ nguyên) | $\rightarrow$ **CANCELLED** *(Hợp lệ - S2)*<br>Hoàn pool gốc & gọi E5 | ❌ Bị từ chối<br>(409 CANCELLATION_CUTOFF_PASSED) | ❌ Không áp dụng | $\rightarrow$ **REGISTERED (Done)**<br>Hoàn thành sự kiện | $\rightarrow$ **NO_SHOW** *(Hợp lệ - S2/S3)*<br>Áp dụng chế tài cấm 7 ngày (sau `MAX(close+24h, evidence_deadline)`, miễn `PENDING`) | $\rightarrow$ **EVENT_CANCELLED**<br>Không phạt No-show |
| **WAITING_LIST** *(Sprint 2)* | ❌ Bất hợp lệ / Lũy biến<br>(200 OK giữ nguyên `wait_order`) | ❌ Bất hợp lệ / Lũy biến<br>(200 OK giữ nguyên `wait_order`) | $\rightarrow$ **CANCELLED** *(Hợp lệ - S2)*<br>Rút khỏi hàng đợi | $\rightarrow$ **CANCELLED** *(Hợp lệ - S2, `D-5`)*<br>**Miễn mốc 12h**, không chạm quota, không promote | $\rightarrow$ **REGISTERED** *(Hợp lệ - S2)*<br>Nhận suất trống từ người hủy (kể cả sau `registration_deadline` miễn trước `startAt - 12h`) | ❌ Bất hợp lệ<br>(Chưa có suất) | $\rightarrow$ **CANCELLED / CLOSED**<br>Không phạt No-show | $\rightarrow$ **EVENT_CANCELLED**<br>Không phạt No-show |
| **CANCELLED** *(Sprint 2)* | $\rightarrow$ **REGISTERED** *(Hợp lệ - `D-5`)*<br>Được phép đăng ký lại nếu còn hạn & không bị `STUDENT_PENALIZED` (tạo/tái kích hoạt theo `uq_registrations_active`) | $\rightarrow$ **WAITING_LIST** *(Hợp lệ - S2)*<br>Xếp hàng chờ mới theo `wait_order` nếu còn hạn | ❌ Bất hợp lệ<br>(200 OK Idempotent) | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |
| **NO_SHOW** *(Sprint 2)* | ❌ Bị chặn bởi chế tài 7 ngày<br>(403 STUDENT_PENALIZED) | ❌ Bị chặn bởi chế tài 7 ngày<br>(403 STUDENT_PENALIZED) | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |

---

### 1.3. Quy tắc Nghiệp vụ cốt lõi (Registration Business Rules)
1. **BR-REG-01 (Giữ suất nguyên tử - Sprint 1):** Đăng ký vào `REGISTERED` phải thực hiện atomic lock (`SELECT FOR UPDATE` hoặc Native Atomic Query `UPDATE events SET registered_count = registered_count + 1 WHERE id = :id AND registered_count < quota_total`) để đảm bảo không vượt quá `quota_total`.
2. **BR-REG-02 (Tự động đôn danh sách chờ - Sprint 2 / QE-39, QE-40):** Khi 1 sinh viên `REGISTERED` chuyển sang `CANCELLED` (hoặc khi Owner tăng `quotaTotal`/`capacity` theo `A-EVT-01`), hệ thống trả suất về đúng `pool_id` và tìm bản ghi `WAITING_LIST` có `wait_order` nhỏ nhất (`events.next_wait_order`) trong cùng pool bằng `FOR UPDATE SKIP LOCKED`, tái xác thực điều kiện (bỏ qua `#01` nếu không đủ điều kiện), chuyển thành `REGISTERED`; vẫn tiếp tục đôn suất sau `registration_deadline` miễn còn trước mốc `startAt - 12h` (`D-4`, `D-5`).
3. **BR-REG-03 (Chống No-show oan - Sprint 2–3 / QE-14):** Chỉ sinh viên có trạng thái `REGISTERED` (không có `ACCEPTED`, không có `EVIDENCE` `PENDING`, và không đang có Appeal Vòng 1 hợp lệ) mới bị xét `NO_SHOW` sau mốc `MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`. Tuyệt đối không phạt `WAITING_LIST`, `CANCELLED` hay sự kiện `CANCELLED` (`D-9`).
4. **BR-REG-04 (Hạn chót hủy đăng ký - Sprint 2 / QE-40):** Mốc cắt hủy đăng ký cho `REGISTERED` là **12 giờ trước thời điểm sự kiện bắt đầu** (`server_time < startAt - 12h`, theo `REQ-REG-012..014`, `UC011`, `docs/Design/quota-rules.md`). Từ mốc `startAt - 12h`, mọi yêu cầu hủy từ `REGISTERED` bị từ chối với **`409 CANCELLATION_CUTOFF_PASSED`**; riêng **`WAITING_LIST` được miễn mốc 12h** (`D-5`).
5. **BR-REG-05 (Cưỡng chế CSDL chống đăng ký trùng - Sprint 1):** Unique Partial Index `uq_registrations_active` trên `(event_id, student_id) WHERE status IN ('REGISTERED', 'WAITING_LIST')` bảo vệ tính toàn vẹn và **cho phép đăng ký lại sau khi `CANCELLED`**.

---

## PHẦN 2: MÁY TRẠNG THÁI ĐIỂM DANH (ATTENDANCE STATE MACHINE)

### 2.1. Định nghĩa các Trạng thái
| Mã trạng thái | Tên tiếng Việt | Phạm vi | Ý nghĩa nghiệp vụ | Quyền lợi & Ràng buộc |
|---|---|---|---|---|
| **NOT_CHECKED_IN** | Chưa điểm danh | Sprint 1 | Sinh viên đã đăng ký nhưng chưa thực hiện lần điểm danh nào. | Trạng thái mặc định trước giờ sự kiện. |
| **ATTEMPTED** | Đã thử điểm danh | Sprint 1 | Ghi nhận một lần gửi request điểm danh lên máy chủ (log trong `attendance_attempts`). | Mỗi sinh viên được tối đa 3 lần thử thất bại trước khi bị khóa. |
| **PENDING** | Chờ xét duyệt | Sprint 2 (`QE-42`) | Sinh viên nộp ảnh minh chứng (Evidence) hoặc điểm danh hộ có lý do. | Chờ Ban Cán Sự (BCS) lớp duyệt; chưa được tính điểm rèn luyện. |
| **ACCEPTED** | Điểm danh hợp lệ | Sprint 1 | Đã xác thực thành công qua Dynamic QR hợp lệ trong cửa sổ điểm danh. | Ghi nhận chính thức, tích lũy điểm rèn luyện; mỗi sinh viên chỉ có **duy nhất 1 bản ghi** `ACCEPTED`. |
| **REJECTED** | Bị từ chối | Sprint 1 | Lượt điểm danh không hợp lệ (sai mã QR, quá hạn grace 3s, hoặc BCS bác minh chứng). | Cho phép thử lại nếu số lần thử < 3; nếu bị BCS từ chối thì được quyền nộp khiếu nại (Appeal). |
| **APPEALED** | Đang khiếu nại | Sprint 2 (`QE-59`, `QE-60`) | Sinh viên nộp đơn khiếu nại lên **CVHT (Vòng 1)** hoặc **EVENT_OWNER (Phúc khảo Vòng 2)** (`D-2`; BCS chỉ duyệt Evidence, không xử lý Appeal quyết định của mình). | Hồ sơ khiếu nại đang mở; Round 1 hợp lệ tạm dừng bộ đếm chế tài No-show (`D-9`). |

---

### 2.2. Ma trận Chuyển trạng thái Điểm danh (Attendance State Transition Matrix)

Bảng đối chiếu 2 chiều: **[Trạng thái hiện tại]** $\times$ **[Sự kiện kích hoạt / Hành động]**:

| Trạng thái hiện tại | A1: Quét Dynamic QR hợp lệ *(Sprint 1)* | A2: Quét QR sai / hết hạn (Lần 1-2) *(Sprint 1)* | A3: Quét QR sai lần thứ 3 (Chạm ngưỡng) *(Sprint 1)* | A4: Nộp minh chứng Evidence *(Sprint 2)* | A5: BCS duyệt Evidence *(Sprint 2)* | A6: BCS bác Evidence *(Sprint 2)* | A7: Sinh viên nộp Appeal *(Sprint 2)* | A8: Khiếu nại được chấp thuận *(Sprint 2)* | A9: Khiếu nại bị bác bỏ *(Sprint 2)* |
|---|---|---|---|---|---|---|---|---|---|
| **NOT_CHECKED_IN** | $\rightarrow$ **ACCEPTED** *(Hợp lệ - S1)*<br>HTTP 200 OK | $\rightarrow$ **REJECTED (Attempted)**<br>HTTP **422 QR_BAD_SIGNATURE** hoặc **422 QR_EXPIRED** | $\rightarrow$ **REJECTED (Locked)**<br>HTTP **422** (Lần 4 bị chặn 409) | $\rightarrow$ **PENDING** *(S2)*<br>Chờ BCS duyệt | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng |
| **PENDING** *(Sprint 2)* | $\rightarrow$ **ACCEPTED** *(Hợp lệ - `D-7`)*<br>QR/GPS hợp lệ ghi đè `PENDING` thành `REJECTED` (`SUPERSEDED_BY_ACCEPTED`) | `PENDING` giữ nguyên; lỗi QR/GPS tính vào 3 lượt tự động (`counted = true`) | `PENDING` giữ nguyên; khóa 3 lượt tự động (vẫn chờ BCS duyệt Evidence) | ❌ Bất hợp lệ<br>(Đã có đơn) | $\rightarrow$ **ACCEPTED** *(Hợp lệ - S2)*<br>BCS lớp duyệt | $\rightarrow$ **REJECTED** *(Hợp lệ - S2)*<br>BCS ghi rõ lý do (`<= 255` ký tự) | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng |
| **REJECTED (Attempts < 3)** | $\rightarrow$ **ACCEPTED** *(Hợp lệ - S1)*<br>Thử lại đúng mã trong giờ | $\rightarrow$ **REJECTED**<br>Tăng số lần thử lên 2 | $\rightarrow$ **REJECTED (Locked)**<br>Đạt 3 lần sai $\rightarrow$ Khóa | $\rightarrow$ **PENDING** *(S2)*<br>Nộp minh chứng | ❌ Không áp dụng | ❌ Không áp dụng | $\rightarrow$ **APPEALED** *(S2)* | ❌ Không áp dụng | ❌ Không áp dụng |
| **REJECTED (Attempts = 3)** | ❌ Bị từ chối ngay<br>(HTTP 409 ATTEMPT_LIMIT_REACHED) | ❌ Bị từ chối ngay<br>(HTTP 409 ATTEMPT_LIMIT_REACHED) | ❌ Bị từ chối ngay<br>(HTTP 409 ATTEMPT_LIMIT_REACHED) | $\rightarrow$ **PENDING** *(S2)*<br>Nộp minh chứng | ❌ Không áp dụng | ❌ Không áp dụng | $\rightarrow$ **APPEALED** *(S2)* | ❌ Không áp dụng | ❌ Không áp dụng |
| **ACCEPTED** | ❌ Bất hợp lệ<br>(HTTP 409 ALREADY_CHECKED_IN) | ❌ Bất hợp lệ<br>(HTTP 409 ALREADY_CHECKED_IN) | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng |
| **APPEALED** *(Sprint 2)* | ❌ Bị chặn | ❌ Bị chặn | ❌ Bị chặn | ❌ Bị chặn | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Bất hợp lệ | $\rightarrow$ **ACCEPTED** *(Hợp lệ - S2)* | $\rightarrow$ **REJECTED (Final)** *(S2)* |

---

### 2.3. Quy tắc Nghiệp vụ cốt lõi (Attendance Business Rules)
1. **BR-ATT-01 (Giới hạn lần thử - Sprint 1):** Mỗi sinh viên có tối đa **03 lần thử điểm danh thất bại** cho một sự kiện (ghi nhận trong bảng `attendance_attempts`). Đến lần thứ 4, hệ thống từ chối ngay lập tức với mã lỗi `409 ATTEMPT_LIMIT_REACHED` kể cả khi xuất trình mã QR hợp lệ.
2. **BR-ATT-02 (Tính bất biến của kết quả Accepted - Sprint 1):** Mỗi cặp `(event_id, student_id)` chỉ có tối đa **1 bản ghi trạng thái `ACCEPTED`** (được cưỡng chế bởi Unique Partial Index `uq_attendances_valid` trên `(event_id, student_id) WHERE status = 'ACCEPTED'`).
3. **BR-ATT-03 (Điều kiện tiên quyết - Sprint 1):** Sinh viên bắt buộc phải có trạng thái đăng ký là `REGISTERED` mới được thực hiện điểm danh. Nếu chưa đăng ký, hệ thống trả về lỗi `403 NOT_REGISTERED`.
4. **BR-ATT-04 (Khung giờ điểm danh - Sprint 1):** Điểm danh chỉ được phép thực hiện khi thời gian hiện tại nằm trong khoảng `[attendance_open_at, attendance_close_at]`. Ngoài khung giờ này, hệ thống trả về `409 ATTENDANCE_NOT_OPEN`.
5. **BR-ATT-05 (Mã lỗi xác thực QR - Sprint 1):** Lượt quét thất bại do sai chữ ký hoặc mã QR đã hết hạn (> 3s grace) trả về HTTP **`422 UNPROCESSABLE_ENTITY`** với mã lỗi `QR_BAD_SIGNATURE` hoặc `QR_EXPIRED`.
6. **BR-ATT-06 (Quy trình khiếu nại 3 tầng - Sprint 2 / QE-59, QE-60, `D-2`, `D-9`):** **BCS** duyệt/từ chối Evidence (`QE-43`) → **CVHT** tiếp nhận & giải quyết Appeal **Vòng 1** (hạn nộp **5 ngày lịch từ `T0`**) → **EVENT_OWNER** giải quyết Phúc khảo **Vòng 2** (hạn nộp **3 ngày lịch** từ kết quả Vòng 1, trần tổng **30 ngày lịch từ `T0`**) và gỡ chế tài No-show. Khiếu nại được chấp thuận chuyển attendance sang `ACCEPTED` và gỡ bỏ hạn chế `student_restrictions` tương ứng.

---

## PHẦN 3: DANH MỤC CA KIỂM THỬ CHUYỂN TRẠNG THÁI (TEST MATRIX SCENARIOS)

### 3.1. Nhóm Kịch bản Sprint 1 — Kiểm thử Tự động qua REST API & DB Invariants (`BusinessStateTransitionIT.java`)

| Mã Test Case | Nhóm kiểm thử | Trạng thái ban đầu | Sự kiện kích hoạt | Điều kiện kiểm tra / Dữ liệu | Kết quả kỳ vọng (State & HTTP) | Kiểm tra Ràng buộc CSDL (DB Invariants) |
|---|---|---|---|---|---|---|
| **STTC-REG-01** | Registration | NONE | Đăng ký sự kiện | Sự kiện `PUBLISHED`, còn quota | $\rightarrow$ `REGISTERED`<br>HTTP **201 Created** | `registrations.status = 'REGISTERED'`; `registered_count` tăng +1 |
| **STTC-REG-02** | Registration | NONE | Đăng ký sự kiện | Sự kiện `PUBLISHED`, hết quota (`quotaTotal = registeredCount`) | $\rightarrow$ `NONE` (Không tạo bản ghi)<br>HTTP **409 CAPACITY_FULL** | Không tạo dòng mới trong `registrations`; `registered_count` giữ nguyên |
| **STTC-REG-03** | Registration | REGISTERED | Đăng ký lại (Cùng SV) | Gửi lại request đăng ký cùng tài khoản | $\rightarrow$ `REGISTERED`<br>HTTP **200 OK** (`alreadyRegistered: true`) | Không tạo thêm dòng mới; `registered_count` không tăng (Idempotent) |
| **STTC-REG-DB** | Registration | REGISTERED | DB Concurrency Violations | Cố ý insert trực tiếp bản ghi active thứ 2 cùng sinh viên | ❌ CSDL ném lỗi vi phạm Unique Constraint | Bị chặn bởi Unique Partial Index `uq_registrations_active` |
| **STTC-ATT-01** | Attendance | NOT_CHECKED_IN | Quét QR hợp lệ | Đúng mã QR động, trong khung giờ mở điểm danh | $\rightarrow$ `ACCEPTED`<br>HTTP **200 OK** | `attendances.status = 'ACCEPTED'`; `attendance_attempts` ghi outcome=ACCEPTED |
| **STTC-ATT-02** | Attendance | NOT_CHECKED_IN | Quét QR hết hạn | Mã QR quá hạn quay vòng và quá 3s grace | $\rightarrow$ `REJECTED`<br>HTTP **422 QR_EXPIRED** | `attendance_attempts` ghi outcome=REJECTED (reason=QR_EXPIRED) |
| **STTC-ATT-03** | Attendance | NOT_CHECKED_IN | Quét QR sai chữ ký (Lần 1-3) | Mã QR bị sửa đổi chữ ký HMAC | $\rightarrow$ `REJECTED`<br>HTTP **422 QR_BAD_SIGNATURE** | `attendance_attempts` ghi nhận lần lượt 3 lần thử thất bại |
| **STTC-ATT-04** | Attendance | REJECTED (3 lần sai) | Quét QR hợp lệ lần 4 | Xuất trình mã QR đúng chuẩn sau khi đã sai 3 lần | ❌ Bị từ chối ngay<br>HTTP **409 ATTEMPT_LIMIT_REACHED** | Không tạo bản ghi `ACCEPTED`; bị chặn ở tầng nghiệp vụ kiểm tra số lần thử |
| **STTC-ATT-05** | Attendance | ACCEPTED | Quét QR lần 2 (Cùng SV) | Sinh viên đã có `ACCEPTED` gửi quét lại mã mới | ❌ Bị từ chối<br>HTTP **409 ALREADY_CHECKED_IN** | Không tạo thêm bản ghi `ACCEPTED` nào |
| **STTC-ATT-06** | Attendance | NOT_REGISTERED | Quét mã QR hợp lệ | Sinh viên chưa từng đăng ký sự kiện | ❌ Bị từ chối<br>HTTP **403 NOT_REGISTERED** | Không tạo bản ghi attendance nào |
| **STTC-ATT-07** | Attendance | NOT_CHECKED_IN | Quét mã QR ngoài giờ | Điểm danh trước `attendanceOpenAt` hoặc sau `attendanceCloseAt` | ❌ Bị từ chối<br>HTTP **409 ATTENDANCE_NOT_OPEN** | Không tạo bản ghi attendance; audit ghi nhận từ chối |
| **STTC-ATT-DB** | Attendance | ACCEPTED | DB Concurrency Violations | Cố tình insert trực tiếp bản ghi `ACCEPTED` thứ 2 cho cùng SV | ❌ CSDL ném lỗi vi phạm Unique Constraint | Bị chặn bởi Unique Partial Index `uq_attendances_valid` |

---

### 3.2. Nhóm Kịch bản Sprint 2 Roadmap — Kế hoạch Mở rộng (Triển khai khi hoàn thiện endpoint tương ứng)

| Mã Test Case | Task liên quan | Trạng thái ban đầu | Sự kiện kích hoạt | Điều kiện kiểm tra / Dữ liệu | Kết quả kỳ vọng (State & HTTP) | Ghi chú lộ trình |
|---|---|---|---|---|---|---|
| **STTC-REG-04** | `QE-40` | REGISTERED | Bấm hủy đăng ký | Trước mốc `startAt - 12h` | $\rightarrow$ `CANCELLED`<br>HTTP 200 OK | Hoàn trả quota (`registered_count - 1`) |
| **STTC-REG-05** | `QE-39`, `QE-40` | REGISTERED & WAITING_LIST | SV1 hủy đăng ký | Có SV2 đang chờ trong Waiting List | SV1 $\rightarrow$ `CANCELLED`<br>SV2 $\rightarrow$ `REGISTERED` | SV2 được tự động đôn suất theo thứ tự FIFO |
| **STTC-REG-06** | `QE-40` | REGISTERED | Bấm hủy đăng ký | Tại hoặc sau mốc `startAt - 12h` | ❌ Bị từ chối<br>HTTP 409 `CANCELLATION_CUTOFF_PASSED` | Quá hạn chót hủy 12h cho `REGISTERED` (`WAITING_LIST` được miễn — `D-5`) |
| **STTC-REG-07** | `QE-14` | REGISTERED | Sự kiện kết thúc (`MAX(close+24h, evidence_deadline)`) | Không có `ACCEPTED` và không có `EVIDENCE` `PENDING` | $\rightarrow$ `NO_SHOW` | Ghi `student_restrictions` (7 ngày, chồng nhau lấy `MAX` — `D-9`) |
| **STTC-REG-08** | `QE-14` | WAITING_LIST | Sự kiện kết thúc | Không được đôn suất | $\rightarrow$ `CANCELLED / CLOSED` | **Tuyệt đối KHÔNG chuyển thành `NO_SHOW`** |
| **STTC-ATT-08** | `QE-42` | NOT_CHECKED_IN | Nộp ảnh Evidence | Trong thời hạn `evidence_deadline` | $\rightarrow$ `PENDING`<br>HTTP 201/202 | Lưu private storage (`EvidenceStoragePort`), cho phép QR/GPS ghi đè `ACCEPTED` (`D-7`) |
| **STTC-ATT-09** | `QE-43` | PENDING | Ban Cán Sự duyệt (Approve) | BCS có thẩm quyền lớp (`bcs_class_assignment`) | $\rightarrow$ `ACCEPTED`<br>HTTP 200 OK | Ghi audit người duyệt (`D-2`) |
| **STTC-ATT-10** | `QE-43` | PENDING | Ban Cán Sự từ chối (Reject) | BCS ghi rõ lý do (`<= 255` ký tự) | $\rightarrow$ `REJECTED`<br>HTTP 200 OK | Mở `T0 = reviewed_at` cho quyền nộp Appeal Vòng 1 |
| **STTC-ATT-11** | `QE-59` | REJECTED / NO_SHOW | Sinh viên nộp Appeal | Trong vòng **5 ngày lịch từ `T0`** (Vòng 1, `23:59:59` GMT+7) | $\rightarrow$ `APPEALED`<br>HTTP 201 Created | Tạo hồ sơ `appeals` (`round = 1`) lên **CVHT**; tạm dừng bộ đếm No-show (`D-9`) |
| **STTC-ATT-12** | `QE-60` | APPEALED | Thẩm định Appeal thành công | **CVHT** (Vòng 1) hoặc **EVENT_OWNER** (Phúc khảo Vòng 2, hạn 3 ngày từ R1, trần 30 ngày) chấp thuận | $\rightarrow$ `ACCEPTED`<br>HTTP 200 OK | Chuyển thành `ACCEPTED`; **EVENT_OWNER** gỡ `student_restrictions` nếu có (`D-2`) |
