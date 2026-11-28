# Thiết kế Nhất quán Dữ liệu: Atomic Quota, Idempotency & Chống Trùng Điểm danh (QE-8)

> **Jira:** `QE-8` ("Thiết kế attendance consistency: atomic quota, idempotency & duplicate prevention", 8 SP) — liên thông trực tiếp với `QE-54` (3 attempts & chống trùng), `QE-71` (Sprint 1 đăng ký), `QE-56`/`QE-57` (Sprint 1 QR động), `QE-12`/`QE-38`/`QE-39`/`QE-40` (multi-tier quota, waiting list, hủy & promote), `QE-55` (GPS), `QE-42`/`QE-43`/`QE-19` (Evidence & Proxy), và `QE-33`/`QE-76`–`QE-78` (Offline Sync).
> **Nguồn sự thật đối chiếu:**
> - Mã nguồn & Schema thực tế: [`V1__sprint1_core_schema.sql`](../../src/main/resources/db/migration/V1__sprint1_core_schema.sql), [`docs/Design/erd.md`](../Design/erd.md), [`docs/Design/v2-schema-blueprint.md`](../Design/v2-schema-blueprint.md), [`docs/Design/quota-rules.md`](../Design/quota-rules.md), [`docs/api/sprint1-contract.md`](../api/sprint1-contract.md), [`docs/api/error-codes.md`](../api/error-codes.md), [`docs/decisions.md`](../decisions.md).
> - Đặc tả chuẩn hóa: `SRS v1.1` ([`docs/spec/QNU_Event_SRS.docx`](../spec/QNU_Event_SRS.docx)), `Proposal v2` ([`docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx`](../spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx)).
> - Figma MVP (`HX4313AoLQX4zNidDz2Ixo`): luồng Đăng ký / Waiting List (`#03`, bỏ qua `#01` không đủ điều kiện, cắt hủy trước 12h), Màn hình QR Owner (`234/312`, xoay 10s), Scanner Sinh viên (`Lượt gửi xác nhận: 0/3 -> 1/3`), Offline Queue (`Đã lưu lượt quét` / `Chờ đồng bộ` / `Thử đồng bộ`), GPS (màn ví dụ `60m`; mặc định hệ thống `50m`), Evidence & Điểm danh thay nhiều người (BCS duyệt).
> - **Trạng thái cập nhật (`28/11/2026`):** Toàn bộ giả định `A-QE8-01..A-QE8-11` đã chuyển sang **`APPROVED`** theo các quyết định `D-4` → `D-9` và khớp 100% mô hình `event_pools` + `wait_order` trong [`v2-schema-blueprint.md`](../Design/v2-schema-blueprint.md). Chỉ tiêu chí chi tiết `accuracy`/vận tốc của `suspicious_flag` (`TBD-ATT-03`) giữ mở cho tinh chỉnh thử nghiệm.

---

## 1. Kiểm toán Hiện trạng Sprint 1 (Current Implementation Audit)

### 1.1 Ràng buộc & Chỉ mục DB đã có trong `V1__sprint1_core_schema.sql`

| Bảng | Ràng buộc / Chỉ mục / Trigger thực tế trong SQL | Ý nghĩa bảo đảm tính nhất quán |
|---|---|---|
| `events` | `quota_total int NOT NULL CHECK (quota_total > 0)`<br>`registered_count int NOT NULL DEFAULT 0`<br>`CONSTRAINT ck_events_quota CHECK (registered_count >= 0 AND registered_count <= quota_total)`<br>`qr_rotation_seconds int NOT NULL DEFAULT 10 CHECK (qr_rotation_seconds BETWEEN 5 AND 120)`<br>`qr_grace_seconds int NOT NULL DEFAULT 3 CHECK (qr_grace_seconds BETWEEN 0 AND 10)`<br>`version int NOT NULL DEFAULT 0` | Chặn tuyệt đối oversell hoặc âm quota ở tầng PostgreSQL (`ck_events_quota`); giữ cấu hình xoay QR và optimistic lock (`@Version Integer` trên `Event.java`). |
| `registrations` | `status CHECK (status IN ('REGISTERED','WAITING_LIST','CANCELLED','NO_SHOW'))`<br>`CREATE UNIQUE INDEX uq_registrations_active ON registrations(event_id, student_id) WHERE status IN ('REGISTERED','WAITING_LIST')`<br>`CREATE INDEX ix_registrations_student ON registrations(student_id, created_at DESC)` | Một sinh viên chỉ có tối đa 1 bản ghi đăng ký đang hiệu lực (`REGISTERED` hoặc `WAITING_LIST`) cho mỗi sự kiện, nhưng vẫn cho phép đăng ký lại sau khi `CANCELLED`. |
| `attendances` | `method CHECK (method IN ('DYNAMIC_QR','GPS','EVIDENCE','OFFLINE_QR','OFFLINE_GPS','PROXY_EVIDENCE'))`<br>`status CHECK (status IN ('PENDING','ACCEPTED','REJECTED'))`<br>`CREATE UNIQUE INDEX uq_attendances_valid ON attendances(event_id, student_id) WHERE status = 'ACCEPTED'` | Bảo đảm bất biến tối đa **01 bản ghi `ACCEPTED`** cho mỗi `(event_id, student_id)` trên mọi phương thức, trong khi cho phép nhiều bản ghi `PENDING`/`REJECTED` (ví dụ nộp lại evidence). |
| `attendance_attempts` | `UNIQUE (event_id, student_id, idempotency_key)`<br>`outcome CHECK (outcome IN ('ACCEPTED','REJECTED'))`<br>`counted boolean NOT NULL`, `http_status smallint NOT NULL`, `response_body jsonb NOT NULL` | Khóa duy nhất cho Idempotency Replay theo `(event_id, student_id, idempotency_key)` kèm cờ `counted` và payload phản hồi đã lưu. |
| `audit_logs` | `outcome CHECK (outcome IN ('SUCCESS','REJECTED','DENIED'))`<br>Trigger `trg_audit_logs_immutable` (`BEFORE UPDATE OR DELETE`) gọi `audit_logs_immutable()` | Bảo đảm nhật ký kiểm toán append-only, chống sửa/xóa ở mức CSDL. |

### 1.2 Hiện trạng Sprint 1 trên `main` vs Các mở rộng & hardening còn lại cho Sprint 2–3

