# Pre-Sprint 2 Documentation Alignment & V2 Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconcile 100% of QNU-Events documentation across `QNU-Events-BE` (Markdown + Word `.docx` + governance) and `QNU-Events-FE` (`docs/api/sprint1-contract.md`) against approved decisions `D-1`–`D-10`, `A-EVT-01..04`, and `R1`–`R3`, locking the Sprint 2 `V2` baseline with zero cross-document contradictions and zero lost UML media.

**Architecture:** Execute the work across two independent Git repositories on dedicated `docs/` branches (`R3`): sync the frozen Sprint 1 contract in FE first, then in BE establish two new canonical reference documents (`docs/Design/v2-schema-blueprint.md` and `docs/api/error-codes.md`), reconcile all existing Markdown design/architecture/QA/governance docs against them, and surgically patch both `.docx` files in-place via `python-docx` XML editing so all 54 embedded UML diagrams in `QNU_Event_SRS.docx` remain bit-for-bit intact. Close with an automated cross-repo integrity script and the mandatory `R5` build/test gates.

**Tech Stack:** Git, Markdown, Python 3 + `python-docx` + `zipfile` + `lxml`, Maven Wrapper (`.\mvnw.cmd`), Node/npm + Vitest + TypeScript + Vite (PowerShell 5.1 on Windows — chain commands with `;`, not `&&`).

**Spec:** [`docs/superpowers/specs/2026-11-28-pre-sprint2-docs-alignment-design.md`](../specs/2026-11-28-pre-sprint2-docs-alignment-design.md)

## Global Constraints

- **R1 (No Fabricated Evidence):** Never invent meeting dates, attendance logs, or test counts. Use only verified `26/11/2026` `main` counts (`151` BE JUnit = `40` unit + `111` IT; `43` FE Vitest across 12 files; `7` Playwright mocked; `359` Bruno `.bru`; `35` Selenium; `4` JMeter; `15/15` Jira Bugs `Done`; `41/41` Sprint 1 items `Done`). Preserve all `[Chờ thành viên xác nhận...]` placeholders in `docs/scrum/sprint1-scrum-ceremonies-and-dod.md`.
- **R2 (No Weakened Tests):** Never skip, disable, or edit test assertions.
- **R3 (Dedicated Branches):** Never commit directly to `main` or `develop`. Use `docs/QE-70-sync-sprint1-contract` (from `develop`) in FE and `docs/QE-81-pre-sprint2-docs-alignment` (from `origin/main`, inheriting `eaff6f1` `docs/spec/*.docx`) in BE. Do NOT push to remote unless explicitly instructed.
- **R4 (Conventional Commits):** Every commit must match `type(scope): mô tả ngắn [QE-xx]`.
- **R5 (Verify Before Commit):** Run FE `npm run lint; npm run typecheck; npm run test -- --run; npm run build` and BE `.\mvnw.cmd -B -ntp test` (`40/40` unit tests PASS).
- **R6 (Immutable V1 / No V2 SQL Yet):** Never modify `V1__sprint1_core_schema.sql` and do NOT create `V2__*.sql` or Sprint 2 Java/React feature code.
- **R7 & R11 (No Secrets / Honest Open Items):** Never write real secrets. Mark `APPROVED (28/11/2026)` only for decisions approved in `D-1`–`D-10`, `A-EVT-01..04`, `R1`–`R3`; keep genuine external/infra items `Open` (`Q-AUTH-03` Google Cloud credentials, `TBD-ATT-03`, `TBD-DATA-02`, `TBD-NFR-01..05`, `TBD-SEC-01`, `TBD-SEC-03..04`, `TBD-OTH-01`).
- **DOCX Preservation:** Preserve 100% of the `54` embedded UML images (`word/media/*`) in `QNU_Event_SRS.docx` (`0` in `Proposal_v2.docx`) and keep both files valid OOXML packages.
- **Workspace Hygiene:** Leave pre-existing untracked `c:\Workspaces\QNU-Events-FE\pnpm-lock.yaml` untouched (never stage or delete it). Delete any scratch verification scripts before final commit.

## Review Focus

1. **DOCX XML & Embedded Media Corruption:** Editing `QNU_Event_SRS.docx` or `Proposal_v2.docx` drops any of the 54 `word/media/*` UML files, breaks merged cells in Proposal `TBL[3]` (6×4), or invalidates OOXML reopening → *Pinned by Task 5 `verify_docx` checks (`media == 54` SRS, `media == 0` Proposal, Zip+Document reopen, merged-row integrity).*
2. **Residual Contradictions & Hidden Secondary Drift:** Leaving old terms/rules in secondary spots such as `SRS TBL[44] R[2]` (`UC016 7a` `400 Bad Request` → `422 GPS_OUT_OF_RANGE`), `mvp-architecture-and-boundaries.md:248` (GPS out-of-radius non-reject), `test-matrix-attendance-br-att.md:68/170/201/206/207/239` (`cấm vĩnh viễn`, `location_radius_meters`, `student_penalties`, `/register`, `11` tests), or `quota-rules.md:17` (`phân bổ/h hoàn suất`) → *Pinned by Task 3, Task 4, Task 5, and Task 6 cross-repo grep assertions.*
3. **Broken Internal Links & Personal `file:///` URIs:** Leaving stale `docs/QNU_Event_SRS.docx`, `../QNU_Event_SRS.docx`, `../../QNU_Event_SRS.docx` (in `docs/test/testcase/4.1..4.6`), `file:///c:/Users/fleya/...`, or broken links to newly created `v2-schema-blueprint.md` / `error-codes.md` → *Pinned by Task 6 automated relative-link verifier across all active BE & FE Markdown files.*
4. **Guardrail Violations (`R1`, `R3`, `R6`, `R11`):** Accidentally touching Java/TS/SQL files, erasing `[Chờ thành viên xác nhận...]` placeholders, closing external `TBD-NFR-*`/`TBD-SEC-03..04`/`Q-AUTH-03`, or staging `FE/pnpm-lock.yaml` → *Pinned by Task 1 & Task 6 git diff scope and open-item/placeholder checks.*
5. **BE ↔ FE Contract & Branch Base Drift:** Branching BE from `develop` (losing `eaff6f1` `docs/spec/`) or allowing FE `docs/api/sprint1-contract.md` endpoint rows to drift from BE → *Pinned by Task 1 exact contract comparison and Task 2 branch-base check.*

---

## File Structure

### Repository 1: `c:\Workspaces\QNU-Events-FE` (Branch: `docs/QE-70-sync-sprint1-contract`)
- **Modify:** `docs/api/sprint1-contract.md:57-58` (+ note in §1) — sync `GET /preview` and `POST /publish` notes to BE verbatim and link `QNU-Events-BE/docs/api/error-codes.md`.

### Repository 2: `c:\Workspaces\QNU-Events-BE` (Branch: `docs/QE-81-pre-sprint2-docs-alignment`)
- **Create:**
  - `docs/Design/v2-schema-blueprint.md` — single frozen V2 DDL blueprint (`V2__sprint2_core_extensions.sql` input for `A0`, Spec §5.4).
  - `docs/api/error-codes.md` — canonical Error Code catalog for Sprint 1–3 (Spec §5.3).
- **Delete:**
  - `docs/plan/sprint1_rescue_plan.md` — exact duplicate of `docs/agent/sprint1-rescue-plan.md`.
