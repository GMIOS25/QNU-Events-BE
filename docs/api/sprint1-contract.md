# Hợp đồng API Sprint 1 (Frozen Contract - v1)

> **Lưu ý quan trọng**: Tài liệu này đóng băng hợp đồng API giữa Backend và Frontend cho Sprint 1. Bất kỳ thay đổi nào liên quan đến API path, request body, response schema, hoặc mã lỗi đều phải sửa trực tiếp tại file này, ghi rõ lý do trong PR và thông báo cho toàn đội ngũ trước khi triển khai.

---

## 1. Bảng mã lỗi chuẩn (Error Codes)
Mọi phản hồi lỗi (HTTP status ≥ 400) đều trả về cấu trúc ApiError:
```json
{
  "code": "ERROR_CODE",
  "message": "Thông báo thân thiện bằng tiếng Việt",
  "details": {},
  "traceId": "uuid-or-request-id",
  "timestamp": "2026-11-09T03:30:00Z"
}
```

| HTTP Status | Mã lỗi (code) | Trường hợp áp dụng |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Sai dữ liệu đầu vào; `details.fields = {tên_trường: lý_do}` |
| 401 | `UNAUTHENTICATED` | Yêu cầu đăng nhập để thực hiện |
| 401 | `INVALID_CREDENTIALS` | Sai tên đăng nhập hoặc mật khẩu |
| 401 | `SESSION_REVOKED` | Phiên đăng nhập đã bị thu hồi hoặc đã đăng xuất |
| 403 | `FORBIDDEN` | Không có quyền thực hiện thao tác theo vai trò |
| 403 | `NOT_IN_AUDIENCE` | Sinh viên đăng ký sự kiện ngoài phạm vi lớp cho phép |
| 403 | `NOT_REGISTERED` | Sinh viên điểm danh khi chưa đăng ký sự kiện |
| 404 | `NOT_FOUND` | Tài nguyên không tồn tại hoặc không có quyền xem (bản nháp, ngoài lớp) |
| 409 | `EVENT_NOT_PUBLISHED` | Sự kiện chưa được công bố |
| 409 | `REGISTRATION_CLOSED` | Thời gian đăng ký sự kiện đã kết thúc |
| 409 | `CAPACITY_FULL` | Sự kiện đã hết suất tham gia (không để lại bản ghi mồ côi) |
| 409 | `PREVIEW_REQUIRED` | Cần xem trước sự kiện trước khi công bố |
| 409 | `PREVIEW_OUTDATED` | Nội dung sự kiện đã thay đổi, cần xem trước lại |
| 409 | `VERSION_CONFLICT` | Dữ liệu bị thay đổi bởi người khác (lệch version) |
| 409 | `ATTENDANCE_NOT_OPEN` | Chưa đến hoặc đã qua thời gian mở điểm danh |
| 409 | `ALREADY_CHECKED_IN` | Sinh viên đã điểm danh sự kiện này rồi |
| 409 | `ATTEMPT_LIMIT_REACHED` | Đã thử điểm danh thất bại quá 3 lần (lần thứ 4 bị từ chối) |
| 422 | `QR_MALFORMED` | Token QR sai định dạng 4 phần |
| 422 | `QR_BAD_SIGNATURE` | Chữ ký HMAC-SHA256 của mã QR không hợp lệ |
| 422 | `QR_WRONG_EVENT` | Token QR không thuộc về sự kiện này |
| 422 | `QR_EXPIRED` | Mã QR đã hết hạn theo rotation window + grace period |
| 422 | `QR_FROM_FUTURE` | Mã QR mang epoch tương lai |
| 500 | `INTERNAL_ERROR` | Lỗi máy chủ (không để lộ stack trace hay thông tin nhạy cảm) |

---

## 2. Danh sách Endpoint API Sprint 1