#### A. Luồng Đăng ký (`RegistrationService`, `RegistrationRepository`, `RegistrationController`)
- **Đã hiện thực đúng trên `main`:**
  - `RegistrationService.register(UUID eventId)` sử dụng `TransactionTemplate` (giải phóng DB connection #1 trước khi gọi `auditService.record(...)` với `Propagation.REQUIRES_NEW` — đã khắc phục lỗi nghẽn HikariCP pool ở tải đồng thời), kiểm tra role `STUDENT`, `EventStatus.PUBLISHED`, `Instant.now(clock) <= registrationDeadline`, `student.getClassId() != null && event.getAudienceClassIds().contains(student.getClassId())`.
  - Gọi native query `RegistrationRepository.tryInsertRegistration(...)` (`INSERT INTO registrations ... ON CONFLICT (event_id, student_id) WHERE status IN ('REGISTERED', 'WAITING_LIST') DO NOTHING`). Nếu trả `0` (đã đăng ký), truy vấn `findActiveByEventAndStudent` và trả `200 OK` `{id, status, alreadyRegistered: true}` mà **không** chạm tới `events.registered_count`.
  - Nếu chèn được dòng mới (`rowsInserted == 1`), gọi native query `RegistrationRepository.reserveSeat(eventId)` (`UPDATE events SET registered_count = registered_count + 1 WHERE id = :eventId AND registered_count < quota_total`). Nếu trả `0`, thoát `TransactionTemplate`, ghi audit `REJECTED / CAPACITY_FULL` rồi ném `ApiException(ErrorCode.CAPACITY_FULL)` để rollback toàn bộ `INSERT` ở bước trước.
  - Được kiểm chứng bởi 7 test trong `MigrationIT` và 12 test trong `RegistrationIT` (bao gồm tranh suất cuối `quota=1`, tranh 5 suất với 30 luồng song song, và 10 request song song từ cùng 1 sinh viên).
- **Hardening & Điểm cần mở rộng cho Sprint 2–3:**
  1. **Kiểm toán `REQUIRES_NEW` trên luồng nóng khác (`P1`):** `RegistrationService` đã tách được audit khỏi `TransactionTemplate`, nhưng vẫn cần cân nhắc ghi success audit cùng ACID transaction hoặc `outbox_events` (`ARCH-A02`) để giảm xuống còn 1 transaction khi burst `100 req/s` (`QE-46`).
  2. **Hợp đồng `Idempotency-Key` trên `/registrations` (`A-QE8-03`):** `RegistrationController.register` chưa đọc header `Idempotency-Key`, trong khi FE (`src/api/events.ts:61-67`) có gửi header này; ở Sprint 2 (`QE-40`) cần hỗ trợ tùy chọn `Idempotency-Key` cho đăng ký/hủy, còn FE `RegistrationSlot` không dùng mã lỗi `ALREADY_REGISTERED` vì BE trả `200 OK {alreadyRegistered: true}`.
  3. **`reserveSeat` cập nhật trực tiếp `events.registered_count` mà không tăng `events.version`:** An toàn ở Sprint 1 vì `EventService.update` chặn sửa sự kiện khác `DRAFT`; từ Sprint 2 (`A-EVT-01`) khi sửa sức chứa sự kiện `PUBLISHED` phải dùng canonical lock order (`events(id) FOR UPDATE`).
  4. **Tính năng Sprint 2–3:** Multi-tier quota (`event_pools`, `QE-12`, `QE-38`), `WAITING_LIST` FIFO & auto-promote (`QE-39`, `QE-40`), hủy đăng ký với ngưỡng cắt 12h (`QE-40`), khóa đăng ký 7 ngày do `NO_SHOW` (`QE-14`).

#### B. Luồng Điểm danh QR (`AttendanceService`, `AttendanceAttemptService`, `QrTokenService`, `AttendanceController`)
- **Đã hiện thực đúng trên `main` (`QE-56`/`QE-57`/`QE-54`/`QE-90`):**
  - `QrTokenService` tạo/xác thực token định dạng `qr1.<eventId>.<epoch>.<base64url(HMAC-SHA256)>` dùng `MessageDigest.isEqual` chống timing attack, sử dụng `Clock` server (`ClockConfiguration`), fail-fast khi thiếu `QR_SIGNING_SECRET >= 32` byte ngoài profile `dev`/`demo`, hỗ trợ `epoch == currentEpoch` và `epoch == currentEpoch - 1` trong `graceSeconds` (`10s` / `3s` ở `application.yml`). Được phủ bởi **13 unit test trong `QrTokenServiceTest`**.
  - `AttendanceService.checkInWithQr` trên `main` **đã hiện thực**:
    * Khóa tuần tự từng cặp `(event_id, student_id)` bằng 1-arg advisory lock **`pg_advisory_xact_lock(hashtextextended(CAST(?1 AS text) || ':' || CAST(?2 AS text), 0))`** (`AttendanceService.java:403–412`) và re-check `findCachedAttempt` sau khi nhận lock (chặn in-flight collision).
    * Kiểm tra cửa sổ `[attendanceOpenAt, attendanceCloseAt]`, trạng thái `REGISTERED`, kiểm tra đã có `ACCEPTED`, đếm `counted = true >= 3`, trả `CheckInResult` trực tiếp (không ném `ApiException`), lưu `ATTEMPT_LIMIT_REACHED` với `counted = false` (giữ bộ đếm `<= 3`), và trả `errorDetails` (`method`, `attemptCount`, `maxAttempts`, `remainingAttempts`, `canRetry`).
    * `AttendanceAttemptService.recordAttempt` dùng `@Transactional` mặc định (`REQUIRED`) trong cùng transaction và đã nhận tham số `String method` động.
    * Ghi `AuditLog` với `tokenMasked` (`qrTokenService.fingerprint`) cắt gọn để tuân thủ `R7`; được phủ bởi **25 test trong `AttendanceIT`** (và **5 unit test trong `AttendanceServiceTest`**).
- **Hardening & Mở rộng cần hoàn tất cho Sprint 2–3 (`QE-54`, `QE-55`, `QE-33`, `QE-42`, `QE-19`):**
  1. **Tách `AuditService.record` (`REQUIRES_NEW`) khỏi `AttendanceService` TX:** Hiện `AttendanceService.checkInWithQr` là `@Transactional`, nên gọi `auditService.record(...)` ở dòng `275` / `380` vẫn tạm treo connection chính và mượn connection #2 trong HikariCP (`maximum-pool-size: 30`) — cần chuyển sang mô hình `TransactionTemplate` hoặc `outbox_events` (`A-QE8-01`) để đạt `P1`.
  2. **Mapping phòng vệ `DataIntegrityViolationException` & khóa hàng đăng ký `FOR UPDATE`:** Hiện tại `AttendanceService` dựa vào `pg_advisory_xact_lock` + `saveAndFlush`; cần bổ sung `@ExceptionHandler(DataIntegrityViolationException.class)` ánh xạ mọi va chạm `uq_attendances_valid` / `uq_attempts_idempotency` sang `409 ALREADY_CHECKED_IN` (tuyệt đối cấm lọt xuống `500 INTERNAL_ERROR`).
  3. **Mở rộng phương thức & `payload_hash` (`V2`):** Hiện `AttendanceService.processCheckIn` mới mở `"DYNAMIC_QR"` (`line 165`); Sprint 2–3 bổ sung `payload_hash` (`SHA-256`) vào `attendance_attempts` (`A-QE8-02`) và mở khóa cho `GPS`, `EVIDENCE`, `PROXY_EVIDENCE`, `OFFLINE_QR`, `OFFLINE_GPS`.
  4. **Kiểm thử đồng thời & FE UI Sync:** Bổ sung `AttendanceConcurrencyIT` đa luồng song song thật (`ExecutorService`) cho `QR` + `GPS` + BCS duyệt `EVIDENCE`/`PROXY_EVIDENCE`; FE `src/api/events.ts` giữ nguyên `Idempotency-Key` khi retry lỗi mạng và `EventQrPage.tsx` hiển thị bộ đếm tham dự trực tiếp (`234/312`).

---

## 2. Thiết kế Atomic Quota & Xử lý Burst Concurrency

### 2.1 Mô hình Nhất quán Đăng ký: Từ Sprint 1 (1 tầng) đến Sprint 2–3 (Multi-tier + Waitlist)

Theo SRS (`REQ-EVT-005..006`, `REQ-REG-004..016`, `UC010`, `UC011`, `UC013`, `NFR-QUAL-001`) và Proposal §4.2–4.3, §12 (`#9`):

1. **Nguyên tắc kiểm tra trước khi chạm quota (Server-Authoritative Preconditions — R9):**
   - Kiểm tra đồng bộ tại backend bằng `Clock` server (`NFR-PERF-008`): `event.status == PUBLISHED`, `now <= event.registrationDeadline`, tài khoản `ACTIVE`, dữ liệu hồ sơ bắt buộc (`student_code`, `class_id`, `cohort_year`, `faculty_name`) đầy đủ — **dữ liệu hồ sơ thiếu tuyệt đối không được mặc định là đủ điều kiện (`REQ-REG-005`)**, phạm vi lớp/đối tượng khớp sự kiện (`403 NOT_IN_AUDIENCE`), và không bị hạn chế `NO_SHOW` (`banned_until <= now`, nếu còn hạn → **`403 STUDENT_PENALIZED`**). Sinh viên đã `CANCELLED` trước đó **được phép đăng ký lại** (`D-5`).
2. **Sprint 1 Single-Tier Atomic Update (đang chạy):**
   - `tryInsertRegistration` (`ON CONFLICT DO NOTHING`) -> `reserveSeat` (`UPDATE events SET registered_count = registered_count + 1 WHERE id = :eventId AND registered_count < quota_total`).
   - Tại PostgreSQL mức cô lập mặc định `READ COMMITTED`, `UPDATE` giữ row lock trên bản ghi `events`, đánh giá lại điều kiện `registered_count < quota_total` trên phiên bản mới nhất vừa commit của hàng đó, đảm bảo không bao giờ vượt `quota_total` dù có hàng trăm luồng tranh suất cuối.
   - **Điều chỉnh bắt buộc cho Sprint 1/2 (Chống cạn HikariCP Pool):** Không gọi `AuditService.record` (`REQUIRES_NEW`) từ bên trong `@Transactional` đang giữ connection/row lock. Thành công -> ghi audit **trong cùng transaction** (`REQUIRED`) hoặc qua Transactional Outbox / `@TransactionalEventListener(AFTER_COMMIT)`; thất bại -> ném `ApiException` cho rollback xong kết nối gốc, sau đó tầng ngoài (hoặc façade không có `@Transactional`) mới ghi audit `REJECTED`.

### 2.2 Multi-Tier Quota (`event_pools`: `1 seat = 1 pool`) & Thứ tự Khóa Chống Deadlock (Sprint 2–3)

Theo quyết định `D-4` và đặc tả [`docs/Design/quota-rules.md`](../Design/quota-rules.md) (`QE-38`), mỗi sự kiện cấu hình:
- **Quota tổng (`events.quota_total` / `registered_count`)** là trần tuyệt đối, và
- **Danh sách Pool phân bổ (`event_pools`)**: `scope_type ∈ {'LOP','KHOA','KHOI','CLB','ALL'}`, `scope_ref`, `capacity >= 1`, `used`, `priority` (số nhỏ hơn ưu tiên hơn), `allow_overflow`, `is_mandatory` (`is_mandatory = true` luôn kèm `allow_overflow = false`), `reserved_until`.
- **Ràng buộc bất biến:** $\sum \text{capacity} \le \text{quota\_total}$ và **1 suất đăng ký `REGISTERED` thuộc về đúng 01 pool** (`registrations.pool_id`).

**Vấn đề Deadlock:** Nếu giao dịch $T_1$ khóa `Pool A -> Event E` trong khi giao dịch $T_2$ (hủy/promote hoặc điều chỉnh quota) khóa `Pool B -> Event E` hoặc khóa các hàng chờ theo thứ tự ngược nhau, PostgreSQL sẽ phát hiện vòng chờ (`40P01 deadlock_detected`).

**Giao thức Khóa Chuẩn hóa (Canonical Lock Ordering — Vô hiệu hóa Deadlock):**
Mọi luồng ghi làm thay đổi bộ đếm suất (`Register`, `Cancel`, `Auto-Promote`, `Adjust Quota` theo `A-EVT-01`) **bắt buộc** tuân thủ thứ tự khóa một chiều nghiêm ngặt:

```
1. SELECT ... FROM events WHERE id = :eventId FOR UPDATE          (Khóa gốc duy nhất theo eventId)
2. Kiểm tra đăng ký active hiện tại của student trong event:
   SELECT ... FROM registrations
   WHERE event_id = :eventId AND student_id = :studentId
     AND status IN ('REGISTERED', 'WAITING_LIST') FOR UPDATE
   -> Nếu đã tồn tại: trả về ngay trạng thái & wait_order hiện tại (giữ nguyên vị trí chờ, không chạm quota!).
3. Khóa danh sách event_pools khớp scope của sinh viên theo thứ tự priority tăng dần, cùng priority xếp theo id:
   SELECT * FROM event_pools
   WHERE event_id = :eventId
   ORDER BY priority ASC, id ASC FOR UPDATE
4. Duyệt từng pool khớp scope của sinh viên theo priority ASC:
   - Nếu pool chưa bị reserved_until khóa, còn chỗ (used < capacity) VÀ events.registered_count < quota_total:
     -> UPDATE event_pools SET used = used + 1 WHERE id = :poolId AND used < capacity;
     -> UPDATE events SET registered_count = registered_count + 1 WHERE id = :eventId;
     -> INSERT INTO registrations(..., status='REGISTERED', pool_id = :poolId); (hoặc UPDATE nếu tái kích hoạt bản ghi theo uq_registrations_active)
   - Nếu pool đầy & allow_overflow = true: thử pool kế tiếp theo priority ASC.
   - Nếu pool đầy & allow_overflow = false (đặc biệt mandatory) HOẶC hết mọi pool hợp lệ / quota_total đã đầy:
     -> Chọn pool ưu tiên cao nhất mà sinh viên thuộc scope làm targetPoolId;
     -> UPDATE events SET next_wait_order = next_wait_order + 1 ...;
     -> INSERT INTO registrations(..., status='WAITING_LIST', pool_id = :targetPoolId, waiting_at = :now, wait_order = allocatedOrder);
   - Nếu sinh viên không thuộc bất kỳ pool/audience nào được cấu hình -> rollback & trả 403 NOT_IN_AUDIENCE.
```

> **Phân tích hiệu năng dưới Burst (`Proposal P1` = 100 req/s trong 10s):** Khi mọi thao tác trong TX chỉ gồm SQL nội bộ được đánh chỉ mục (không gọi mạng ngoài, không upload file, không `REQUIRES_NEW` lồng nhau), thời gian giữ khóa `events(E)` tại PostgreSQL chỉ khoảng 1–3 ms, đạt thông lượng lý thuyết ~300–1,000 TX/s trên một sự kiện — đáp ứng tốt ngưỡng burst 100 req/s trong `P1` mà vẫn bảo toàn tuyệt đối thứ tự FIFO của `wait_order` và loại bỏ 100% khả năng deadlock.

### 2.3 Luồng Hủy Đăng ký (Ngưỡng 12h), Waiting List FIFO & Auto-Promotion

1. **Quy tắc Ngưỡng 12 giờ (`REQ-REG-012..014`, `UC011`, `D-5`):**
   - Áp dụng cho trạng thái **`REGISTERED`**: chỉ cho phép hủy khi **`Instant.now(clock).isBefore(event.getStartAt().minus(12, ChronoUnit.HOURS))`**. Tại thời điểm đúng bằng hoặc sau `start_at - 12h`: từ chối với mã lỗi **`409 CANCELLATION_CUTOFF_PASSED`** (`counted = false`, giữ nguyên trạng thái).
   - **Miễn trừ cho `WAITING_LIST` (`D-5`):** Sinh viên ở trạng thái `WAITING_LIST` được phép tự rút bất kỳ lúc nào (miễn mốc 12 giờ); chỉ chuyển sang `CANCELLED`, không hoàn quota và không kích hoạt promote.
2. **Tính chất Idempotent khi Hủy (`REQ-REG-013`, `UC011 4b`):**
   - Khóa `events(E) FOR UPDATE` và `registrations(regId) FOR UPDATE`.
   - Nếu trạng thái hiện tại đã là `CANCELLED`: trả ngay `200 OK` trạng thái `CANCELLED`, **không** trừ `registered_count` lần 2 và **không** kích hoạt promote lần 2.
   - Nếu trạng thái đang là `REGISTERED`: cập nhật thành `CANCELLED`, hoàn suất đúng pool gốc (`UPDATE event_pools SET used = used - 1 WHERE id = :poolId`), giảm `events.registered_count = registered_count - 1`, sau đó kích hoạt Auto-Promotion cho pool đó ngay trong cùng transaction.
3. **Auto-Promotion FIFO & Quy tắc Bỏ qua `#01` Không Đủ Điều Kiện (`REQ-REG-015..016`, `UC011 5–6`, `UC012 A2`, `D-4`, `D-5`):**
   - Vẫn tiếp tục auto-promote cho người đã nằm sẵn trong `WAITING_LIST` kể cả **sau `registration_deadline` miễn còn trước mốc `start_at - 12h`** và sự kiện chưa `CANCELLED` (`D-5`, đóng `TBD-REG-02`). Khi tăng `quotaTotal`/`capacity` của sự kiện đã `PUBLISHED` có hàng chờ (`A-EVT-01`), hệ thống cũng kích hoạt vòng lặp promote này trong cùng giao dịch.
   - Truy vấn ứng viên của đúng `pool_id` vừa có suất trống theo thứ tự FIFO:
     ```sql
     SELECT r.* FROM registrations r
     WHERE r.event_id = :eventId AND r.pool_id = :poolId AND r.status = 'WAITING_LIST'
     ORDER BY r.wait_order ASC
     FOR UPDATE SKIP LOCKED
     ```
   - Duyệt tuần tự `#01, #02, #03...`:
     - Tái xác thực đầy đủ (`REQ-REG-015`): user còn `ACTIVE`, không bị `NO_SHOW` restriction đang hiệu lực (`student_restrictions`), lớp vẫn thuộc scope pool & `event_audience`, `events.registered_count < quota_total` và `pool.used < pool.capacity`.
     - **Nếu `#01` không đủ điều kiện** (bị ban `NO_SHOW`, khóa tài khoản, hay chuyển lớp ngoài scope): **bỏ qua `#01`** và ghi audit `WAITLIST_PROMOTE_SKIPPED`, rồi kiểm tra tiếp `#02`!
     - **Nếu `#02` đủ điều kiện:** chuyển `#02` → `REGISTERED`, tăng `event_pools.used` (+1) và `events.registered_count` (+1), ghi `audit_logs` (`WAITLIST_PROMOTED`) và phát bản ghi `outbox_events` (`AFTER_COMMIT`) để thông báo cho `#02`. Dừng vòng lặp.
     - Nếu không có ứng viên nào hợp lệ: suất trống từ người hủy được giữ nguyên (`used` và `registered_count` giữ giá trị đã hoàn).

### 2.4 Sơ đồ Tuần tự: Tranh chấp Burst / Suất cuối & Waiting List

```mermaid
sequenceDiagram
    autonumber
    participant S1 as SV1 - 1 slot cuoi
    participant S2 as SV2 - Den sau 1ms
    participant S1Dup as SV1 - Click lap
    participant API as RegistrationService
    participant DB as PostgreSQL

    par Burst tranh suat cuoi
        S1->>API: POST /events/{id}/registrations
    and
        S2->>API: POST /events/{id}/registrations
    end

    API->>DB: TX1: SELECT events WHERE id=? FOR UPDATE
    Note over DB: TX1 giu row lock events(id); TX2 cho lock
    API->>DB: TX1: Check active reg (khong co) + Lock pools (priority ASC, id ASC)
    API->>DB: TX1: registered_count (99) < quota_total (100) & pools OK
    API->>DB: TX1: UPDATE events SET registered_count=100; INSERT reg(REGISTERED)
    DB-->>API: TX1 COMMIT
    API-->>S1: 201 Created {status: "REGISTERED"}

    Note over DB: TX2 nhan lock events(id) voi registered_count=100
    API->>DB: TX2: Check active reg (khong co) + Check quota -> FULL (100/100)
    alt Sprint 1 (No Waitlist)
        DB-->>API: TX2 ROLLBACK (khong de lai dong mo coi)
        API-->>S2: 409 Conflict CAPACITY_FULL
    else Sprint 2 (Waitlist FIFO)
        API->>DB: TX2: next_wait_order++ (#01), INSERT reg(WAITING_LIST, order=1)
        DB-->>API: TX2 COMMIT
        API-->>S2: 201 Created {status: "WAITING_LIST", waitPosition: 1}
    end

    S2->>API: Click dang ky lan 2 (Duplicate click)
    API->>DB: TX3: SELECT active reg (S2, event) -> tim thay order=1
    Note over DB: uq_registrations_active bao ve; khong doi wait_order, khong tru quota
    API-->>S2: 200 OK {status: "WAITING_LIST", waitPosition: 1, alreadyRegistered: true}

    S1Dup->>API: Click dang ky lan 2 (SV1 da REGISTERED)
    API->>DB: TX4: SELECT active reg (S1, event) -> tim thay REGISTERED
    API-->>S1Dup: 200 OK {status: "REGISTERED", alreadyRegistered: true}
```

---

## 3. Bảo đảm Duy nhất & Chống Trùng Điểm danh (Duplicate Prevention)

### 3.1 Bất biến Duy nhất Toàn cục qua Mọi Phương thức

Theo `BR-ATT-01`, `REQ-ATT-022`, `OTH-DATA-001` (`#9`) và `V1__sprint1_core_schema.sql`:
- **Ràng buộc vật lý bất biến:** `CREATE UNIQUE INDEX uq_attendances_valid ON attendances(event_id, student_id) WHERE status = 'ACCEPTED';`
- **Tính chất liên phương thức:** Chỉ mục duy nhất không chứa cột `method`. Do đó, dù sinh viên gửi từ `DYNAMIC_QR`, `GPS`, `EVIDENCE`, `OFFLINE_QR`, `OFFLINE_GPS`, hay `PROXY_EVIDENCE` (điểm danh hộ nhiều người), PostgreSQL **ngăn chặn tuyệt đối** việc tồn tại >= 2 bản ghi `ACCEPTED` cho cùng một `(event_id, student_id)`.

| Tình huống chéo phương thức | Hành xử chuẩn hóa |
|---|---|
| Đã có `ACCEPTED` (bất kỳ phương thức nào) -> gửi mới `QR` / `GPS` / `OFFLINE_QR` / `OFFLINE_GPS` (với `Idempotency-Key` mới) | Trả `409 ALREADY_CHECKED_IN`, **không** tạo dòng `attendances` mới, **không** tính vào 3 attempt (`counted = false`). |
| Đã có `ACCEPTED` -> gửi mới `EVIDENCE` hoặc bị bạn chọn trong `PROXY_EVIDENCE` | Từ chối ngay sinh viên đó với lý do `ALREADY_CHECKED_IN` (trong batch proxy: các sinh viên khác trong danh sách vẫn được tạo `PENDING` bình thường, kết quả trả về chi tiết từng sinh viên). |
| Sinh viên có 1 dòng `EVIDENCE` đang `PENDING` -> quét thành công `DYNAMIC_QR` (hoặc `GPS`) trong giờ | `DYNAMIC_QR`/`GPS` chèn được `ACCEPTED` (`PENDING` không nằm trong `uq_attendances_valid`). Hệ thống **chấp nhận `ACCEPTED`** và tự động chuyển các dòng `PENDING` dư thừa sang `REJECTED` (`reason = SUPERSEDED_BY_ACCEPTED`, `D-7`). |
| BCS A và BCS B cùng bấm duyệt 1 dòng `PENDING` hoặc duyệt 2 dòng `PENDING` khác nhau của cùng 1 SV | Cập nhật điều kiện `UPDATE attendances SET status='ACCEPTED' WHERE id=:id AND status='PENDING'` kết hợp bắt lỗi `uq_attendances_valid` -> trả `409 ALREADY_CHECKED_IN` (hoặc replay idempotent nếu cùng trạng thái). |

### 3.2 Xử lý Tranh chấp Điểm danh Đồng thời (Race -> `409 ALREADY_CHECKED_IN`, cấm `500`)

Để khắc phục lỗi `500 INTERNAL_ERROR` đang tồn tại ở Sprint 1 (`AttendanceService.java:207` + `GlobalExceptionHandler`):

1. **Tầng 1 — Khóa tuần tự theo `(event_id, student_id)` trong PostgreSQL (`AttendanceService.java:403–412`):**
   Đầu mỗi giao dịch check-in (QR, GPS, Offline Sync item), gọi 1-arg 64-bit advisory transaction lock:
   ```sql
   SELECT pg_advisory_xact_lock(
       hashtextextended(CAST(:eventIdText AS text) || ':' || CAST(:studentIdText AS text), 0)
   );
   ```
   và (khi đã đăng ký) khóa hàng đăng ký tương ứng:
   ```sql
   SELECT * FROM registrations
   WHERE event_id = :eventId AND student_id = :studentId AND status = 'REGISTERED'
   FOR UPDATE;
   ```
   Tất cả request đồng thời của cùng 1 sinh viên trên 1 sự kiện sẽ xếp hàng tuần tự tại DB (~1–2ms/request), loại bỏ hoàn toàn race condition giữa đọc-rồi-ghi ở bước idempotency, kiểm tra `ACCEPTED`, và đếm 3 lần thử.
2. **Tầng 2 — Chèn an toàn & Ánh xạ `DataIntegrityViolationException`:**
   - Dùng `INSERT INTO attendances (...) VALUES (...) ON CONFLICT (event_id, student_id) WHERE status = 'ACCEPTED' DO NOTHING` (kiểm tra `affectedRows == 0`), **HOẶC** bổ sung `@ExceptionHandler(DataIntegrityViolationException.class)` kiểm tra tên constraint/index (`uq_attendances_valid`) để trả về `409 Conflict` (`ErrorCode.ALREADY_CHECKED_IN`) — bảo đảm không bao giờ lọt xuống `500 INTERNAL_ERROR`.

---

## 4. Giao thức Idempotency & Replay (Thống nhất `QE-54` & `QE-33`)

### 4.1 Hợp đồng Key & Lưu trữ Kết quả

- **Khóa duy nhất:** `UNIQUE (event_id, student_id, idempotency_key)` trên bảng `attendance_attempts` (`V1__sprint1_core_schema.sql:112`).
- **Quy tắc sinh key phía Client (`FE / Mobile / Offline Queue`):**
  - Sinh **1 UUID v4 cho mỗi hành động điểm danh của người dùng** (ví dụ: 1 lần quét camera ra 1 chuỗi QR, 1 lần bấm nút GPS, 1 lần bấm gửi Evidence, 1 mục trong Batch Proxy, hoặc lúc lưu bản ghi vào hàng đợi Offline).
  - **BẮT BUỘC tái sử dụng nguyên `Idempotency-Key` đó** khi retry do rớt mạng, timeout, `5xx`, hoặc nhấn đúp nút gửi (cần sửa `checkInQr` trong FE `src/api/events.ts:77-78` để giữ key qua TanStack Query retry / offline queue, chỉ tạo key mới khi người dùng quét một mã mới hoặc chủ động thực hiện lượt thử mới sau khi nhận lỗi nghiệp vụ như `422 QR_EXPIRED`).
- **Bảo vệ sai lệch payload (`A-QE8-02` — Approved):** Lưu thêm `payload_hash` (`SHA-256`) của `(method, canonicalPayload)` trong `attendance_attempts` (`V2`). Nếu cùng `(event_id, student_id, idempotency_key)` nhưng `payload_hash` khác -> từ chối **`400 VALIDATION_FAILED`** (`IDEMPOTENCY_PAYLOAD_MISMATCH`), tránh trường hợp bug client tái sử dụng key cũ cho token QR khác mà nhận nhầm replay.
- **Bảo mật dữ liệu replay (`R7`):** `response_body` lưu JSON phản hồi đầy đủ (bao gồm `code`, `message`, `details` cho lỗi hoặc `status`, `acceptedAt` cho thành công), **tuyệt đối không lưu token QR thô, JWT hay tọa độ nhạy cảm ngoài cấu trúc DTO chuẩn**.

### 4.2 Luồng Thực thi 1-Transaction & Xử lý Retry Đang Bay (In-Flight Concurrency)

Khắc phục triệt để bug `REQUIRES_NEW` + in-flight collision ở Sprint 1:

```
Request (eventId, studentId, idempotencyKey, payload)
  │
  ├─► [1] Validate cú pháp header Idempotency-Key (không rỗng, <= 100 ký tự) -> sai trả 400 VALIDATION_FAILED (không lưu DB)
  │
  ├─► [2] Fast-path Read (không khóa): tìm trong attendance_attempts theo (eventId, studentId, idempotencyKey)
  │       └─ Nếu ĐÃ CÓ: kiểm tra payload_hash (nếu bật) -> trả nguyên vẹn (http_status, response_body)
  │          (kể cả ACCEPTED 200, PENDING 202, hay REJECTED 403/409/422 theo UC019 4b.1). KHÔNG tăng attempt!
  │
  └─► [3] Bắt đầu 1 @Transactional duy nhất (sử dụng TransactionTemplate hoặc Outbox, KHÔNG lồng REQUIRES_NEW):
        ├─ 3a. Gọi pg_advisory_xact_lock(hashtextextended(CAST(:eventIdText AS text) || ':' || CAST(:studentIdText AS text), 0))
        ├─ 3b. Re-check attendance_attempts theo (eventId, studentId, idempotencyKey)
        │      └─ Nếu luồng song song vừa commit trước 1ms -> trả nguyên vẹn (http_status, response_body)!
        ├─ 3c. Kiểm tra lần lượt:
        │      - Event PUBLISHED & cửa sổ điểm danh hợp lệ (hoặc Offline sync window) -> else REJECTED (counted=false, 409)
        │      - Registrations status = 'REGISTERED' (FOR UPDATE) -> else REJECTED (counted=false, 403)
        │      - Attendances chưa có 'ACCEPTED' -> else REJECTED (counted=false, 409 ALREADY_CHECKED_IN)
        │      - Đếm counted=true < 3 -> else REJECTED (counted=false, 409 ATTEMPT_LIMIT_REACHED)
        │      - Xác thực nghiệp vụ (QR / GPS / Evidence / Offline) -> nếu lỗi xác thực: REJECTED (counted=true, 422/400)
        ├─ 3d. Nếu hợp lệ: INSERT INTO attendances ... ON CONFLICT DO NOTHING (nếu 0 dòng -> REJECTED ALREADY_CHECKED_IN)
        ├─ 3e. INSERT INTO attendance_attempts (..., http_status, response_body) -- TRONG CÙNG TX!
        ├─ 3f. INSERT INTO audit_logs / outbox -- TRONG CÙNG TX (hoặc AFTER_COMMIT)!
        └─ 3g. Service TRẢ VỀ CheckInResult(httpStatus, body) (KHÔNG ném ApiException để Spring COMMIT toàn TX!)
               -> AttendanceController trả ResponseEntity.status(result.httpStatus()).body(result.body()).
```

> **Lợi ích cốt lõi:**
> 1. **Không bao giờ chia tách trạng thái (`split-brain`):** `attendances`, `attendance_attempts` và `audit_logs` cùng commit hoặc cùng rollback nguyên tử trong **1 kết nối DB duy nhất**.
> 2. **Retry khi đang xử lý (in-flight):** Luồng đến sau cùng `Idempotency-Key` chờ `pg_advisory_xact_lock` vài mili-giây rồi đọc được kết quả vừa commit ở bước 3b và replay chính xác, không dính lỗi `UNIQUE` violation (`500`).
> 3. **Thống nhất liên phương thức (`QE-54` & `QE-33`):** `AttendanceAttemptService.recordAttempt` đã hỗ trợ tham số `method` động thay cho `"DYNAMIC_QR"` cố định, áp dụng đồng nhất cho:
>    - `POST /events/{id}/attendance/qr` (`DYNAMIC_QR`)
>    - `POST /events/{id}/attendance/gps` (`GPS`)
>    - `POST /events/{id}/attendance/evidence` (`EVIDENCE`, replay trả `202`/`200 PENDING` kèm `attendanceId`, không tạo file/bản ghi trùng)
>    - `POST /events/{id}/attendance/proxy` (`PROXY_EVIDENCE`: lưu idempotency cấp batch theo `(event_id, submitter_id, idempotency_key)` hoặc từng `(event_id, target_student_id, idempotency_key)`)
>    - `POST /attendance/offline-sync` (mỗi phần tử offline mang `idempotencyKey` riêng sinh từ lúc quét offline -> server xử lý độc lập từng item, retry cả batch không bao giờ lặp lại item đã đồng bộ thành công hay thất bại ở đợt trước — `UC019 4b.1`).

---

## 5. Chính sách Giới hạn 3 Lần Thử (3-Attempt Cap — `QE-54` & `TBD-ATT-02`)

### 5.1 Ranh giới Chính xác: `0, 1, 2, 3` và Lần thứ 4

Theo `BR-ATT-02`, `REQ-ATT-021`, `UC015`, `UC018` (`#9`) và màn hình Figma (`Lượt gửi xác nhận: 0/3` -> `1/3`):
- Số lần thử tiêu tốn được tính bằng:
  `attemptsUsed = COUNT(*) FROM attendance_attempts WHERE event_id = :eventId AND student_id = :studentId AND counted = true`
- **Biên giá trị:**
  - `attemptsUsed = 0`: Chưa thử sai lần nào (`0/3`, còn 3 lượt).
  - `attemptsUsed = 1`: Thử sai 1 lần (`1/3`, còn 2 lượt).
  - `attemptsUsed = 2`: Thử sai 2 lần (`2/3`, còn 1 lượt).
  - `attemptsUsed = 3`: Đã dùng hết 3 lượt (`3/3`, còn 0 lượt).
  - **Lần thử mới thứ 4** (với `Idempotency-Key` mới, kể cả gửi token QR hoặc GPS **hoàn toàn hợp lệ**): bị chặn ngay tại bước kiểm tra giới hạn **trước khi** xác thực QR/GPS, trả về **`409 ATTEMPT_LIMIT_REACHED`** *(quy chuẩn thống nhất thay thế `429` trong `SRS v1.0`; đã đồng bộ lên `SRS v1.1`, `error-codes.md` và `AttendanceIT` — `A-QE8-05` Approved)*.
- **Sửa lỗi đếm dư ở Sprint 1 (`AttendanceService.java:170`):** Bản ghi từ chối `ATTEMPT_LIMIT_REACHED` ở lần thứ 4+ **BẮT BUỘC ghi `counted = false`** (vì lượt này bị chặn ở cổng chứ không tiêu thụ thêm lượt thử), giữ bất biến `0 <= attemptsUsed <= 3` vĩnh viễn và giúp UI hiển thị chuẩn `3/3`.
- **Phản hồi hỗ trợ UI Figma:** Bổ sung/giữ đồng bộ `attemptCount`, `maxAttempts: 3`, `remainingAttempts`, `canRetry` trong `/attendance/me` (`QE-98`) và `ApiError.details` (`A-QE8-04` Approved) để FE hiển thị `Lượt gửi xác nhận: x/3` đồng bộ với trạng thái thật từ server (`R9`).

### 5.2 Ma trận Phân loại `counted = true` vs `counted = false`

> **Đã chốt `TBD-ATT-02` (`D-7`, `28/11/2026`):** Giới hạn 3 lượt (`counted = true`) **dùng chung** cho nhóm xác thực tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`); **`EVIDENCE` là kênh dự phòng riêng** không bị chặn bởi trần 3 lượt lỗi:

| Tình huống / Kết quả | `counted` | HTTP Status | Ghi chú & Nguồn |
|---|:-:|:-:|---|
| Replay cùng `Idempotency-Key` đã xử lý trước đó | **`false`** | Trả lại `http_status` cũ | Bắt buộc theo `REQ-ATT-023`, `UC018`, `UC019 4b.1` (chạy ở Sprint 1) |
| Điểm danh thành công (`ACCEPTED`) | **`false`** | `200` | Không tiêu tốn lượt lỗi (chạy ở Sprint 1) |
| Nộp Evidence chờ duyệt (`PENDING`) | **`false`** | `202` / `200` | Chuyển hàng đợi BCS (`D-2`, `D-7`) |
| Thiếu/sai format `Idempotency-Key`, JSON lỗi (`VALIDATION_FAILED`) | **`false`** | `400` | Lỗi đầu vào / hạ tầng (`error-codes.md`) |
| Chưa đăng nhập / phiên hết hạn / sai role (`UNAUTHENTICATED`, `SESSION_REVOKED`, `FORBIDDEN`) | **`false`** | `401` / `403` | Cổng bảo mật (chạy ở Sprint 1) |
| Ngoài cửa sổ điểm danh / sự kiện chưa publish (`ATTENDANCE_NOT_OPEN`, `EVENT_NOT_PUBLISHED`) | **`false`** | `409` | Cổng trạng thái sự kiện (`A-04` Approved) |
| Chưa đăng ký hợp lệ (`NOT_REGISTERED` — gồm cả `WAITING_LIST` / `CANCELLED`) | **`false`** | `403` | Cổng đăng ký (`A-04` Approved) |
| Đã có điểm danh `ACCEPTED` (`ALREADY_CHECKED_IN`) | **`false`** | `409` | Chống trùng (`A-04` Approved) |
| Đã hết 3 lượt trước đó (`ATTEMPT_LIMIT_REACHED`) | **`false`** | `409` | Chặn ở cổng, không tăng quá 3 (`A-QE8-04`) |
| Lỗi kỹ thuật hệ thống / DB timeout (`5xx` chưa commit) | **`false`** | `500` / `503` | Lỗi hạ tầng, rollback toàn bộ (`REQ-ATT-023`) |
| Lỗi QR (`QR_MALFORMED`, `QR_BAD_SIGNATURE`, `QR_WRONG_EVENT`, `QR_EXPIRED`, `QR_FROM_FUTURE`) | **`true`** | `422` | Đã chạy ở Sprint 1 (`AttendanceIT`, Figma `1/3` khi QR hết hạn) |
| GPS ngoài bán kính (`GPS_OUT_OF_RANGE`) | **`true`** | `422` | Quy chuẩn `D-7` — sai xác thực vị trí |
| GPS đáng ngờ (`isMocked` / accuracy bất thường) nhưng trong vùng & hợp lệ khác | **`false`** | `200` | **`ACCEPTED` + `suspicious_flag = true`** theo `BR-ATT-07`, `REQ-ATT-007` & Figma |
| Offline QR/GPS sync về server bị từ chối xác thực (`OFFLINE_SYNC_WINDOW_EXPIRED` / sai vị trí / sai chữ ký) | **`true`** | `422` | `D-6`, `D-7` — tối đa 3 lượt; retry mạng cùng key vẫn `false` |
| **Phạm vi trần 3 lượt giữa QR/GPS vs Evidence** | — | — | **`APPROVED (D-7)`:** 3 lượt áp dụng chung cho các kênh tự động hóa (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`) trên mỗi `(event_id, student_id)`; khi hết 3 lượt, sinh viên vẫn có thể nộp `EVIDENCE` (chờ BCS duyệt) hoặc gửi `Appeal`. |