- **Modify (Markdown & Governance):**
  - `docs/Design/erd.md` — cross-link Sprint 2 extensions to `v2-schema-blueprint.md`.
  - `docs/api/sprint1-contract.md` — cross-link Sprint 2–3 codes to `error-codes.md` (keep Sprint 1 rows frozen).
  - `docs/decisions.md` — approve `A-001`, `A-01..A-06`, `FE-A01`, `ARCH-A01..07`, `A-QE8-01..11`, `A-AUTH-01..08`, `A-EVT-01..04`, `A-REV-01`; align content to `D-1`–`D-10` & `R1`–`R2`; trim open section to genuine external items; fix `docs/spec/` path.
  - `docs/Design/quota-rules.md` — approve (`28/11/2026`), fix typos (`освоб`, `phân bổ/h hoàn suất`), add `wait_order`, `A-EVT-01` quota-increase auto-promote, post-deadline promote before `startAt - 12h`, `409 CANCELLATION_CUTOFF_PASSED`.
  - `docs/architecture/attendance-consistency-design.md` — switch §2.2 to `event_pools` + `wait_order` + `FOR UPDATE SKIP LOCKED`, align `A-EVT-01` quota adjustment, `NO_SHOW` `MAX(close+24h, evidence_deadline)`, GPS default `50m`, `150 ngày` retention, approve §7.2, fix `docs/spec/` path.
  - `docs/architecture/dynamic-qr-specification.md` — update §4 & `ADR-03` (forbid Offline Prefetch, allow Student Offline Store-and-Forward 24h), rename `A-04..07` → `QR-ADR-01..04` (`APPROVED`), fix `TC-16` → `VALIDATION_FAILED`, §7.1 GPS `10–500m` default `50m`, replace all 4 `file:///c:/Users/fleya/...` links.
  - `docs/architecture/auth-and-session-lifecycle.md` — update §7 with `APPROVED` decisions `D-1`, `D-3`, `D-10`, keep only `Q-AUTH-03` & external security/MFA items open, fix `docs/spec/` paths.
  - `docs/architecture/mvp-architecture-and-boundaries.md` — align 5 roles (`CVHT` in `V2`), align §2.1 Planned `V2+` tables to `v2-schema-blueprint.md`, fix §4.4:248 GPS out-of-radius → `422 GPS_OUT_OF_RANGE`, update Sprint 1 implemented status, §7 approvals, Figma 3-item action list, fix `docs/spec/` paths.
  - `docs/import-contract/student-import-contract.md` & `docs/import-contract/README.md` — mark `APPROVED (D-3, D-10)` (`QE-26`/`QE-73`, `SYSTEM_ADMIN` exclusive, `allowClassTransfer=false` default + audit, `APP_GOOGLE_ALLOWED_DOMAINS`).
  - `docs/test/business-state-transition-matrix.md` — fix 5 contradictions (`WAITING_LIST` exempt 12h, re-registration after `CANCELLED`, 3-tier Appeal BCS→CVHT→EVENT_OWNER, QR/GPS supersedes `PENDING` Evidence, `409 CANCELLATION_CUTOFF_PASSED` & `5 ngày lịch từ T0`) + fix `../spec/QNU_Event_SRS.docx`.
  - `docs/test/test-matrix-attendance-br-att.md` — fix `BCS` approval, `GPS_OUT_OF_RANGE`, `INVALID_EVIDENCE_FILE`, `allowed_radius_meters`, `student_restrictions`, `/registrations`, 3-attempt automated-only scope, `NO_SHOW` timing, `25` `AttendanceIT` tests, `docs/spec/` paths.
  - `docs/traceability-matrix.md` — fix `5 ngày lịch từ T0`, `CVHT`/`EVENT_OWNER` Appeal rounds, `150 ngày` retention, `user_role`, `spec/QNU_Event_SRS.docx`, live `26/11/2026` test & PR counts.
  - `docs/scrum/sprint1-scrum-ceremonies-and-dod.md` — add `25–26/11/2026` milestones, `41/41` Jira Done / `15/15` Bugs Done, `151` BE / `43` FE counts; preserve `[Chờ thành viên xác nhận...]` placeholders.
  - `docs/test/testcase/4.1..4.6-*.md:5` & `docs/plan/TEST PLAN SPRINT 1.md:39` — fix broken `QNU_Event_SRS.docx` links to `spec/QNU_Event_SRS.docx`.
  - `README.md` — update `151` tests (`40` unit + `111` IT), add `GET /api/v1/events/{id}/attendance/me`, update fail-fast secret note as implemented, fix `docs/spec/` links, add links to `v2-schema-blueprint.md` & `error-codes.md`.
  - `AGENTS.md` & `.github/CODEOWNERS` — update Sprint 2 ownership table & rules (`importer/`, shared `/evidence/` `@Dan28-05 @KHANHSKY0808`, `/appeal/`, `/outbox/`).
- **Modify (Word `.docx` via XML-preserving `python-docx`):**
  - `docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx` (`TBL[0]`, `TBL[3]`, `TBL[9]`, `TBL[13]`, `P[131]`, `P[154]`, `P[157]`).
  - `docs/spec/QNU_Event_SRS.docx` → `v1.1` (`P[4]`, `P[17]`, `TBL[0]`, `P[117–119]`, `P[228–230]`, `TBL[15]`, `P[329]`, `P[384]`, `P[393–395]`, `TBL[31]`, `TBL[34]`, `TBL[42]`, `TBL[44]`, `P[550–551]`, `P[707]`, `P[716–718]`, `TBL[81]`, `TBL[82]`).
- **Commit Existing Planning Artifacts:**
  - `docs/superpowers/specs/2026-11-28-pre-sprint2-docs-alignment-design.md`
  - `docs/superpowers/plans/2026-11-28-pre-sprint2-docs-alignment.md`

---

### Task 1: FE Branch Setup & Sprint 1 API Contract Sync (`QNU-Events-FE`)

**Files:**
- Modify: `c:\Workspaces\QNU-Events-FE\docs\api\sprint1-contract.md:7-45,57-58`

**Interfaces:**
- Consumes: `c:\Workspaces\QNU-Events-BE\docs\api\sprint1-contract.md:57-58` (authoritative Sprint 1 preview/publish rules)
- Produces: Synchronized `FE/docs/api/sprint1-contract.md` on branch `docs/QE-70-sync-sprint1-contract` (referenced by Task 6 verification)

- [ ] **Step 1: Verify FE drift fails equality check (RED)**

Run in PowerShell:
```powershell
python -c "fe=open(r'c:\Workspaces\QNU-Events-FE\docs\api\sprint1-contract.md',encoding='utf-8').read(); be=open(r'c:\Workspaces\QNU-Events-BE\docs\api\sprint1-contract.md',encoding='utf-8').read(); assert fe==be, 'DRIFT DETECTED'"
```
Expected: FAIL with `AssertionError: DRIFT DETECTED` (lines 57–58 differ).

- [ ] **Step 2: Create FE docs branch from `origin/develop`**

Run:
```powershell
git -C c:\Workspaces\QNU-Events-FE fetch origin; git -C c:\Workspaces\QNU-Events-FE checkout -b docs/QE-70-sync-sprint1-contract origin/develop; git -C c:\Workspaces\QNU-Events-FE status --short
```
Expected: Switched to `docs/QE-70-sync-sprint1-contract`; only `?? pnpm-lock.yaml` appears in status.

- [ ] **Step 3: Sync `FE/docs/api/sprint1-contract.md` lines 57–58 and add Sprint 2–3 catalog reference**

In `c:\Workspaces\QNU-Events-FE\docs\api\sprint1-contract.md`:
- Update lines 57–58 to match BE verbatim:
  - Line 57: `| \`GET\` | \`/api/v1/events/{id}/preview\` | EVENT_OWNER | — | 200 \`{event: EventDetail, previewVersion: number}\` | Chỉ cho phép khi \`DRAFT\` (khác \`DRAFT\` → \`409 VERSION_CONFLICT\`); đánh dấu \`previewed_version = version\` |`
  - Line 58: `| \`POST\` | \`/api/v1/events/{id}/publish\` | EVENT_OWNER | \`{confirm: true, previewVersion: number}\` | 200 \`EventDetail\` (status: \`PUBLISHED\`) | Kiểm tra \`DRAFT\` (\`CANCELLED\` → \`409 VERSION_CONFLICT\`; \`PUBLISHED\` idempotent 200), đã preview và version khớp |`
- Directly below the §1 error table (after line 43), add one callout line:
  `> **Mở rộng Sprint 2–3:** Danh mục mã lỗi mở rộng thống nhất toàn dự án (bao gồm \`CANCELLATION_CUTOFF_PASSED\`, \`GPS_OUT_OF_RANGE\`, \`INVALID_EVIDENCE_FILE\`, \`OFFLINE_SYNC_WINDOW_EXPIRED\`, \`STUDENT_PENALIZED\`, \`OAUTH_*\`, \`SESSION_EXPIRED\`) xem tại \`QNU-Events-BE/docs/api/error-codes.md\`.`

- [ ] **Step 4: Verify contract sync & run FE `R5` checks (GREEN)**

Run:
```powershell
python -c "fe=open(r'c:\Workspaces\QNU-Events-FE\docs\api\sprint1-contract.md',encoding='utf-8').read(); assert 'khác \`DRAFT\` → \`409 VERSION_CONFLICT\`' in fe and 'error-codes.md' in fe; print('FE CONTRACT OK')"
cd c:\Workspaces\QNU-Events-FE; npm run lint; npm run typecheck; npm run test -- --run; npm run build
```
Expected: `FE CONTRACT OK`; lint `0` warnings/errors, typecheck exit 0, `43/43` Vitest PASS, Vite build succeeds.

- [ ] **Step 5: Commit FE docs change (do NOT stage `pnpm-lock.yaml`)**

```powershell
git -C c:\Workspaces\QNU-Events-FE add docs/api/sprint1-contract.md; git -C c:\Workspaces\QNU-Events-FE commit -m "docs(api): sync sprint1 contract preview/publish rules with BE [QE-70]"; git -C c:\Workspaces\QNU-Events-FE status --short
```
Expected: 1 commit created on `docs/QE-70-sync-sprint1-contract`; only `?? pnpm-lock.yaml` remains untracked.

