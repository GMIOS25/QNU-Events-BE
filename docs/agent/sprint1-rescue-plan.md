# Kế hoạch cứu Sprint 1 — QNU-Events (09/11 → 11/11/2026)

> Soạn lúc 09/11/2026 08:40 (UTC+7). Dữ liệu đọc trực tiếp từ: Jira `QE` + `PROD` (qua TWG), `docs/` của BE (proposal v2, SRS, rubric, `backlog_solutions.md`), mã nguồn + git của `QNU-Events BE`, thư mục `QNU-Events FE`, và chạy thử `mvnw verify`.

## 0. Kết luận nhanh

**Chưa đạt yêu cầu Sprint 1.** Phần *tài liệu & backlog* khá tốt; phần *sản phẩm* gần như bằng 0.

| Hạng mục | Trạng thái thật | Kết luận |
|---|---|---|
| Sprint 1 (38 SP, 10 item) | 0 SP xong · 9 item `To Do` · `QE-70` đang `In Progress` | 🔴 |
| Mã nguồn BE | Chỉ có khung (config, CI, deny-all). **0 entity / controller / migration** | 🔴 |
| Mã nguồn FE | Thư mục `D:\Work\QNU-Events FE` **rỗng, chưa `git init`** | 🔴 |
| Kiểm thử | 9 test (chỉ test cấu hình). Chưa có test plan / test case / automation / bug | 🔴 |
| Git/CI | 1 nhánh, 7 commit, 0 PR; CI có nhưng chưa dùng thật | 🟠 |
| Backlog Jira | Có SP, mô tả, label. **Thiếu** due date, checklist/sub-task, bug, mã khớp SRS | 🟠 |
| SRS / proposal | Mạnh (29 use case, NFR, UML). **Thiếu** ERD, kiến trúc, wireframe truy cập được | 🟠 |

**Khả thi?** 38 SP × 5 người trong ~2 ngày **chỉ khả thi** nếu: (1) cắt scope đúng mức (§4.2), (2) làm song song theo module với hợp đồng kỹ thuật cố định (§6), (3) AI-Agent làm theo task card nhỏ có lệnh kiểm chứng (§7).

### 5 điểm chặn (Trạng thái sau khi chốt ngày 09/11)

| # | Việc | Hiện trạng | Hành động tiếp theo |
|---|---|---|---|
| B1 | Chốt ngày review Sprint 1 | ✅ **ĐÃ CHỐT** (D1): Review ngày **11/11/2026**; Sprint 2 từ 12/11 (2 tuần), các sprint sau 10–13 ngày | Chạy J1 cập nhật sprint 41 trên Jira về ngày 11/11 |
| B2 | Cài **Docker / PostgreSQL 17** | ✅ **ĐÃ HOÀN TẤT**: PostgreSQL 17 (PostGIS container WSL2) đã tạo DB `qnu_events`, kết nối và chạy `mvnw verify -Pintegration` **9/9 test xanh**! | Sẵn sàng cho toàn bộ integration test BE |
| B3 | Cấp quyền đọc **Figma** | ✅ **ĐÃ GIẢI QUYẾT** (D8): Bản draft `jVs22zFVUQjDbYQ5d0pwmU` đã đọc thành công qua MCP (pages: `0:1`, `106:358`) | Dùng key `jVs22zFVUQjDbYQ5d0pwmU` làm nguồn UI chính |
| B4 | Remote GitHub FE + GitHub username 5 người | ✅ **ĐÃ GIẢI QUYẾT** (D3, D4): `GMIOS25/QNU-Events-FE` + đủ 5 user | Điền vào CODEOWNERS & CI ở task T-00 |
| B5 | Đổi workflow Jira & đổi `QE-19` Task→Story | ⚠️ Cần người quản trị Jira chỉnh bằng tay | Jira → Project settings → Board / Workflow (2 phút) |

## 1. Nguồn đã đọc & giới hạn

| Nguồn | Kết quả |
|---|---|
| Jira `QE` (77 item, board 4, sprint id 41) | ✅ đọc đầy đủ |
| Jira `PROD` (7 item) | ✅ |
| "QNU-Events Spaces" | Hiểu là **Jira space `QE`** (Jira gọi project là *space*). **Confluence trả HTTP 404** trên site `tranthanhcuongit.atlassian.net` → chưa bật Confluence |
| `docs/` (proposal v2, SRS ~194k ký tự, rubric, `backlog_solutions.md`, `backend-foundation.md`) | ✅ (SRS đọc phần text; 54 hình UML chưa xem) |
| Figma `jVs22zFVUQjDbYQ5d0pwmU` (draft copy từ `HX4313AoLQX4zNidDz2Ixo`) | ✅ **Đã đọc thành công** qua MCP Figma (`0:1` QNU EVENT UI, `106:358` phones). UI sẽ bám sát bản draft này |
| `mvnw verify` BE | ✅ `BUILD SUCCESS` (9 test: 5 unit test + 4 integration test trên PostgreSQL 17 thật) |

## 2. Đánh giá theo rubric

> Điểm dưới đây là **ước lượng thô của agent để xếp thứ tự ưu tiên**, không phải điểm chính thức. Công thức Sprint 1: `L = E + min(0.2·F, E) + G/2 + min(0.1·H + 0.3·I + 0.05·J, G)`.

| Tiêu chí (hệ số) | Hiện trạng có bằng chứng | Ước tính | Mục tiêu 11/11 |
|---|---|:---:|:---:|
| Tính trung thực | Số liệu unit test / test case / bug khai trong bảng điểm phải đúng với repo & Jira | rủi ro | giữ 100% |
| Quản lý dự án theo quy trình (1) | Có epic, sprint goal, assignee rõ, mỗi người 6–8 SP. Nhưng 0 tiến độ, workflow 3 trạng thái, không có biên bản Planning/Daily/Review, sprint end lệch ngày | 4 | 7 |
| Quản lý yêu cầu & bug (1) | 61/70 item có SP>0, có label, mô tả tốt. **0/77 có due date, 0 sub-task/checklist, 0 Bug**, mã không khớp SRS | 4 | 7 |
| Quản lý mã nguồn (1) | 7 commit, 1 nhánh, 0 PR, author lẫn lộn (`Tên Của Bạn`, `=`), CODEOWNERS sai chỗ. CI có | 3 | 6 |
| Phân tích & Thiết kế (1) | SRS mạnh. Thiếu ERD/DB design, architecture doc, wireframe không truy cập được | 6 | 8 |
| Coding (1) | 0 business code | 1 | 6 |
| UI/UX (1) | FE chưa tồn tại | 1 | 6 |
| Kiểm thử (1) | Không có test plan/case/automation/report | 1 | 6 |
| Triển khai (0,5) | Không Dockerfile, không staging | 0–1 | 4 (demo HTTPS qua tunnel) |
| Kỹ thuật & Nghiệp vụ (1) | Nghiệp vụ phức tạp (quota nguyên tử, QR ký, offline) | 7 | 7 |
| Báo cáo (1) | SRS tốt, thiếu các tài liệu còn lại | 5 | 7 |

### 2.1 Mức sẵn sàng từng item Sprint 1

| Key | Item | Owner | SP | Hiện trạng | Còn thiếu |
|---|---|---|:-:|---|---|
| QE-70 | Dev env BE/FE/DB | Cường | 3 | In Progress (~30%) | FE repo, migration V1, fixture, README chạy FE+BE, Docker local |
| QE-64 | Login mẫu + phân quyền | Cường | 5 | 0% | JWT, login/logout, role guard, seed 1 owner + ≥3 sinh viên |
| QE-65 | Owner tạo/preview/publish | Hoàng | 5 | 0% | Entity, API, validate, UI form |
| QE-66 | Sinh viên xem event lớp mình | Hoàng | 3 | 0% | Lọc audience ở server, UI danh sách/chi tiết |
| QE-71 | Đăng ký giữ suất theo quota | Dương | 5 | 0% | Giữ suất nguyên tử, unique, test tranh suất |
| QE-56 | Dynamic QR + xác thực online | Đan | 5 | 0% | Token ký, check-in, 3 attempt, idempotency |
| QE-57 | UI hiển thị/quét QR | Đan | 3 | 0% | Màn Owner + màn quét camera (HTTPS) |
| QE-72 | Lịch sử đăng ký/QR | Khánh | 3 | 0% | API `/me/history`, UI |
| QE-61 | Audit tối thiểu | Khánh | 3 | 0% | Bảng + service + gắn vào 3 luồng |
| QE-28 | Smoke/integration + demo | Dương | 3 | 0% | Test plan, TC, automation, bug, bằng chứng |

## 3. Danh sách vấn đề (đầy đủ, có bằng chứng)

Mức: **P0** = chặn Sprint 1 · **P1** = mất điểm rubric đáng kể · **P2** = nên sửa nếu còn giờ.

### A. Quản lý dự án / Jira

| ID | Mức | Vấn đề | Bằng chứng | Xử lý |
|---|:-:|---|---|---|
| A1 | P0 | Ngày kết thúc sprint trong Jira lệch deadline thật | Sprint 41 `endDate=2026-11-19T10:00Z`, deadline là 11/11 | J1 |
| A2 | P0 | Không có tiến độ thật | 76/77 item `To Do`; chỉ `QE-70` In Progress | toàn bộ §7 |
| A3 | P1 | Không có deadline | 0/77 item có `duedate` | J2 |
| A4 | P1 | Không có checklist / sub-task | 0 `Subtask` trong QE | J3 |
| A5 | P1 | Không có bug nào, chưa có quy ước bug | 0 `Bug`; QE-28/46/50/51 yêu cầu bug có Jira issue | J5 |
| A6 | P1 | Workflow chỉ 3 trạng thái, khó chứng minh quy trình Scrum | Status: To Do / In Progress / Done | B5 (thủ công) |
| A7 | P1 | Mã chức năng Jira ≠ mã SRS; không có bảng truy vết | SRS dùng `REQ-AUTH-001`, `UC001…UC029`; Jira dùng "proposal §x", `BR-ATT`; `backlog_solutions.md §1.5` lại đề xuất mã thứ ba `FR-REG-03` | J4, T-22 |
| A8 | P2 | 9 item cha SP=0, 3 cái chưa có "Phân chia triển khai" | QE-11,13,16,17,18,21,22,23,34; QE-17/18/34 không có mục chia; QE-23 là Task nhưng gắn `parent-feature` | J6 |
| A9 | P2 | `QE-19` vẫn là Task; Epic 7 (`QE-80`) trống; item mới ở `backlog_solutions.md §3–4` chưa tạo | QE-19 `Task`; QE-80 0 con | J6, J7 |
| A10 | P2 | Không có bằng chứng Scrum (planning, daily, review, retro) | Không có trong repo/Jira | T-S |
| A11 | P2 | Project PROD lỗi thời | PROD-3 In Progress, due 05/10 (quá hạn 35 ngày); PROD-7 không due; PROD-1..7 mô tả rỗng; chưa link sang QE | J8 |
| A12 | P2 | Key QE-67/68/69 trống (đã xóa) | Truy vấn không thấy | ghi chú trong decision log |
| A13 | P2 | Không có Confluence | HTTP 404 | tài liệu nằm trong repo `docs/` |

### B. Git & CI

