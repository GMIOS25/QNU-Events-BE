# Tài liệu Nghi thức Scrum Sprint 1 & Definition of Done (DoD) — QNU-Events

> **Jira Task:** `QE-81` · **Dự án:** QNU-Events (Nhóm 1 — Đại học Quy Nhơn)  
> **Ngày tổng hợp:** 24/11/2026  
> **Nguyên tắc R1 (`AGENTS.md`):** Tài liệu này chỉ tổng hợp các sự kiện, số liệu và quyết định có bằng chứng thật từ lịch sử Git, Jira (`QE`, Board 4), CI/CD, báo cáo kiểm thử và Báo cáo Sprint 1. **Tuyệt đối không bịa ngày họp, biên bản hay lời thoại cuộc họp.** Các trường thông tin cuộc họp trực tiếp/online chưa lưu biên bản được đánh dấu rõ để thành viên xác nhận bổ sung.

---

## 1. Definition of Done (DoD) Chính thức — QNU-Events

Một **User Story / Task / Bug** chỉ được chuyển sang trạng thái **`Done`** trên Jira khi đáp ứng đồng thời **toàn bộ** tiêu chí sau:

1. **Hoàn thiện chức năng & Acceptance Criteria (AC):** Đáp ứng toàn bộ AC trên ticket Jira, không còn chức năng chạy nửa vời; mọi ràng buộc nghiệp vụ (thời gian, quyền hạn, quota, điểm danh) được xác thực ở phía Backend (**R9**).
2. **Quy trình Git & Code Review:** Phát triển trên nhánh riêng gắn Jira key (`feature/QE-xx-*` hoặc `fix/QE-xx-*`, **R3**), commit theo Conventional Commits kèm `[QE-xx]` (**R4**), mở Pull Request vào `develop` và được review qua PR (không push thẳng vào `main`/`develop`).
3. **Kiểm chứng tự động (CI & Local — R2, R5):**
   - **Backend:** `.\mvnw.cmd -B -ntp verify -Pintegration` (Unit + Integration tests trên PostgreSQL 17) xanh 100%, không dùng `@Disabled` hay `-DskipTests`.
   - **Frontend:** `npm run lint` (`0` errors, `0` warnings), `npm run typecheck`, `npm run test -- --run`, `npm run build` đều PASS.
4. **Hợp đồng API & Schema:** Nếu có thay đổi DTO/endpoint, đã đồng bộ `docs/api/sprint1-contract.md` trên cả BE & FE; không tự ý sửa migration đã merge (**R6**).
5. **Bằng chứng & Tài liệu:** Quyết định kỹ thuật ngoài SRS đã ghi vào `docs/decisions.md` dạng `ASSUMPTION` (**R11**); ticket Jira có comment đính kèm link PR/commit/tài liệu nghiệm thu (**R10**).
6. **Xử lý Bug:** Mọi lỗi QA phát hiện được log thành issue loại `Bug` trên Jira, gắn với GitHub Issue tương ứng và có kiểm thử hồi quy xác nhận trước khi đóng.

---

## 2. Dòng thời gian thực tế Sprint 1 (`Sprint 41` — đối chiếu Jira & Git)

