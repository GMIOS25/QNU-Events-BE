# ĐẶC TẢ MA TRẬN CHUYỂN TRẠNG THÁI NGHIỆP VỤ & KỊCH BẢN KIỂM THỬ
## (BUSINESS STATUS & STATE TRANSITION TEST MATRIX)

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Jira Task:** [QE-25] Business status/state transition test  
> **Nhãn (Labels):** `mvp`, `phase-1`, `req-evt`, `req-reg`, `req-qa`  
> **Tài liệu căn cứ:** [Proposal v2.1 §15](../../docs/decisions.md), [QNU_Event_SRS.md](../../../QNU_Event_SRS.md)  
> **Mục tiêu:** Đặc tả máy trạng thái (State Machine), ma trận chuyển trạng thái hợp lệ/bất hợp lệ và bộ kịch bản kiểm thử tự động cho:
> 1. **Registration State Machine:** `REGISTERED`, `WAITING_LIST`, `CANCELLED`, `NO_SHOW`
> 2. **Attendance State Machine:** `ATTEMPTED`, `PENDING`, `ACCEPTED`, `REJECTED`, `APPEALED`

---

## PHẦN 1: MÁY TRẠNG THÁI ĐĂNG KÝ (REGISTRATION STATE MACHINE)

### 1.1. Định nghĩa các Trạng thái
| Mã trạng thái | Tên tiếng Việt | Ý nghĩa nghiệp vụ | Quyền lợi & Ràng buộc |
|---|---|---|---|
| **NONE** | Chưa đăng ký | Trạng thái ban đầu của sinh viên đối với sự kiện. | Chưa có bản ghi trong bảng `registrations`. |
| **REGISTERED** | Đã giữ suất chính thức | Sinh viên đã đăng ký thành công và giữ một suất trong quota sự kiện. | Được phép tham gia điểm danh (Check-in); nếu vắng mặt không lý do sẽ bị xét `NO_SHOW`. |
| **WAITING_LIST** | Danh sách chờ | Sinh viên đăng ký khi quota sự kiện đã đầy; xếp hàng theo thứ tự FIFO. | Không được phép điểm danh; không bị phạt No-show; tự động đôn lên `REGISTERED` khi có suất trống. |
| **CANCELLED** | Đã hủy đăng ký | Sinh viên chủ động hủy đăng ký trước hạn chót (`registrationDeadline`). | Suất được hoàn trả lại quota hoặc chuyển giao cho sinh viên đầu tiên trong Waiting List; không bị phạt. |
| **NO_SHOW** | Vắng mặt không lý do | Sự kiện đã kết thúc mà sinh viên có trạng thái `REGISTERED` nhưng không có điểm danh `ACCEPTED`. | Tự động kích hoạt chế tài **cấm đăng ký sự kiện trong 07 ngày**; có thể khiếu nại (Appeal). |
| **EVENT_CANCELLED** | Sự kiện bị hủy | Sự kiện bị Ban tổ chức hủy bỏ trước hoặc trong khi diễn ra. | Toàn bộ đăng ký đóng lại; sinh viên không bị xử phạt chế tài No-show. |

---

### 1.2. Ma trận Chuyển trạng thái Đăng ký (Registration State Transition Matrix)

Bảng đối chiếu 2 chiều: **[Trạng thái hiện tại]** $\times$ **[Sự kiện kích hoạt / Hành động]**:

| Trạng thái hiện tại | E1: Đăng ký (Còn quota) | E2: Đăng ký (Hết quota) | E3: Sinh viên bấm Hủy trước hạn | E4: Sinh viên bấm Hủy sau hạn | E5: Tự động đôn suất (Waiting List Promotion) | E6: Sự kiện kết thúc (Có điểm danh ACCEPTED) | E7: Sự kiện kết thúc (Không điểm danh) | E8: BTC hủy sự kiện |
|---|---|---|---|---|---|---|---|---|
| **NONE (Chưa ĐK)** | $\rightarrow$ **REGISTERED** *(Hợp lệ)*<br>Tăng `registered_count` | $\rightarrow$ **WAITING_LIST** *(Hợp lệ)*<br>Xếp số thứ tự queue | ❌ Bất hợp lệ<br>(404 NOT_FOUND) | ❌ Bất hợp lệ<br>(404 NOT_FOUND) | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |
| **REGISTERED** | ❌ Bất hợp lệ<br>(200 Idempotent) | ❌ Bất hợp lệ<br>(200 Idempotent) | $\rightarrow$ **CANCELLED** *(Hợp lệ)*<br>Hoàn quota & gọi E5 | ❌ Bất hợp lệ<br>(409 REGISTRATION_CLOSED) | ❌ Không áp dụng | $\rightarrow$ **REGISTERED (Done)**<br>Hoàn thành sự kiện | $\rightarrow$ **NO_SHOW** *(Hợp lệ)*<br>Áp dụng chế tài 7 ngày | $\rightarrow$ **EVENT_CANCELLED**<br>Không phạt No-show |
| **WAITING_LIST** | ❌ Bất hợp lệ<br>(200 Idempotent) | ❌ Bất hợp lệ<br>(200 Idempotent) | $\rightarrow$ **CANCELLED** *(Hợp lệ)*<br>Rút khỏi hàng đợi | ❌ Bất hợp lệ<br>(409 REGISTRATION_CLOSED) | $\rightarrow$ **REGISTERED** *(Hợp lệ)*<br>Nhận suất trống từ người hủy | ❌ Bất hợp lệ<br>(Chưa có suất) | $\rightarrow$ **CANCELLED / CLOSED**<br>Không phạt No-show | $\rightarrow$ **EVENT_CANCELLED**<br>Không phạt No-show |
| **CANCELLED** | ❌ Bất hợp lệ / Chặn<br>(409 ALREADY_CANCELLED) | ❌ Bất hợp lệ / Chặn | ❌ Bất hợp lệ<br>(409 ALREADY_CANCELLED) | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |
| **NO_SHOW** | ❌ Bất hợp lệ (Đã kết thúc) | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Bất hợp lệ | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Không áp dụng |

---

### 1.3. Quy tắc Nghiệp vụ cốt lõi (Registration Business Rules)
1. **BR-REG-01 (Giữ suất nguyên tử):** Đăng ký vào `REGISTERED` phải thực hiện atomic lock (`SELECT FOR UPDATE` hoặc Native Atomic Query) để đảm bảo không vượt quá `quota_total`.
2. **BR-REG-02 (Tự động đôn danh sách chờ):** Khi 1 sinh viên `REGISTERED` chuyển sang `CANCELLED`, hệ thống tự động tìm bản ghi `WAITING_LIST` có `created_at` sớm nhất thuộc cùng audience pool để chuyển thành `REGISTERED` mà không giảm `registered_count`.
3. **BR-REG-03 (Chống No-show oan):** Chỉ sinh viên có trạng thái `REGISTERED` mới bị xét `NO_SHOW`. Tuyệt đối không áp dụng chế tài No-show cho sinh viên ở trạng thái `WAITING_LIST` hoặc `EVENT_CANCELLED`.
4. **BR-REG-04 (Hạn chót hủy):** Sau thời điểm `registration_deadline`, mọi yêu cầu chuyển trạng thái sang `CANCELLED` đều bị từ chối với mã lỗi `409 REGISTRATION_CLOSED`.

---

## PHẦN 2: MÁY TRẠNG THÁI ĐIỂM DANH (ATTENDANCE STATE MACHINE)

### 2.1. Định nghĩa các Trạng thái
| Mã trạng thái | Tên tiếng Việt | Ý nghĩa nghiệp vụ | Quyền lợi & Ràng buộc |
|---|---|---|---|
| **NOT_CHECKED_IN** | Chưa điểm danh | Sinh viên đã đăng ký nhưng chưa thực hiện lần điểm danh nào. | Trạng thái mặc định trước giờ sự kiện. |
| **ATTEMPTED** | Đã thử điểm danh | Ghi nhận một lần gửi request điểm danh lên máy chủ (được log trong `attendance_attempts`). | Mỗi sinh viên được tối đa 3 lần thử thất bại trước khi bị khóa. |
| **PENDING** | Chờ xét duyệt | Sinh viên nộp ảnh minh chứng (Evidence) hoặc điểm danh hộ có lý do. | Chờ Ban Cán Sự (BCS) lớp duyệt; chưa được tính điểm rèn luyện. |
| **ACCEPTED** | Điểm danh hợp lệ | Đã xác thực thành công qua Dynamic QR / GPS hợp lệ hoặc Evidence được duyệt. | Ghi nhận chính thức, tích lũy điểm rèn luyện; mỗi sinh viên chỉ có **duy nhất 1 bản ghi** `ACCEPTED`. |
| **REJECTED** | Bị từ chối | Lượt điểm danh không hợp lệ (sai mã QR, ngoài bán kính GPS, hoặc BCS bác minh chứng). | Cho phép thử lại nếu số lần thử < 3; nếu bị BCS từ chối thì được quyền nộp khiếu nại (Appeal). |
| **APPEALED** | Đang khiếu nại | Sinh viên nộp đơn khiếu nại kết quả `REJECTED` lên BCS (Vòng 1) hoặc Admin (Vòng 2). | Hồ sơ khiếu nại đang mở; tạm hoãn chế tài No-show nếu khiếu nại hợp lệ. |