| ID | Mức | Vấn đề | Bằng chứng | Xử lý |
|---|:-:|---|---|---|
| B1 | P0 | Không có git workflow thật | Chỉ nhánh `main`; 7 commit; 0 PR | T-00 |
| B2 | P1 | Danh tính commit sai, message mơ hồ | Author `Tên Của Bạn`, `=`, `Cường`, `YangKlee`; commit `= update markdown rubric document`; không gắn Jira key | T-00 |
| B3 | P1 | CODEOWNERS vô hiệu | File ở `.github/workflows/CODEOWNERS` (phải là `.github/CODEOWNERS`), chỉ `@GMIOS25` | T-00 |
| B4 | P1 | Thiếu CONTRIBUTING, PR template, protect branch; CI chỉ BE, không lint/coverage/report | `.github/workflows/ci.yml` | T-00, T-05 |
| B5 | P2 | Rác nhỏ | 2 file `.code-workspace` trùng; `docs/Assessment criteria/test.txt` rỗng | T-00 |

### C. Phân tích & Thiết kế

| ID | Mức | Vấn đề | Bằng chứng | Xử lý |
|---|:-:|---|---|---|
| C1 | P1 | Không có ERD / thiết kế CSDL | SRS Phụ lục B: *"chưa có ERD riêng"*; `db/migration` chỉ `.gitkeep` | T-02, T-22 |
| C2 | P1 | Không có tài liệu kiến trúc | Chỉ có `backend-foundation.md` (30 dòng); QE-6 chưa làm | T-22 |
| C3 | P1 | Không đối chiếu được wireframe với SRS | Figma MCP lỗi quyền; không có ảnh export trong repo | B3 |
| C4 | P2 | 25 mục TBD còn Open có thể chặn code | SRS Appendix C: TBD-AUTH-01, TBD-ATT-02, TBD-EVT-03… | `docs/decisions.md` (Assumption log, §6.9) |
| C5 | P2 | Tên role không thống nhất | Proposal 4 role; SRS 5 role (có BCS, CVHT); BE `CLASS_REPRESENTATIVE`; Jira "Admin"; người xử lý appeal: SRS = CVHT/Admin, Jira = Admin | T-22 (bảng ánh xạ role) |
| C6 | P2 | SRS thiếu quản trị thay đổi | Revision History trống; nguồn `uml/*.puml` không có trong repo | T-22 |

### D. Code & Môi trường

| ID | Mức | Vấn đề | Bằng chứng | Xử lý |
|---|:-:|---|---|---|
| D1 | P0 | Chưa có business code | Các package chỉ có `package-info.java`; `SecurityConfiguration` có `anyRequest().denyAll()` | T-02…T-17 |
| D2 | P0 | Không có FE | `D:\Work\QNU-Events FE` rỗng, không `.git` | T-05 |
| D3 | P0 | Không chạy được Postgres/IT local | `docker` không tồn tại trên máy | B2 |
| D4 | P1 | Test chỉ kiểm cấu hình | 9 test: `SecurityConfigurationTest` ×5, `ApplicationIT` ×4 | T-10…T-21 |
| D5 | P1 | Thiếu hạ tầng dùng chung | Chưa có JWT/login, `Clock`, error envelope JSON, 401/403 JSON, idempotency, audit, seed, Dockerfile | T-03, T-04 |
| D6 | P2 | Chưa ép coding convention | Không Spotless/Checkstyle/ESLint | T-00, T-05 |

### E. Kiểm thử · F. Triển khai · G. Trung thực & rủi ro

| ID | Mức | Vấn đề | Xử lý |
|---|:-:|---|---|
| E1 | P0 | Không có test plan, manual TC, automation, bug report; QE-28 chưa bắt đầu | T-21 |
| E2 | P1 | Không có cách thống kê số liệu để khai bảng điểm | T-21.7 (script metrics) |
| F1 | P1 | Không staging; camera điện thoại cần HTTPS (QE-57) | T-23 (tunnel) |
| G1 | P0 | Bảng điểm Sprint 1 yêu cầu khai chính xác; GV vấn đáp, sai → trừ cả nhóm | §5 quy tắc R1 |
| G2 | P0 | Rủi ro scope: 38 SP / 2 ngày / chưa có nền | §4 |

---

## 4. Chiến lược 2 ngày

### 4.1 Nguyên tắc
1. **Một lát cắt dọc chạy được** quan trọng hơn nhiều tính năng dở dang: *login → owner tạo/preview/publish → sinh viên đúng lớp xem → đăng ký (tranh suất cuối) → QR → lịch sử* (đúng Sprint Goal của sprint 41).
2. **Hợp đồng trước, code sau**: schema V1 + API contract (§6) đóng băng trong buổi sáng ngày 1 để 5 luồng chạy song song.
3. **Mỗi module một chủ**: mỗi agent chỉ sửa package/thư mục của Jira owner tương ứng (bảng §4.3).
4. **Mọi thứ phải kiểm chứng bằng lệnh** (test/build). Không có bằng chứng = chưa xong.

### 4.2 Cắt scope (Must / Should / Won't) — bám acceptance criteria của từng item
| Item | MUST (demo 11/11) | SHOULD (nếu còn giờ) | WON'T (để Sprint 2+) |
|---|---|---|---|
| QE-64 | Login/logout/`me`, JWT + session thu hồi được, 2 role (STUDENT, EVENT_OWNER), thông báo lỗi chung, hash BCrypt, seed 1 owner + ≥3 sinh viên, test role sai | Khóa 5 lần sai/15 phút (REQ-AUTH-015) | Google OAuth, cấp tài khoản, BCS/CVHT |
| QE-65 | Tạo nháp, sửa nháp, preview, publish có xác nhận, validate thời gian/quota>0, 1 điều kiện lớp, quyền + ownership | Cửa sổ attendance cấu hình | Quota nhiều tầng, banner, hủy sự kiện |
| QE-66 | Danh sách + chi tiết event đã publish đúng lớp; nháp/ngoài lớp bị lọc ở server | Tìm theo tên | Bộ lọc nâng cao |
| QE-71 | Đăng ký giữ suất nguyên tử; từ chối sai lớp/chưa publish/quá hạn/hết chỗ; unique; đăng ký lặp không chiếm suất; IT tranh suất cuối | — | Waiting list, hủy, restriction no-show |
| QE-56 | Token QR ký HMAC theo epoch, grace 3s, check-in online, 3 attempt, idempotency, 1 attendance hợp lệ | Endpoint ảnh PNG (ZXing) | GPS, evidence, offline |
| QE-57 | Owner: QR tự đổi; Sinh viên: quét camera + ô dán token dự phòng; hiển thị Accepted sau API; chặn bấm lặp | — | Offline queue |
| QE-72 | `/me/history` + trang lịch sử, trạng thái rỗng | — | Evidence/appeal |
| QE-61 | Bảng audit bất biến + 5 hành động (publish, publish bị từ chối, giữ suất, QR accept, QR reject) | Đăng nhập thành công/thất bại | API tra audit |
| QE-70 | BE+FE+DB chạy từ clone sạch, V1, seed, README, lệnh test/reset | CI cho FE | Staging 24/7 |
| QE-28 | Test plan, ≥30 manual TC, automation API (luồng chính + tranh suất + replay QR), 1–2 E2E UI, bug thật lên Jira, bằng chứng | Playwright mobile viewport | Load test (QE-46) |

### 4.3 Phân công theo Jira owner (mỗi người chạy 1 agent session riêng, 1 nhánh/Jira key)
| Agent | Owner | Việc | Phạm vi file được sửa |
|---|---|---|---|
| A0 | Cường | T-00…T-06, QE-64, QE-70 | `config/`, `auth/`, `common/`, `db/migration/V1*`, FE scaffold, CI, docs hạ tầng |
| A1 | Hoàng | T-10, T-11, T-12 (QE-65, QE-66) | `event/`, `FE src/features/events` |
| A2 | Dương | T-13, T-21 (QE-71, QE-28) | `registration/`, `FE src/features/registration`, `src/test/**/e2e`, `docs/test/` |
| A3 | Đan | T-14, T-15 (QE-56, QE-57) | `attendance/`, `FE src/features/attendance` |
| A4 | Khánh | T-16, T-17 (QE-72, QE-61) | `audit/`, `student/` (history), `FE src/features/history` |
| A-J | Cường/Dương | Jira governance J1–J8 | chỉ gọi `twg`, ghi `docs/scrum/jira-change-log.md` |
| A-D | Khánh | T-22 (tài liệu) | `docs/` |

### 4.4 Lịch (đề xuất, giờ UTC+7)
| Mốc | Nội dung | Cổng kiểm tra |
|---|---|---|
| **Thứ Hai 09/11 08:45–10:30** | T-00, T-01, T-02; A-J chạy J1–J3 song song | `mvnw verify` xanh trên nhánh `develop` |
| 10:30–12:30 | T-03, T-04, T-05, T-06 (shared kernel, login, FE scaffold, **khóa contract**) | Login chạy qua Swagger; FE gọi `/api/v1/auth/me` được |
| 13:30–22:00 | T-10…T-17 song song (BE trước, FE sau); QA viết test plan + TC (T-21.1–21.3) | Mỗi PR: build+test xanh, 1 reviewer |
| **Thứ Ba 10/11 08:30–14:00** | Hoàn thiện FE từng màn; nối end-to-end; T-20 | Smoke script chạy xanh |
| 14:00–18:00 | T-21 (automation + chạy manual TC + bug), T-22, T-23 (tunnel + điện thoại thật) | Quét QR bằng điện thoại thành công |
| **18:00** | **FEATURE FREEZE** — từ đây chỉ sửa bug | Tag `v0.1.0-sprint1-rc1` |
| 18:00–23:00 | Bug triage, retest, T-S ngày 2, bằng chứng, báo cáo test | 0 bug Critical mở |
| **Thứ Tư 11/11 07:30–09:30** | Regression cuối, `scripts/sprint1-metrics`, cập nhật Jira (Done + due), diễn tập demo | Demo script chạy trơn 2 lần |
| Giờ review | Demo + Sprint Review + Retro; tag `v0.1.0-sprint1` | — |

> Nếu đến **Thứ Ba 14:00** `QE-56` hoặc `QE-71` chưa qua integration test → dừng mọi việc SHOULD, tập trung 2 item này (đó là lõi của Sprint Goal).

## 5. Quy tắc cho AI-Agent (đưa nguyên văn vào `AGENTS.md`)