---

## 6. Thiết kế Dynamic QR, GPS & Cửa sổ Đồng bộ Offline (`QE-56`, `QE-57`, `QE-33`)

### 6.1 Token QR Động (`QrTokenService`) & Đồng bộ Cấu hình

- **Định dạng hiện tại (`QrTokenService.java`):** `qr1.<eventId>.<epoch>.<base64url(HMAC-SHA256(secret, "qr1." + eventId + "." + epoch))>` với `epoch = floor(nowEpochSeconds / rotationSeconds)`.
- **Xác thực thời gian dựa trên `Clock` server (`ClockConfiguration`):**
  - `epoch > currentEpoch` -> `422 QR_FROM_FUTURE`.
  - `epoch == currentEpoch` -> hợp lệ.
  - `epoch == currentEpoch - 1` và `now.getEpochSecond() - (currentEpoch * rotationSeconds) <= graceSeconds` -> hợp lệ (mặc định `rotationSeconds = 10`, `graceSeconds = 3`; trong số học giây nguyên, hiệu lực tối đa từ cuối epoch trước sang đầu epoch mới lên đến ~3.99s, đã được test tại `QrTokenServiceTest#verifyPreviousEpochWithinGraceSuccess`).
  - Các trường hợp còn lại -> `422 QR_EXPIRED`.
  - Yêu cầu multi-node (`NFR-PERF-009` `[Đề xuất]`): sai lệch đồng hồ giữa các node BE `<= 1 giây` (NTP).
