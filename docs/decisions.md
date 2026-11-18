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