| # | Quy tắc |
|---|---|
| R1 | **Không bịa số liệu/bằng chứng.** Không tạo biên bản họp, kết quả test, bug, commit hay PR giả. Template thì được; nội dung chỉ lấy từ dữ liệu thật (git, Jira, kết quả chạy test). |
| R2 | **Không làm yếu test**: cấm `@Disabled`, xóa assert, hạ ngưỡng, `-DskipTests` để "xanh". Test fail 3 lần liên tiếp → ghi `BLOCKERS.md` rồi dừng, hỏi người. |
| R3 | **Một nhánh = một Jira key**: `feature/QE-65-event-crud`. Không push thẳng `main`/`develop`; mở PR. |
| R4 | **Commit**: `type(scope): mô tả ngắn [QE-65]` (Conventional Commits; type ∈ feat, fix, test, docs, chore, refactor, ci). Mỗi commit một mục đích. |
| R5 | Chạy `bash ./mvnw -B -ntp verify` (BE) / `npm run lint && npm run test && npm run build` (FE) **trước mỗi commit**. |
| R6 | **Không sửa migration đã merge vào `develop`.** Thay đổi schema = file mới `V2__…`, `V3__…`. Chỉ A0 tạo migration. |
| R7 | **Không commit bí mật**. Mọi secret qua biến môi trường; `.env.example` chỉ có giá trị dev. Không log mật khẩu, JWT, token QR đầy đủ. |
| R8 | Chỉ sửa file trong **phạm vi của agent** (§4.3). Cần thứ ngoài phạm vi → nêu trong PR, không tự sửa. |
| R9 | **Server là nguồn sự thật** cho thời gian, quyền, quota. Không tin dữ liệu client (role, studentId, timestamp). |
| R10 | **Jira (TWG)**: `help describe` lệnh trước → đọc trạng thái hiện tại → ghi từng lô nhỏ → đọc lại xác minh → ghi vào `docs/scrum/jira-change-log.md`. Không `delete`, không `--yes` cho thao tác phá hủy. Không chạy `twg login/setup/upgrade`. |
| R11 | Quyết định chưa có trong SRS → ghi vào `docs/decisions.md` dạng **Assumption (chưa duyệt)**, không coi là đã chốt. |
| R12 | Thiếu thông tin (Figma, remote, quyền) → **dừng và hỏi**, không đoán, không đi đường vòng qua cơ chế kiểm soát truy cập. |

---

## 6. Hợp đồng kỹ thuật chung (đóng băng trước 12:30 ngày 09/11)

Stack có sẵn: Java 21, Spring Boot 4.1.2, PostgreSQL 17, Flyway, Spring Security (Resource Server/JOSE đã có, **không thêm JJWT**), springdoc, ZXing. Package gốc `vn.edu.qnu.events` với `auth, student, event, registration, attendance, audit, config` (+ mới `common`).

### 6.1 Quy ước chung
- JSON camelCase; thời gian ISO-8601 UTC (`Instant`), UI hiển thị `Asia/Ho_Chi_Minh`.
- Bean `java.time.Clock` (`Clock.systemUTC()`); **mọi** logic thời gian dùng `Clock` để test được bằng `Clock.fixed`.
- Id là `uuid`. Tên bảng số nhiều: `events`, `registrations`, `attendances`, …
- Tiền tố API: `/api/v1`. Trường hợp thành công trả DTO trực tiếp, lỗi trả **error envelope**:
```json
{ "code": "CAPACITY_FULL", "message": "Sự kiện đã hết suất.", "details": {}, "traceId": "…", "timestamp": "2026-11-10T03:00:00Z" }
```
- `message` tiếng Việt có dấu, **không lộ** dữ liệu nhạy cảm (REQ-AUTH-004).

### 6.2 Bảng mã lỗi (ổn định, dùng cho FE & test)
| HTTP | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Sai dữ liệu; `details.fields = {tên-trường: lý do}` |
| 401 | `UNAUTHENTICATED` / `INVALID_CREDENTIALS` / `SESSION_REVOKED` | Chưa đăng nhập / sai MSSV-mật khẩu (thông báo chung) / phiên đã đăng xuất |
| 403 | `FORBIDDEN` / `NOT_IN_AUDIENCE` / `NOT_REGISTERED` | Sai role / đăng ký ngoài lớp / điểm danh khi chưa Registered |
| 404 | `NOT_FOUND` | Không có **hoặc** không được phép xem (nháp, ngoài lớp) — không phân biệt |
| 409 | `EVENT_NOT_PUBLISHED`, `REGISTRATION_CLOSED`, `CAPACITY_FULL`, `PREVIEW_REQUIRED`, `PREVIEW_OUTDATED`, `VERSION_CONFLICT`, `ATTENDANCE_NOT_OPEN`, `ALREADY_CHECKED_IN`, `ATTEMPT_LIMIT_REACHED` | Vi phạm trạng thái/nghiệp vụ |
| 422 | `QR_MALFORMED`, `QR_BAD_SIGNATURE`, `QR_WRONG_EVENT`, `QR_EXPIRED`, `QR_FROM_FUTURE` | Token QR không hợp lệ (có tính 1 attempt) |

### 6.3 Lược đồ CSDL — `src/main/resources/db/migration/V1__sprint1_core_schema.sql`
```sql
CREATE TABLE org_class (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(30) NOT NULL UNIQUE,
  name varchar(150) NOT NULL,
  faculty_name varchar(150) NOT NULL,
  cohort_year smallint,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  login_id varchar(50) NOT NULL UNIQUE,
  password_hash varchar(100) NOT NULL,
  full_name varchar(150) NOT NULL,
  student_code varchar(30) UNIQUE,
  class_id uuid REFERENCES org_class(id),
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','LOCKED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_role (
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  role varchar(30) NOT NULL CHECK (role IN ('STUDENT','CLASS_REPRESENTATIVE','EVENT_OWNER','SYSTEM_ADMIN')),
  PRIMARY KEY (user_id, role)
);

CREATE TABLE auth_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id),
  issued_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz
);
CREATE INDEX ix_auth_session_user ON auth_session(user_id);

CREATE TABLE events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES app_user(id),
  title varchar(200) NOT NULL,
  description text,
  location varchar(300) NOT NULL,
  start_at timestamptz NOT NULL,
  end_at timestamptz NOT NULL,
  registration_deadline timestamptz NOT NULL,
  attendance_open_at timestamptz NOT NULL,
  attendance_close_at timestamptz NOT NULL,
  attendance_method varchar(20) NOT NULL DEFAULT 'DYNAMIC_QR'
    CHECK (attendance_method IN ('DYNAMIC_QR','GPS','EVIDENCE')),
  qr_rotation_seconds int NOT NULL DEFAULT 10 CHECK (qr_rotation_seconds BETWEEN 5 AND 120),
  qr_grace_seconds int NOT NULL DEFAULT 3 CHECK (qr_grace_seconds BETWEEN 0 AND 10),
  quota_total int NOT NULL CHECK (quota_total > 0),
  registered_count int NOT NULL DEFAULT 0,
  activity_type varchar(50) NOT NULL,
  activity_points int NOT NULL DEFAULT 0 CHECK (activity_points >= 0),
  status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED')),
  version int NOT NULL DEFAULT 0,
  previewed_version int,
  published_at timestamptz,
  published_by uuid REFERENCES app_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_events_time CHECK (end_at > start_at),
  CONSTRAINT ck_events_attendance_window CHECK (attendance_close_at > attendance_open_at),
  CONSTRAINT ck_events_quota CHECK (registered_count >= 0 AND registered_count <= quota_total)
);
CREATE INDEX ix_events_owner ON events(owner_id);
CREATE INDEX ix_events_status ON events(status);

CREATE TABLE event_audience (
  event_id uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  class_id uuid NOT NULL REFERENCES org_class(id),
  PRIMARY KEY (event_id, class_id)
);

CREATE TABLE registrations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  status varchar(20) NOT NULL CHECK (status IN ('REGISTERED','WAITING_LIST','CANCELLED','NO_SHOW')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_registrations_active
  ON registrations(event_id, student_id) WHERE status IN ('REGISTERED','WAITING_LIST');
CREATE INDEX ix_registrations_student ON registrations(student_id, created_at DESC);

CREATE TABLE attendances (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  method varchar(20) NOT NULL
    CHECK (method IN ('DYNAMIC_QR','GPS','EVIDENCE','OFFLINE_QR','OFFLINE_GPS','PROXY_EVIDENCE')),
  status varchar(20) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED')),
  recorded_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_attendances_valid ON attendances(event_id, student_id) WHERE status = 'ACCEPTED';

CREATE TABLE attendance_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  method varchar(20) NOT NULL,
  idempotency_key varchar(100) NOT NULL,
  outcome varchar(20) NOT NULL CHECK (outcome IN ('ACCEPTED','REJECTED')),
  reason_code varchar(50),
  counted boolean NOT NULL,
  http_status smallint NOT NULL,
  response_body jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (event_id, student_id, idempotency_key)
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at timestamptz NOT NULL DEFAULT now(),
  actor_id uuid REFERENCES app_user(id),
  actor_role varchar(30),
  action varchar(60) NOT NULL,
  target_type varchar(40) NOT NULL,
  target_id varchar(64),
  outcome varchar(20) NOT NULL CHECK (outcome IN ('SUCCESS','REJECTED','DENIED')),
  reason_code varchar(60),
  details jsonb NOT NULL DEFAULT '{}',
  request_id varchar(64)
);
CREATE INDEX ix_audit_target ON audit_logs(target_type, target_id);
CREATE INDEX ix_audit_time ON audit_logs(occurred_at);

CREATE FUNCTION audit_logs_immutable() RETURNS trigger LANGUAGE plpgsql AS
$$ BEGIN RAISE EXCEPTION 'audit_logs is append-only'; END $$;
CREATE TRIGGER trg_audit_logs_immutable BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();
```
> Hibernate chạy `ddl-auto=validate`: entity phải khớp từng cột/kiểu. `events.version` map `@Version Integer`.

### 6.4 Bảo mật & xác thực
- **JWT HS256** tự ký bằng `NimbusJwtEncoder` + `NimbusJwtDecoder.withSecretKey`. Secret từ `JWT_SECRET` (≥ 32 byte), không có mặc định ở `application.yml` (chỉ có ở `.env.example`/`application-dev.yml`).
- Claims: `sub=userId`, `sid=sessionId`, `roles=[…]`, `exp`. TTL 8 giờ *(Assumption A-01, TBD-AUTH-01)*.
- Mỗi login tạo 1 dòng `auth_session`. `JwtDecoder` có validator: phiên **chưa thu hồi, chưa hết hạn** và user `ACTIVE` → logout là thu hồi thật (`revoked_at`), không chỉ xóa token ở client.
- Chống dò user: user không tồn tại vẫn so khớp BCrypt với hash giả; luôn trả `INVALID_CREDENTIALS`.
- Bean `PasswordEncoder` BCrypt(12) đã có — giữ nguyên.
- Authority = `ROLE_<role>`. Rule trong `SecurityConfiguration` (thay `anyRequest().denyAll()` bằng danh sách tường minh, **giữ `denyAll()` làm rule cuối**):

| Request | Quyền |
|---|---|
| `GET /actuator/health/**`, `POST /api/v1/auth/login` | permitAll |
| `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` | authenticated |
| `GET /api/v1/events`, `GET /api/v1/events/{id}` | `STUDENT` hoặc `EVENT_OWNER` (service lọc theo role) |
| `POST /api/v1/events`, `PUT /api/v1/events/{id}`, `GET …/{id}/preview`, `POST …/{id}/publish`, `GET …/{id}/qr` | `EVENT_OWNER` (+ kiểm tra `owner_id` trong service) |
| `POST …/{id}/registrations`, `GET …/{id}/registrations/me`, `POST …/{id}/attendance/qr`, `GET /api/v1/me/history` | `STUDENT` |
- Thêm `AuthenticationEntryPoint` + `AccessDeniedHandler` trả error envelope JSON (401/403).
- `SecurityConfigurationTest` hiện có (SYSTEM_ADMIN gọi `GET /api/v1/events` → 403) **vẫn phải đúng** — giữ test, bổ sung test mới.

