# Báo cáo Thực thi Kiểm thử Sprint 1 — QNU-Events

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Thời điểm chạy xác minh:** 19/11/2026 (UTC+7)  
> **Nhánh kiểm thử:** BE `docs/QE-82-Test-plan-manual-testcase` (`c9a1dcd`, tách từ `main` `667a54e`) · FE `develop` (`0fe4a3c`)  
> **Môi trường kiểm thử:** PostgreSQL 17.11 (`postgres:17-alpine`, Docker `qnu-events-postgres-1`, `127.0.0.1:5432/qnu_events`), Java (`25.0.4` runtime / target Java 21), Spring Boot 4.1.2, React 19.3.1, Vite 8.3.6, Vitest 5.1.0, Playwright 1.65.0

---

## 1. Tổng quan Kết quả Kiểm thử Thực tế

| Hạng mục kiểm thử | Công cụ & Môi trường | Số test case | Đạt (Pass) | Lỗi (Fail) | Bỏ qua (Skip) | Tỷ lệ thành công |
|---|---|---:|---:|---:|---:|---:|
| **Backend Unit Tests** | Maven Surefire · JUnit 5 | 16 | 16 | 0 | 0 | **100%** |
| **Backend Integration Tests** | Maven Failsafe · Spring Boot Test + PostgreSQL 17.11 thật | 72 | 72 | 0 | 0 | **100%** |
| **Frontend Unit & Component Tests** | Vitest 5.1.0 + React Testing Library | 11 | 11 | 0 | 0 | **100%** |
| **Frontend End-to-End (E2E) Tests** | Playwright (`e2e/*.spec.ts`, API mock qua `page.route`, chạy bằng Chrome cục bộ)* | 7 | 7 | 0 | 0 | **100%*** |
| **TỔNG CỘNG TOÀN HỆ THỐNG** | | **106** | **106** | **0** | **0** | **100%** |