| Method | Endpoint | Quyền hạn | Request Body / Headers | Phản hồi thành công | Ghi chú |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Public | `{loginId, password}` | 200 `{accessToken, expiresAt, user:{id,fullName,roles,classId}}` | TTL 8 giờ |
| `POST` | `/api/v1/auth/logout` | Authenticated | — | 204 No Content | Thu hồi phiên làm việc tức thì |
| `GET` | `/api/v1/auth/me` | Authenticated | — | 200 `{id, loginId, fullName, studentCode, roles, classId}` | Lấy thông tin người dùng từ JWT |
| `GET` | `/api/v1/classes` | EVENT_OWNER | — | 200 `[{id, code, name, facultyName}]` | Lấy danh sách lớp để chọn audience |
| `POST` | `/api/v1/events` | EVENT_OWNER | `EventUpsert` | 201 `EventDetail` (status: `DRAFT`) | Tạo sự kiện nháp |
| `PUT` | `/api/v1/events/{id}` | EVENT_OWNER | `EventUpsert + version` | 200 `EventDetail` | Chỉ sửa khi `DRAFT`; lệch version → 409 |
| `GET` | `/api/v1/events/{id}/preview` | EVENT_OWNER | — | 200 `{event: EventDetail, previewVersion: number}` | Đánh dấu `previewed_version = version` |
| `POST` | `/api/v1/events/{id}/publish` | EVENT_OWNER | `{confirm: true, previewVersion: number}` | 200 `EventDetail` (status: `PUBLISHED`) | Kiểm tra đã preview và version khớp |
| `GET` | `/api/v1/events` | STUDENT / EVENT_OWNER | Query params: `q`, `page`, `size` | 200 `Page<EventSummary>` | Sinh viên: chỉ thấy `PUBLISHED` và đúng lớp; Chủ sự kiện: thấy sự kiện của mình |
| `GET` | `/api/v1/events/{id}` | STUDENT / EVENT_OWNER | — | 200 `EventDetail` | Không đủ quyền xem → 404 |
| `POST` | `/api/v1/events/{id}/registrations` | STUDENT | — | 201 (mới) / 200 (lặp lại) `{id, status, alreadyRegistered}` | Giữ suất nguyên tử, chống race condition |
| `GET` | `/api/v1/events/{id}/registrations/me` | STUDENT | — | 200 `{id, status, registeredAt}` | Trả về trạng thái đăng ký của bản thân |
| `GET` | `/api/v1/events/{id}/qr` | EVENT_OWNER | — | 200 `{token, epoch, validUntil, rotationSeconds, serverTime}` | Tạo mã QR động HMAC theo epoch |
| `POST` | `/api/v1/events/{id}/attendance/qr` | STUDENT | `{token}` + Header: `Idempotency-Key` (bắt buộc) | 200 `{status: "ACCEPTED", acceptedAt}` | Điểm danh QR động, tối đa 3 lần thử |
| `GET` | `/api/v1/me/history` | STUDENT | — | 200 `[HistoryItem]` | Lịch sử tham gia và điểm rèn luyện |

---

## 3. Data Transfer Objects (DTO Schemas)

### EventUpsert
```typescript
interface EventUpsert {
  title: string;                 // 3 - 200 ký tự
  description?: string;
  location: string;              // tối đa 300 ký tự
  startAt: string;               // ISO 8601
  endAt: string;                 // ISO 8601, endAt > startAt
  registrationDeadline: string;  // ISO 8601, registrationDeadline <= attendanceCloseAt
  attendanceOpenAt: string;      // ISO 8601
  attendanceCloseAt: string;     // ISO 8601, attendanceCloseAt > attendanceOpenAt
  attendanceMethod: "DYNAMIC_QR";
  quotaTotal: number;            // >= 1
  audienceClassIds: string[];    // >= 1 uuid lớp hợp lệ
  activityType: string;
  activityPoints: number;        // >= 0
  version?: number;
}
```

### EventSummary
```typescript
interface EventSummary {
  id: string;
  title: string;
  location: string;
  startAt: string;
  endAt: string;
  quotaTotal: number;
  registeredCount: number;
  remainingQuota: number;        // server tính: max(0, quotaTotal - registeredCount)
  registrationOpen: boolean;     // server tính: now <= registrationDeadline && remainingQuota > 0
  attendanceOpen: boolean;       // server tính: attendanceOpenAt <= now <= attendanceCloseAt
  myRegistrationStatus?: "REGISTERED" | "CANCELLED" | null;
  status: "DRAFT" | "PUBLISHED" | "CANCELLED";
  activityType: string;
  activityPoints: number;
}
```

### EventDetail (kế thừa EventSummary và bổ sung)
```typescript
interface EventDetail extends EventSummary {
  description?: string;
  registrationDeadline: string;
  attendanceOpenAt: string;
  attendanceCloseAt: string;
  attendanceMethod: "DYNAMIC_QR";
  qrRotationSeconds: number;
  qrGraceSeconds: number;
  ownerId: string;
  audienceClassIds: string[];
  version: number;
  previewedVersion?: number;
  publishedAt?: string;
}
```

### HistoryItem
```typescript
interface HistoryItem {
  eventId: string;
  title: string;
  startAt: string;
  location: string;
  registrationStatus: "REGISTERED" | "CANCELLED";
  registeredAt: string;
  attendanceStatus: "NOT_CHECKED_IN" | "ACCEPTED";
  attendanceAt?: string;
  method: "DYNAMIC_QR";
  activityType: string;
  activityPoints: number;
}
```

---

## 4. Thuật toán QR động & Điểm danh (QE-56, QE-57)
- Cấu trúc Token: `qr1.<eventId>.<epoch>.<base64url(HMAC-SHA256(secret, "qr1."+eventId+"."+epoch))>`
- `epoch = floor(nowEpochSeconds / rotationSeconds)` (mặc định rotation = 10s, grace = 3s).
- Header `Idempotency-Key` là bắt buộc khi gọi `/attendance/qr`. Cùng key sẽ trả lại response đã lưu mà không tính thêm lượt attempt.
- Tối đa 3 lần thử sai (`QR_*`). Lần thứ 4 sẽ bị từ chối với `ATTEMPT_LIMIT_REACHED`.