---

### Task 2: BE Branch Setup, New Canonical Docs & Duplicate Cleanup (`QNU-Events-BE`)

**Files:**
- Create: `c:\Workspaces\QNU-Events-BE\docs\Design\v2-schema-blueprint.md`
- Create: `c:\Workspaces\QNU-Events-BE\docs\api\error-codes.md`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\Design\erd.md:1-6,155-157`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\api\sprint1-contract.md:43-45`
- Delete: `c:\Workspaces\QNU-Events-BE\docs\plan\sprint1_rescue_plan.md`

**Interfaces:**
- Consumes: Approved Spec §5.3 (`Canonical Error Code Catalog`) & §5.4 (`Blueprint Schema V2`)
- Produces:
  - `docs/Design/v2-schema-blueprint.md` (target linked by Tasks 3 & 4)
  - `docs/api/error-codes.md` (target linked by Tasks 1, 3 & 4)
  - Branch `docs/QE-81-pre-sprint2-docs-alignment` containing `docs/spec/*.docx`

- [ ] **Step 1: Verify branch base & preconditions fail (RED)**

Run:
```powershell
python -c "import os; assert os.path.exists(r'c:\Workspaces\QNU-Events-BE\docs\Design\v2-schema-blueprint.md') and os.path.exists(r'c:\Workspaces\QNU-Events-BE\docs\api\error-codes.md') and not os.path.exists(r'c:\Workspaces\QNU-Events-BE\docs\plan\sprint1_rescue_plan.md')"
```
Expected: FAIL (`AssertionError`).

- [ ] **Step 2: Create BE docs branch from `origin/main` and confirm `docs/spec/*.docx` exists**

Run:
```powershell
git -C c:\Workspaces\QNU-Events-BE checkout -b docs/QE-81-pre-sprint2-docs-alignment origin/main; ls c:\Workspaces\QNU-Events-BE\docs\spec\
```
Expected: Switched to `docs/QE-81-pre-sprint2-docs-alignment`; lists `QNU_Event_Participation_Management_Project_Proposal_v2.docx` and `QNU_Event_SRS.docx`.

- [ ] **Step 3: Create `docs/Design/v2-schema-blueprint.md` and `docs/api/error-codes.md`**

- Write `c:\Workspaces\QNU-Events-BE\docs\Design\v2-schema-blueprint.md` containing the complete frozen V2 schema blueprint from Spec §5.4:
  - Header noting status `APPROVED (28/11/2026)`, owner `A0 (@GMIOS25)` per `R6`, single consolidated target migration `V2__sprint2_core_extensions.sql` (superseding fragmented `V2__event_pools.sql` on draft PRs `#10/#11/#12`), and cross-link to [`erd.md`](erd.md) and [`../decisions.md`](../decisions.md).
  - Exact 4 DDL sections from Spec §5.4:
    1. Auth & 5 Roles (`user_role` CHECK adding `'CVHT'`; `app_user` adding `email` + `uq_app_user_email_lower`, `google_sub UNIQUE`, `must_change_password`, `failed_login_attempts`, `locked_until`, `auth_version`, `unit_id`; `auth_session` adding `refresh_token_hash`, `remember_me`).
    2. Organization, Class Assignment & Student Profile (`org_department`, `org_specialization`, `intake_program`, `org_class` adding `department_id`/`specialization_id`, `student_profile`, `bcs_class_assignment`, `advisor_class_assignment`).
    3. Events, Quota Pools & Waiting List (`events` adding `next_wait_order`, `registration_closed_early`, `allow_offline_sync`, `gps_lat`, `gps_lng`, `allowed_radius_meters DEFAULT 50 CHECK (10..500)`, `evidence_deadline`, `cancel_reason`, `ck_events_evidence_deadline`; `event_pools` with `UNIQUE (event_id, scope_type, scope_ref)` & `CHECK (NOT is_mandatory OR NOT allow_overflow)`; `registrations` adding `pool_id`, `waiting_at`, `wait_order` + `ix_registrations_waitlist`).
    4. Attendance, Evidence, Appeal, No-show & Outbox (`attendance_attempts.payload_hash`; `attendances` adding `gps_lat`, `gps_lng`, `gps_accuracy`, `suspicious_flag`, `suspicious_reason`, `is_offline_late_sync`; `attendance_evidences` with `file_size_bytes < 5242880` & backup/retention timestamps; `appeals` with `round IN (1,2)` & `UNIQUE (event_id, student_id, round)`; `student_restrictions`; `outbox_events`).
  - Summary table mapping legacy planned names (`event_quota_pool`, `registration_pool_allocations`, `student_penalties`, `org_faculty`, `org_major`, `login_attempt_tracker`) → canonical `V2` table names.
- Write `c:\Workspaces\QNU-Events-BE\docs\api\error-codes.md` containing the complete canonical Error Code catalog from Spec §5.3 (`ApiError` JSON envelope, every HTTP status/code/sprint/`counted` flag/replaced legacy alias, plus explicit note for `IDEMPOTENCY_PAYLOAD_MISMATCH` under `400 VALIDATION_FAILED` and import file/row error codes referenced in `../import-contract/student-import-contract.md`).

- [ ] **Step 4: Cross-link `docs/Design/erd.md` & `docs/api/sprint1-contract.md`, and remove duplicate `docs/plan/sprint1_rescue_plan.md`**

- In `docs/Design/erd.md`: add a top note and §3 link pointing Sprint 2+ extensions to [`v2-schema-blueprint.md`](v2-schema-blueprint.md) (keeping V1 ERD intact).
- In `docs/api/sprint1-contract.md`: add the identical Sprint 2–3 cross-link note below the §1 error table pointing to [`error-codes.md`](error-codes.md) (do NOT alter Sprint 1 contract rows).
- Delete `docs/plan/sprint1_rescue_plan.md` via `git -C c:\Workspaces\QNU-Events-BE rm docs/plan/sprint1_rescue_plan.md`.

- [ ] **Step 5: Verify new canonical docs & duplicate removal (GREEN) and commit**

Run:
```powershell
python -c "import os; b=r'c:\Workspaces\QNU-Events-BE'; v2=open(b+r'\docs\Design\v2-schema-blueprint.md',encoding='utf-8').read(); ec=open(b+r'\docs\api\error-codes.md',encoding='utf-8').read(); assert all(k in v2 for k in ['CVHT','event_pools','next_wait_order','attendance_evidences','appeals','student_restrictions','outbox_events','bcs_class_assignment','advisor_class_assignment','allowed_radius_meters']); assert all(k in ec for k in ['CANCELLATION_CUTOFF_PASSED','GPS_OUT_OF_RANGE','INVALID_EVIDENCE_FILE','ATTEMPT_LIMIT_REACHED','OFFLINE_SYNC_WINDOW_EXPIRED','STUDENT_PENALIZED','OAUTH_DOMAIN_NOT_ALLOWED']); assert not os.path.exists(b+r'\docs\plan\sprint1_rescue_plan.md'); print('TASK 2 OK')"
git -C c:\Workspaces\QNU-Events-BE add docs/Design/v2-schema-blueprint.md docs/api/error-codes.md docs/Design/erd.md docs/api/sprint1-contract.md; git -C c:\Workspaces\QNU-Events-BE commit -m "docs(design): add frozen V2 schema blueprint, canonical error catalog and remove duplicate rescue plan [QE-81]"
```
Expected: `TASK 2 OK` and clean commit created on `docs/QE-81-pre-sprint2-docs-alignment`.

---

### Task 3: Reconcile Decision Log, Design, Architecture & Import Docs (`QNU-Events-BE`)