### 6.5 API Sprint 1
| Method + path | Body / Header | Thành công | Ghi chú |
|---|---|---|---|
| `POST /auth/login` | `{loginId, password}` | 200 `{accessToken, expiresAt, user:{id,fullName,roles,classId}}` | REQ-AUTH-001/003/004 |
| `POST /auth/logout` | — | 204 | thu hồi session |
| `GET /auth/me` | — | 200 user | |
| `POST /events` | `EventUpsert` (dưới) | 201 `EventDetail` (status `DRAFT`) | REQ-EVT-001/002/004 |
| `PUT /events/{id}` | `EventUpsert + version` | 200 | chỉ `DRAFT`; `version` lệch → 409 `VERSION_CONFLICT` |
| `GET /events/{id}/preview` | — | 200 `{event, previewVersion}` | ghi `previewed_version = version` (REQ-EVT-011) |
| `POST /events/{id}/publish` | `{confirm:true, previewVersion}` | 200 `EventDetail` (`PUBLISHED`) | `confirm≠true` → 400; chưa preview → 409 `PREVIEW_REQUIRED`; version lệch → 409 `PREVIEW_OUTDATED`; đã publish → 200 trả hiện trạng (REQ-EVT-022) |
| `GET /events?q=&page=&size=` | — | 200 `Page<EventSummary>` | Student: `PUBLISHED` + lớp trong `event_audience`; Owner: event của mình |
| `GET /events/{id}` | — | 200 `EventDetail` | không đủ quyền xem → 404 |
| `POST /events/{id}/registrations` | — | 201 (mới) / 200 (lặp) `{id,status,alreadyRegistered}` | thuật toán §6.6 |
| `GET /events/{id}/registrations/me` | — | 200 / 404 | |
| `GET /events/{id}/qr` | — | 200 `{token, epoch, validUntil, rotationSeconds, serverTime}` | Owner của event, trong cửa sổ attendance, event `PUBLISHED` |
| `POST /events/{id}/attendance/qr` | `{token}` + header **`Idempotency-Key` bắt buộc** | 200 `{status:"ACCEPTED", acceptedAt}` | thuật toán §6.7 |
| `GET /me/history` | — | 200 `[HistoryItem]` | id lấy từ JWT, **không nhận studentId** |

`EventUpsert`: `{title, description, location, startAt, endAt, registrationDeadline, attendanceOpenAt, attendanceCloseAt, attendanceMethod("DYNAMIC_QR"), quotaTotal, audienceClassIds[], activityType, activityPoints, version?}`.
`EventSummary/Detail` thêm (do server tính): `status, remainingQuota, registrationOpen, attendanceOpen, myRegistrationStatus`.
`HistoryItem`: `{eventId, title, startAt, location, registrationStatus, registeredAt, attendanceStatus("NOT_CHECKED_IN"|"ACCEPTED"), attendanceAt, method}`.

Validate (REQ-EVT-004): `title` 3–200; `endAt > startAt`; `quotaTotal ≥ 1`; `attendanceCloseAt > attendanceOpenAt`; `registrationDeadline ≤ attendanceCloseAt`; `audienceClassIds` có ≥ 1 lớp tồn tại; `activityType` không rỗng. *(Quan hệ giữa các mốc là Assumption A-03, TBD-EVT-03.)*

### 6.6 Giữ suất nguyên tử (QE-71) — một `@Transactional`
1. Kiểm tra: user là `STUDENT`; event `PUBLISHED` (else `EVENT_NOT_PUBLISHED`); `now ≤ registration_deadline` (else `REGISTRATION_CLOSED`); `student.class_id ∈ event_audience` (else `NOT_IN_AUDIENCE`).
2. `INSERT INTO registrations(event_id, student_id, status) VALUES (?,?, 'REGISTERED') ON CONFLICT (event_id, student_id) WHERE status IN ('REGISTERED','WAITING_LIST') DO NOTHING RETURNING id`.
   - Không trả dòng → đã đăng ký → đọc và trả bản ghi hiện có (**không** trừ suất).
3. `UPDATE events SET registered_count = registered_count + 1 WHERE id = ? AND registered_count < quota_total`.
   - 0 dòng → ném `ApiException(CAPACITY_FULL)` với `rollbackFor` để **rollback cả INSERT ở bước 2**.
4. Audit `REGISTRATION_CREATE` (SUCCESS) — ghi **sau commit** hoặc `REQUIRES_NEW`. Lần bị từ chối cũng ghi audit `REJECTED` bằng `REQUIRES_NEW`.
- Sprint 1 *không* có waiting list: hết chỗ → `CAPACITY_FULL` (Assumption A-02; REQ-REG-009 thực hiện ở QE-39).

### 6.7 Token QR + check-in (QE-56)
- `QrTokenService.issue(eventId, now)` → `qr1.<eventId>.<epoch>.<base64url(HMAC-SHA256(secret, "qr1."+eventId+"."+epoch))>`, `epoch = floor(nowEpochSeconds / rotationSeconds)`.
- `verify(token, eventId, now)`: tách 4 phần (sai → `QR_MALFORMED`); so chữ ký bằng `MessageDigest.isEqual` (sai → `QR_BAD_SIGNATURE`); `eventId` khác → `QR_WRONG_EVENT`; `epoch > current` → `QR_FROM_FUTURE`; `epoch == current` → OK; `epoch == current-1` và `now − current·rotation < grace` → OK; còn lại `QR_EXPIRED`. Chỉ dùng `Clock` của server.
- Secret: `QR_SIGNING_SECRET` (≥ 32 byte), cấu hình riêng với `JWT_SECRET`.
- Thuật toán `POST /attendance/qr` (service **trả kết quả, không ném exception khi từ chối** để dòng attempt vẫn được commit):
  1. Tra `attendance_attempts` theo `(event, student, Idempotency-Key)` → có → **trả lại đúng response đã lưu**, không tính thêm.
  2. Event phải `PUBLISHED` và `attendance_open_at ≤ now ≤ attendance_close_at` (else `ATTENDANCE_NOT_OPEN`, không tính attempt).
  3. `SELECT … FROM registrations WHERE event_id=? AND student_id=? AND status='REGISTERED' FOR UPDATE` (khóa tuần tự theo sinh viên/event; không có → `NOT_REGISTERED`).
  4. Đã có attendance `ACCEPTED` → `ALREADY_CHECKED_IN` (không tính attempt).
  5. Đếm attempt `counted=true` của `(event, student)`; ≥ 3 → `ATTEMPT_LIMIT_REACHED` (lần thứ 4 bị từ chối).
  6. `verify` token: lỗi → lưu attempt `counted=true`, `reason_code=QR_*`, audit `ATTENDANCE_QR_REJECT`, trả 422.
  7. Hợp lệ → `INSERT INTO attendances … ON CONFLICT DO NOTHING` (unique `ACCEPTED`) + lưu attempt `ACCEPTED` + audit `ATTENDANCE_QR_ACCEPT`.
- *Assumption A-04 (TBD-ATT-02)*: chỉ lỗi `QR_*` tính attempt; `ATTENDANCE_NOT_OPEN`, `NOT_REGISTERED`, `ALREADY_CHECKED_IN` không tính.
- Response lưu trong `response_body` phải **không** chứa token đầy đủ.

### 6.8 Audit (QE-61)
`AuditService.record(actor, action, targetType, targetId, outcome, reasonCode, Map details)` chạy `REQUIRES_NEW`. Action Sprint 1: `EVENT_PUBLISH`, `EVENT_PUBLISH_DENIED`, `REGISTRATION_CREATE`, `REGISTRATION_REJECTED`, `ATTENDANCE_QR_ACCEPT`, `ATTENDANCE_QR_REJECT` (+ tùy chọn `AUTH_LOGIN_SUCCESS/FAILED`). `details` chỉ chứa mã/ID/`tokenHash` (8 hex đầu của SHA-256), **không** mật khẩu/JWT/QR đầy đủ. `occurred_at` do server đặt. Không có API sửa/xóa; trigger DB chặn UPDATE/DELETE.

### 6.9 Sổ giả định `docs/decisions.md` (ghi là *chưa duyệt*)
| ID | Giả định | Liên quan |
|---|---|---|
| A-01 | TTL phiên 8 giờ, không "ghi nhớ đăng nhập" | TBD-AUTH-01 |
| A-02 | Sprint 1 hết chỗ → từ chối `CAPACITY_FULL`, chưa waiting list | QE-39 |
| A-03 | `registrationDeadline ≤ attendanceCloseAt`; cửa sổ điểm danh cấu hình tự do để demo | TBD-EVT-03 |
| A-04 | Chỉ lỗi `QR_*` tính vào 3 attempt | TBD-ATT-02 |
| A-05 | Grace QR = 3 giây theo proposal; cửa sổ sync offline chưa xét | TBD-ATT-01 |
| A-06 | Ánh xạ role: `CLASS_REPRESENTATIVE` = BCS; CVHT chưa có trong Sprint 1; "Admin/Event Owner" = `EVENT_OWNER` | C5 |

### 6.10 Fixture demo (`DemoDataSeeder`, `@Profile({"dev","demo"})`, idempotent theo `login_id`)
| login_id | Vai trò | Lớp |
|---|---|---|
| `owner01` | EVENT_OWNER | — |
| `sv001`, `sv002` | STUDENT | `K49CNTT1` (hai người tranh suất cuối) |
| `sv003` | STUDENT | `K49CNTT2` (khác lớp — kiểm tra quyền) |
| `admin01` | SYSTEM_ADMIN | — (dùng cho test 403) |
Mật khẩu từ `DEMO_PASSWORD` (mặc định dev `Demo@12345`), **băm bằng PasswordEncoder lúc seed**, không commit hash. Đặt lại dữ liệu: `scripts/reset-demo-data.(ps1|sh)` (TRUNCATE các bảng nghiệp vụ rồi restart app) — chỉ chạy được khi profile `dev`/`demo`.

---

## 7. Task card cho AI-Agent

Mẫu mỗi task: **Mục tiêu · Phụ thuộc · Nhánh · Các bước (làm đúng thứ tự) · File · Kiểm chứng · DoD**. Số test là *mục tiêu thực tế*; mỗi test phải assert hành vi thật, **cấm test rỗng để tăng số lượng** (R1, R2).

### PHASE 0 — Nền tảng (09/11 08:45 → 12:30)

#### T-00 · Vệ sinh repo & quy trình Git (A0) — nhánh `chore/QE-70-repo-hygiene`
1. Cấu hình git: `git config user.name "Tran Thanh Cuong"` và `git config user.email "tranthanhcuongit@gmail.com"` (đã xác nhận ở D4).
2. Từ `main` tạo `develop` và push. Mọi nhánh feature rẽ từ `develop`.
3. Tạo `AGENTS.md` ở gốc repo: dán §5 (R1–R12), bảng phạm vi file (§4.3), lệnh build/test, trỏ tới `docs/agent/sprint1-rescue-plan.md`. Sao chép bản kế hoạch này vào `docs/agent/sprint1-rescue-plan.md`.
4. `git mv .github/workflows/CODEOWNERS .github/CODEOWNERS`; nội dung:
   ```text
   * @GMIOS25
   /src/main/java/vn/edu/qnu/events/event/ @hoangkq157
   /src/main/java/vn/edu/qnu/events/registration/ @YangKlee
   /src/main/java/vn/edu/qnu/events/attendance/ @Dan28-05
   /src/main/java/vn/edu/qnu/events/audit/ @KHANHSKY0808
   /src/main/java/vn/edu/qnu/events/student/ @KHANHSKY0808
   /docs/ @KHANHSKY0808 @GMIOS25
   ```