| Mốc thời gian | Sự kiện thực tế (có log đối chiếu) | Nguồn bằng chứng |
|---|---|---|
| `05/11/2026` | Khởi tạo Sprint 1 (`Sprint 41`) trên Jira Board 4 | Jira API (`createdDate: 2026-11-05T16:20:27Z`) |
| `09/11/2026` | Chạy kế hoạch giải cứu Sprint 1 (`sprint1-rescue-plan.md`), hoàn thành & nghiệm thu 10 hạng mục MVP lõi (`QE-28, 56, 57, 61, 64, 65, 66, 70, 71, 72`) | `docs/scrum/jira-change-log.md` |
| `17/11/2026` | Lập kế hoạch trước Sprint 2 (`Sprint 42`) & Sprint 3 (`Sprint 43`); tạo 5 ticket Epic 7 (`QE-81..QE-85`); đóng và mở lại Sprint 1 gia hạn đến `20/11/2026` (bổ sung 17 hạng mục thiết kế, kiểm thử, tài liệu & hoàn thiện lên tổng `27` items / `100` SP) | `docs/scrum/jira-change-log.md` |
| `18–23/11/2026` | Triển khai & merge các PR thiết kế kiến trúc (`QE-6, 8, 36`), hoàn thiện nghiệp vụ (`QE-15, 20, 31, 37, 38, 41, 44, 45, 54`), kiểm thử tổng thể (`QE-29, 82, 83`, 259 manual TCs, 263 Bruno API tests, 35 Selenium tests, 4 JMeter plans), và phát hiện 15 bug duy nhất trên GitHub Issues | GitHub PR `#1–#48` (BE), `#1–#4` (FE), `docs/Test_Evidence/TEST_REPORT.md` |
| `24/11/2026` | Rà soát tổng thể Tech Lead & Scrum Master: bổ sung mục tiêu Sprint 41, tạo 15 Jira Bugs (`QE-86`–`QE-100`), bổ sung bằng chứng vào 11 ticket thiếu comment, vá các lỗi BE/FE trên nhánh `fix/QE-70-*`, chuẩn hóa tài liệu Sprint 1 | `docs/scrum/jira-change-log.md`, `docs/test/sprint1-test-execution-report.md` §5 |

---

## 3. Tổng hợp Sprint 1 Review (Dữ liệu thật)

- **Ngày họp Review nhóm:** `[Chờ thành viên xác nhận ngày/giờ họp thực tế]`
- **Thành phần tham dự:** `[Chờ thành viên xác nhận điểm danh thực tế: Trần Thanh Cường, Nguyễn Huy Hoàng, Khánh Dương Nguyễn, Huỳnh Hữu Đan, Nguyễn Nhật Khánh]`
- **Phạm vi hoàn thành trên Jira (`Sprint 41`):** **27/27 hạng mục `Done` (100 Story Points):**
  - Trần Thanh Cường (A0): 27 SP (`QE-6, 8, 36, 64, 70`)
  - Khánh Dương Nguyễn (A2): 20 SP (`QE-28, 29, 45, 71, 81, 82`)
  - Huỳnh Hữu Đan (A3): 19 SP (`QE-20, 44, 54, 56, 57`)
  - Nguyễn Nhật Khánh (A4): 19 SP (`QE-25, 31, 41, 61, 72, 83`)
  - Nguyễn Huy Hoàng (A1): 15 SP (`QE-15, 37, 38, 65, 66`)
- **Kết quả kiểm thử xác minh (24/11/2026):**
  - BE: **35/35** Unit Tests PASS cục bộ + **110** Integration Tests biên dịch sạch (chờ chạy PostgreSQL 17 trên GitHub Actions CI sau khi đẩy nhánh fix).
  - FE: **33/33** Vitest PASS (0 cảnh báo `act`), `oxlint` 0 lỗi/0 cảnh báo, `tsc` & `vite build` PASS; **7/7** Playwright E2E (mocked API).
  - QA Suites: **259** manual test cases, **263** Bruno API tests (350 `.bru`), **35** Selenium UI tests, **4** JMeter performance plans.
- **Defect & Remediation:** 15 Bug duy nhất đã được đồng bộ sang Jira (`QE-86`–`QE-100`); `QE-95` (`BUG-01`) đã `Done` qua BE PR #48 & FE PR #4; 14 Bug còn lại đã có code fix trên `fix/QE-70-sprint1-bugfixes-and-docs` (BE) và `fix/QE-70-sprint1-fe-bugfixes` (FE), chờ merge PR & QA retest.

---

## 4. Sprint 1 Retrospective (Tổng hợp vấn đề thật & Hành động cải tiến cho Sprint 2)