---

### 2.2. Ma trận Chuyển trạng thái Điểm danh (Attendance State Transition Matrix)

Bảng đối chiếu 2 chiều: **[Trạng thái hiện tại]** $\times$ **[Sự kiện kích hoạt / Hành động]**:

| Trạng thái hiện tại | A1: Quét Dynamic QR hợp lệ | A2: Quét QR sai / hết hạn (Lần 1-2) | A3: Quét QR sai lần thứ 3 (Chạm ngưỡng) | A4: Nộp minh chứng Evidence | A5: BCS duyệt Evidence (Approve) | A6: BCS bác Evidence (Reject) | A7: Sinh viên nộp Appeal (Vòng 1/2) | A8: Khiếu nại được chấp thuận | A9: Khiếu nại bị bác bỏ cuối cùng |
|---|---|---|---|---|---|---|---|---|---|
| **NOT_CHECKED_IN** | $\rightarrow$ **ACCEPTED** *(Hợp lệ)*<br>Ghi nhận điểm danh | $\rightarrow$ **REJECTED (Attempted)**<br>Tăng số lần thử lên 1-2 | $\rightarrow$ **REJECTED (Locked)**<br>Khóa điểm danh sự kiện | $\rightarrow$ **PENDING** *(Hợp lệ)*<br>Tải ảnh & chờ BCS duyệt | ❌ Bất hợp lệ<br>(Chưa nộp minh chứng) | ❌ Bất hợp lệ | ❌ Bất hợp lệ<br>(Chỉ appeal khi bị reject) | ❌ Không áp dụng | ❌ Không áp dụng |
| **PENDING** | ❌ Bị chặn<br>(Đang chờ duyệt evidence) | ❌ Bị chặn | ❌ Bị chặn | ❌ Bất hợp lệ<br>(Đã có evidence chờ duyệt) | $\rightarrow$ **ACCEPTED** *(Hợp lệ)*<br>Tích lũy điểm rèn luyện | $\rightarrow$ **REJECTED** *(Hợp lệ)*<br>Có lý do từ chối từ BCS | ❌ Bất hợp lệ<br>(Chưa có kết quả duyệt) | ❌ Không áp dụng | ❌ Không áp dụng |
| **REJECTED (Attempts < 3)** | $\rightarrow$ **ACCEPTED** *(Hợp lệ)*<br>Sinh viên thử lại đúng mã | $\rightarrow$ **REJECTED**<br>Tăng số lần thử lên 2 | $\rightarrow$ **REJECTED (Locked)**<br>Đạt 3 lần sai $\rightarrow$ Chặn hẳn | $\rightarrow$ **PENDING** *(Hợp lệ)*<br>Chuyển sang nộp minh chứng | ❌ Không áp dụng | ❌ Không áp dụng | $\rightarrow$ **APPEALED** *(Hợp lệ)*<br>Nếu do BCS từ chối | ❌ Không áp dụng | ❌ Không áp dụng |
| **REJECTED (Attempts = 3)** | ❌ Bất hợp lệ<br>(409 ATTEMPT_LIMIT_REACHED) | ❌ Bất hợp lệ<br>(409 ATTEMPT_LIMIT_REACHED) | ❌ Bất hợp lệ | $\rightarrow$ **PENDING** *(Hợp lệ)*<br>Nộp giải trình minh chứng | ❌ Không áp dụng | ❌ Không áp dụng | $\rightarrow$ **APPEALED** *(Hợp lệ)* | ❌ Không áp dụng | ❌ Không áp dụng |
| **ACCEPTED** | ❌ Bất hợp lệ<br>(409 ALREADY_CHECKED_IN) | ❌ Bất hợp lệ<br>(409 ALREADY_CHECKED_IN) | ❌ Bất hợp lệ | ❌ Bất hợp lệ<br>(Đã có điểm danh hợp lệ) | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Bất hợp lệ<br>(Không khiếu nại khi đã đạt) | ❌ Không áp dụng | ❌ Không áp dụng |
| **APPEALED** | ❌ Bị chặn (Đang xử lý đơn) | ❌ Bị chặn | ❌ Bị chặn | ❌ Bị chặn | ❌ Không áp dụng | ❌ Không áp dụng | ❌ Bất hợp lệ<br>(Đang có đơn mở) | $\rightarrow$ **ACCEPTED** *(Hợp lệ)*<br>Cập nhật kết quả thành công | $\rightarrow$ **REJECTED (Final)**<br>Đóng hồ sơ khiếu nại |