5. Tạo `CONTRIBUTING.md`: mô hình nhánh `main` ← `develop` ← `feature/QE-xx-*`; Conventional Commits có `[QE-xx]`; PR cần ≥ 1 review + CI xanh; Definition of Done; lệnh test. Tạo `.github/pull_request_template.md` (Jira key, mô tả, REQ/UC liên quan, cách test, checklist DoD, ảnh/bằng chứng).
6. Sửa `.github/workflows/ci.yml`: `on: push: branches: [main, develop]` + `pull_request`; bỏ `master`; thêm bước upload `target/surefire-reports` và `target/failsafe-reports` (`actions/upload-artifact`).
7. Xóa `docs/Assessment criteria/test.txt` (file rỗng).
8. **Việc thủ công của người dùng** (ghi vào PR): GitHub → Settings → Branches → bảo vệ `main` và `develop` (require PR, 1 approval, status check `verify`).
- **Kiểm chứng:** `git branch -a` có `develop`; `.github/CODEOWNERS` tồn tại; CI chạy trên PR.
- **DoD:** PR đầu tiên được merge theo đúng quy trình (có review thật của người khác).

#### T-01 · Môi trường local (A0 + người dùng) — nhánh `chore/QE-70-dev-env`
1. **Người dùng** thực hiện B2 (Docker Desktop hoặc dùng Codespaces).
2. Chạy: `Copy-Item .env.example .env` → `docker compose up -d --wait postgres` → `.\mvnw.cmd -B -ntp -Pintegration verify`. Kỳ vọng hiện tại: **9 test xanh** (5 unit + 4 IT).
3. Bổ sung vào `.env.example`: `JWT_SECRET`, `QR_SIGNING_SECRET`, `DEMO_PASSWORD` (giá trị chỉ để dev, ≥ 32 ký tự cho 2 secret) và vào `application.yml` các khóa `app.security.jwt-secret`, `app.qr.signing-secret`, `app.seed.demo-password` (đọc từ biến môi trường, **không** default cho prod).
4. FE: `git init -b main` trong `D:\Work\QNU-Events FE` (T-05 làm tiếp).
- **Kiểm chứng:** log `Tests run: 9, Failures: 0`. **DoD:** từng thành viên dựng lại được trên máy mình (yêu cầu của QE-70).

#### T-02 · Schema V1 + ERD (A0) — nhánh `feature/QE-70-schema-v1`
1. Tạo `src/main/resources/db/migration/V1__sprint1_core_schema.sql` đúng nội dung §6.3 (xóa `.gitkeep`).
2. Test `db/MigrationIT` (`@SpringBootTest`, profile `dev`): (a) tất cả bảng tồn tại; (b) insert `quota_total=0` bị CHECK từ chối; (c) hai `registrations` active cùng `(event, student)` bị unique chặn, nhưng `CANCELLED` + `REGISTERED` thì cho phép; (d) hai `attendances` `ACCEPTED` cùng `(event, student)` bị chặn; (e) `UPDATE`/`DELETE` `audit_logs` ném lỗi.
3. Tạo `docs/design/erd.md` bằng Mermaid `erDiagram` **sinh đúng từ SQL ở bước 1** (đủ PK/FK, ghi chú index unique).
- **Kiểm chứng:** `.\mvnw.cmd -B -ntp -Pintegration verify` xanh; `SELECT version, success FROM flyway_schema_history` có V1 `true`.
- **DoD:** merge vào `develop` **trước 10:30**. Sau merge, V1 bị đóng băng (R6).

#### T-03 · Shared kernel & Security (A0) — nhánh `feature/QE-64-shared-kernel`
Tạo trong `vn.edu.qnu.events`:
1. `common/ErrorCode` (enum mã §6.2 kèm HTTP status), `ApiException`, `ApiError` (record, §6.1), `GlobalExceptionHandler` (`@RestControllerAdvice`: `ApiException`, `MethodArgumentNotValidException` → `VALIDATION_FAILED` + `details.fields`, `ConstraintViolationException`, `HttpMessageNotReadableException` → 400, `AccessDeniedException` → 403, còn lại → 500 `INTERNAL_ERROR` **không** lộ stack/message).
2. `config/ClockConfiguration` (bean `Clock.systemUTC()`); test helper `src/test/.../support/MutableClock`.
3. `common/CurrentUser` (record `userId, sessionId, roles, classId`) + `CurrentUserProvider.require()` đọc từ `JwtAuthenticationToken`.
4. `audit/AuditService` + `AuditLog` entity/repo theo §6.8 (`@Transactional(propagation = REQUIRES_NEW)`).
5. `config/SecurityConfiguration`: thay `denyAll()` bằng bảng quyền §6.4 (giữ `denyAll()` cuối), thêm `AuthenticationEntryPoint` và `AccessDeniedHandler` trả error envelope; `AccessDeniedHandler` **ghi audit `EVENT_PUBLISH_DENIED`** (DENIED) khi đường dẫn khớp `/api/v1/events/*/publish`.
6. Test helper `support/TestUsers` (tạo user theo role trực tiếp qua repository) + `support/TestAuth` (phát JWT hợp lệ cho user test).
- **Kiểm chứng:** test mới: 401 JSON đúng shape; 403 JSON đúng shape; 400 validation có `details.fields`; 500 không lộ stack; `SecurityConfigurationTest` cũ vẫn xanh.
- **DoD:** merge trước 11:30; các agent khác rebase `develop`.

#### T-04 · Đăng nhập & phân quyền — QE-64 (A0) — nhánh `feature/QE-64-login`
1. Entity `auth/AppUser`, `auth/UserRole`, `auth/AuthSession`, `student/OrgClass` + repository (khớp V1).
2. `JwtService` (phát/giải mã, §6.4), `SessionJwtValidator` (kiểm `sid` chưa thu hồi/hết hạn, user `ACTIVE`).
3. `AuthController` + `AuthService`: `POST /auth/login`, `POST /auth/logout` (204, đặt `revoked_at`), `GET /auth/me`; thêm `GET /classes` (EVENT_OWNER) trả `[{id, code, name}]`.
4. `DemoDataSeeder` theo §6.10 (idempotent, băm mật khẩu bằng `PasswordEncoder`).
5. Test (mục tiêu ≥ 8): login đúng → token; sai mật khẩu và user không tồn tại → **cùng** body `INVALID_CREDENTIALS`; user `LOCKED` bị từ chối; mật khẩu trong DB bắt đầu `$2` và ≠ plaintext; logout rồi dùng lại token → 401 `SESSION_REVOKED`; chưa đăng nhập → 401; STUDENT gọi `POST /events` → 403; EVENT_OWNER gọi `POST /events/{id}/registrations` → 403.
- **Kiểm chứng:** Swagger `http://localhost:8080/swagger-ui/index.html` đăng nhập `owner01` thành công.
- **DoD:** merge trước 12:30 để mở khóa T-10…T-17.

#### T-05 · FE scaffold (A0) — nhánh `feature/QE-70-fe-scaffold` (repo FE)
1. Trong `D:\Work\QNU-Events FE`:
```powershell
git init -b main
npm create vite@latest . -- --template react-ts
npm i react-router-dom @tanstack/react-query react-hook-form zod @hookform/resolvers qrcode.react html5-qrcode
npm i -D tailwindcss @tailwindcss/vite vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event @playwright/test
```
2. `vite.config.ts`: `server.proxy['/api'] = 'http://localhost:8080'`, `server.host = true`, `server.allowedHosts = ['.trycloudflare.com', '.loca.lt']` (để dùng tunnel HTTPS cho camera). FE gọi API qua đường dẫn tương đối `/api/v1` ⇒ **một tunnel là đủ**, không vướng CORS/mixed content.
3. Cấu trúc:
```
src/app/            router, providers, layout (mobile-first)
src/api/client.ts   fetch wrapper: gắn Bearer, parse error envelope, ném ApiError{code,message,details}
src/features/{auth,events,registration,attendance,history}/
src/components/     Button, Field, StatusBadge, EmptyState, ErrorBanner, ConfirmDialog, Toast
src/lib/            time.ts (Asia/Ho_Chi_Minh), idempotency.ts (crypto.randomUUID)
```
4. Auth: `AuthProvider`, `useAuth`, `RequireRole`; token lưu **bộ nhớ + `sessionStorage`** *(Assumption, ghi vào decisions.md: chấp nhận rủi ro XSS ở MVP, không dùng localStorage)*. Màn `/login`.
5. **Stub bắt buộc** để các agent không đụng nhau: `features/registration/index.ts` export `RegistrationSlot`, `features/attendance/index.ts` export `AttendanceSlot` (mỗi cái `({eventId}) => null`), `features/history/index.ts` export `HistoryPage`.
6. Quy ước UI (NFR-QUAL-006, NFR-SAFE-001): trạng thái hiển thị **chữ + biểu tượng**, không chỉ màu; mọi input có label; lỗi gắn đúng trường; viewport tối thiểu 320px; mọi thông báo bằng tiếng Việt có dấu. Màu/typography lấy từ Figma khi có (B3), nếu chưa thì dùng token mặc định và ghi TODO.
7. Scripts `lint`, `typecheck`, `test`, `build`, `e2e`; `.env.example`; `README.md` (chạy cùng BE); `.github/workflows/ci.yml`: `npm ci && npm run lint && npm run typecheck && npm run test -- --run && npm run build`.
8. Tạo repo remote (D3), push `main` + `develop`.
- **Kiểm chứng:** `npm run dev`, đăng nhập `sv001` thấy trang chủ theo role; CI xanh.
- **DoD:** merge trước 12:30.

#### T-06 · Đóng băng hợp đồng (A0)
Tạo `docs/api/sprint1-contract.md` (sao chép §6.2, §6.5, §6.7 nguyên văn) và tag `contract-v1`. Từ đây đổi contract phải: sửa file này + nêu trong PR + báo agent liên quan (R8).

### PHASE 1 — Luồng tính năng song song (09/11 13:00 → 10/11 14:00)

> Thứ tự trong từng task: **viết test trước (đỏ) → code → xanh → refactor** (nên dùng skill `test-driven-development`).

#### T-10 · Event BE — QE-65 (A1 Hoàng) — nhánh `feature/QE-65-event-crud`
- **Phụ thuộc:** T-02, T-03, T-04.
- **File (package `event/`):** `Event`, `EventStatus`, `EventAudience`, `EventRepository`, `EventService`, `EventController`, DTO record `EventUpsert/EventSummary/EventDetail/PublishRequest/PreviewResponse`, `EventValidator`.
- **Bước:** entity khớp V1 (`@Version Integer version`); `create` (status `DRAFT`, `owner_id` = người gọi); `update` (chỉ `DRAFT`, kiểm `owner_id`, `version` lệch → `VERSION_CONFLICT`); `preview` (ghi `previewed_version`); `publish` theo bảng §6.5; mỗi lần publish thành công/bị từ chối gọi `AuditService`.
- **Test (≥ 10):** tạo nháp hợp lệ; `endAt ≤ startAt`; `quotaTotal=0`; `audienceClassIds` rỗng; STUDENT tạo → 403; owner A sửa event owner B → 403; publish khi chưa preview → `PREVIEW_REQUIRED`; publish với version cũ → `PREVIEW_OUTDATED`; publish thiếu `confirm` → 400; publish đúng → `PUBLISHED`, `published_by`, `published_at`; publish lần 2 → 200 không đổi dữ liệu/không audit trùng; sửa sau publish → 409; có audit `EVENT_PUBLISH` và `EVENT_PUBLISH_DENIED`.
- **Kiểm chứng:** `.\mvnw.cmd -B -ntp -Pintegration verify`. **DoD:** AC của QE-65 (phần API) pass.