**Files:**
- Modify: `c:\Workspaces\QNU-Events-BE\docs\decisions.md:1-117`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\Design\quota-rules.md:1-108`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\architecture\attendance-consistency-design.md:1-10,72-132,326-389`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\architecture\dynamic-qr-specification.md:1-13,172-211,317-390`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\architecture\auth-and-session-lifecycle.md:1-11,39-46,354-399`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\architecture\mvp-architecture-and-boundaries.md:1-18,32-40,140-177,200-206,232-249,298-373`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\import-contract\student-import-contract.md:1-17,26-66,209-211`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\import-contract\README.md:1-25,89-99`

**Interfaces:**
- Consumes: `docs/Design/v2-schema-blueprint.md`, `docs/api/error-codes.md` (from Task 2), Spec §5.1–§5.5
- Produces: Reconciled core design & architecture docs with zero `ASSUMPTION — chưa duyệt` on approved decisions and zero legacy table/error names

- [ ] **Step 1: Verify Task 3 banned patterns currently exist (RED)**

Run:
```powershell
python -c "b=r'c:\Workspaces\QNU-Events-BE'; checks=[('docs/Design/quota-rules.md','освоб'),('docs/architecture/dynamic-qr-specification.md','file:///c:/Users/fleya'),('docs/architecture/dynamic-qr-specification.md','MISSING_HEADER'),('docs/architecture/attendance-consistency-design.md','registration_pool_allocations'),('docs/architecture/mvp-architecture-and-boundaries.md','event_quota_pool')]; found=[f for f,s in checks if s in open(b+'/'+f,encoding='utf-8').read()]; assert len(found)==5, found; print('RED CONFIRMED:', len(found))"
```
Expected: `RED CONFIRMED: 5`.

- [ ] **Step 2: Update `docs/decisions.md` and `docs/Design/quota-rules.md`**

- **`docs/decisions.md`:**
  - Mark `A-001`, `A-01..A-06`, `FE-A01`, `ARCH-A01..07`, `A-QE8-01..11`, `A-AUTH-01..08`, `A-EVT-01..04`, `A-REV-01` as **`APPROVED (28/11/2026)`** and update their wording to match `D-1`–`D-10` & `R1`–`R2`:
    - `A-01`/`FE-A01`/`A-AUTH-07`: Sprint 1 `sessionStorage` + 8h TTL; Sprint 2+ `"Lưu đăng nhập"` = short Access Token + `HttpOnly; Secure; SameSite=Lax` Refresh Cookie (7d) + CSRF + max 5 sessions (`A-AUTH-04`).
    - `A-03` + `TBD-EVT-03`: keep `startAt < endAt`, `attendanceOpenAt < attendanceCloseAt`, `registrationDeadline <= attendanceCloseAt`; add `evidence_deadline >= attendance_close_at` (default `+24h`); `UC007` calendar filtered strictly by role scope (`STUDENT`/`EVENT_OWNER`/`CVHT`).
    - `A-04`/`A-QE8-05`: 3-attempt cap shared across automated channels (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`), `EVIDENCE` separate fallback, `409 ATTEMPT_LIMIT_REACHED`, QR/GPS supersedes `PENDING` Evidence (`SUPERSEDED_BY_ACCEPTED`).
    - `A-05`/`A-QE8-07`: online grace `3s`; forbid Offline Prefetch; support Student Offline Store-and-Forward within `OFFLINE_SYNC_MAX_WINDOW = 24h` after `attendance_close_at` + `events.allow_offline_sync` + `is_offline_late_sync`.
    - `A-06`/`ARCH-A04`: 5 roles official (`CVHT` added in `V2`); 3-tier BCS → CVHT R1 → EVENT_OWNER R2; `SYSTEM_ADMIN` exclusive for Master Data/Import.
    - `A-QE8-06`: GPS configurable `10–500m`, default **`50m`** (SRS `REQ-EVT-009`; Figma `60m` is per-event example), out-of-range = `422 GPS_OUT_OF_RANGE`.
    - `A-QE8-08`/`A-QE8-09`: `event_pools` + `events.next_wait_order` → `wait_order` + `FOR UPDATE SKIP LOCKED`, `WAITING_LIST` exempt from 12h cutoff, re-register allowed after `CANCELLED`, auto-promote continues after `registration_deadline` before `startAt - 12h`, `409 CANCELLATION_CUTOFF_PASSED`.
    - `A-QE8-11`: `NO_SHOW` runs after `MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`, skips `PENDING` Evidence & active Round 1 Appeal, overlapping ban = `MAX(current_banned_until, now + 7 days)`.
    - `ARCH-A03`: `EvidenceStoragePort` default Local Private Filesystem (outside web root, UUID, magic bytes `< 5MB`, BE auth stream, pluggable MinIO/S3), `150 ngày` retention from `events.end_at` excluding open Appeals with verified backup before delete.
    - `A-AUTH-03`/`A-AUTH-05`/`A-AUTH-08`: check `LOCKED` after BCrypt (`QE-30`), Google SSO allowlist `APP_GOOGLE_ALLOWED_DOMAINS=st.qnu.edu.vn,qnu.edu.vn` for all pre-provisioned roles, CSV temp password/activation token + `must_change_password = true`, `allowClassTransfer = false` default + audited Admin override.
    - `A-EVT-01`: allow safe fields (`title`, `description`) AND `quotaTotal`/`event_pools.capacity` adjustment (`>= used`, auto-promoting waitlist on increase per `quota-rules.md §7`), while forbidding time/location/audience edits once registrations exist.
    - `A-REV-01`: 5 calendar days R1 from `T0` (`reviewed_at` for rejected Evidence; `attendance_close_at`/No-show timestamp for QR/GPS/No-show), 3 calendar days R2 from R1 decision, 30 calendar days absolute cap (`23:59:59` GMT+7).
  - Update `Điểm mở chờ Stakeholder` section to reference `docs/spec/QNU_Event_SRS.docx` (v1.1), `docs/Design/v2-schema-blueprint.md`, `docs/api/error-codes.md`, and keep **only** genuine external/infra items open (`Q-AUTH-03`, `TBD-ATT-03`, `TBD-DATA-02`, `TBD-NFR-01..05`, `TBD-SEC-01`, `TBD-SEC-03..04`, `TBD-OTH-01`).
- **`docs/Design/quota-rules.md`:**
  - Change header banner from `⏳ CHỜ PRODUCT OWNER / STAKEHOLDER DUYỆT` to `✅ APPROVED (28/11/2026 — D-4, D-5, A-EVT-01)`.
  - Fix line 17 `phân bổ/h hoàn suất` → `phân bổ/hoàn suất` and line 63 `освоб phóng` → `giải phóng`.
  - Add `events.next_wait_order` → `registrations.wait_order` (closing `TBD-REG-01`) to W1/W2, note auto-promote continues after `registration_deadline` before `startAt - 12h` (closing `TBD-REG-02`), note re-registration allowed after `CANCELLED`, note `409 CANCELLATION_CUTOFF_PASSED` on H1, note increasing `capacity`/`quotaTotal` in §7 automatically triggers FIFO auto-promote (`A-EVT-01`), and link `v2-schema-blueprint.md`.

- [ ] **Step 3: Update `attendance-consistency-design.md`, `dynamic-qr-specification.md`, `auth-and-session-lifecycle.md`**

- **`docs/architecture/attendance-consistency-design.md`:**
  - Update header status (`APPROVED — 28/11/2026`, link `../Design/v2-schema-blueprint.md` and `../api/error-codes.md`).
  - Rewrite §2.2 & §2.3 to use **`event_pools`** (`1 seat = 1 pool`, `registrations.pool_id`, `priority ASC`, `allow_overflow`, `is_mandatory`, `reserved_until`, `events.next_wait_order` → `wait_order`, `FOR UPDATE SKIP LOCKED`, eligibility skip on `#01`) — completely removing `event_quota_pools` and `registration_pool_allocations`.
  - Align §2.3 & §6.2–§6.3 with `D-5` (`WAITING_LIST` exempt 12h, `409 CANCELLATION_CUTOFF_PASSED`, promote after `registration_deadline` before `startAt - 12h`), `D-6` (`OFFLINE_SYNC_MAX_WINDOW = 24h`, `allow_offline_sync`, `is_offline_late_sync`), `D-7` (GPS default **`50m`**, `10–500m`, `422 GPS_OUT_OF_RANGE`, 3-attempt automated channels, `PENDING` Evidence superseded by QR/GPS `ACCEPTED`), `D-8` (`150 ngày` retention), `D-9` (`NO_SHOW` after `MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))` + `MAX(current_banned_until, now + 7 days)`), and `A-EVT-01` quota adjustment.
  - Update §7.2 table: mark `A-QE8-01..A-QE8-11` as `APPROVED (28/11/2026)` (keeping `TBD-ATT-03` open), fix `docs/spec/QNU_Event_SRS.docx`.
- **`docs/architecture/dynamic-qr-specification.md`:**
  - Fix all 4 `file:///c:/Users/fleya/...` links (lines 6 & 362) to valid relative links (`../agent/sprint1-rescue-plan.md`, `../../AGENTS.md`, plain label for FE repo).
  - Update status to `APPROVED (28/11/2026)` & note cross-link to `../api/error-codes.md`.
  - Update §4 (`§4.1`–`§4.2`) and `ADR-03` to clearly distinguish: strictly forbid **Offline Prefetch**, while supporting **Student Offline Store-and-Forward** (scan live displayed QR while offline → local `PENDING_SYNC` → sync within `OFFLINE_SYNC_MAX_WINDOW = 24h` after `attendance_close_at`, server verifies HMAC + signed `epoch` within attendance window + `REGISTERED` precondition + per-event `allow_offline_sync` + `is_offline_late_sync` audit flag).
  - Fix `TC-16` (line 209): `MISSING_HEADER` → `VALIDATION_FAILED`.
  - Fix §7.1 (line 322): GPS radius `$R \le 100\text{m}$` → cấu hình theo sự kiện `10–500m`, mặc định `50m` (`REQ-EVT-009`).
  - Rename `Assumption A-04..A-07` in §9 (lines 367, 372, 377, 382) to **`QR-ADR-01..QR-ADR-04`** (eliminating ID collision with `decisions.md` `A-04..A-06`) and mark **`APPROVED (28/11/2026)`**.