---

### 2.3. Quy tắc Nghiệp vụ cốt lõi (Attendance Business Rules)
1. **BR-ATT-01 (Giới hạn lần thử):** Mỗi sinh viên có tối đa **03 lần thử điểm danh thất bại** cho một sự kiện. Đến lần thứ 4, hệ thống từ chối ngay lập tức với mã lỗi `409 ATTEMPT_LIMIT_REACHED` kể cả khi xuất trình mã QR hợp lệ.
2. **BR-ATT-02 (Tính bất biến của kết quả Accepted):** Mỗi cặp `(event_id, student_id)` chỉ có tối đa **1 bản ghi trạng thái `ACCEPTED`** (được cưỡng chế bởi Unique Partial Index `uq_attendances_valid`).
3. **BR-ATT-03 (Điều kiện tiên quyết):** Sinh viên bắt buộc phải có trạng thái đăng ký là `REGISTERED` mới được thực hiện điểm danh. Nếu chưa đăng ký hoặc đang ở `WAITING_LIST`, hệ thống trả về lỗi `403 NOT_REGISTERED`.
4. **BR-ATT-04 (Khung giờ điểm danh):** Điểm danh chỉ được phép thực hiện khi thời gian hiện tại nằm trong khoảng `[attendance_open_at, attendance_close_at]`. Ngoài khung giờ này, hệ thống trả về `409 ATTENDANCE_NOT_OPEN`.
5. **BR-ATT-05 (Quy trình khiếu nại):** Khiếu nại được chấp thuận sẽ chuyển trạng thái attendance từ `APPEALED` $\rightarrow$ `ACCEPTED`, đồng thời tự động gỡ bỏ cờ No-show và hoàn trả quyền đăng ký cho sinh viên.

---

## PHẦN 3: DANH MỤC CA KIỂM THỬ CHUYỂN TRẠNG THÁI (TEST MATRIX SCENARIOS)