- **3 Điểm chuẩn hóa kỹ thuật (`A-QE8-10` — Approved `28/11/2026` & Security `R7`):**
  1. **Đồng bộ cấu hình `application.yml` (đã sửa trên `main`):** Bản cũ trước đây từng đặt `15` giây, nay `application.yml:61` đã thống nhất `app.qr.epoch-seconds: ${QR_EPOCH_SECONDS:10}` khớp với `V1__sprint1_core_schema.sql:50`, `Event.java`, `EventService.java`, và FE `EventQrPage.tsx` (`10s` / `3s` grace).
  2. **Fail-fast `QR_SIGNING_SECRET` trong `QrTokenService.java:28–39` (đã sửa trên `main`):** Đã loại bỏ chuỗi fallback cố định; ngoài profile `dev`/`demo`, ứng dụng từ chối khởi động (`IllegalStateException`) nếu thiếu `QR_SIGNING_SECRET` (`>= 32` byte, tách biệt hoàn toàn với `JWT_SECRET`).
  3. **Liên kết `qr1` vs `random_nonce` (`SRS UC015` / `QE-44`):** `qr1` không có `random_nonce` nên hai màn hình owner hoặc hai node BE cùng hiển thị một mã QR đồng nhất trong mỗi 10s. Định dạng `qr1` giữ ổn định cho Sprint 1–2; khi cần `nonce` cho mỗi phiên chiếu có thể nâng cấp sang `qr2.<eventId>.<epoch>.<nonce>.<sig>` (`A-QE8-10` — Approved).