- **`docs/architecture/auth-and-session-lifecycle.md`:**
  - Fix lines 6–7 `docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx` and `docs/spec/QNU_Event_SRS.docx`, update status banner to `APPROVED Baseline (28/11/2026)`.
  - Update §2.1 `CVHT` row to official `V2` role (Appeal Round 1 & Calendar scope, `advisor_class_assignment`).
  - Update §7.1 (`Q-AUTH-01..07`) marking `Q-AUTH-01, 02, 04, 05, 06, 07` (except MFA `TBD-SEC-04`) as **`CLOSED (28/11/2026)`** with exact resolved decisions (`D-1`, `D-3`, `D-10`), keeping **`Q-AUTH-03`** (Google Cloud Project / Consent Screen / Client ID) and `TBD-SEC-01`/`TBD-SEC-04` **`OPEN`**.
  - Update §7.2 `A-01` & `A-AUTH-01..08` to **`APPROVED (28/11/2026)`** (noting `A-AUTH-01` already implemented on `main` `24/11/2026`).

- [ ] **Step 4: Update `mvp-architecture-and-boundaries.md` and `docs/import-contract/{student-import-contract,README}.md`**

- **`docs/architecture/mvp-architecture-and-boundaries.md`:**
  - Fix line 12 `docs/spec/...` paths and update header status (`APPROVED Baseline — 28/11/2026`).
  - Update Mermaid diagram line 38 (`CVHT - V2`), line 78 (`Local Private Filesystem default / MinIO / S3 — D-8`), and §2.1 table **Planned `V2+` column** to match `docs/Design/v2-schema-blueprint.md` 100% (replacing `login_attempt_tracker`, `admin_role_audit`, `oauth_identity_link`, `event_quota_pool`, `event_gps_config`, `activity_category`, `waitlist_promotion_log`, `offline_sync_batches`, `proxy_attendance_items`, `retention_archive_log`, `appeal_histories`, `org_faculty`, `org_major`, `academic_term`, `import_jobs` with the exact V2 tables/columns in `v2-schema-blueprint.md`).
  - Update §2.3 ownership table to match Sprint 2 ownership (`AGENTS.md` / Spec §5.5, removing the stale "chưa gán CODEOWNERS" gap note).
  - Update §4.3 5-role table (3-tier Evidence/Appeal BCS → CVHT R1 → EVENT_OWNER R2; `SYSTEM_ADMIN` exclusive for Import/Master Data).
  - **Fix §4.4 line 248 GPS contradiction:** split cleanly — ngoài bán kính trả **`422 GPS_OUT_OF_RANGE`** (`counted = true`), chỉ khi **trong bán kính** nhưng `isMocked` hoặc `accuracy` bất thường mới `ACCEPTED` + `suspicious_flag = TRUE`.
  - Update §5.3 (`NO_SHOW` after `MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`; `150 ngày` retention), §6.2 (mark Sprint 1 drifts `/history`, `alreadyRegistered`, `QR_EPOCH_SECONDS=10`, fail-fast secret as **Resolved on `main` 26/11/2026**, and add the explicit 3-item Figma handoff list: move Master Data/Import menu `EVENT_OWNER` → `SYSTEM_ADMIN`, add Google SSO + Forgot Password + Logout mobile to `01 · login`, note GPS `60m` is per-event example vs `50m` default).
  - Update §7.1–§7.3 to `APPROVED (28/11/2026)` and trim §7.3 to only genuine open external items.
- **`docs/import-contract/student-import-contract.md` & `docs/import-contract/README.md`:**
  - Update status banners & §2.2 / §3 headings from `ASSUMPTION chưa duyệt` / `Chờ Stakeholder phê duyệt` to **`APPROVED (28/11/2026 — D-3, D-10)`** (confirming `SYSTEM_ADMIN` exclusive role, `QE-26`/`QE-73` split, `allowClassTransfer = false` default + audited explicit override, `APP_GOOGLE_ALLOWED_DOMAINS=st.qnu.edu.vn,qnu.edu.vn` env allowlist, and cross-link `../Design/v2-schema-blueprint.md`). Keep `Q-AUTH-03` (Google Cloud Console setup) noted as deployment-time open item.

- [ ] **Step 5: Verify Task 3 changes (GREEN) and commit**

Run:
```powershell
python -c "b=r'c:\Workspaces\QNU-Events-BE'; banned=[('docs/Design/quota-rules.md','освоб'),('docs/Design/quota-rules.md','phân bổ/h hoàn suất'),('docs/architecture/dynamic-qr-specification.md','file:///c:/Users/fleya'),('docs/architecture/dynamic-qr-specification.md','MISSING_HEADER'),('docs/architecture/dynamic-qr-specification.md','Assumption A-04'),('docs/architecture/attendance-consistency-design.md','registration_pool_allocations'),('docs/architecture/mvp-architecture-and-boundaries.md','event_quota_pool'),('docs/architecture/mvp-architecture-and-boundaries.md','org_faculty')]; hits=[(f,s) for f,s in banned if s in open(b+'/'+f,encoding='utf-8').read()]; assert not hits, f'STILL PRESENT: {hits}'; print('TASK 3 OK')"
git -C c:\Workspaces\QNU-Events-BE add docs/decisions.md docs/Design/quota-rules.md docs/architecture/ docs/import-contract/student-import-contract.md docs/import-contract/README.md; git -C c:\Workspaces\QNU-Events-BE commit -m "docs(arch): reconcile decisions, quota, attendance, QR, auth and import specs for Sprint 2 [QE-81]"
```
Expected: `TASK 3 OK` and clean commit created.

---

### Task 4: Reconcile Test Matrices, RTM, Scrum Log, README, Testcase Links & Sprint 2 Governance (`QNU-Events-BE`)

**Files:**
- Modify: `c:\Workspaces\QNU-Events-BE\docs\test\business-state-transition-matrix.md:1-124`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\test\test-matrix-attendance-br-att.md:1-13,68-74,166-243`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\traceability-matrix.md:1-98`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\scrum\sprint1-scrum-ceremonies-and-dod.md:1-79`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\test\testcase\4.1-xac-thuc-quan-ly-phien-dang-nhap.md:5` (and `4.2` through `4.6`:5)
- Modify: `c:\Workspaces\QNU-Events-BE\docs\plan\TEST PLAN SPRINT 1.md:39`, `src/test/AutomationTest/README.md:3`, `src/test/AutomationTest/JMeterTest/README.md:3`
- Modify: `c:\Workspaces\QNU-Events-BE\README.md:1-45,109-158`
- Modify: `c:\Workspaces\QNU-Events-BE\AGENTS.md:27-39`
- Modify: `c:\Workspaces\QNU-Events-BE\.github\CODEOWNERS:1-10`

**Interfaces:**
- Consumes: `docs/Design/v2-schema-blueprint.md`, `docs/api/error-codes.md` (Task 2), Spec §5.1–§5.5 & §6.2 #9–#15
- Produces: Reconciled QA/RTM/Scrum/README docs and Sprint 2 `AGENTS.md` + `.github/CODEOWNERS`

- [ ] **Step 1: Verify Task 4 contradictions currently exist (RED)**

Run:
```powershell
python -c "b=r'c:\Workspaces\QNU-Events-BE'; checks=[('docs/test/business-state-transition-matrix.md','CANCELLATION_DEADLINE_PASSED'),('docs/test/test-matrix-attendance-br-att.md','OUT_OF_GPS_RADIUS'),('docs/traceability-matrix.md','trong vòng 48h'),('README.md','94 phương thức')]; hits=[f for f,s in checks if s in open(b+'/'+f,encoding='utf-8').read()]; assert len(hits)==4, hits; print('RED CONFIRMED:', len(hits))"
```
Expected: `RED CONFIRMED: 4`.

- [ ] **Step 2: Update `business-state-transition-matrix.md` and `test-matrix-attendance-br-att.md`**