> [!NOTE]
> - Tuân thủ nguyên tắc **R1** và **R2** (`AGENTS.md`): không dùng `-DskipTests`, không `@Disabled`; toàn bộ 72 integration tests BE đã chạy trực tiếp trên PostgreSQL 17.11 thật bằng lệnh `.\mvnw.cmd -B -ntp verify -Pintegration` (`BUILD SUCCESS`, `01:47 min`).
> - **Ghi chú đối chiếu lệch số liệu trước đây:** Bản báo cáo cũ ghi tổng `84` test (`66` BE + `11` FE Vitest + `7` FE E2E, thậm chí các mục con BE cộng lại chỉ bằng `64`) và có một số mô tả chưa khớp mã nguồn (ví dụ: ghi sai `423 ACCOUNT_LOCKED`, `401 UNAUTHENTICATED` khi sai mật khẩu, `StatusBadge.test.tsx` kiểm tra `ACCEPTED`/`NOT_CHECKED_IN`, và bỏ sót `ApplicationIT`, `MigrationIT`, `SharedKernelSecurityIT`).
> - **Ghi chú sai lệch nhánh Git BE:** Số liệu `88` test BE là của nhánh hiện tại (`docs/QE-82-Test-plan-manual-testcase` / `main` `667a54e`). Trên nhánh `develop` cục bộ (`8ef2ca1`) hiện có `89` test BE và `origin/develop` (`4ac5da1`) có `94` test BE (do nhánh `develop` đã merge thêm PR #5, #6, #8 nhưng chưa merge PR #4 `fix/QE-56-dynamic-qr-hardening` ở `main`).
> - **(\*) Ghi chú Playwright trên máy kiểm thử:** Lệnh mặc định `npx playwright test` trên FE lỗi khởi chạy do máy chưa tải binary `chromium_headless_shell-1254` (chỉ có sẵn `chromium-1248` trong `ms-playwright`); khi trỏ sang Google Chrome cài sẵn trên máy (`channel: 'chrome'`), toàn bộ `7/7` kịch bản E2E chạy đạt trong `8.9s`. Ngoài ra, các kịch bản E2E hiện tại dùng dữ liệu giả lập `page.route()`, chưa chạy E2E trực tiếp nối FE với BE + PostgreSQL.

### Kết quả kiểm tra tĩnh & Build Frontend (`QNU-Events-FE`)
- `npm run lint` (`oxlint src -A react/only-export-components`): **0 warnings, 0 errors** (32 files).
- `npm run typecheck` (`tsc -b --noEmit`): **Pass**.
- `npm run test` (`vitest run`): **5 test files, 11/11 tests Pass** (có cảnh báo `act(...)` ở stderr của `AuthProvider` trong `RegistrationSlot.test.tsx`).
- `npm run build` (`tsc -b && vite build`): **Pass** (`dist/` build trong `1.67s`).

---

## 2. Chi tiết Bộ Kiểm thử Backend (88/88 Pass — 16 Unit + 72 Integration)

### 2.1 Kiểm thử Hạ tầng, Migration & Shared Kernel (`ApplicationIT` 4 + `MigrationIT` 7 + `SharedKernelSecurityIT` 5 = 16 tests)
- **`ApplicationIT` (4/4 Pass):**
  - `applicationAndPostgresAreHealthy`: `/actuator/health` trả về `UP` khi kết nối PostgreSQL 17.11.
  - `developmentOpenApiIsGenerated`: `/v3/api-docs` sinh tài liệu OpenAPI (`QNU Events API`) ở profile `dev`.
  - `developmentSwaggerUiIsAvailable`: `/swagger-ui/index.html` trả về `200 OK`.
  - `businessApiRemainsProtected`: `GET /api/v1/events` không token trả `401`.
- **`MigrationIT` (7/7 Pass):**
  - `allTablesExist`: Xác nhận đủ 10 bảng Sprint 1 (`org_class`, `app_user`, `user_role`, `auth_session`, `events`, `event_audience`, `registrations`, `attendances`, `attendance_attempts`, `audit_logs`).
  - `quotaCheckConstraintRejectsZero`: Constraint `events_quota_total_check` chặn `quota_total = 0`.
  - `registrationsUniqueActiveConstraintBlocksDuplicate`: Partial unique index `uq_registrations_active` chặn 2 bản ghi `REGISTERED` trùng lặp.
  - `registrationsAllowsReRegistrationAfterCancelled`: Cho phép đăng ký lại `REGISTERED` sau khi bản ghi cũ là `CANCELLED`.
  - `attendancesUniqueAcceptedConstraint`: Unique index `uq_attendances_valid` chặn 2 bản ghi `ACCEPTED` trùng lặp.
  - `auditLogsAreImmutableRejectsUpdate` & `auditLogsAreImmutableRejectsDelete`: Trigger `trg_audit_logs_immutable` chặn mọi thao tác `UPDATE`/`DELETE` trực tiếp trên `audit_logs` (`audit_logs is append-only`).
- **`SharedKernelSecurityIT` (5/5 Pass):**
  - `unauthenticatedReturnsJsonErrorEnvelope`: `401 UNAUTHENTICATED` kèm `traceId`, `timestamp`.
  - `forbiddenReturnsJsonErrorEnvelope`: `403 FORBIDDEN` khi `STUDENT` gọi `POST /api/v1/events`.
  - `validationFailureReturnsDetailsFields`: `400 VALIDATION_FAILED` kèm `details.fields`.
  - `serverErrorDoesNotLeakStackTraceOrInternalMessage`: `500 INTERNAL_ERROR` ẩn chi tiết exception nội bộ, `details` rỗng.
  - `apiExceptionReturnsCorrectStatusAndCode`: `ApiException` ánh xạ đúng HTTP status và mã lỗi (`409 CAPACITY_FULL`).

### 2.2 Xác thực & Phân quyền (`AuthIT` 8 + `SecurityConfigurationTest` 5 = 13 tests)
- **`SecurityConfigurationTest` (5/5 Unit Tests Pass):**
  - `anonymousRequestsCannotAccessBusinessEndpoints`: Yêu cầu ẩn danh vào `/api/v1/events` trả `401`.
  - `evenAuthenticatedRequestsAreDeniedUntilExplicitRulesExist`: `SYSTEM_ADMIN` gọi `/api/v1/events` bị từ chối `403`.
  - `apiDocumentationIsPrivateByDefault`: `/v3/api-docs` trả `401` khi `springdoc.api-docs.enabled=false`.
  - `configuredFrontendCanPreflightBearerRequests`: CORS preflight cho `http://localhost:5173` hợp lệ.
  - `unconfiguredOriginsAreRejected`: Origin không cấu hình (`https://untrusted.example`) bị chặn `403`.
- **`AuthIT` (8/8 Integration Tests Pass):**
  - `loginWithLockedUserReturnsForbidden`: Tài khoản `LOCKED` bị từ chối với `403 FORBIDDEN` (`"Tài khoản của bạn đã bị khóa."`). *(Lưu ý: tính năng tự khóa sau 5 lần sai / `423 ACCOUNT_LOCKED` không nằm trong phạm vi Sprint 1 này.)*
  - `passwordsInDatabaseAreBcryptHashedNotPlaintext`: Mật khẩu lưu DB bắt đầu bằng `$2`, không lưu plaintext.
  - `studentCallingPostEventsReturns403`: `STUDENT` gọi `POST /api/v1/events` -> `403 FORBIDDEN`.
  - `ownerCallingPostRegistrationsReturns403`: `EVENT_OWNER` gọi `POST /api/v1/events/{id}/registrations` -> `403 FORBIDDEN`.
  - `loginWithValidCredentialsReturnsJwtAndUserInfo`: `owner01` đăng nhập thành công -> JWT + thông tin user.
  - `loginWithInvalidCredentialsReturnsIdenticalError`: Sai mật khẩu và tài khoản không tồn tại đều trả cùng `401 INVALID_CREDENTIALS` (`"Thông tin đăng nhập không chính xác."`).
  - `logoutRevokesSessionAndSubsequentCallFails`: `POST /api/v1/auth/logout` trả `204`, dùng lại JWT cũ trả `401 SESSION_REVOKED`.
  - `ownerCanFetchClasses`: `EVENT_OWNER` lấy danh sách lớp qua `GET /api/v1/classes` -> `200 OK`.

### 2.3 Quản lý Sự kiện & Quy trình Duyệt (`EventIT` — 10/10 Pass)
- `createDraftEventSuccess`: Tạo sự kiện `DRAFT`, `version = 0`.
- `createEventWithInvalidTimeFails`: `endAt <= startAt` -> `400 VALIDATION_FAILED`.
- `createEventWithZeroQuotaFails`: `quotaTotal = 0` -> `400 VALIDATION_FAILED`.
- `createEventWithEmptyAudienceFails`: `audienceClassIds = []` -> `400 VALIDATION_FAILED`.
- `studentCannotCreateEvent`: `STUDENT` gọi `POST /api/v1/events` -> `403 FORBIDDEN`.
- `previewAndPublishWorkflowSuccess`: Chưa preview -> `409 PREVIEW_REQUIRED`; `GET /preview` -> `previewVersion = 0`; `confirm = false` -> `400 VALIDATION_FAILED`; publish hợp lệ -> `200 PUBLISHED`; publish lặp lần 2 -> `200`; sửa sau publish -> `409`.
- `publishOutdatedVersionFails`: Sửa sự kiện tăng `version = 1`, publish với `previewVersion = 0` -> `409 PREVIEW_OUTDATED`.
- `ownerBCannotUpdateOwnerAEvent`: `owner02` sửa sự kiện của `owner01` -> `403 FORBIDDEN`.
- `studentAudienceClassIsolation`: `sv001` (`K49CNTT1`) xem chi tiết -> `200`; `sv003` (`K49CNTT2`) -> `404 NOT_FOUND`.
- `searchEventsByTitle`: `GET /api/v1/events?q=khởi nghiệp` lọc không phân biệt hoa/thường.

### 2.4 Tranh chấp đồng thời & Giữ chỗ nguyên tử (`RegistrationIT` — 12/12 Pass)
- `singleStudentRegistrationSuccess`: Đăng ký lần đầu -> `201 Created`, `alreadyRegistered = false`, `registered_count = 1`.
- `duplicateRegistrationReturns200AlreadyRegistered`: Đăng ký lại -> `200 OK`, `alreadyRegistered = true`, giữ nguyên `registered_count = 1`.
- `getMyRegistrationSuccessAndNotFound`: `GET /registrations/me` trả `404 NOT_FOUND` khi chưa đăng ký, `200 OK` sau khi đăng ký.
- `wrongClassStudentCannotRegister`: Sinh viên ngoài lớp (`sv003`) -> `403 NOT_IN_AUDIENCE`.
- `draftEventCannotBeRegistered`: Sự kiện `DRAFT` -> `409 EVENT_NOT_PUBLISHED`.
- `pastDeadlineCannotBeRegistered`: Quá hạn đăng ký -> `409 REGISTRATION_CLOSED`.
- `unauthenticatedCannotRegister`: Chưa đăng nhập -> `401 UNAUTHENTICATED`.
- `eventOwnerCannotRegister`: `EVENT_OWNER` đăng ký -> `403 FORBIDDEN`.
- `capacityFullRollbackLeavesNoOrphanRow`: Hết suất trả `409 CAPACITY_FULL`, rollback hoàn toàn không để lại bản ghi mồ côi trong `registrations`.
- `concurrencyRaceConditionQuota1`: 2 luồng tranh 1 suất -> đúng 1 `201`, 1 `409`, `registered_count = 1`.
- `concurrencyRaceConditionQuota5With30Students`: 30 luồng tranh 5 suất -> đúng 5 `201`, 25 `409`, `registered_count = 5`.
- `sameStudentParallel10Requests`: 10 luồng song song của cùng 1 sinh viên -> đúng 1 `201`, 9 `200` (`alreadyRegistered = true`), `registered_count = 1`.

### 2.5 Thuật toán Dynamic QR & Điểm danh (`QrTokenServiceTest` 11 + `AttendanceIT` 17 = 28 tests)
- **`QrTokenServiceTest` (11/11 Unit Tests Pass):**
  - `issueTokenFormat`, `verifyCurrentEpochSuccess`, `verifyPreviousEpochWithinGraceSuccess` (`+2s`), `verifyPreviousEpochPastGraceFails` (`+4s` -> `QR_EXPIRED`), `verifyFutureTokenFails` (`QR_FROM_FUTURE`), `verifyTamperedSignatureFails` (`QR_BAD_SIGNATURE`), `verifyWrongEventFails` (`QR_WRONG_EVENT`), `verifyMalformedTokenFails` (`QR_MALFORMED` cho 5 biến thể), `forgedWrongEventTokenFailsSignatureValidation`, `tokenFingerprintIsShortSha256Prefix`, `shortSigningSecretIsRejected`.
- **`AttendanceIT` (17/17 Integration Tests Pass):**
  - `ownerCanGetQrTokenDuringAttendanceWindow`, `studentCannotGetQrToken` (`403`), `getQrTokenOutsideAttendanceWindowFails` (`409 ATTENDANCE_NOT_OPEN`).
  - `checkInWithValidQrTokenSuccess` (`200 ACCEPTED`), `checkInWithSameIdempotencyKeyReturnsCachedResponse`, `checkInWithoutIdempotencyKeyFails` (`400 VALIDATION_FAILED`), `checkInWithoutRegistrationFails` (`403 NOT_REGISTERED`), `checkInTwiceWithDifferentKeysFails` (`409 ALREADY_CHECKED_IN`).
  - `checkInWithExpiredTokenFails` (`422 QR_EXPIRED`), `checkInWithBadSignatureFails` (`422 QR_BAD_SIGNATURE`), `checkInAttemptLimitReachedAfter3Failures` (3 lần sai -> lần 4 dù token hợp lệ vẫn trả `409 ATTEMPT_LIMIT_REACHED`, `counted = 3`).
  - `rejectedAttemptReplayReturnsStoredResponse`, `blankTokenIsCountedAsMalformedQrAttempt` (`422 QR_MALFORMED`), `oversizedIdempotencyKeyFailsValidation` (`101` ký tự -> `400 VALIDATION_FAILED`).
  - `concurrentValidRequestsCreateOnlyOneAttendance` (10 luồng khác key -> 1 `200`, 9 `409 ALREADY_CHECKED_IN`), `concurrentRequestsWithSameKeyReturnIdenticalResponse` (2 luồng cùng key -> 2 `200` giống hệt, 1 `attendance_attempts`), `acceptedCheckInAuditDoesNotLeakToken`.

### 2.6 Lịch sử Sinh viên & Kiểm toán (`HistoryIT` 6 + `AuditIT` 3 = 9 tests)
- **`HistoryIT` (6/6 Pass):**
  - `unauthenticatedCallFails` (`401`), `ownerRoleCallFails` (`403`), `studentWithoutRegistrationsReturnsEmptyList` (`[]`), `registeredEventShowsNotCheckedIn`, `registeredAndAttendedEventShowsAccepted`, `studentDataIsolation`.
- **`AuditIT` (3/3 Pass):**
  - `recordAuditLogSuccess`, `databaseRejectsUpdateOnAuditLogs`, `databaseRejectsDeleteOnAuditLogs`.

---

## 3. Chi tiết Bộ Kiểm thử Frontend (11 Vitest + 7 Playwright E2E)

### 3.1 Unit & Component Tests (`vitest run` — 5 files, 11/11 Pass)
- `src/test/time.test.ts` (**2 tests**): Định dạng `formatDateTime` theo múi giờ `Asia/Ho_Chi_Minh` và fallback `'—'` cho `'invalid-date'` / `null`.
- `src/test/StatusBadge.test.tsx` (**3 tests**): Kiểm tra render nhãn chữ tiếng Việt cho `PUBLISHED` (`Đã công bố`), `DRAFT` (`Bản nháp`), `REGISTERED` (`Đã đăng ký`). *(Chưa có unit test riêng cho `ACCEPTED`, `NOT_CHECKED_IN` hoặc kiểm tra màu sắc/icon).*
- `src/test/Button.test.tsx` (**2 tests**): Render nhãn nút và trạng thái `disabled` khi `loading`.
- `src/test/RegistrationSlot.test.tsx` (**2 tests**): Hiển thị tiến trình chỉ tiêu `10/50 chỗ` + nút `Đăng ký giữ chỗ` cho sinh viên, và trạng thái hết chỗ khi `remainingQuota = 0`.
- `src/test/HistoryPage.test.tsx` (**2 tests**): Trạng thái rỗng (`Bạn chưa có lịch sử sự kiện nào`) và danh sách đã đăng ký / đã điểm danh cùng KPI (`2`, `1`, `+10 đ`).

### 3.2 End-to-End Tests (`e2e/*.spec.ts` — 2 files, 7/7 Pass với Chrome cục bộ)
> Các test E2E hiện dùng `page.route()` giả lập phản hồi API (`/api/v1/...`), chưa chạy E2E nối trực tiếp BE + PostgreSQL.
- `e2e/auth-and-routes.spec.ts` (**4 tests**):
  1. Điều hướng người dùng chưa đăng nhập từ `/events` về `/login`.
  2. Hiển thị thông báo lỗi khi API login trả `401`.
  3. Chặn tài khoản `STUDENT` truy cập `/events/new` và đẩy về `/events`.
  4. Sinh viên xem trang `/history` với bản ghi `ACCEPTED` và `+10 điểm (Hội thảo)`.
- `e2e/owner-flow.spec.ts` (**3 tests**):
  5. Ban tổ chức thấy tiêu đề `Quản lý sự kiện đơn vị` và nút `Tạo sự kiện mới`.
  6. Ban tổ chức mở `/events/new` và thấy danh sách lớp (`K49CNTT1`).
  7. Ban tổ chức mở `/events/evt-999/qr` và thấy `Epoch #12345`, token và nút `Chép mã`.

---

## 4. Bug thật phát hiện trong mã nguồn & Trạng thái Issue Bug / Retest

> [!WARNING]
> **Trạng thái Issue Bug & Retest (theo R1, R8, R10, R12):** Các lỗi dưới đây được xác nhận trực tiếp qua đối chiếu mã nguồn/hợp đồng API tại thời điểm `19/11/2026`. Tuy nhiên **chưa tạo được Jira Bug issue thật** (máy kiểm thử chưa cài đặt & đăng nhập `twg` / Atlassian Teamwork Graph CLI hoặc `gh` CLI) và **chưa thực hiện sửa/retest** (do phần lớn lỗi nằm ngoài phạm vi module A2 theo bảng phân công `AGENTS.md` §2 hoặc cần chốt nhánh sửa). Mục này sẽ được cập nhật số liệu thực thi lại sau khi tạo Bug issue thật và hoàn tất sửa lỗi.

| Mã tạm | Phạm vi / File vị trí | Mô tả Bug thật đã phát hiện | Jira Bug Issue | Trạng thái sửa & Kết quả chạy lại |
|---|---|---|---|---|
| **BUG-01** *(một phần phạm vi A2)* | FE `src/features/registration/index.tsx` (L57-58, L118-140) & `src/features/events/types.ts` (L76-80) | Khi `event` chưa tải (`undefined`), `remainingQuota` mặc định `0` nên UI hiển thị nhầm `"Sự kiện đã hết suất tham gia."`, và khi sự kiện ở trạng thái `DRAFT`/`CANCELLED` nhưng còn suất lại hiển thị `"Đã hết thời hạn đăng ký sự kiện."`. Đồng thời `types.ts` khai báo `RegistrationResponse.cached: boolean` lệch với BE (`alreadyRegistered: boolean`). | *(Chưa tạo — thiếu TWG/Jira CLI)* | Chưa sửa / Chưa retest |
| **BUG-02** *(trong phạm vi A2)* | BE `src/main/java/vn/edu/qnu/events/registration/RegistrationService.java` (L59-143) | Các nhánh từ chối đăng ký ghi `audit_logs.action = "REGISTRATION_CREATE"` thay vì `"REGISTRATION_REJECTED"` (lệch `sprint1-rescue-plan.md` §6.8), `NOT_IN_AUDIENCE` ghi `outcome = "DENIED"` khác các lỗi khác ghi `"REJECTED"`, và `requestId` truyền `null`. | *(Chưa tạo — thiếu TWG/Jira CLI)* | Chưa sửa / Chưa retest |
| **BUG-03** *(ngoài phạm vi A2 — `common/`)* | BE `src/main/java/vn/edu/qnu/events/common/GlobalExceptionHandler.java` | Thiếu `@ExceptionHandler(MethodArgumentTypeMismatchException.class)`: gọi API với UUID sai định dạng (vd `/api/v1/events/not-a-uuid/registrations` hoặc `/events/not-a-uuid`) rơi vào handler chung trả **HTTP 500 `INTERNAL_ERROR`** thay vì `400 VALIDATION_FAILED`. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-04** *(ngoài phạm vi A2 — `event/`)* | BE `src/main/java/vn/edu/qnu/events/event/EventUpsert.java` (L47-48) | `activityType` thiếu `@Size(max = 50)` khớp cột DB `varchar(50)` (`V1`), gửi chuỗi > 50 ký tự lọt qua `@Valid` và ném lỗi DB **HTTP 500 `INTERNAL_ERROR`**. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-05** *(ngoài phạm vi A2 — FE `events/`)* | FE `src/features/events/EventFormPage.tsx` (L47-51) | Khi chỉnh sửa sự kiện, FE cắt trực tiếp chuỗi ISO UTC (`data.startAt.slice(0, 16)`) đưa vào `<input type="datetime-local">` thay vì chuyển sang `Asia/Ho_Chi_Minh`, làm sai lệch `-7 giờ` mỗi lần lưu chỉnh sửa. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-06** *(ngoài phạm vi A2 — `event/`)* | BE `src/main/java/vn/edu/qnu/events/event/EventService.java` (L266, L299) | Trên nhánh hiện tại (`docs/QE-82-Test-plan-manual-testcase` / `main`), `myRegistrationStatus` bị gán cứng `null`, khiến UI `EventsPage.tsx` không hiện `"✓ Bạn đã giữ chỗ"`. *(Lưu ý: `origin/develop` PR #8 đã bổ sung truy vấn này).* | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-07** *(ngoài phạm vi A2 — `student/` & FE `history/`)* | BE `HistoryService.java` (L38) & FE `HistoryPage.tsx` (L72-75, L80) | `HistoryService` không lọc `r.status` nên sinh viên đăng ký lại sau `CANCELLED` bị trả 2 dòng trùng `eventId` và cộng sai KPI; `HistoryPage` thiếu nút **"Thử lại"** khi API lỗi theo `TEST PLAN SPRINT 1.md` §4.6. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
