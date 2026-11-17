# Jira Change Log — QNU-Events

> Nhật ký ghi nhận các thay đổi cấu hình, trạng thái, trường dữ liệu trên Jira (qua TWG CLI) theo quy tắc R10 của kế hoạch giải cứu Sprint 1.

| Thời điểm (UTC+7) | Đối tượng / Key | Thao tác | Trường thay đổi | Giá trị cũ | Giá trị mới | Lý do / Nhiệm vụ |
|---|---|---|---|---|---|---|
| 09/11/2026 10:11 | Sprint 41 (Sprint 1) | Update | `endDate` | `2026-11-19T10:00:00.000Z` | `2026-11-11T10:00:00.000Z` | [J1] Căn ngày kết thúc Sprint 1 khớp deadline 11/11 (D1) |
| 09/11/2026 10:12 | PROD-3 | Transition | `status` | `In Progress` | `Done` | [J8] Đóng ticket quá hạn 35 ngày (D6) |
| 09/11/2026 10:13 | 10 item Sprint 1 (QE-28, 56, 57, 61, 64, 65, 66, 70, 71, 72) | Update | `duedate` | (none) | `2026-11-11` | [J2] Gán hạn chót khớp ngày review Sprint 1 |
| 09/11/2026 10:14 | 10 item Sprint 1 (QE-28, 56, 57, 61, 64, 65, 66, 70, 71, 72) | Update | `labels`, `comment` | (none) | Gắn nhãn `uc*`, `req-*` & comment chi tiết | [J4] Khớp mã chức năng và yêu cầu SRS ↔ Jira |
| 09/11/2026 13:47 | 9 item Sprint 1 (QE-64, QE-65, QE-66, QE-71, QE-56, QE-57, QE-61, QE-72, QE-28) | Transition | `status` | `To Do` | `Done` | [J5] Nghiệm thu 100% Sprint 1 sau khi pass toàn bộ 66 BE tests, 11 FE tests, 7 Playwright E2E tests |