- **`docs/test/business-state-transition-matrix.md`:**
  - Line 7: fix `../QNU_Event_SRS.docx` → `../spec/QNU_Event_SRS.docx` and link `../api/error-codes.md`.
  - Line 10: fix Appeal ticket reference (`QE-43` → `QE-59`/`QE-60`; BCS Evidence review = `QE-43`).
  - Line 35, 46, 117: replace `CANCELLATION_DEADLINE_PASSED` → **`CANCELLATION_CUTOFF_PASSED`**.
  - Line 36 (`WAITING_LIST` × `E4`): allow withdrawing from `WAITING_LIST` exempt from the 12h cutoff (`D-5`).
  - Line 37 (`CANCELLED` × `E1/E2`): allow re-registration after `CANCELLED` if event is still `PUBLISHED`, open, and student is not under `NO_SHOW` restriction (`D-5`, matching `V1` `uq_registrations_active`).
  - Line 44 (`BR-REG-02`): specify ordering by `wait_order ASC` (`events.next_wait_order`) + `FOR UPDATE SKIP LOCKED` + eligibility re-check skipping ineligible `#01`, continuing after `registration_deadline` before `startAt - 12h`.
  - Line 61, 76, 86, 123–124: update Appeal to 3-tier **BCS (Evidence) → CVHT (Appeal Vòng 1, trong vòng 5 ngày lịch từ `T0`) → EVENT_OWNER (Phúc khảo Vòng 2, trong vòng 3 ngày lịch, trần 30 ngày)** (`QE-59`/`QE-60`, `D-2`, `D-9`).
  - Line 72 (`PENDING` × `A1`): allow scanning valid QR/GPS while `PENDING` Evidence exists → **`ACCEPTED`** (auto-superseding `PENDING` → `REJECTED` / `SUPERSEDED_BY_ACCEPTED`, `D-7`).
- **`docs/test/test-matrix-attendance-br-att.md`:**
  - Lines 9–10: fix `docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx` and `docs/spec/QNU_Event_SRS.docx`; link `docs/api/error-codes.md`.
  - Line 68: clarify `ATTEMPT_LIMIT_REACHED` blocks automated channels (`DYNAMIC_QR`, `GPS`, `OFFLINE_QR`, `OFFLINE_GPS`); `EVIDENCE` remains separate fallback (`D-7`).
  - Lines 70, 192–193: replace `EVENT_OWNER` / `Event Owner` with **`CLASS_REPRESENTATIVE (BCS)`** within assigned classes (`bcs_class_assignment`, `QE-43`, `D-2`).
  - Line 170: `event.location_radius_meters` → `events.allowed_radius_meters` (`10–500m`, default `50m`).
  - Lines 176, 231: `OUT_OF_GPS_RADIUS` → **`GPS_OUT_OF_RANGE`**.
  - Line 232: `INVALID_EVIDENCE` → **`INVALID_EVIDENCE_FILE`**.
  - Lines 201–208: align `NO_SHOW` job timing (`MAX(attendance_close_at + 24h, COALESCE(evidence_deadline, attendance_close_at))`, skips `PENDING` Evidence & pauses for active Round 1 Appeal, overlapping ban = `MAX(current_banned_until, now + 7 days)`), table name `student_penalties` → **`student_restrictions`**, endpoint `/register` → **`/registrations`**, ticket `QE-14`.
  - Line 239: update `11 bài kiểm thử tích hợp` → **`25` bài kiểm thử tích hợp trong `AttendanceIT`** (verified on `main` `26/11/2026`).

- [ ] **Step 3: Update `traceability-matrix.md`, `sprint1-scrum-ceremonies-and-dod.md`, `testcase/4.1..4.6`, and `TEST PLAN SPRINT 1.md`**

- **`docs/traceability-matrix.md`:**
  - Line 5: fix `[QNU_Event_SRS.docx](spec/QNU_Event_SRS.docx)` and `[Product Backlog](architecture/mvp-architecture-and-boundaries.md)`.
  - Line 17: fix `app_user_roles` → `user_role`.
  - Lines 37–39 (`UC022`–`UC024`): fix `trong vòng 48h sau sự kiện` → `trong vòng 5 ngày lịch kể từ T0 lên CVHT (Vòng 1)`; `UC023` → `trong vòng 3 ngày lịch lên EVENT_OWNER (Vòng 2, trần 30 ngày)`; `UC024` → `CVHT (Vòng 1) / EVENT_OWNER (Vòng 2)`.
  - Line 44 (`UC029`): `sau thời hạn 4–5 tháng` → `sau 150 ngày từ events.end_at (loại trừ Appeal đang mở, xác minh backup trước khi xóa)`.
  - Lines 25, 33, 36, 78–98: update to verified `26/11/2026` `main` counts (`151` BE JUnit = `40` Unit across 8 files + `111` IT across 10 files; `43` FE Vitest across 12 files including `AttendanceSlot` 7, `RegistrationSlot` 4, `HistoryPage` 8, `EventFormPage` 3, `EventPublishFlow` 3; `7` Playwright mocked; `359` `.bru`; `35` Selenium; `4` JMeter; `15/15` Jira Bugs `Done`; PR `#49–#52` BE & `#14–#19` FE). Clarify Manual TC count (`155` unique black-box TC IDs in `4.1..4.6`, `259` total including white-box TCs).
- **`docs/scrum/sprint1-scrum-ceremonies-and-dod.md`:**
  - Update date header to `28/11/2026`.
  - Add timeline row `25–26/11/2026` (merge PR `#49–#52` BE & `#14–#19` FE into `develop` & release to `main`; `15/15` Jira Bugs `Done`; Sprint 1 `41/41` items `Done` awaiting Complete; `151` BE / `43` FE tests passing).
  - Update §3 verification counts & §5 Sprint status accordingly.
  - **CRITICAL (`R1`):** Leave lines 38, 39, 56 `[Chờ thành viên xác nhận...]` placeholders completely untouched.
- **`docs/test/testcase/4.1..4.6-*.md` (line 5), `docs/plan/TEST PLAN SPRINT 1.md` (line 39), `src/test/AutomationTest/README.md` (line 3), & `src/test/AutomationTest/JMeterTest/README.md` (line 3):**
  - Replace `../../QNU_Event_SRS.docx` → `../../spec/QNU_Event_SRS.docx` in all 6 testcase files, and `docs/QNU_Event_SRS.docx` → `docs/spec/QNU_Event_SRS.docx` (and Proposal if present) in `TEST PLAN SPRINT 1.md` and the two AutomationTest READMEs.

- [ ] **Step 4: Update `README.md`, `AGENTS.md`, and `.github/CODEOWNERS`**

- **`README.md`:**
  - Line 3: fix links to `docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx` and `docs/spec/QNU_Event_SRS.docx`.
  - Line 22: update Evidence storage note (`EvidenceStoragePort`, default Local Private Filesystem, pluggable MinIO/S3 — `D-8`).
  - Line 41: add `GET /api/v1/events/{id}/attendance/me` in the Sprint 1 API table.
  - Lines 111–120: update test counts from `94` (`13` unit + `81` IT) → **`151` phương thức `@Test`** (**`40` unit `@Test`** across 8 files + **`111` integration `@Test`** across 10 files) with exact per-file counts.
  - Lines 146–152: add links to `docs/Design/v2-schema-blueprint.md` and `docs/api/error-codes.md`.
  - Line 157: update fail-fast secret note (implemented via `resolveJwtSecret` / QR signing secret fail-fast outside `dev`/`demo`/`test` on `main`).
- **`AGENTS.md` (Scope Table §2):**
  - Update table to match Spec §5.5 (Sprint 2 full BE + FE + Docs ownership per member, using valid Java package `importer/`, co-owned `evidence/` for A3 & A4, `appeal/` for A4, `outbox/` + `V2` migration exclusively for A0).
- **`.github/CODEOWNERS`:**
  - Update exact rules:
    ```text
    # Toan bo repository do Cuong (@GMIOS25) quan ly mac dinh
    * @GMIOS25

    # Phan quyen review theo tung module cua thanh vien (Sprint 2+)
    /src/main/java/vn/edu/qnu/events/event/ @hoangkq157
    /src/main/java/vn/edu/qnu/events/registration/ @YangKlee @hoangkq157
    /src/main/java/vn/edu/qnu/events/importer/ @YangKlee
    /src/main/java/vn/edu/qnu/events/attendance/ @Dan28-05
    /src/main/java/vn/edu/qnu/events/evidence/ @Dan28-05 @KHANHSKY0808
    /src/main/java/vn/edu/qnu/events/appeal/ @KHANHSKY0808
    /src/main/java/vn/edu/qnu/events/audit/ @KHANHSKY0808
    /src/main/java/vn/edu/qnu/events/student/ @KHANHSKY0808
    /src/main/java/vn/edu/qnu/events/outbox/ @GMIOS25
    /docs/ @KHANHSKY0808 @GMIOS25
    ```

- [ ] **Step 5: Verify Task 4 changes (GREEN) and commit**

