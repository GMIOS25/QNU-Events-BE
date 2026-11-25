# Quy tắc Quota nhiều tầng — QE-38

> **Trạng thái: ⏳ CHỜ PRODUCT OWNER / STAKEHOLDER DUYỆT** (AC của QE-38 yêu cầu bảng quyết định được xác nhận).
> Sau khi duyệt: cập nhật `docs/decisions.md` (mục A-QE8 tương ứng / A-003 trong PR #10) thành **Approved** và ánh xạ vào code QE-12/QE-39/QE-40.
> Nguồn tổng hợp: Proposal §5.3, SRS (REQ-REG-002..012, UC-03/04/011), review kỹ thuật PR #10/#11/#12 (17/11/2026).

---

## 1. Nguyên tắc chung

| # | Quy tắc |
|---|---|
| N1 | `quotaTotal` của sự kiện là **trần tuyệt đối** — không tầng nào được vượt |
| N2 | Σ `capacity` của mọi pool ≤ `quotaTotal` (chặn ở `EventValidator`) |
| N3 | `priority`: **số nhỏ hơn = ưu tiên cao hơn**; phân bổ duyệt pool theo thứ tự tăng dần |
| N4 | Một suất đã cấp thuộc về **đúng một pool** (`registrations.pool_id`) — hoàn suất phải về đúng pool đó (BR-QTA-06) |
| N5 | Mọi quyết định phân bổ/h hoàn suất thực hiện **nguyên tử ở mức DB** (UPDATE có điều kiện + transaction) — không dựa vào check-then-write |

## 2. Ý nghĩa trường cấu hình pool (`event_pools`)

| Trường | Ý nghĩa |
|---|---|
| `scope_type` + `scope_ref` | Phạm vi pool: LOP (mã lớp), KHOA (tên khoa), KHOI, CLB, ALL (toàn trường) |
| `capacity` / `used` | Sức chứa / số suất đã cấp của pool; `used ≤ capacity` (CHECK) |
| `allow_overflow` | `true`: khi pool đầy được **mượn chỗ pool khác** theo priority; `false`: pool đầy → **dừng**, vào waiting của pool này |
| `is_mandatory` | Pool giữ chỗ riêng cho nhóm (ví dụ suất riêng cho lớp chủ nhà) — luôn đi kèm `allow_overflow = false` |
| `reserved_until` | Suất được giữ trước cho mục đích riêng; trước mốc này không cấp cho đăng ký thường |

## 3. Quy tắc phân bổ suất (khi sinh viên đăng ký)

| # | Tình huống | Hành vi chuẩn |
|---|---|---|
| A1 | Pool phù hợp còn chỗ | Cấp suất pool đó (`claimSeat` nguyên tử), lưu `pool_id`, tăng `registered_count` sự kiện |
| A2 | Pool đầy, `allowOverflow = true` | Thử pool kế tiếp theo priority (mượn chỗ) |
| A3 | Pool đầy, `allowOverflow = false` (đặc biệt mandatory) | **DỪNG — vào WAITING_LIST của đúng pool đó**, không nhảy sang pool khác |
| A4 | Hết mọi pool hợp lệ | WAITING_LIST ở pool ưu tiên cao nhất mà sinh viên thuộc scope |
| A5 | Claim pool thành công nhưng quota tổng đầy | **WAITING_LIST** (không trả 409 — sinh viên hợp lệ không bị mất cơ hội) |
| A6 | Suất `reserved_until` chưa hết hạn | Chưa cấp được (giữ riêng); sau mốc, suất chưa dùng giải phóng về pool |
| A7 | Sinh viên không thuộc pool nào được cấu hình | 403 `NOT_IN_AUDIENCE` (đã chặn trước đó ở mức audience) |
| A8 | Đăng ký lặp (cùng sinh viên) | Idempotent — trả registration hiện tại, không tốn suất |

## 4. Ví dụ minh họa

Sự kiện của Khoa CNTT, `quotaTotal = 100`:

| Pool | Phạm vi | capacity | priority | allowOverflow | mandatory |
|---|---|---|---|---|---|
| P1 | LOP K49CNTT1 | 40 | 1 | false | true |
| P2 | LOP K49CNTT2 | 30 | 2 | false | false |
| P3 | KHOA CNTT | 20 | 3 | true | false |
| P4 | ALL | 10 | 4 | true | false |

- SV **K49CNTT1** khi P1 đầy → **WAITING P1** (mandatory, không mượn chỗ P3/P4)
- SV **K49CNTT2** khi P2 đầy → thử P3 (overflow) → P4 → hết → **WAITING P2**
- SV **K49CNTT3** (lớp khác cùng khoa) → P3 → P4 → hết → **WAITING P3**
- SV **khoa khác** → P4 → hết → **WAITING P4**

## 5. Waiting list & promote

| # | Quy tắc |
|---|---|
| W1 | Waiting thuộc **đúng pool** (pool ưu tiên cao nhất phù hợp scope); xếp theo `waiting_at` tăng dần |
| W2 | Có suất освоб phóng ở pool X → promote **đầu hàng chờ của X** trong cùng transaction |
| W3 | Promote phải khóa hàng an toàn đồng thời: `SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1` — 2 lệnh hủy song song không được promote trùng một người |
| W4 | Người được promote phải còn `ACTIVE` + đúng scope pool; không đủ điều kiện → skip, lấy người kế tiếp |
| W5 | "Thông báo" ở Sprint 2 = audit log `WAITING_PROMOTED` (email/push: Sprint 3+) |

## 6. Hủy đăng ký & hoàn suất

| # | Quy tắc |
|---|---|
| H1 | Hạn hủy: **trước giờ bắt đầu 12 giờ** — chặn kể từ đúng mốc `startAt − 12h` (dùng `!now.isBefore(deadline)`, theo REQ-REG-012/UC011) |
| H2 | Hủy khi REGISTERED: hoàn suất về đúng `pool_id` + giảm `registered_count`, rồi promote theo W2-W4 |
| H3 | Rời WAITING_LIST: **miễn giới hạn 12h** (không làm thay đổi suất đã cấp), không promote |
| H4 | Audit `REGISTRATION_CANCEL` (thành công/từ chối) cho mọi thao tác |

## 7. Quota thay đổi sau khi đã có đăng ký

| Thao tác | Cho phép? |
|---|---|
| Tăng capacity pool | Có — miễn Σ capacity ≤ quotaTotal |
| Giảm capacity pool | Chỉ khi giá trị mới ≥ `used` của pool (BR-QTA-10) |
| Giảm quotaTotal | Chỉ khi ≥ Σ `used` toàn bộ pool |
| Tăng quotaTotal | Có (không trần) |
| reserved_until hết hạn | Suất chưa dùng giải phóng về pool |

## 8. Ma trận case biên & đồng thời

| # | Kịch bản | Kết quả bắt buộc | Test hiện có/planned |
|---|---|---|---|
| C1 | 2 request tranh suất cuối của pool | 1 REGISTERED + 1 WAITING, `used` không vượt capacity | RegistrationIT #14 ✅ |
| C2 | 2 người hủy REGISTERED đồng thời cùng pool | 2 lượt promote cho 2 người chờ **khác nhau** (SKIP LOCKED), không hụt suất | planned (PR #12 review) |
| C3 | Hủy **đúng mốc** startAt − 12h | Bị chặn (biên tính từ mốc, không sau mốc) | planned |
| C4 | Claim pool OK nhưng quota tổng đầy | WAITING_LIST, không 409 | planned (PR #11 review) |
| C5 | Pool đầy + allowOverflow=false (mandatory) | WAITING đúng pool, không fall-through | planned (PR #11 review) |
| C6 | Đăng ký lặp / double-click | Idempotent, suất không tăng | RegistrationIT #2, #15 ✅ |
| C7 | scopeRef LOP không tồn tại trong org_class | 400 VALIDATION_FAILED khi cấu hình | planned (PR #10 review) |

## 9. Ánh xạ triển khai

- **QE-12 (PR #10)**: validator bổ sung C7 (scopeRef phải tồn tại `org_class` ∧ ∈ `audienceClassIds`); `EventDetail` trả danh sách pools
- **QE-39 (PR #11)**: sửa vòng phân bổ theo A2/A3/A5 (hiện đang fall-through sai); query lớp 1 lần trước vòng lặp; hợp đồng API Sprint 2 thêm `status: WAITING_LIST`
- **QE-40 (PR #12)**: SKIP LOCKED theo W3; eligibility promote theo W4; boundary H1; audit ngoài transaction giữ lock (A-QE8-01)
- Migration V2 thống nhất do A0 tổng hợp (R6) trước khi rebase chuỗi PR

---

*Ký duyệt PO/Stakeholder: ____________________ Ngày: ____/____/2026*