#### T-11 · Event FE — QE-65 (A1) — nhánh `feature/QE-65-event-ui` (repo FE)
- Route: `/owner/events` (danh sách + trạng thái), `/owner/events/new`, `/owner/events/:id/edit`, `/owner/events/:id/preview`.
- Form: `react-hook-form + zod`; nạp lớp từ `GET /classes`; input thời gian chuyển đổi đúng `Asia/Ho_Chi_Minh` ↔ UTC; lỗi server `details.fields` hiện đúng ô.
- Preview: banner "Bản xem trước — chưa công bố"; nút **Công bố** mở `ConfirmDialog` (nêu rõ tác động); chỉ gửi `confirm: true` sau khi xác nhận; xử lý `PREVIEW_OUTDATED` bằng thông báo "Nội dung đã thay đổi, xem trước lại".
- **Test (vitest, ≥ 3):** hiển thị lỗi theo trường; nút Công bố cần xác nhận; xử lý `PREVIEW_OUTDATED`.

#### T-12 · Sinh viên xem sự kiện — QE-66 (A1) — nhánh `feature/QE-66-student-events`
- **BE:** `GET /events` (Student: `PUBLISHED` ∧ lớp ∈ `event_audience`; `q` lọc theo tiêu đề không phân biệt hoa thường; phân trang; Owner: event của mình), `GET /events/{id}` (không thấy → **404**). Server tính `remainingQuota, registrationOpen, attendanceOpen, myRegistrationStatus`.
- **FE:** `/events` (thẻ sự kiện, trạng thái rỗng "Chưa có sự kiện dành cho lớp bạn"), `/events/:id` hiển thị chi tiết và **render** `<RegistrationSlot eventId>` + `<AttendanceSlot eventId>` (import từ stub của T-05).
- **Test BE (≥ 6):** đúng lớp thấy; sai lớp → 404 ở chi tiết và vắng mặt ở danh sách; nháp ẩn cả list và detail (gọi trực tiếp → 404); tìm theo `q`; owner chỉ thấy event của mình; các trường tính toán đúng với `MutableClock`.

#### T-13 · Đăng ký giữ suất — QE-71 (A2 Dương) — nhánh `feature/QE-71-register`
- **File (`registration/`):** `Registration`, `RegistrationStatus`, `RegistrationRepository` (native query `INSERT … ON CONFLICT … RETURNING id` và `UPDATE events … WHERE registered_count < quota_total`), `RegistrationService`, `RegistrationController`.
- **Bước:** làm **đúng** thuật toán §6.6, trong **một** `@Transactional(rollbackFor = Exception.class)`.
- **Test (≥ 8, trong đó IT trên Postgres thật):**
  1. quota=1, hai sinh viên cùng lớp đăng ký **đồng thời** (`ExecutorService` + `CountDownLatch`) → đúng 1 `REGISTERED`, người kia `CAPACITY_FULL`, `registered_count=1`.
  2. quota=5, 30 sinh viên đồng thời → đúng 5 dòng `REGISTERED` và `registered_count=5`.
  3. Cùng một sinh viên gửi 10 request song song → 1 dòng, `registered_count` tăng 1, các request còn lại trả bản ghi hiện có (`alreadyRegistered=true`).
  4. Sai lớp → 403 `NOT_IN_AUDIENCE`; event `DRAFT` → 409 `EVENT_NOT_PUBLISHED`; quá hạn (đặt `MutableClock`) → 409 `REGISTRATION_CLOSED`; chưa đăng nhập → 401; owner đăng ký → 403.
  5. Đăng ký thất bại `CAPACITY_FULL` **không** để lại dòng `registrations` mồ côi (assert đếm bảng).
- **FE (`features/registration/RegistrationPanel.tsx` + export `RegistrationSlot`):** nút *Đăng ký*; chỉ báo thành công **sau khi API trả**; vô hiệu hóa nút khi đang gửi; hiển thị lý do lỗi theo `code` (bảng ánh xạ trong file `messages.ts`); test vitest ≥ 3.
- **DoD:** AC QE-71 pass, bằng chứng test tranh suất lưu vào `docs/test/evidence/`.

#### T-14 · QR BE — QE-56 (A3 Đan) — nhánh `feature/QE-56-qr-backend`
- **File (`attendance/`):** `QrTokenService`, `Attendance`, `AttendanceAttempt`, repositories, `AttendanceService`, `AttendanceController`, `CheckInResult`.
- **Bước:** làm **đúng** §6.7. Service trả `CheckInResult` (không ném exception khi từ chối) để attempt luôn được commit. `GET /events/{id}/qr` chỉ cho owner của event, trong cửa sổ attendance, event `PUBLISHED`.
- **Test (≥ 12):** ma trận token với `MutableClock`: hiện tại ✓, epoch trước trong grace ✓, epoch trước ngoài grace ✗, epoch tương lai ✗, chữ ký sửa ✗, sai event ✗, 3 dạng malformed ✗; ngoài cửa sổ → `ATTENDANCE_NOT_OPEN`; chưa Registered → `NOT_REGISTERED`; thành công tạo đúng 1 `attendances`; **cùng `Idempotency-Key` gửi lại** → response y hệt và không tăng attempt; khác key sau khi đã Accepted → `ALREADY_CHECKED_IN`; 3 token sai rồi lần 4 (kể cả token đúng) → `ATTEMPT_LIMIT_REACHED`; 10 request song song hợp lệ của 1 sinh viên → đúng 1 attendance; có audit accept/reject; `response_body` và `details` audit **không chứa** token đầy đủ.
- **DoD:** AC QE-56 pass; `QrTokenService` có test đơn vị độc lập với DB.

#### T-15 · QR FE — QE-57 (A3) — nhánh `feature/QE-57-qr-ui` (repo FE)
- **Owner** `/owner/events/:id/qr`: lấy `GET …/qr`, vẽ bằng `qrcode.react`, tự làm mới trước `validUntil` (dùng `serverTime` để bù lệch đồng hồ), vòng đếm ngược, thông báo khi ngoài cửa sổ attendance.
- **Sinh viên** `AttendancePanel` (+ export `AttendanceSlot`): nút *Quét QR điểm danh* → camera toàn màn hình bằng `html5-qrcode`; từ chối quyền camera → hướng dẫn + **ô dán mã dự phòng**; mỗi lần quét sinh **một** `Idempotency-Key` và **tái dùng khi retry do mạng**; nút khóa khi đang xử lý; chỉ hiện "Đã điểm danh" sau khi API trả `ACCEPTED`.
- **Ánh xạ thông báo (file `messages.ts`):**

| code | Hiển thị |
|---|---|
| `QR_EXPIRED` | "Mã đã hết hạn. Hãy quét mã đang hiển thị trên màn hình." |
| `QR_BAD_SIGNATURE`, `QR_MALFORMED`, `QR_WRONG_EVENT` | "Mã QR không hợp lệ cho sự kiện này." |
| `ATTENDANCE_NOT_OPEN` | "Chưa đến hoặc đã hết thời gian điểm danh." |
| `NOT_REGISTERED` | "Bạn chưa có suất đăng ký sự kiện này." |
| `ALREADY_CHECKED_IN` | "Bạn đã điểm danh rồi." |
| `ATTEMPT_LIMIT_REACHED` | "Bạn đã dùng hết số lần thử điểm danh." |
- **Test (vitest, ≥ 4):** ánh xạ lỗi; tái dùng idempotency key khi retry; nút bị khóa khi đang gửi; luồng dán mã dự phòng.
- **Cảnh báo:** camera chỉ chạy ở `localhost` hoặc HTTPS → xem T-23.

#### T-16 · Lịch sử — QE-72 (A4 Khánh) — nhánh `feature/QE-72-history`
- **BE:** `GET /me/history` (id lấy từ JWT, **không có** tham số studentId): `registrations ⟕ attendances(ACCEPTED) ⋈ events`, sắp xếp `start_at DESC`.
- **FE:** `/history` — thẻ có chữ + biểu tượng cho trạng thái; trạng thái rỗng "Bạn chưa đăng ký sự kiện nào"; lỗi có nút thử lại.
- **Test BE (≥ 5):** chưa điểm danh → `NOT_CHECKED_IN`; sau QR → `ACCEPTED` kèm thời điểm do server ghi; hai sinh viên không thấy dữ liệu của nhau; danh sách rỗng trả `[]`; tải lại vẫn thấy.

#### T-17 · Audit — QE-61 (A4) — nhánh `feature/QE-61-audit`
- Rà soát việc gắn `AuditService` vào 3 luồng (event publish, giữ suất, QR) và `AccessDeniedHandler`; bổ sung phần còn thiếu.
- **Test (≥ 5):** đủ 6 action §6.8 xuất hiện sau luồng chuẩn; mỗi bản ghi có actor/action/target/outcome/`occurred_at` do server; **không** có mật khẩu, JWT hay token QR đầy đủ trong `details`; UPDATE/DELETE bị trigger chặn; không có endpoint nào sửa/xóa audit (kiểm bằng danh sách mapping).

### PHASE 2 — Tích hợp, kiểm thử, tài liệu, demo (10/11 08:30 → 11/11)

#### T-20 · Tích hợp (A0 + mọi owner)
1. Viết `scripts/smoke.ps1` (+ `smoke.sh`): đăng nhập owner → `GET /classes` → tạo event (mốc thời gian tính theo *bây giờ*: deadline +1h, attendance `now-1m → now+2h`, quota 1) → preview → publish → đăng nhập `sv001`, `sv002` → cùng đăng ký → **đúng 1** thành công → owner lấy QR → `sv001` check-in → `GET /me/history` → check-in lần 2 → `ALREADY_CHECKED_IN`. In bảng PASS/FAIL, **exit code ≠ 0 nếu có bước sai**.
2. Xuất `docs/api/openapi-sprint1.json` từ `/v3/api-docs` (profile `dev`).
3. Sửa lỗi tích hợp; mọi lỗi tìm thấy do QA phải có Bug (J5).

#### T-21 · QA — QE-28 (A2 Dương) — nhánh `feature/QE-28-qa`
1. `docs/test/test-plan.md`: phạm vi (10 item Sprint 1), chiến lược (unit / IT / E2E / manual), môi trường, tiêu chí vào/ra, vai trò, rủi ro, quy trình bug, liên kết truy vết.
2. `docs/test/manual-test-cases.csv` (cột: `ID, REQ/UC, Jira, Tiêu đề, Điều kiện, Các bước, Kết quả mong đợi, Kết quả thực tế, Pass/Fail, Người test, Ngày, Bug key, Bằng chứng`). **≥ 30 TC**: Auth 6, Event 7, Hiển thị 4, Đăng ký 7, QR 8, Lịch sử 3. Các cột *thực tế / kết quả / người test / ngày / bằng chứng* **chỉ điền sau khi chạy thật** (R1).
3. Automation API (`src/test/java/.../e2e/`, gắn `@Tag("e2e")`): `Sprint1CoreFlowIT`, `ConcurrentLastSeatIT` (nếu T-13 chưa có), `QrReplayIT`, `RolesMatrixIT` (tham số hóa role × endpoint, kỳ vọng 200/401/403/404).
4. E2E UI (repo FE, Playwright, viewport 390×844): `e2e/core-flow.spec.ts` — owner tạo → preview → publish; student đăng ký; check-in qua ô dán mã; lịch sử hiện Accepted. *Camera không test được trong CI → có manual TC chạy trên điện thoại thật.*
5. Chạy manual TC trên môi trường tunnel/staging (T-23), điền kết quả thật; mỗi lỗi → Bug (J5) rồi retest sau khi sửa.
6. `docs/test/sprint1-test-report.md`: bảng TC (đã chạy/pass/fail/blocked), số automation, bug (mở/đã sửa + key Jira), khoảng trống đã biết, xác nhận người chạy.
7. `scripts/sprint1-metrics.ps1`: in ra (kèm timestamp) số `@Test` của unit test, IT/e2e, test FE (`it(`/`test(`), số dòng TC trong CSV, số Bug từ Jira (`twg jira workitem query --jql "project = QE AND issuetype = Bug"`). Dùng để điền bảng điểm.
8. Bằng chứng ảnh/video: `docs/test/evidence/<TC-ID>/`.