| Mã Test Case | Nhóm kiểm thử | Trạng thái ban đầu | Sự kiện kích hoạt | Điều kiện kiểm tra / Dữ liệu | Kết quả kỳ vọng (State & HTTP) | Kiểm tra Ràng buộc CSDL (DB Invariants) |
|---|---|---|---|---|---|---|
| **STTC-REG-01** | Registration | NONE | Đăng ký sự kiện | Sự kiện `PUBLISHED`, còn quota | $\rightarrow$ `REGISTERED`<br>HTTP 201 Created | `registrations.status = 'REGISTERED'`; `registered_count` tăng +1 |
| **STTC-REG-02** | Registration | NONE | Đăng ký sự kiện | Sự kiện `PUBLISHED`, hết quota | $\rightarrow$ `WAITING_LIST`<br>HTTP 200/201 | `registrations.status = 'WAITING_LIST'`; `registered_count` giữ nguyên |
| **STTC-REG-03** | Registration | REGISTERED | Đăng ký lại (Cùng SV) | Gửi lại request đăng ký | $\rightarrow$ `REGISTERED`<br>HTTP 200 OK (Idempotent) | Không tạo thêm dòng mới; `registered_count` không tăng |
| **STTC-REG-04** | Registration | REGISTERED | Bấm hủy đăng ký | Trước `registration_deadline` | $\rightarrow$ `CANCELLED`<br>HTTP 200 OK | `status = 'CANCELLED'`; `registered_count` giảm -1 |
| **STTC-REG-05** | Registration | REGISTERED & WAITING_LIST | SV1 hủy đăng ký | Có SV2 đang chờ trong Waiting List | SV1 $\rightarrow$ `CANCELLED`<br>SV2 $\rightarrow$ `REGISTERED` | SV2 được đôn lên tự động; `registered_count` không đổi |
| **STTC-REG-06** | Registration | REGISTERED | Bấm hủy đăng ký | Sau `registration_deadline` | ❌ Bị từ chối<br>HTTP 409 REGISTRATION_CLOSED | Trạng thái giữ nguyên `REGISTERED`; không hoàn quota |
| **STTC-REG-07** | Registration | REGISTERED | Sự kiện kết thúc | Không có bản ghi attendance hợp lệ | $\rightarrow$ `NO_SHOW` | `status = 'NO_SHOW'`; bảng hạn chế tạo cấm 7 ngày |
| **STTC-REG-08** | Registration | WAITING_LIST | Sự kiện kết thúc | Không được đôn suất | $\rightarrow$ `CANCELLED / CLOSED` | **Tuyệt đối KHÔNG chuyển thành `NO_SHOW`** |
| **STTC-ATT-01** | Attendance | NOT_CHECKED_IN | Quét QR hợp lệ | Đúng mã, trong khung giờ điểm danh | $\rightarrow$ `ACCEPTED`<br>HTTP 200 OK | `attendances.status = 'ACCEPTED'`; `attendance_attempts` ghi outcome=ACCEPTED |
| **STTC-ATT-02** | Attendance | NOT_CHECKED_IN | Quét QR không hợp lệ | Mã QR hết hạn / sai chữ ký (Lần 1) | $\rightarrow$ `REJECTED`<br>HTTP 409 QR_EXPIRED | `attendance_attempts` ghi outcome=REJECTED; lần thử = 1 |
| **STTC-ATT-03** | Attendance | REJECTED (2 lần sai) | Quét QR không hợp lệ lần 3 | Mã QR sai chữ ký lần 3 | $\rightarrow$ `REJECTED (Locked)`<br>HTTP 409 QR_BAD_SIGNATURE | `attendance_attempts` ghi 3 lần thất bại |
| **STTC-ATT-04** | Attendance | REJECTED (3 lần sai) | Quét mã QR hợp lệ lần 4 | Xuất trình mã QR đúng chuẩn | ❌ Bị từ chối ngay<br>HTTP 409 ATTEMPT_LIMIT_REACHED | Không tạo bản ghi `ACCEPTED`; từ chối ở tầng nghiệp vụ |
| **STTC-ATT-05** | Attendance | ACCEPTED | Quét QR lần 2 (Cùng SV) | Điểm danh lại với key mới | ❌ Bị từ chối<br>HTTP 409 ALREADY_CHECKED_IN | DB chặn bởi unique index `uq_attendances_valid` |
| **STTC-ATT-06** | Attendance | NOT_REGISTERED | Quét mã QR hợp lệ | Sinh viên chưa từng đăng ký sự kiện | ❌ Bị từ chối<br>HTTP 403 NOT_REGISTERED | Không tạo bản ghi attendance nào |
| **STTC-ATT-07** | Attendance | NOT_CHECKED_IN | Quét mã QR ngoài giờ | Trước giờ mở hoặc sau giờ đóng | ❌ Bị từ chối<br>HTTP 409 ATTENDANCE_NOT_OPEN | Không tạo bản ghi attendance; audit ghi nhận từ chối |
| **STTC-ATT-08** | Attendance | NOT_CHECKED_IN | Nộp ảnh Evidence | Trong thời hạn cho phép nộp minh chứng | $\rightarrow$ `PENDING`<br>HTTP 201 Created | `attendances.status = 'PENDING'`; đính kèm image URL |
| **STTC-ATT-09** | Attendance | PENDING | Ban Cán Sự duyệt (Approve) | BCS có thẩm quyền duyệt lớp mình | $\rightarrow$ `ACCEPTED`<br>HTTP 200 OK | `attendances.status = 'ACCEPTED'`; ghi audit người duyệt |
| **STTC-ATT-10** | Attendance | PENDING | Ban Cán Sự từ chối (Reject) | BCS ghi rõ lý do ảnh không hợp lệ | $\rightarrow$ `REJECTED`<br>HTTP 200 OK | `attendances.status = 'REJECTED'`; mở quyền nộp Appeal |
| **STTC-ATT-11** | Attendance | REJECTED | Sinh viên nộp Appeal | Trong vòng 48h sau sự kiện | $\rightarrow$ `APPEALED`<br>HTTP 201 Created | Tạo hồ sơ `appeals` (Round 1); tạm hoãn No-show |
| **STTC-ATT-12** | Attendance | APPEALED | Thẩm định Appeal thành công | Admin/CVHT chấp thuận giải trình | $\rightarrow$ `ACCEPTED`<br>HTTP 200 OK | `attendances.status = 'ACCEPTED'`; gỡ bỏ chế tài nếu có |