Run:
```powershell
python -c "b=r'c:\Workspaces\QNU-Events-BE'; banned=[('docs/test/business-state-transition-matrix.md','CANCELLATION_DEADLINE_PASSED'),('docs/test/test-matrix-attendance-br-att.md','OUT_OF_GPS_RADIUS'),('docs/test/test-matrix-attendance-br-att.md','student_penalties'),('docs/traceability-matrix.md','trong vòng 48h'),('README.md','94 phương thức')]; hits=[(f,s) for f,s in banned if s in open(b+'/'+f,encoding='utf-8').read()]; assert not hits, hits; scrum=open(b+'/docs/scrum/sprint1-scrum-ceremonies-and-dod.md',encoding='utf-8').read(); assert scrum.count('[Chờ thành viên xác nhận')==3, 'R1 placeholder altered'; co=open(b+'/.github/CODEOWNERS',encoding='utf-8').read(); assert '/importer/ @YangKlee' in co and '/evidence/ @Dan28-05 @KHANHSKY0808' in co; print('TASK 4 OK')"
git -C c:\Workspaces\QNU-Events-BE add docs/test/ docs/traceability-matrix.md docs/scrum/sprint1-scrum-ceremonies-and-dod.md "docs/plan/TEST PLAN SPRINT 1.md" src/test/AutomationTest/README.md src/test/AutomationTest/JMeterTest/README.md README.md AGENTS.md .github/CODEOWNERS; git -C c:\Workspaces\QNU-Events-BE commit -m "docs(qa): align test matrices, RTM, scrum log, README and Sprint 2 CODEOWNERS [QE-81]"
```
Expected: `TASK 4 OK` and clean commit created.

---

### Task 5: Surgical XML-Preserving Updates to Proposal v2 & SRS v1.1 (`.docx`) (`QNU-Events-BE`)

**Files:**
- Modify: `c:\Workspaces\QNU-Events-BE\docs\spec\QNU_Event_Participation_Management_Project_Proposal_v2.docx`
- Modify: `c:\Workspaces\QNU-Events-BE\docs\spec\QNU_Event_SRS.docx`
- Stage & Commit: `c:\Workspaces\QNU-Events-BE\docs\superpowers\specs\2026-11-28-pre-sprint2-docs-alignment-design.md` & `docs/superpowers/plans/2026-11-28-pre-sprint2-docs-alignment.md`

**Interfaces:**
- Consumes: Approved Spec §5.1–§5.4 & §6.3; exact verified indices from `Proposal_v2.docx` (`TBL[0,3,9,13]`, `P[131,154,157]`) and `QNU_Event_SRS.docx` (`P[4,17,117-119,228-230,329,384,393-395,550-551,707,716-718]`, `TBL[0,15,31,34,42,44,81,82]`)
- Produces: Valid updated OOXML `.docx` files with `54` preserved UML images (SRS) and `0` media (Proposal)

- [ ] **Step 1: Write temporary DOCX verification test script and run RED**

Write `.superpowers/scratch/verify_docx.py` (do NOT commit) asserting:
1. Both files open cleanly via `zipfile.ZipFile` (`testzip() is None`) and `docx.Document`.
2. Media counts: `len(word/media/*) == 0` for Proposal, `len(word/media/*) == 54` for SRS.
3. Proposal assertions:
   - `TBL[0] R[0]` mentions `28/11/2026` / `SRS v1.1`.
   - `TBL[3]` contains a `Cố vấn học tập (CVHT)` row before the merged `Lưu ý BA` last row (last row still merged across cols 0–3, `len(rows) == 7`), and `Admin / Event Owner` row mentions Appeal Vòng 2 / no Master Data Import.
   - `TBL[9]` (`len(rows) == 6`) and `TBL[13]` (`len(rows) == 6`) both contain `CVHT`.
   - `P[131]` mentions `allowClassTransfer` audited override by `SYSTEM_ADMIN`.
   - `P[154]` no longer presents `offline QR token prefetch` as open (forbids Offline Prefetch, uses Store-and-Forward `24 giờ`), and §18 notes resolution in `SRS v1.1` & `docs/decisions.md`.
4. SRS assertions:
   - `P[4]` has `Version 1.1 approved`; `P[17]` has `SRS Version 1.1`; `TBL[0]` has `1.0` (05/11/2026) and `1.1` (28/11/2026).
   - `TBL[15] R[3]` (`UC005 A3`) reflects `A-EVT-01` (safe fields + `quotaTotal`/`event_pools.capacity >= used` with auto-promote; time/location/audience locked).
   - `P[329]` (`UC007`) specifies role-scoped calendar filters (`STUDENT`/`EVENT_OWNER`/`CVHT`).
   - `P[384]` (`REQ-REG-012`) exempts `WAITING_LIST` from the 12h cutoff; `TBL[31] R[2]` (`UC010 4a.1`) has `403 Forbidden (NOT_IN_AUDIENCE)` (not `400 Bad Request`); `TBL[34] R[1]` (`UC011 3a.1`) has `409 Conflict (CANCELLATION_CUTOFF_PASSED)` (not `400 Bad Request`).
   - `TBL[42] R[1]` (`UC015 5a`) has `409 Conflict (ATTEMPT_LIMIT_REACHED)` (not `429 Too Many Requests`); `TBL[44] R[2]` (`UC016 7a`) has `422 Unprocessable Entity (GPS_OUT_OF_RANGE)` (not `400 Bad Request`).
   - `P[707]` (`REQ-REV-014`) clearly states 3-tier `BCS` → `CVHT (Vòng 1: 5 ngày lịch từ T0)` → `EVENT_OWNER (Vòng 2: 3 ngày lịch, trần 30 ngày)`.
   - Inline `TBD-*` paragraphs (`P[117–119]`, `P[228–230]`, `P[393–395]`, `P[550–551]`, `P[716–718]`) include `[Đã chốt 28/11/2026]` summary.
   - `TBL[81]` (19×4) marks all 16 resolved functional TBDs (`TBD-AUTH-01..03`, `TBD-EVT-01..03`, `TBD-REG-01..03`, `TBD-ATT-01..02`, `TBD-REV-01..03`, `TBD-DATA-01`, `TBD-DATA-03`) as `Closed — 28/11/2026` (with `TBD-AUTH-03` noting `Q-AUTH-03` Google Console operational credentials remain open) and keeps `TBD-ATT-03` & `TBD-DATA-02` `Open`.
   - `TBL[82]` (11×4) marks `TBD-SEC-02` as `Closed — 28/11/2026` and keeps the other 9 rows (`TBD-NFR-01..05`, `TBD-SEC-01`, `TBD-SEC-03..04`, `TBD-OTH-01`) `Open`.

Run:
```powershell
python c:\Workspaces\QNU-Events-BE\.superpowers\scratch\verify_docx.py
```
Expected: FAIL on pre-edit `.docx` files.

- [ ] **Step 2: Apply surgical in-place XML edits to `Proposal_v2.docx` and `QNU_Event_SRS.docx`**

Run a Python `python-docx` script that mutates **only** the targeted paragraphs/cells (preserving existing paragraph/run font styling via helper `set_para_text_preserve_style(para, new_text)` and inserting new table rows by deep-copying a styled sibling `<w:tr>` element via `copy.deepcopy(row._tr)` so borders, widths, and cell merges remain 100% identical to native Word formatting — never rebuilding either document from scratch):
- In `Proposal_v2.docx`:
  - `TBL[0] R[0]`: append update note `— Đã rà soát đồng bộ 5 vai trò (bổ sung CVHT) và quy tắc nghiệp vụ với SRS v1.1 (28/11/2026)`.
  - `TBL[3]` (6×4, where cols 2–3 are merged horizontally and `R[5]` is merged across all 4 cols): deep-copy `R[2]` (`<w:tr>`), insert directly BEFORE `R[5]`, populate as `Cố vấn học tập (CVHT)` (Tiếp nhận & xử lý Khiếu nại Vòng 1, xem lịch tuần/tháng theo lớp, xem Evidence liên quan khi giải quyết khiếu nại; Chỉ xem/xử lý sinh viên và hồ sơ thuộc lớp được phân công cố vấn; không tạo sự kiện, không quyết định Phúc khảo Vòng 2). Update `R[3]` (`Admin / Event Owner`) to clarify handling Phúc khảo Vòng 2 & gỡ No-show and no CRUD/Import Master Data; update `R[4]` (`System Admin`) to explicitly state sole CRUD/Import Master Data & Student CSV authority.
  - `TBL[9]` (5×2): insert `CVHT` row before `System Admin` (`Chỉ xem minh chứng của sinh viên thuộc lớp được phân công khi giải quyết khiếu nại Vòng 1.`).
  - `TBL[13]` (5×2): insert `CVHT` row before `System Admin` (`Xem lịch sự kiện tuần/tháng theo lớp phụ trách; xem minh chứng liên quan khi xử lý khiếu nại; tiếp nhận và quyết định Khiếu nại Vòng 1 (Chấp nhận / Từ chối / Yêu cầu bổ sung).`) and clarify `Admin/Event Owner` handles Appeal Vòng 2 & gỡ No-show (not Master Data import).
  - `P[131]`: append exception `Mặc định allowClassTransfer = false; chỉ System Admin mới được bật cờ xác nhận tường minh để đính chính dữ liệu nhập sai ban đầu kèm nhật ký kiểm toán (audit_logs).`
  - `P[154]`: update bullet to state Offline Prefetch bị cấm tuyệt đối; áp dụng Student Offline Store-and-Forward trong cửa sổ 24 giờ sau `attendance_close_at` và grace period 3 giây cho QR online (`SRS v1.1`).
  - `P[157]`: add concise summary note that all functional items in §18 have been finalized in `SRS v1.1` & `docs/decisions.md` (`28/11/2026`), leaving only load-test/infra parameters (`TBD-NFR-01..05`) open.