#### T-22 · Tài liệu thiết kế & truy vết (A-D Khánh) — nhánh `docs/QE-80-design-docs`
Tạo, đều bằng Markdown + Mermaid, **bám sát code đã có** (không mô tả thứ chưa làm là đã làm):
- `docs/design/architecture.md` (C4 mức 1–2, bản đồ module, luồng request, mô hình bảo mật, nhất quán dữ liệu, side-effect async, topology demo, phạm vi loại trừ).
- `docs/design/sequence-sprint1.md`: login · publish · giữ suất nguyên tử · check-in QR.
- `docs/design/state-machines.md`: Event, Registration (tập con Sprint 1), Attendance.
- `docs/roles-mapping.md` (C5), `docs/decisions.md` (A-01…A-06 + các TBD liên quan), `docs/release-notes-sprint1.md`.
- `docs/traceability-matrix.md`: `REQ/UC → Jira → class/package → test (TC id / lớp automation) → PR` (khởi tạo từ §8.1; cột PR chỉ điền link thật).
- `docs/srs-addendum-traceability.md`: bảng sẵn để **người dùng dán** làm Phụ lục D của SRS (agent không sửa file `.docx`).
- Khi có Figma (B3): `docs/design/ui-spec-sprint1.md` (màn hình → route → node Figma → UC → API).

#### T-23 · Môi trường demo (A0 + người dùng)
1. Chạy BE `dev` + FE `npm run dev -- --host`.
2. **HTTPS cho camera:** `winget install -e --id Cloudflare.cloudflared` rồi `cloudflared tunnel --url http://localhost:5173` (hoặc `npx localtunnel --port 5173`). Mở URL `https://*.trycloudflare.com` trên **1 điện thoại Android + 1 iPhone (Safari)**, thử quét QR thật.
3. `docs/demo/sprint1-demo-script.md` (~10 phút, §10) kèm **phương án dự phòng**: dán mã khi camera lỗi; lệnh reset dữ liệu; event dựng sẵn; video quay màn hình làm backup.
4. Diễn tập 2 lần; ghi lại thời lượng và lỗi gặp phải thành Bug thật.
5. *(Tùy chọn)* `Dockerfile` đa tầng (Temurin 21) + deploy lên Railway/Render để tăng điểm "Triển khai".

#### T-S · Bằng chứng Scrum (mọi thành viên; agent chỉ soạn khung + dữ kiện)
- `docs/scrum/sprint-1/{planning,daily-2026-11-09,daily-2026-11-10,daily-2026-11-11,review,retro}.md`.
- Agent **tự điền phần dữ kiện thật**: commit từ `git log --since`, chuyển trạng thái Jira, PR đã merge; còn các ô "đã làm/sẽ làm/vướng mắc" và "người xác nhận" **do con người điền** (R1).

---

## 8. Runbook Jira (TWG) — chạy song song từ sáng 09/11

Chuẩn bị (PowerShell):
```powershell
$env:TWG_AGENT_DEFAULTS = "1"
$t    = "$env:LOCALAPPDATA\Programs\twg\bin\twg.exe"
$site = "tranthanhcuongit.atlassian.net"   # project QE, board id 4, sprint id 41
```
Quy trình mỗi lệnh ghi (R10): `& $t help describe "<lệnh>"` → đọc trạng thái hiện tại → ghi **từng lô ≤ 10 item** → đọc lại xác minh → ghi dòng vào `docs/scrum/jira-change-log.md` (thời điểm, key, trường, giá trị cũ → mới). Đã xác nhận có các lệnh: `jira workitem create|update|transition`, `jira sprint get|update`, `jira space issue-types` (QE có `Epic, Story, Task, Feature, Bug, Subtask`).

| ID | Việc | Lệnh / cách làm | Điều kiện |
|---|---|---|---|
| **J1** | Căn ngày sprint với deadline thật | `& $t jira sprint get --id 41 --site $site` (đọc) → sau khi người dùng trả lời D1: `& $t jira sprint update --id 41 --board-id 4 --end-date "<ISO-8601 +07:00 của giờ review>" --site $site` → đọc lại. **Không** đổi `name`, `goal`, `state`, `startDate` | Cần D1 |
| **J2** | Due date | 10 item Sprint 1: `foreach ($k in 'QE-28','QE-56','QE-57','QE-61','QE-64','QE-65','QE-66','QE-70','QE-71','QE-72') { & $t jira workitem update --id $k --due-date 2026-11-11 --site $site }` (đổi ngày nếu D1 khác). Item `decision-needed` (QE-15, 36, 38, 41, 44, 45, 53): **đề xuất** `2026-11-18`. Item còn lại gán theo Sprint dự kiến ở `backlog_solutions.md §1.2` (ghi rõ "dự kiến" trong comment) — làm sau, không chặn demo | — |
| **J3** | Sub-task = checklist (§8.2) | `& $t jira workitem create --space QE --type Subtask --parent QE-64 --summary "[QE-64][BE] …" --assignee <accountId-owner> --labels "sprint-1" --site $site`. `accountId` lấy từ `twg jira workitem get QE-64 -o json` (`assignee.accountId`) | — |
| **J4** | Khớp mã SRS ↔ Jira (§8.1) | Thêm label `uc0xx` và `req-auth/req-evt/req-reg/req-att/req-rev/req-data` bằng `--add-labels`; thêm **comment** liệt kê chính xác mã: `… workitem update --id QE-64 --comment "Mã SRS: UC001, UC003; REQ-AUTH-001, 003…" --comment-format markdown`. **Không ghi đè `--description`** (sẽ mất nội dung/định dạng) | — |
| **J5** | Quản lý bug | Bug **chỉ tạo khi có lỗi thật**: `& $t jira workitem create --space QE --type Bug --summary "[QE-71] <ngắn gọn>" --description-format markdown --description "<mẫu bên dưới>" --priority High --labels "bug,found-by-qa" --assignee <dev> --site $site`; rồi `help describe "jira workitem link workitem"` để link tới story + `QE-28`. Khi sửa: comment kèm PR/commit, chuyển trạng thái, ghi retest | — |
| **J6** | Dọn cấu trúc | `QE-19`: `& $t jira workitem update --id QE-19 --type Story --site $site` (nếu bị từ chối do team-managed → báo người dùng đổi tay: B5). Với QE-17, 18, 34 bổ sung comment "Phân chia triển khai" theo mẫu của QE-13; QE-23: đổi `Task → Story` hoặc bỏ label `parent-feature` | — |
| **J7** | Epic 7 (`QE-80`) | Tạo Task con cho tài liệu bàn giao: *Git workflow & convention (2SP), CI/CD (5), Test plan (3), Manual TC (5), Automation (5), Test report (2), Kiến trúc (3), UML (5), Wireframe (8), SRS (8)* theo `backlog_solutions.md §4`. **KHÔNG thêm vào Sprint 1** (tránh đổi scope giữa sprint, xem D5); chuyển `Done` đúng lúc deliverable thực sự có | Cần D5 |
| **J8** | Dọn PROD | PROD-3: theo D6 (đóng hoặc đặt `--due-date` mới); PROD-7: thêm due; PROD-1..7: bổ sung mô tả "Output: QE-xx" + link `relates to` | Cần D6 |
| **J9** | Cập nhật trạng thái thật | Khi bắt đầu nhánh → `--status "In Progress"`; khi PR merge → `Done` (hoặc `In Review`/`Testing` nếu B5 đã thêm cột). **Không** chuyển `Done` nếu chưa đạt DoD | liên tục |

**Mẫu mô tả Bug:** `**Môi trường:** … (commit SHA) · **Các bước tái hiện:** 1… 2… · **Kết quả mong đợi:** … · **Kết quả thực tế:** … · **Mức độ:** Critical/Major/Minor · **Liên quan:** QE-xx, TC-xxx · **Bằng chứng:** đường dẫn ảnh/log`.

### 8.1 Bảng truy vết SRS ↔ Jira (Sprint 1) — dùng cho J4 và `docs/traceability-matrix.md`
| Jira | Use case (SRS) | Yêu cầu (SRS) | Ghi chú phạm vi Sprint 1 |
|---|---|---|---|
| QE-64 | UC001, UC003 | REQ-AUTH-001, 003, 004, 005, 006, 007; NFR-SEC-003 | Không có Google (REQ-AUTH-002), khóa 5 lần (REQ-AUTH-015) là SHOULD |
| QE-65 | UC004, UC013 | REQ-EVT-001, 002, 004, 005 (1 tầng), 007, 008, 011, 012, 013, 022, 023 | Quota nhiều tầng/banner ngoài scope |
| QE-66 | UC009 | REQ-REG-001, 002; REQ-EVT-002 | Bộ lọc nâng cao (TBD-REG-01) ngoài scope |
| QE-71 | UC010 | REQ-REG-004, 005, 007, 008 (một phần: hết chỗ → từ chối), 010; REQ-EVT-016, 017; NFR-QUAL-001 | Waiting list thuộc QE-39 |
| QE-56 | UC015, UC018 | REQ-ATT-001, 002, 003, 004, 021, 022, 023, 024; BR-ATT-01…05; NFR-PERF-008 | Offline/GPS/Evidence ngoài scope |
| QE-57 | UC015 (UI) | REQ-ATT-002, 003 (phản hồi UI); NFR-SAFE-001, NFR-SAFE-007 | |
| QE-61 | UC028 | REQ-EVT-023, REQ-ATT-024, REQ-DATA-016; NFR-SEC-012 | Chưa có màn tra audit (REQ-DATA-017) |
| QE-72 | UC021 | REQ-REV-008, 009 (một phần) | Evidence/appeal thuộc QE-58 |
| QE-70 | — | OTH-OPS-001, NFR-QUAL-008 | Hạ tầng |
| QE-28 | — (xác minh UC001/004/009/010/015/021) | NFR-QUAL-009 | |

> Dùng **mã của SRS** (`REQ-*`, `UC###`, `BR-ATT-*`). **Bỏ đề xuất `FR-<module>-<nn>`** ở `backlog_solutions.md §1.5` vì tạo thêm hệ mã thứ ba, làm lệch rubric "mã chức năng khớp bản đặc tả".

