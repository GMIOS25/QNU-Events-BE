# Nhật ký quyết định kỹ thuật (Decision Log)

> Quy tắc R11 (AGENTS.md): mọi quyết định kỹ thuật chưa có trong SRS phải được ghi tại đây dưới dạng **Assumption (chưa duyệt)**. Không tự ý coi là đã chốt cho đến khi nhóm xác nhận và cập nhật vào SRS.

---

## A-001 · Bắt buộc truyền `version` khi `PUT /api/v1/events/{id}`

- **Ngày:** 15/11/2026
- **Ticket:** QE-65 · **Agent:** A1 (Hoàng)
- **Bối cảnh:** `EventService.update` trước đây chấp nhận `version = null` và bỏ qua kiểm tra optimistic locking, trong khi hợp đồng API Sprint 1 ([docs/api/sprint1-contract.md](api/sprint1-contract.md)) quy định request `PUT` gồm `EventUpsert + version` kèm điều kiện "lệch version → 409". Hành vi cũ mở khe ghi đè nhầm khi hai người cùng sửa một bản nháp mà client quên gửi `version`.
- **Quyết định (Assumption):**
  - `PUT` thiếu `version` → 400 `VALIDATION_FAILED`, kèm `details.fields.version` giải thích lỗi.
  - `version` lệch giá trị hiện tại → 409 `VERSION_CONFLICT` (giữ nguyên như trước).
- **Tác động:** FE form sửa sự kiện luôn đọc `version` từ `EventDetail` nên không bị ảnh hưởng; client cũ bỏ sót `version` sẽ nhận lỗi rõ ràng thay vì ghi đè ngầm. Hợp đồng API không đổi — đây là siết đúng cam kết đã đóng băng.
- **Trạng thái:** ASSUMPTION — chờ nhóm duyệt và ghi vào SRS.

---

## A-003 · Ngữ nghĩa pool quota nhiều tầng (QE-12/39/40)

- **Ngày:** 18/11/2026
- **Ticket:** QE-12 · **Agent:** A1 (Hoàng)
- **Bối cảnh:** SRS/Proposal quy định quota nhiều tầng theo phạm vi (lớp/khoa/khối/CLB/toàn trường) nhưng chưa chốt ngữ nghĩa chi tiết ở mức triển khai.
- **Quyết định (Assumption):**
  - Tổng capacity các pool ≤ `quotaTotal`; `quotaTotal` là trần tuyệt đối.
  - Phân bổ suất: duyệt pool theo `priority` tăng dần (số nhỏ = ưu tiên cao), chỉ nhận pool mà sinh viên thuộc scope; `allow_overflow=true` cho phép dùng suất pool khác khi pool của mình hết.
  - Hết pool hợp lệ → vào `WAITING_LIST` của pool ưu tiên cao nhất mà sinh viên thuộc scope, xếp theo `waiting_at`.
  - Hủy đăng ký hoàn suất về đúng `pool_id` đã cấp (BR-QTA-06) và tự promote người đầu waiting list cùng pool trong một transaction.
  - "Thông báo" khi promote ở Sprint 2 = ghi audit log (chưa có email/push).
  - Bảng đặt tên `event_pools` theo quy ước số nhiều của V1 (events, registrations); `registrations.status` đã sẵn giá trị `WAITING_LIST` trong V1 nên không cần mở rộng constraint.
- **Tác động:** Migration V2 được kèm trong PR này và cần A0 (Cường) xem xét duyệt theo R6 trước khi merge.
- **Trạng thái:** ASSUMPTION — chờ nhóm duyệt.