- In `QNU_Event_SRS.docx`:
  - `P[4]` → `Version 1.1 approved`; `P[17]` → update `SRS Version 1.0` to `SRS Version 1.1 (cập nhật ngày 28/11/2026 — đồng bộ quyết định D-1..D-10 trước Sprint 2)`.
  - `TBL[0]` (`Revision History`): fill `R[1]` with `Nhóm 1 | 05/11/2026 | Phát hành SRS Version 1.0 cơ sở | 1.0` and `R[2]` with `Nhóm 1 | 28/11/2026 | Chuẩn hóa trước Sprint 2 (D-1..D-10): 5 vai trò & phân cấp 3 tầng BCS→CVHT→EVENT_OWNER, event_pools + wait_order, miễn mốc 12h cho WAITING_LIST, Offline Store-and-Forward 24h, chuẩn hóa mã lỗi 403/409/422, đóng 17 mục TBD trong Appendix C | 1.1`.
  - Update `TBL[15] R[3]` (`UC005 A3`), `P[329]` (`UC007`), `P[384]` (`REQ-REG-012`), `TBL[31] R[2]` (`UC010 4a.1`), `TBL[34] R[1]` (`UC011 3a.1`), `TBL[42] R[1]` (`UC015 5a`), `TBL[44] R[2]` (`UC016 7a`), `P[707]` (`REQ-REV-014`).
  - Append concise `[Đã chốt 28/11/2026 — ...]` resolutions to inline `TBD-*` paragraphs (`P[117–119]`, `P[228–230]`, `P[393–395]`, `P[550–551]`, `P[716–718]`).
  - Update `TBL[81]` (col 3) & `TBL[82]` (col 3) with exact `Closed — 28/11/2026: ...` vs `Open — ...` statuses per Spec §6.3 #2.

- [ ] **Step 3: Run DOCX verification test script (GREEN) and remove scratch scripts**

Run:
```powershell
python c:\Workspaces\QNU-Events-BE\.superpowers\scratch\verify_docx.py; Remove-Item -Recurse -Force c:\Workspaces\QNU-Events-BE\.superpowers\scratch
```
Expected: `ALL DOCX CHECKS PASSED (Proposal media=0, SRS media=54)` and scratch folder deleted cleanly.

- [ ] **Step 4: Commit `.docx` updates, Design Spec, and Implementation Plan**

Run:
```powershell
git -C c:\Workspaces\QNU-Events-BE add docs/spec/QNU_Event_Participation_Management_Project_Proposal_v2.docx docs/spec/QNU_Event_SRS.docx docs/superpowers/specs/2026-11-28-pre-sprint2-docs-alignment-design.md docs/superpowers/plans/2026-11-28-pre-sprint2-docs-alignment.md; git -C c:\Workspaces\QNU-Events-BE commit -m "docs(spec): update Proposal v2 and SRS to v1.1 preserving UML diagrams [QE-81]"
```
Expected: Clean commit created on `docs/QE-81-pre-sprint2-docs-alignment`.

---

### Task 6: Whole-Repo Integrity Verification Gate (`BE` + `FE`)

**Files:**
- Verify (read-only): All active Markdown files in `QNU-Events-BE` (`README.md`, `AGENTS.md`, `docs/**` excluding `docs/superpowers/` and bannered historical snapshots `docs/scrum/sprint1-tech-lead-review.md`, `docs/Test_Evidence/TEST_REPORT.md`, `docs/test/sprint1-test-execution-report.md` §1–4) and `QNU-Events-FE` (`docs/**`), plus `V1__sprint1_core_schema.sql` and Git branch state.

**Interfaces:**
- Consumes: All deliverables from Tasks 1–5
- Produces: Verified pass evidence for `verification-before-completion` and final whole-branch review

- [ ] **Step 1: Run cross-repo zero-contradiction & relative-link integrity check**

Run inline Python verifier (stdin, leaves no file on disk) checking:
1. `0` hits across active BE & FE Markdown docs for all banned patterns: `file:///c:/Users/fleya`, `освоб`, `phân bổ/h hoàn suất`, `OUT_OF_GPS_RADIUS`, `CANCELLATION_DEADLINE_PASSED`, `MISSING_HEADER`, `trong vòng 48h`, `94 phương thức`, `INVALID_EVIDENCE` (not followed by `_FILE`), `registration_pool_allocations`, `student_penalties`, `docs/QNU_Event_SRS.docx`, `docs/QNU_Event_Participation_Management_Project_Proposal_v2.docx`, `../QNU_Event_SRS.docx`, `](QNU_Event_SRS.docx)`, `sprint1_rescue_plan.md`.
2. `0` broken relative Markdown links and `0` `file://` URIs across all active BE & FE `.md` files.
3. `FE/docs/api/sprint1-contract.md` and `BE/docs/api/sprint1-contract.md` have identical §2 endpoint tables (lines matching `^\| \`(GET|POST|PUT|DELETE)\``).
4. `git -C c:\Workspaces\QNU-Events-BE diff --name-only origin/main..HEAD` touches **zero** `.java`, `.sql`, `.xml`, `.yml` files (only `docs/`, `README.md`, `AGENTS.md`, `.github/CODEOWNERS`, `src/test/AutomationTest/**/README.md`), and `git -C c:\Workspaces\QNU-Events-FE diff --name-only origin/develop..HEAD` touches **only** `docs/api/sprint1-contract.md`.
5. Re-verify `.docx` ZIP integrity and `word/media/` counts (`54` SRS, `0` Proposal).

Expected: `WHOLE-REPO INTEGRITY GATE PASSED (0 banned hits, 0 broken links, 0 code/V1 edits, SRS media=54, Proposal media=0)`.

- [ ] **Step 2: Run mandatory `R5` automated build & test suites on both repos**

Run:
```powershell
cd c:\Workspaces\QNU-Events-BE; .\mvnw.cmd -B -ntp test
cd c:\Workspaces\QNU-Events-FE; npm run lint; npm run typecheck; npm run test -- --run; npm run build
```
Expected:
- BE: `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).
- FE: `oxlint` 0 errors/0 warnings, `tsc` exit 0, `43 passed` (`12` test files), `vite build` exit 0.

- [ ] **Step 3: Inspect final Git status & commit logs on both repos**

Run:
```powershell
git -C c:\Workspaces\QNU-Events-BE status --short; git -C c:\Workspaces\QNU-Events-BE log --oneline origin/main..HEAD; git -C c:\Workspaces\QNU-Events-FE status --short; git -C c:\Workspaces\QNU-Events-FE log --oneline origin/develop..HEAD
```
Expected:
- BE: clean working tree on `docs/QE-81-pre-sprint2-docs-alignment` with 4 atomic Conventional Commits `[QE-81]`.
- FE: only pre-existing untracked `pnpm-lock.yaml` on `docs/QE-70-sync-sprint1-contract` with 1 atomic Conventional Commit `[QE-70]`.
- Neither branch pushed to remote.

---

## Handoff Notes (Included in Final Completion Report — Not Automated Here)

1. **Figma Manual Updates (3 items for UI Owner):**
   - Move Master Data CRUD & Student CSV Import menu from `ADMIN / EVENT OWNER` → `SYSTEM_ADMIN` (`D-3`).
   - Add Google SSO button (`Email QNU`), Forgot/Reset Password link, and Mobile Logout button (`01 · login` & mobile shell, `D-10`).
   - Annotate GPS screen radius `60m` as a per-event example (system default = `50m`, range `10–500m`, `D-7`).
2. **Jira Operational Reminder (`R10` — No mutations performed here):**
   - Sprint 1 (`Sprint 41`) has `41/41` items `Done` (`15/15` Bugs `Done`) and is ready to click **Complete Sprint** before starting Sprint 2 (`Sprint 42`).