### 8.2 Sub-task cho 10 item Sprint 1 (43 sub-task; assignee = owner của item cha, label `sprint-1`)
| Item cha | Sub-task |
|---|---|
| QE-64 | `[BE]` Entity + seed user + JWT/session · `[BE]` login/logout/me + role guard · `[UI]` Màn đăng nhập + RequireRole · `[QA]` Test đúng/sai/logout/role sai · `[Docs]` Cách nạp & reset dữ liệu mẫu |
| QE-65 | `[BE]` Entity + API tạo/sửa nháp · `[BE]` Preview + publish có xác nhận · `[UI]` Form + preview/publish · `[QA]` Test validate/quyền/biên thời gian · `[Audit]` Audit publish |
| QE-66 | `[BE]` API list/detail lọc theo lớp · `[UI]` Danh sách + chi tiết · `[QA]` Test đúng/sai lớp, nháp, gọi trực tiếp |
| QE-71 | `[BE]` Giữ suất nguyên tử + unique · `[BE]` Từ chối sai lớp/ngoài hạn/hết chỗ · `[UI]` Panel đăng ký + lý do lỗi · `[QA]` IT tranh suất cuối + đăng ký lặp · `[Audit]` Audit giữ suất |
| QE-56 | `[BE]` QrTokenService ký + verify · `[BE]` API QR owner + check-in · `[BE]` 3 attempt + idempotency + chống trùng · `[QA]` Test token/biên/replay/đồng thời · `[Audit]` Audit accept/reject |
| QE-57 | `[UI]` Màn Owner hiển thị QR · `[UI]` Màn quét camera + dán mã dự phòng · `[UI]` Trạng thái lỗi/Accepted/chặn bấm lặp · `[QA]` Quét bằng điện thoại thật + bằng chứng |
| QE-61 | `[BE]` AuditService + bảng bất biến · `[BE]` Gắn vào publish/giữ suất/QR · `[QA]` Test trường tối thiểu + không lộ bí mật · `[Docs]` Chính sách log |
| QE-72 | `[BE]` API /me/history · `[UI]` Trang lịch sử + trạng thái rỗng · `[QA]` Test phân quyền & tải lại |
| QE-70 | `[BE]` Migration V1 + fixture · `[FE]` Repo FE + scaffold + CI · `[Docs]` README chạy BE+FE+DB, lệnh test/reset · `[QA]` Thành viên khác dựng lại thành công |
| QE-28 | `[QA]` Test plan + ≥30 TC · `[QA]` Automation API · `[QA]` E2E UI · `[QA]` Chạy TC + bug + retest · `[Docs]` Test report + bằng chứng |

## 9. Bằng chứng & số liệu cho bảng điểm Sprint 1

Công thức: `L = E + min(0.2·F, E) + G/2 + min(0.1·H + 0.3·I + 0.05·J, G)` (E = SP code, F = số unit test, G = SP test, H = TC manual, I = TC automation, J = bug).

| Người | E (SP code) | G (SP test) | Mục tiêu F (unit test) | Ghi chú |
|---|:-:|:-:|---|---|
| Cường | 8 (QE-64, QE-70) | 0 | ≥ 10 | |
| Hoàng | 8 (QE-65, QE-66) | 0 | ≥ 12 | |
| Đan | 8 (QE-56, QE-57) | 0 | ≥ 12 | `QrTokenService` test thuần |
| Khánh | 6 (QE-61, QE-72) | 0 | ≥ 8 | |
| Dương | 5 (QE-71) | 3 (QE-28) | ≥ 8 | H ≥ 30, I ≥ 10, J = số bug **thật** |

Lưu ý quan trọng:
- Phần cộng thêm từ H/I/J **bị chặn ở G**. Với G = 3, khoảng **30 TC manual** đã chạm trần (`0,1 × 30 = 3`) → thêm TC không tăng điểm; hãy đầu tư vào **chất lượng** (có bước, expected/actual, bằng chứng).
- Unit test bị chặn ở E (E = 5 → tối đa 25 test có giá trị); 10 test = +2 điểm.
- Mọi số liệu lấy từ `scripts/sprint1-metrics.ps1` và đối chiếu được với repo/Jira. **Không khai thừa** (G1 — GV vấn đáp, sai sẽ trừ cả nhóm).

## 10. Nghiệm thu & kịch bản demo

### 10.1 Checklist nghiệm thu (đánh dấu khi có bằng chứng thật)
| Item | Bằng chứng cần có |
|---|---|
| QE-70 | Clone sạch → làm theo README → BE+FE+DB chạy; thành viên khác dựng lại được; CI xanh cả 2 repo |
| QE-64 | Test login đúng/sai/logout/role sai xanh; seed 1 owner + 3 sinh viên; mật khẩu băm |
| QE-65 | Demo tạo→preview→publish; test validate/quyền; audit `EVENT_PUBLISH` |
| QE-66 | Sinh viên đúng lớp thấy, sai lớp/nháp không thấy (cả khi gọi API trực tiếp) |
| QE-71 | IT tranh suất cuối xanh, đăng ký lặp không chiếm thêm suất |
| QE-56 / QE-57 | Test token + attempt + replay xanh; **video quét QR bằng điện thoại thật** |
| QE-61 | Truy vấn `audit_logs` cho thấy 6 action, không lộ bí mật; trigger chặn sửa/xóa |
| QE-72 | Lịch sử hiện Accepted sau quét, tải lại vẫn còn |
| QE-28 | Test plan, ≥ 30 TC có kết quả thật, automation chạy xanh trong CI, bug có key Jira, test report |
| Quy trình | Mọi thay đổi qua PR có review; commit đúng convention; Jira cập nhật đúng trạng thái; có biên bản Planning/Daily/Review/Retro **thật** |

### 10.2 Kịch bản demo (~10 phút)
1. *(Trước giờ)* Reset dữ liệu, BE+FE+tunnel chạy, 2 điện thoại đã mở URL HTTPS.
2. `owner01` đăng nhập → tạo event (quota 1, lớp `K49CNTT1`) → thử `endAt < startAt` để thấy lỗi theo trường → **Preview** → **Công bố** (xác nhận).
3. `sv003` (khác lớp) không thấy event; `sv001` thấy, mở chi tiết.
4. `sv001` và `sv002` bấm **Đăng ký cùng lúc** (2 máy) → đúng 1 *Registered*, người còn lại thấy *Hết suất*.
5. Owner mở màn QR (QR tự đổi). `sv001` quét bằng điện thoại → **Accepted**; quét lại → "Đã điểm danh"; thử mã cũ → "Mã đã hết hạn".
6. `sv001` mở **Lịch sử** → thấy Accepted; tải lại vẫn còn.
7. Chạy `SELECT action, outcome, reason_code, occurred_at FROM audit_logs ORDER BY occurred_at;` để chứng minh audit.
8. Mở Jira board, PR đã merge, CI xanh, test report.
- **Dự phòng:** camera lỗi → dán mã; dữ liệu lệch → chạy `scripts/reset-demo-data`; mạng chập chờn → phát video quay màn hình đã chuẩn bị.

## 11. Bảng chốt quyết định (Đã duyệt 09/11/2026)

| ID | Vấn đề | Quyết định đã chốt | Trạng thái & Hành động tiếp theo |
|---|---|---|---|
| **D1** | Review Sprint 1 & Lịch các sprint | **Review Sprint 1: 11/11/2026**; Sprint 2: 12/11/2026 (2 tuần); các sprint sau: 10–13 ngày | Đã chốt. J1 sẽ cập nhật Sprint 41 endDate thành `2026-11-11T10:00Z` (17:00 UTC+7). Sprint 2 bắt đầu 12/11 |
| **D2** | Cắt scope Sprint 1 (§4.2) | **Đồng ý** (bám Must/Should/Won't) | Đã chốt. Sprint 1 tập trung luồng lõi; hoãn waiting list, OAuth, GPS, offline |
| **D3** | Remote GitHub FE | **`https://github.com/GMIOS25/QNU-Events-FE.git`** | Đã tạo và cấu hình remote xong |
| **D4** | GitHub username 5 thành viên | **Cường (`@GMIOS25`, `tranthanhcuongit@gmail.com`), Hoàng (`@hoangkq157`), Dương (`@YangKlee`), Đan (`@Dan28-05`), Khánh (`@KHANHSKY0808`)** | Đã điền vào CODEOWNERS tại T-00 |
| **D5** | Epic 7 (`QE-80` - Docs/CI) | **Không đưa vào Sprint 1** | Giữ ở Product Backlog; chuyển Done theo thực tế hoàn thành |
| **D6** | Xử lý ticket `PROD-3` | **Đóng (Done/Closed)** | Đã chốt đóng (do đã chuyển scope sang project `QE`) |
| **D7** | Giả định kỹ thuật A-01…A-06 | **Đồng ý** | Chấp nhận tạm; ghi vào `docs/decisions.md` (Assumption) |
| **D8** | Quyền Figma | **Bản Draft đã cấp quyền:** [Figma Draft](https://www.figma.com/design/jVs22zFVUQjDbYQ5d0pwmU/QNU-Event---Attendance-MVP--Copy-?node-id=0-1) | Đã đọc thành công qua MCP Figma (fileKey: `jVs22zFVUQjDbYQ5d0pwmU`) |

## 12. Cách giao việc cho Gemini Flash

Một **session = một agent = các task của một owner** (bảng §4.3). Prompt mẫu (đổi `<…>`):
```text
Bạn là agent <A1> của dự án QNU-Events (owner: <Hoàng>).
Đọc theo thứ tự: AGENTS.md → docs/agent/sprint1-rescue-plan.md (§5, §6 và task card <T-10, T-11, T-12>).
Làm từng task một: tạo nhánh theo thẻ → viết test trước → code → chạy lệnh kiểm chứng → commit đúng quy ước → mở PR.
Chỉ sửa file trong phạm vi được giao. Không bịa số liệu/bằng chứng. Test fail 3 lần, thiếu quyền hoặc thiếu thông tin
→ ghi BLOCKERS.md và dừng, báo người dùng.
Cuối mỗi task báo: lệnh đã chạy + kết quả thật, danh sách file đổi, giả định mới.
```
| Agent | Tasks | Chạy khi |
|---|---|---|
| A0 | T-00 → T-06, T-20, T-23 | ngay |
| A-J | J1 → J9 (§8) | ngay (chạy J1 với ngày 11/11) |
| A1 | T-10, T-11, T-12 | sau khi T-04 merge (~12:30) |
| A2 | T-13, T-21 | T-13 sau T-04; T-21.1–21.3 viết ngay |
| A3 | T-14, T-15 | sau T-04; `QrTokenService` + test thuần làm ngay |
| A4 | T-16, T-17, T-22 | `T-22` làm ngay; T-16/17 sau T-04 |

> **Trong lúc chờ T-04 (đến ~12:30)**, A1–A4 vẫn làm được: viết test + domain thuần (validator, `QrTokenService`, DTO, màn FE với dữ liệu giả), để khi `develop` có auth thì chỉ còn nối dây.

## 13. Việc đầu tiên làm ngay
1. ✅ **D1, D3, D4, D8 và B2 (Docker/PostgreSQL 17) đã hoàn tất 100%!** Môi trường test integration đã xanh 9/9 test.
2. Mở 2 luồng: **A0** (T-00 → T-06) và **A-J** (J1 → J8 trên Jira).
3. Sau khi T-04 merge, mở A1–A4 song song.
4. Cuối ngày 09/11: kiểm tra `develop` build xanh + có ít nhất login và V1; nếu không, cắt scope thêm (xem mốc Thứ Ba 14:00).