### 6.2 GPS Check-in (`QE-55`)

- **Tham số cấu hình theo sự kiện (`REQ-ATT-007`, `D-7`):** Mỗi sự kiện lưu `gps_lat`, `gps_lng`, `allowed_radius_meters` (`10–500m`), **mặc định `50m`** (khớp `SRS REQ-EVT-009`; giá trị `60m` trên Figma là ví dụ cấu hình cho một sự kiện cụ thể).
- **Quy tắc server (`BR-ATT-07`, `REQ-ATT-007`):**
  - Kiểm tra giá trị hữu hạn (`lat ∈ [-90, 90]`, `lng ∈ [-180, 180]`, `accuracy >= 0` hữu hạn).
  - Tính khoảng cách Haversine trên server; nếu `> allowed_radius_meters` -> từ chối **`422 GPS_OUT_OF_RANGE`** (`counted = true`).
  - Nếu **trong bán kính** nhưng có dấu hiệu bất thường (`isMocked == true`, `accuracy > threshold` — `TBD-ATT-03` giữ mở để tinh chỉnh): vẫn lưu `status = 'ACCEPTED'` nhưng gắn `suspicious_flag = true` và `suspicious_reason` để Owner/BCS hậu kiểm (đúng Figma & `BR-ATT-07`).

