# Bảng Mã Lỗi Chuẩn Thống Nhất Toàn Dự Án (`ErrorCode` — Sprint 1–3)

> **Trạng thái:** ✅ **APPROVED (28/11/2026 — `D-5`, `D-7`, `D-10`, `A-QE8-02/05/08`)**  
> **Phạm vi áp dụng:** Nguồn chuẩn duy nhất cho Backend (`common/ErrorCode.java`), Frontend (`QNU-Events-FE`), đặc tả `SRS v1.1`, tài liệu kiến trúc và ma trận kiểm thử QA. Các mã lỗi cốt lõi Sprint 1 vẫn giữ nguyên trong [`sprint1-contract.md`](sprint1-contract.md).

---

## 1. Cấu trúc `ApiError` Envelope (RFC 7807 Style JSON)

```json
{
  "code": "ERROR_CODE",
  "message": "Thông báo thân thiện bằng tiếng Việt",
  "details": {},
  "traceId": "uuid-or-request-id",
  "timestamp": "2026-11-28T08:30:00Z"
}
```

> Với các lỗi xác thực tự động bị tính vào giới hạn 3 lần thử (`counted = true`) hoặc `ATTEMPT_LIMIT_REACHED`, trường `details` mang thêm metadata: `{method, attemptCount, maxAttempts, remainingAttempts, canRetry}` ([`sprint1-contract.md`](sprint1-contract.md) §4).

---

## 2. Bảng Mã Lỗi Chuẩn Toàn Dự Án (Canonical Error Code Catalog)