- **Ngày họp Retrospective:** `[Chờ thành viên xác nhận ngày/giờ họp thực tế]`
- **Điểm làm tốt:**
  1. Hoàn thiện toàn bộ luồng cốt lõi MVP Sprint 1 (Auth/Session/RBAC, Event CRUD/Preview/Publish, Atomic Registration, Dynamic QR + 3-attempt limit, Student History & Immutable Audit Logs).
  2. Hệ thống kiểm thử đa tầng (JUnit Unit/IT, Vitest, Bruno API, Selenium UI, JMeter) phát hiện kịp thời nhiều lỗi biên, lệch múi giờ, race condition và pool starvation trước khi sang Sprint 2.
  3. Giao diện Frontend đã đồng bộ Design Tokens và Layout Shell với Figma (`#F3F8FC`, `#0789B7`, `#25435E`).
- **Điểm tồn tại (bằng chứng thực tế):**
  1. **Quản trị nhánh & commit:** `origin/main` tụt lại sau `origin/develop` (thiếu release PR cuối Sprint 1), chưa bật Branch Protection; 28/100 commit trên BE chưa tuân thủ đầy đủ Conventional Commits + `[QE-xx]` (**R4**).
  2. **Quản lý Bug bị phân tán:** QA log 17 GitHub Issues (gồm 3 issue trùng của `BUG-13`) nhưng chưa đồng bộ sang Jira (`0` Jira Bug trước ngày 24/11).
  3. **Hợp đồng BE ↔ FE và quản lý Migration:** Phát sinh lệch DTO (`Page<HistoryItem>`, `alreadyRegistered` vs `cached`, `facultyName` vs `faculty`) và 3 PR Sprint 2 (`#10/#11/#12`) cùng tự tạo `V2__event_pools.sql` vi phạm R6.
- **Hành động cam kết cho Sprint 2:**
  1. Bật Branch Protection trên `main` và `develop` (bắt buộc PR + CI xanh).
  2. Mọi Bug phát hiện phải log trực tiếp trên Jira (type `Bug`, kiểm tra trùng trước khi tạo) và liên kết PR fix.
  3. A0 tổng hợp & đóng băng migration `V2__...` duy nhất trên `develop` trước khi rebase các nhánh tính năng Sprint 2 (`QE-12, 39, 40, 36, 73`).
  4. Cài đặt kiểm tra commit message / cập nhật ngay `docs/api/sprint1-contract.md` (và `sprint2-contract.md`) khi có bất kỳ thay đổi DTO nào.

---

## 5. Lịch trình Sprint 2 & Sprint 3 (Khớp Jira Board 4)

| Sprint | Jira Sprint ID | Thời gian dự kiến | Trạng thái Jira (24/11) | Khối lượng | Mục tiêu chính |
|---|---|---|---|---|---|
| **Sprint 2** | `42` | `23/11/2026` → `06/12/2026` | `future` *(chờ Complete Sprint 1 rồi Start Sprint 2)* | **20 items · 87 SP** | Hoàn thiện đăng ký/Waiting List (`QE-12, 39, 40`), điểm danh GPS (`QE-17, 55`), minh chứng & khiếu nại (`QE-18, 19, 22, 42, 43, 59, 60`), Import SV CSV (`QE-26, 62`), cấu hình vòng đời & kiểm thử tự động Sprint 2 |
| **Sprint 3** | `43` | `07/12/2026` → `20/12/2026` | `future` | **18 items · 85 SP** | Điểm danh Offline & Sync (`QE-9, 33, 76, 77, 78`), Chế tài No-show (`QE-14`), Google OAuth2 & Quản trị tài khoản (`QE-35, 73, 74`), Xuất báo cáo & Lưu trữ (`QE-34, 47, 63`), Hiệu năng tải cao (`QE-46`) & Staging/CI/CD (`QE-84`) |
| **Backlog chưa gán Sprint** | — | Cần rà soát Planning Sprint 2 | Backlog | `QE-30` (`5 SP`), 7 Epics, 9 parent stories (`0 SP`) + 14 Bug chưa đóng (`QE-86..94, 96..100`) | Đưa 14 Bug fix PR vào nghiệm thu ngay đầu Sprint 2; gán `QE-30` và bổ sung 4 UC còn thiếu (`UC005, UC006, UC007, UC026`) |