### 6.3 Hàng đợi Offline & Cửa sổ Đồng bộ (`QE-33`, `QE-76`, `QE-77`, `QE-78`, `D-6`)

> **Quyết định `D-6` (`28/11/2026`, đóng `TBD-ATT-01`):** Tuyệt đối **cấm Offline Prefetch** (không tải trước chuỗi QR tương lai về máy SV). Hỗ trợ **Student Offline Store-and-Forward**: Owner vẫn chiếu QR động 10s bình thường; SV mất sóng quét mã đang chiếu lưu cục bộ (`PENDING_SYNC`), chỉ đồng bộ khi có mạng trong cửa sổ **`OFFLINE_SYNC_MAX_WINDOW = 24 giờ`** sau `attendance_close_at` (và trước khi job `NO_SHOW` chạy). Quá cửa sổ → trả **`422 OFFLINE_SYNC_WINDOW_EXPIRED`** (`counted = true`).

1. **Dữ liệu hàng đợi cục bộ (`IndexedDB` / `SQLite` phân vùng theo `(userId, eventId)` — `UC019`):**
   - Mỗi bản ghi hàng đợi gồm: `idempotencyKey` (UUID v4 tạo ngay tại thời điểm quét/chụp offline), `eventId`, `method` (`OFFLINE_QR` | `OFFLINE_GPS` | `EVIDENCE`), `payload` (chuỗi QR nguyên bản / tọa độ + `integrityHash` / blob ảnh nén), `clientCapturedAt`, `syncStatus` (`PENDING_SYNC` | `SYNCED` | `FAILED_PERMANENT`).
   - Trên UI (Figma): hiển thị `Đã lưu lượt quét` -> `Chờ đồng bộ` -> nút `Thử đồng bộ` khi có mạng.