| HTTP | Mã lỗi chuẩn (`ErrorCode`) | Giai đoạn | `counted` (3-attempt) | Ý nghĩa & Thay thế mã cũ bị lệch |
|---:|---|---|:---:|---|
| `400` | `VALIDATION_FAILED` | Sprint 1+ | `false` | Sai dữ liệu đầu vào hoặc thiếu header bắt buộc (dùng kèm `details` cho `IDEMPOTENCY_PAYLOAD_MISMATCH`; **thay thế `MISSING_HEADER`** trong `QE-44`) |
| `401` | `UNAUTHENTICATED` | Sprint 1+ | `false` | Thiếu hoặc JWT không hợp lệ |
| `401` | `INVALID_CREDENTIALS` | Sprint 1+ | `false` | Sai `loginId` hoặc mật khẩu (bao gồm sai pass trên tài khoản `LOCKED` từ `QE-30` Sprint 2) |
| `401` | `SESSION_REVOKED` | Sprint 1+ | `false` | Phiên đã bị thu hồi tức thì (`logout`, `LOCK`, đổi role, đổi/reset mật khẩu) |
| `401` | `SESSION_EXPIRED` | Sprint 2+ | `false` | Phiên/Access Token đã hết hạn TTL (tách rõ khỏi `SESSION_REVOKED` để FE thông báo chính xác) |
| `403` | `FORBIDDEN` | Sprint 1+ | `false` | Không có quyền theo vai trò/phạm vi hoặc tài khoản bị khóa quản trị `LOCKED` (sau khi khớp mật khẩu) |
| `403` | `NOT_IN_AUDIENCE` | Sprint 1+ | `false` | Sinh viên đăng ký sự kiện ngoài phạm vi lớp cho phép (**thay thế `400 Bad Request`** trong `SRS UC010 4a.1`) |
| `403` | `NOT_REGISTERED` | Sprint 1+ | `false` | Sinh viên điểm danh khi chưa có trạng thái `REGISTERED` (kể cả `WAITING_LIST` / `CANCELLED`) |
| `403` | `STUDENT_PENALIZED` | Sprint 3 | `false` | Sinh viên đang trong thời gian bị khóa đăng ký 7 ngày do `NO_SHOW` (`QE-14`) |
| `403` | `OAUTH_DOMAIN_NOT_ALLOWED` | Sprint 3 | `false` | Email Google không nằm trong allowlist biến môi trường `APP_GOOGLE_ALLOWED_DOMAINS` (`QE-74`) |
| `403` | `OAUTH_ACCOUNT_NOT_PROVISIONED` | Sprint 3 | `false` | Email hợp lệ nhưng chưa được `SYSTEM_ADMIN` cấp phát trước trong `app_user` (`QE-74`) |
| `403` | `OAUTH_IDENTITY_MISMATCH` | Sprint 3 | `false` | Xung đột `google_sub` với tài khoản `app_user` đã liên kết trước đó (`QE-74`) |
| `404` | `NOT_FOUND` | Sprint 1+ | `false` | Tài nguyên không tồn tại hoặc không có quyền xem (`DRAFT`/`CANCELLED`, ngoài lớp/chủ sở hữu) |
| `409` | `EVENT_NOT_PUBLISHED` | Sprint 1+ | `false` | Sự kiện chưa ở trạng thái `PUBLISHED` |
| `409` | `REGISTRATION_CLOSED` | Sprint 1+ | `false` | Đã quá `registrationDeadline` hoặc Owner đóng đăng ký sớm |
| `409` | `CAPACITY_FULL` | Sprint 1 | `false` | Sự kiện đã hết suất ở Sprint 1 (từ Sprint 2 chuyển sang `WAITING_LIST` theo `event_pools`) |
| `409` | `PREVIEW_REQUIRED` | Sprint 1+ | `false` | Cần gọi `GET /preview` trước khi `publish` |
| `409` | `PREVIEW_OUTDATED` | Sprint 1+ | `false` | Nội dung/cấu hình sự kiện đã đổi sau lần preview cuối |
| `409` | `VERSION_CONFLICT` | Sprint 1+ | `false` | Lệch `@Version`, sai trạng thái chuyển tiếp, hoặc sửa trường bị khóa sau khi có người đăng ký (`A-EVT-01`) |
| `409` | `CANCELLATION_CUTOFF_PASSED` | Sprint 2 | `false` | Sinh viên `REGISTERED` hủy đăng ký tại/sau mốc `startAt - 12h` (**thay thế `400 Bad Request`** trong `SRS UC011 3a.1` và **`CANCELLATION_DEADLINE_PASSED`** ở `QE-25`) |
| `409` | `ATTENDANCE_NOT_OPEN` | Sprint 1+ | `false` | Ngoài cửa sổ `[attendanceOpenAt, attendanceCloseAt]` |
| `409` | `ALREADY_CHECKED_IN` | Sprint 1+ | `false` | Sinh viên đã có bản ghi `attendances.status = 'ACCEPTED'` |
| `409` | `ATTEMPT_LIMIT_REACHED` | Sprint 1+ | `false` | Đã tiêu tốn hết 3 lượt thất bại (`counted = true`) trên các kênh tự động; lần thử thứ 4+ trả `409` với `counted = false` (**thay thế `429 Too Many Requests`** trong `SRS UC015 5a`) |
| `422` | `QR_MALFORMED` | Sprint 1+ | `true` | Token QR sai cấu trúc 4 phần |
| `422` | `QR_BAD_SIGNATURE` | Sprint 1+ | `true` | Chữ ký HMAC-SHA256 của mã QR không hợp lệ |
| `422` | `QR_WRONG_EVENT` | Sprint 1+ | `true` | Token QR không thuộc về `eventId` đang điểm danh |
| `422` | `QR_EXPIRED` | Sprint 1+ | `true` | Mã QR đã quá `rotationSeconds + graceSeconds` |
| `422` | `QR_FROM_FUTURE` | Sprint 1+ | `true` | Mã QR mang `epoch > currentEpoch` |
| `422` | `GPS_OUT_OF_RANGE` | Sprint 2 | `true` | Khoảng cách Haversine `> allowed_radius_meters` (**thay thế `OUT_OF_GPS_RADIUS`** trong tài liệu kiểm thử & **`400 Bad Request`** trong `SRS UC016 7a`) |
| `422` | `INVALID_EVIDENCE_FILE` | Sprint 2 | `false` | Ảnh minh chứng sai magic bytes JPEG/PNG hoặc dung lượng `>= 5 MB` (**thay thế `INVALID_EVIDENCE`** trong tài liệu kiểm thử; không trừ lượt tự động) |
| `422` | `OFFLINE_SYNC_WINDOW_EXPIRED` | Sprint 3 | `true` | Bản ghi offline đồng bộ sau `attendanceCloseAt + 24h` (`OFFLINE_SYNC_MAX_WINDOW`) |
| `500` | `INTERNAL_ERROR` | Sprint 1+ | `false` | Lỗi máy chủ không xác định (không lộ stack trace) |

> **Mã lỗi chuyên biệt cho luồng Import CSV/Excel (`QE-26`, `UC025–026`):** Danh mục lỗi cấp tệp (`FILE_*`: HTTP `400`/`413`/`415`/`422`) và lỗi cấp dòng (`ROW_*` trong `ImportReport` HTTP `200`) được quy chuẩn riêng tại [`../import-contract/student-import-contract.md`](../import-contract/student-import-contract.md) §6.3.