2. **Xác thực Offline trên Server mà không tin đồng hồ Client (`R9`, `NFR-PERF-008`):**
   - **Với `OFFLINE_QR` (`QE-76`):** Chuỗi QR `qr1.<eventId>.<epoch>.<sig>` đã mang **mốc thời gian được server ký HMAC**! Server kiểm tra cờ `events.allow_offline_sync = true`, xác thực SV đã `REGISTERED` trước đó và tính:
     `tokenStart = Instant.ofEpochSecond(epoch * rotationSeconds)`
     `tokenEnd = Instant.ofEpochSecond((epoch + 1) * rotationSeconds + graceSeconds)`
     Vì client không thể giả mạo `epoch` mà không làm hỏng HMAC, server xác minh được mã QR đó **thực sự được phát hành trong thời gian sự kiện mở điểm danh** (`tokenEnd >= attendanceOpenAt && tokenStart <= attendanceCloseAt && tokenStart <= serverNow`) mà **không cần tin `clientCapturedAt`**.
     - Điều kiện hợp lệ về độ trễ đồng bộ (`OFFLINE_SYNC_MAX_WINDOW = 24 giờ`):
       `serverReceivedAt <= attendanceCloseAt + 24 hours` (trễ hơn trả `422 OFFLINE_SYNC_WINDOW_EXPIRED`). Nếu nhận sau `attendanceCloseAt`, đánh dấu **`is_offline_late_sync = true`** để phục vụ hậu kiểm.
   - **Với `OFFLINE_GPS` (`QE-77`):**
     - Lưu `client_captured_at`, tọa độ, `accuracy`, `isMocked`, và `integrity_hash = SHA-256(canonicalPayload + clientAppPepper)`. Theo `NFR-SEC-006/010`, secret ở client chỉ là lớp chống sửa đổi ngẫu nhiên trong DB local chứ không phải bằng chứng mật mã tuyệt đối.
     - Server bắt buộc: `events.allow_offline_sync = true`, `serverReceivedAt <= attendanceCloseAt + 24 hours`, kiểm tra `clientCapturedAt` nằm trong `[attendanceOpenAt, attendanceCloseAt]` và không vượt quá `serverReceivedAt + clockSkewTolerance`, đồng thời kiểm tra Haversine; nếu hợp lệ lưu `method = 'OFFLINE_GPS'` (kèm `is_offline_late_sync = true` nếu nhận sau `attendanceCloseAt` và `suspicious_flag = true` nếu có bất thường).
   - **Với `EVIDENCE` offline (`QE-78`):**
     - Upload ảnh JPEG/PNG `< 5 MB` lên private storage (`EvidenceStoragePort` — Local Private FS mặc định, retention `150 ngày`), tạo `attendances(method='EVIDENCE', status='PENDING')` chờ BCS duyệt nếu `serverReceivedAt <= evidence_deadline` (`evidence_deadline >= attendance_close_at`, mặc định `+24 giờ` — `TBD-EVT-03`).

---

## 7. Ma trận Kiểm chứng & Sổ Giả định (`Verification Matrix & Assumptions`)

### 7.1 Ma trận Kiểm chứng (Mapping Bất biến -> Test Thực tế & Cần Bổ sung)

| Mã Bất biến | Mô tả Bất biến | Kiểm thử ĐÃ CÓ (Sprint 1 — `develop @ 4ac5da1`) | Kiểm thử CẦN BỔ SUNG (`QE-54` & Sprint 2–4) |
|---|---|---|---|
| **INV-REG-01** | Không oversell (`registered_count <= quota_total`), không tạo dòng mồ côi khi đầy | `MigrationIT#quotaCheckConstraintRejectsZero`<br>`RegistrationIT#capacityFullRollbackLeavesNoOrphanRow`<br>`RegistrationIT#concurrencyRaceConditionQuota1`<br>`RegistrationIT#concurrencyRaceConditionQuota5With30Students` | `MultiTierQuotaIT` (`QE-12`/`QE-38`): tranh suất cuối giao nhau giữa Total / Cohort / Class không deadlock, tổng pool <= tổng sự kiện |
| **INV-REG-02** | Tối đa 1 đăng ký active (`REGISTERED`/`WAITING_LIST`) mỗi `(event, student)`; click lặp idempotent | `MigrationIT#registrationsUniqueActiveConstraintBlocksDuplicate`<br>`MigrationIT#registrationsAllowsReRegistrationAfterCancelled`<br>`RegistrationIT#duplicateRegistrationReturns200AlreadyRegistered`<br>`RegistrationIT#sameStudentParallel10Requests` | `WaitlistConcurrencyIT` (`QE-39`): đăng ký khi đầy vào `WAITING_LIST` FIFO, click lặp giữ nguyên `#waitPosition` |
| **INV-REG-03** | Cắt hủy 12h, hủy idempotent, auto-promote FIFO & bỏ qua `#01` không đủ điều kiện | *(Chưa có — Sprint 2)* | `RegistrationCancellationIT` (`QE-40`): biên `== 12h` vs `12h + 1s`; hủy 2 lần song song không trừ đôi quota; `#01` bị `NO_SHOW` ban hoặc khác pool -> tự động skip và promote `#02` |
| **INV-ATT-01** | Tối đa 1 bản ghi `ACCEPTED` mỗi `(event, student)` qua mọi phương thức | `MigrationIT#attendancesUniqueAcceptedConstraint`<br>`AttendanceIT#checkInTwiceWithDifferentKeysFails` | `AttendanceConcurrencyIT` (`QE-54`): 10 luồng song song của cùng SV (khác `Idempotency-Key`) -> đúng 1 `200`, 9 `409 ALREADY_CHECKED_IN` (**0 lỗi `500`**); test chéo `QR` + `GPS` + BCS duyệt `EVIDENCE`/`PROXY_EVIDENCE` cùng lúc |
| **INV-ATT-02** | Idempotent Replay trả nguyên trạng thái (`ACCEPTED`/`PENDING`/`REJECTED`), không tăng attempt/attendance | `AttendanceIT#checkInWithSameIdempotencyKeyReturnsCachedResponse`<br>`AttendanceIT#checkInWithoutIdempotencyKeyFails` | `AttendanceConcurrencyIT` (`QE-54`): 10 luồng song song **cùng `Idempotency-Key`** (in-flight retry) -> tất cả nhận cùng `http_status` & body, 0 lỗi `500`, `count(attempts) == 1`; retry `REJECTED` giữ đúng `422` & `counted=true` chỉ tính 1 lần |
| **INV-ATT-03** | Trần 3 lần thử (`0,1,2,3` và lần 4 -> `409 ATTEMPT_LIMIT_REACHED`, `counted` giữ `<= 3`) | `AttendanceIT#checkInAttemptLimitReachedAfter3Failures` | `AttendanceAttemptBoundaryIT` (`QE-54`): 5 luồng sai song song khi đã có 2 lỗi -> đúng 1 luồng tăng lên 3, 4 luồng còn lại nhận `409 ATTEMPT_LIMIT_REACHED`; xác nhận DB `count(counted=true) == 3` (không thành 4) |
| **INV-QR-01** | Token HMAC-SHA256 dùng server `Clock`, rotation/grace đúng biên, không lộ token trong log/DB | `QrTokenServiceTest` (13 tests: format, current, grace, expired, future, tampered, wrong event, malformed, fingerprint)<br>`AttendanceIT` (25 tests)<br>`AuditIT` (3 tests) | Test xác nhận `response_body` & `audit_logs.details` không chứa token QR đầy đủ; giữ nhất quán `rotationSeconds` giữa `Event` & `application.yml` (`10s`) |
| **INV-OFF-01** | Offline queue replay an toàn & xác thực `OFFLINE_QR`/`OFFLINE_GPS` không tin clock client | *(Chưa có — Sprint 3–4)* | `OfflineSyncIT` (`QE-33`, `QE-76..78`): batch retry với partial failure; `OFFLINE_QR` quá `OFFLINE_SYNC_MAX_WINDOW` bị từ chối; `OFFLINE_GPS` `isMocked=true` giữ `ACCEPTED` + `suspicious_flag=true` |
| **INV-POOL-01** | Không cạn kiệt connection pool & đạt ngưỡng tải `Proposal P1` | *(Chưa có)* | `BurstLoadTest` (`QE-46`): 100 req/s trong 10s (3,000 req/60s) trên HikariCP pool=30 không timeout/deadlock |

### 7.2 Tổng hợp Quyết định Kiến trúc (`A-QE8-01..A-QE8-11` — `APPROVED 28/11/2026`)

> **Đã đồng bộ vào [`docs/spec/QNU_Event_SRS.docx`](../spec/QNU_Event_SRS.docx) (`v1.1`) và [`docs/decisions.md`](../decisions.md) ngày `28/11/2026`.** Toàn bộ các mục dưới đây đã chuyển sang trạng thái **`APPROVED`** (chỉ riêng tinh chỉnh ngưỡng chi tiết `accuracy`/tốc độ của `suspicious_flag` thuộc `TBD-ATT-03` được giữ mở cho thử nghiệm hiện trường):

| Mã | Mục / Ngữ cảnh | Quy tắc đã chốt (`APPROVED 28/11/2026`) | Tham chiếu chuẩn |
|---|---|---|---|
| **A-QE8-01** | Hạn chế `REQUIRES_NEW` trong luồng nóng (`RegistrationService` & `AttendanceService`) | Loại bỏ `REQUIRES_NEW` lồng bên trong TX giữ row lock: dùng `CheckInResult` commit attempt + attendance + success audit trong **1 TX duy nhất**; rejection audit ghi sau khi TX ngoài kết thúc. | `RegistrationService`, `AttendanceService`, `QE-54` |
| **A-QE8-02** | Bảo vệ `Idempotency-Key` sai lệch payload | Bổ sung `payload_hash` (`SHA-256`) trên `attendance_attempts` (`V2`); reuse `Idempotency-Key` với payload/method khác -> **`400 VALIDATION_FAILED`** (`IDEMPOTENCY_PAYLOAD_MISMATCH`). | `QE-54`, `QE-33`, `v2-schema-blueprint.md` |
| **A-QE8-03** | Khóa chống double-register/cancel trên `/registrations` | Hỗ trợ tùy chọn `Idempotency-Key` cho `POST /registrations` và `DELETE /registrations/me` ở Sprint 2. | `src/api/events.ts`, `QE-40` |
| **A-QE8-04** | Trả `attemptCount` & `remainingAttempts` + sửa `counted=false` ở lần 4 | `ATTEMPT_LIMIT_REACHED` ghi `counted = false` (giữ DB `<= 3`); trả metadata trong `GET /attendance/me` (`QE-98`) và `ApiError.details`. | `QE-54`, `QE-98`, Figma scanner |
| **A-QE8-05** | HTTP Status cho lần thử thứ 4 (`409`) & Scope 3 lần thử | Chốt **`409 ATTEMPT_LIMIT_REACHED`** (thay `429` ở `SRS v1.0`). Giới hạn 3 lần thử dùng chung cho các kênh tự động (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`), giữ `EVIDENCE`/Appeal làm kênh dự phòng riêng (`D-7`). | `error-codes.md`, `SRS v1.1` `UC015 5a` |
| **A-QE8-06** | Bán kính GPS mặc định & ngưỡng cờ đáng ngờ (`D-7`) | Cấu hình `allowed_radius_meters ∈ [10, 500]` theo từng sự kiện, **mặc định `50m`** (`60m` trên Figma là ví dụ sự kiện); ngoài bán kính trả **`422 GPS_OUT_OF_RANGE`**. Ngưỡng `accuracy`/tốc độ của `suspicious_flag` tinh chỉnh tiếp theo `TBD-ATT-03`. | `SRS v1.1 REQ-EVT-009`, `QE-55` |
| **A-QE8-07** | Cửa sổ đồng bộ Offline & cấm Offline Prefetch (`D-6`) | Tuyệt đối cấm Offline Prefetch; hỗ trợ Student Offline Store-and-Forward với **`OFFLINE_SYNC_MAX_WINDOW = 24 giờ`** sau `attendance_close_at`, `events.allow_offline_sync`, `is_offline_late_sync`, chỉ cho SV đã `REGISTERED`. | `SRS v1.1` (`TBD-ATT-01` đóng), `QE-33` |
| **A-QE8-08** | HTTP Status khi quá hạn hủy 12h, miễn `WAITING_LIST` & tái đăng ký (`D-5`) | Hủy `REGISTERED` khi `server_time >= start_at - 12h` trả **`409 CANCELLATION_CUTOFF_PASSED`**; **`WAITING_LIST` miễn mốc 12h**; **`CANCELLED` được đăng ký lại** nếu còn hạn và không bị ban `NO_SHOW`. | `SRS v1.1` `UC011`, `REQ-REG-012`, `QE-40` |
| **A-QE8-09** | Mô hình `event_pools` + `wait_order` + promote sau deadline (`D-4`, `D-5`) | Dùng `event_pools` (`1 seat = 1 pool`), `events.next_wait_order` → `wait_order`, `FOR UPDATE SKIP LOCKED`, skip `#01` không đủ điều kiện; tiếp tục promote sau `registration_deadline` trước `start_at - 12h`; tăng quota `A-EVT-01` kích hoạt promote ngay. | `quota-rules.md`, `v2-schema-blueprint.md`, `QE-38..40` |
| **A-QE8-10** | Chuẩn hóa cấu hình QR (`10s`) & nâng cấp `qr2` (`nonce`) | Đã đổi `15s` → `10s` và bật fail-fast secret trên `main`; giữ `qr1` cho Sprint 1–2, nâng `qr2` khi kích hoạt `nonce`. | `application.yml`, `QrTokenService`, `QE-44` |
| **A-QE8-11** | Job `NO_SHOW` (`D-9`) & Retention `150 ngày` (`D-8`) | Job `NO_SHOW` (`QE-14`, 7 ngày vào `student_restrictions`) chạy sau **`MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`**; bỏ qua `PENDING` Evidence, `WAITING_LIST`, `CANCELLED` và tạm dừng khi có Appeal Vòng 1; nhiều ban chồng nhau lấy **`MAX(current_banned_until, now + 7 days)`**. | `SRS v1.1` (`TBD-REG-03` đóng), `QE-14`, `v2-schema-blueprint.md` |
