# Báo cáo Thực thi Kiểm thử Sprint 1 — QNU-Events

> **Dự án:** QNU-Events (Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn)  
> **Thời điểm chạy xác minh:** 21/11/2026 (UTC+7)  
> **Nhánh kiểm thử:** BE `docs/QE-82-Test-plan-manual-testcase` (`b919d2e`, đã rebase trên `origin/develop` `488d32a` gồm PR #20–#27) · FE `develop` (`b24ad2f`, sau PR #1)  
> **Môi trường kiểm thử:** PostgreSQL 17.11 (`postgres:17-alpine`, Docker `qnu-events-postgres-1`, `127.0.0.1:5432/qnu_events`), Java (`25.0.4` runtime / target Java 21), Spring Boot 4.1.2, Node.js `v24.18.0`, React 19.3.1, Vite 8.3.6, Vitest 5.1.0, Playwright 1.65.0

---

## 1. Tổng quan Kết quả Kiểm thử Thực tế

| Hạng mục kiểm thử | Công cụ & Môi trường | Số test case | Đạt (Pass) | Lỗi (Fail) | Bỏ qua (Skip) | Tỷ lệ thành công |
|---|---|---:|---:|---:|---:|---:|
| **Backend Unit Tests** | Maven Surefire · JUnit 5 | 16 | 16 | 0 | 0 | **100%** |
| **Backend Integration Tests** | Maven Failsafe · Spring Boot Test + PostgreSQL 17.11 thật | 95 | 95 | 0 | 0 | **100%** |
| **Frontend Unit & Component Tests** | Vitest 5.1.0 + React Testing Library | 11 | 11 | 0 | 0 | **100%** |
| **Frontend End-to-End (E2E) Tests** | Playwright (`e2e/*.spec.ts`, API mock qua `page.route`, chạy bằng Chrome cục bộ)* | 7 | 7 | 0 | 0 | **100%*** |
| **TỔNG CỘNG TOÀN HỆ THỐNG** | | **129** | **129** | **0** | **0** | **100%** |

> [!NOTE]
> - Tuân thủ nguyên tắc **R1** và **R2** (`AGENTS.md`): không dùng `-DskipTests`, không `@Disabled`; toàn bộ **16 unit tests + 95 integration tests BE** (**111/111 Pass**) đã chạy trực tiếp trên PostgreSQL 17.11 thật bằng lệnh `.\mvnw.cmd -B -ntp verify -Pintegration` (`BUILD SUCCESS`, `02:28 min`).
> - **Ghi chú đồng bộ với `origin/develop` (`488d32a`):** Bản nháp trước đó của báo cáo ghi `88` test BE theo nhánh `main` `667a54e` (`EventIT` 10, `AttendanceIT` 17, `HistoryIT` 6). Sau khi nhánh `docs/QE-82-Test-plan-manual-testcase` (`b919d2e`) được rebase lên `origin/develop` (`488d32a`, đã merge PR #20–#27 cùng các PR #1, #3, #5, #6, #8, #19), số lượng bài test BE thực tế là **111** `@Test` (`EventIT`: **21**, `AttendanceIT`: **24**, `HistoryIT`: **11**) và `BUG-06` (`myRegistrationStatus`) đã được khắc phục trên `develop`.
> - **Ghi chú đối chiếu bản báo cáo cũ trên `origin/develop` (`3e15758` / `488d32a`):** Bản báo cáo gốc trên `origin/develop` trước đó ghi `84` test (`66` BE + `11` FE Vitest + `7` FE E2E, các mục con BE cộng lại bằng `64`) và có một số mô tả chưa khớp mã nguồn (ví dụ: ghi `423 ACCOUNT_LOCKED`, `401 UNAUTHENTICATED` khi sai mật khẩu, `StatusBadge.test.tsx` kiểm tra `ACCEPTED`/`NOT_CHECKED_IN`, và bỏ sót `ApplicationIT`, `MigrationIT`, `SharedKernelSecurityIT`).
> - **(\*) Ghi chú Playwright trên máy kiểm thử:** Lệnh mặc định `npx playwright test` trên FE lỗi khởi chạy do máy chưa tải binary `chromium_headless_shell-1254` (chỉ có sẵn `chromium-1248` / `chromium_headless_shell-1248` trong `ms-playwright`); khi trỏ sang Google Chrome cài sẵn trên máy (`channel: 'chrome'`), toàn bộ `7/7` kịch bản E2E chạy đạt trong `14.1s`. Ngoài ra, các kịch bản E2E hiện tại dùng dữ liệu giả lập `page.route()`, chưa chạy E2E trực tiếp nối FE với BE + PostgreSQL.

### Kết quả kiểm tra tĩnh & Build Frontend (`QNU-Events-FE` · `develop` `b24ad2f`)
- `npm run lint` (`oxlint src -A react/only-export-components`): **0 warnings, 0 errors** (32 files, 115 rules).
- `npm run typecheck` (`tsc -b --noEmit`): **Pass**.
- `npm run test` (`vitest run`): **5 test files, 11/11 tests Pass** (`4.93s`; còn cảnh báo `act(...)` ở stderr của `AuthProvider` trong `RegistrationSlot.test.tsx`).
- `npm run build` (`tsc -b && vite build`): **Pass** (`dist/` build trong `961ms`).

---

## 2. Chi tiết Bộ Kiểm thử Backend (111/111 Pass — 16 Unit + 95 Integration)

### 2.1 Kiểm thử Hạ tầng, Migration & Shared Kernel (`ApplicationIT` 4 + `MigrationIT` 7 + `SharedKernelSecurityIT` 5 = 16 tests)
- **`ApplicationIT` (4/4 Pass — `22.99 s`):**
  - `applicationAndPostgresAreHealthy`: `/actuator/health` trả về `UP` khi kết nối PostgreSQL 17.11.
  - `developmentOpenApiIsGenerated`: `/v3/api-docs` sinh tài liệu OpenAPI (`QNU Events API`) ở profile `dev`, cấu hình `bearerAuth` JWT.
  - `developmentSwaggerUiIsAvailable`: `/swagger-ui/index.html` trả về `200 OK`.
  - `businessApiRemainsProtected`: `GET /api/v1/events` không token trả `401`.
- **`MigrationIT` (7/7 Pass — `0.179 s`):**
  - `allTablesExist`: Xác nhận đủ 10 bảng Sprint 1 (`org_class`, `app_user`, `user_role`, `auth_session`, `events`, `event_audience`, `registrations`, `attendances`, `attendance_attempts`, `audit_logs`).
  - `quotaCheckConstraintRejectsZero`: Constraint `events_quota_total_check` chặn `quota_total = 0`.
  - `registrationsUniqueActiveConstraintBlocksDuplicate`: Partial unique index `uq_registrations_active` chặn 2 bản ghi `REGISTERED` trùng lặp.
  - `registrationsAllowsReRegistrationAfterCancelled`: Cho phép đăng ký lại `REGISTERED` sau khi bản ghi cũ là `CANCELLED`.
  - `attendancesUniqueAcceptedConstraint`: Unique index `uq_attendances_valid` chặn 2 bản ghi `ACCEPTED` trùng lặp.
  - `auditLogsAreImmutableRejectsUpdate` & `auditLogsAreImmutableRejectsDelete`: Trigger `trg_audit_logs_immutable` chặn mọi thao tác `UPDATE`/`DELETE` trực tiếp trên `audit_logs` (`audit_logs is append-only`).
- **`SharedKernelSecurityIT` (5/5 Pass — `3.060 s`):**
  - `unauthenticatedReturnsJsonErrorEnvelope`: `401 UNAUTHENTICATED` kèm `traceId`, `timestamp`.
  - `forbiddenReturnsJsonErrorEnvelope`: `403 FORBIDDEN` khi `STUDENT` gọi `POST /api/v1/events`.
  - `validationFailureReturnsDetailsFields`: `400 VALIDATION_FAILED` kèm `details.fields`.
  - `serverErrorDoesNotLeakStackTraceOrInternalMessage`: `500 INTERNAL_ERROR` ẩn chi tiết exception nội bộ, `details` rỗng.
  - `apiExceptionReturnsCorrectStatusAndCode`: `ApiException` ánh xạ đúng HTTP status và mã lỗi (`409 CAPACITY_FULL`).

### 2.2 Xác thực & Phân quyền (`AuthIT` 8 + `SecurityConfigurationTest` 5 = 13 tests)
- **`SecurityConfigurationTest` (5/5 Unit Tests Pass — `5.935 s`):**
  - `anonymousRequestsCannotAccessBusinessEndpoints`: Yêu cầu ẩn danh vào `/api/v1/events` trả `401`.
  - `evenAuthenticatedRequestsAreDeniedUntilExplicitRulesExist`: `SYSTEM_ADMIN` gọi `/api/v1/events` bị từ chối `403`.
  - `apiDocumentationIsPrivateByDefault`: `/v3/api-docs` trả `401` khi `springdoc.api-docs.enabled=false`.
  - `configuredFrontendCanPreflightBearerRequests`: CORS preflight cho `http://localhost:5173` hợp lệ.
  - `unconfiguredOriginsAreRejected`: Origin không cấu hình (`https://untrusted.example`) bị chặn `403`.
- **`AuthIT` (8/8 Integration Tests Pass — `2.169 s`):**
  - `loginWithLockedUserReturnsForbidden`: Tài khoản `LOCKED` bị từ chối với `403 FORBIDDEN` (`"Tài khoản của bạn đã bị khóa."`). *(Lưu ý: tính năng tự khóa sau 5 lần sai / `423 ACCOUNT_LOCKED` không nằm trong phạm vi Sprint 1 này.)*
  - `passwordsInDatabaseAreBcryptHashedNotPlaintext`: Mật khẩu lưu DB bắt đầu bằng `$2`, không lưu plaintext.
  - `studentCallingPostEventsReturns403`: `STUDENT` gọi `POST /api/v1/events` -> `403 FORBIDDEN`.
  - `ownerCallingPostRegistrationsReturns403`: `EVENT_OWNER` gọi `POST /api/v1/events/{id}/registrations` -> `403 FORBIDDEN`.
  - `loginWithValidCredentialsReturnsJwtAndUserInfo`: `owner01` đăng nhập thành công -> JWT + thông tin user.
  - `loginWithInvalidCredentialsReturnsIdenticalError`: Sai mật khẩu và tài khoản không tồn tại đều trả cùng `401 INVALID_CREDENTIALS` (`"Thông tin đăng nhập không chính xác."`).
  - `logoutRevokesSessionAndSubsequentCallFails`: `POST /api/v1/auth/logout` trả `204`, dùng lại JWT cũ trả `401 SESSION_REVOKED`.
  - `ownerCanFetchClasses`: `EVENT_OWNER` lấy danh sách lớp qua `GET /api/v1/classes` -> `200 OK`.

### 2.3 Quản lý Sự kiện, Quy trình Duyệt & Lọc/Chi tiết (`EventIT` — 21/21 Pass, `28.68 s`)
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
- `studentCannotSeeDraftEvent` (`QE-66`): Sinh viên gọi trực tiếp `DRAFT` -> `404 NOT_FOUND`, không xuất hiện trong `GET /api/v1/events`.
- `updateRequiresMatchingVersion` (`QE-65`): `PUT` thiếu `version` -> `400 VALIDATION_FAILED` (`details.fields.version`); sai `version` -> `409 VERSION_CONFLICT`; đúng `version` -> `200`, `version` tăng lên `1`.
- `publishRecordsAuditTrail` (`QE-65`): Ghi `EVENT_PUBLISH_DENIED` (`PREVIEW_REQUIRED` / `PREVIEW_OUTDATED`), `EVENT_PUBLISH` (`SUCCESS`), publish lặp idempotent không sinh audit trùng.
- `ownerBCannotPreviewOrPublishOwnerAEvent` (`QE-65`): `owner02` gọi `/preview` hoặc `/publish` sự kiện của `owner01` -> `403 FORBIDDEN`, giữ nguyên `DRAFT`.
- `computedFieldsMatchServerTimeWindows` (`QE-66`): Kiểm chứng `remainingQuota`, `registrationOpen`, `attendanceOpen` theo mốc thời gian server của sự kiện còn hạn đăng ký, đang điểm danh và đã kết thúc.
- `studentOnlySeesPublishedEventsForTheirClass` (`QE-31`): Sinh viên chỉ thấy sự kiện `PUBLISHED` đúng lớp mình, ẩn `DRAFT` và sự kiện lớp khác.
- `eventDetailDisplaysAllRequiredFieldsAndServerSideRegistrationStatus` (`QE-31`): Kiểm chứng đầy đủ trường chi tiết và `myRegistrationStatus` do server tính (`null` -> `REGISTERED` sau khi đăng ký cho `sv001`, vẫn `null` với `sv002`).
- `filterEventsByActivityTypeAndKeyword` (`QE-31`): Lọc theo `activityType`, kết hợp `q` + `activityType`, trả về rỗng khi không khớp.
- `cancelledOrDraftEventsAreHiddenAndCannotBeRegistered` (`QE-31`): Sự kiện `CANCELLED` trả `404 NOT_FOUND` khi gọi chi tiết và bị ẩn khỏi danh sách của sinh viên.
- `registrationOpenCalculatedAsFalseWhenQuotaExhausted` (`QE-31`): Khi `quota = 1` đã bị chiếm hết, `remainingQuota = 0` và `registrationOpen = false`.
- `eventDetailExposesAudienceClassesAndEligibility` (`QE-37`): Chi tiết sự kiện trả `audienceClasses` (mã, tên lớp) và cờ `eligible` do server tính.

### 2.4 Tranh chấp đồng thời & Giữ chỗ nguyên tử (`RegistrationIT` — 12/12 Pass, `20.86 s`)
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

### 2.5 Thuật toán Dynamic QR & Điểm danh (`QrTokenServiceTest` 11 + `AttendanceIT` 24 = 35 tests)
- **`QrTokenServiceTest` (11/11 Unit Tests Pass — `0.285 s`):**
  - `issueTokenFormat`, `verifyCurrentEpochSuccess`, `verifyPreviousEpochWithinGraceSuccess` (`+2s`), `verifyPreviousEpochPastGraceFails` (`+4s` -> `QR_EXPIRED`), `verifyFutureTokenFails` (`QR_FROM_FUTURE`), `verifyTamperedSignatureFails` (`QR_BAD_SIGNATURE`), `verifyWrongEventFails` (`QR_WRONG_EVENT`), `verifyMalformedTokenFails` (`QR_MALFORMED` cho 5 biến thể), `forgedWrongEventTokenFailsSignatureValidation`, `tokenFingerprintIsShortSha256Prefix`, `shortSigningSecretIsRejected`.
- **`AttendanceIT` (24/24 Integration Tests Pass — `30.57 s`):**
  - `ownerCanGetQrTokenDuringAttendanceWindow`, `studentCannotGetQrToken` (`403`), `getQrTokenOutsideAttendanceWindowFails` (`409 ATTENDANCE_NOT_OPEN`).
  - `checkInWithValidQrTokenSuccess` (`200 ACCEPTED`), `checkInWithSameIdempotencyKeyReturnsCachedResponse`, `checkInWithoutIdempotencyKeyFails` (`400 VALIDATION_FAILED`), `checkInWithoutRegistrationFails` (`403 NOT_REGISTERED`), `checkInTwiceWithDifferentKeysFails` (`409 ALREADY_CHECKED_IN`).
  - `checkInWithExpiredTokenFails` (`422 QR_EXPIRED`), `checkInWithBadSignatureFails` (`422 QR_BAD_SIGNATURE`), `checkInAttemptLimitReachedAfter3Failures` (3 lần sai -> lần 4 dù token hợp lệ vẫn trả `409 ATTEMPT_LIMIT_REACHED`, `counted = 3`).
  - `rejectedAttemptReplayReturnsStoredResponse`, `blankTokenIsCountedAsMalformedQrAttempt` (`422 QR_MALFORMED`), `oversizedIdempotencyKeyFailsValidation` (`101` ký tự -> `400 VALIDATION_FAILED`).
  - `concurrentValidRequestsCreateOnlyOneAttendance` (10 luồng khác key -> 1 `200`, 9 `409 ALREADY_CHECKED_IN`), `concurrentRequestsWithSameKeyReturnIdenticalResponse` (2 luồng cùng key -> 2 `200` giống hệt, 1 `attendance_attempts`), `acceptedCheckInAuditDoesNotLeakToken`.
  - `failedAttemptReturnsRemainingAttemptsMetadata` (`QE-54`): Các lần thử sai trả `details.method = "DYNAMIC_QR"`, `attemptCount`, `maxAttempts = 3`, `remainingAttempts` (`2 -> 1 -> 0`), `canRetry` (`true -> true -> false`), lần 4 trả `409 ATTEMPT_LIMIT_REACHED` với `canRetry = false`.
  - `boundaryTest_ZeroAttempt_InitialState` (`QE-54`): Mốc 0 trước khi thử — `counted = 0`, chưa có bản ghi `ACCEPTED`.
  - `boundaryTest_FailOnce_ThenSuccessOnSecondAttempt` (`QE-54`): Lần 1 sai -> lần 2 đúng đạt `200 ACCEPTED`, `counted = 1`.
  - `boundaryTest_FailTwice_ThenSuccessOnThirdAttempt` (`QE-54`): Lần 1–2 sai -> lần 3 (lượt cuối) đúng đạt `200 ACCEPTED`, `counted = 2`.
  - `boundaryTest_FailThreeTimes_FourthAttemptBlocked` (`QE-54`): Sai cả 3 lần -> lần 4 dù token đúng vẫn bị chặn `409 ATTEMPT_LIMIT_REACHED` (`counted` giữ nguyên `3`).
  - `checkInWhenEventNotPublishedReturnsConflictWithoutAttemptDeduction` (`QE-54`): Sự kiện `DRAFT` trả `409 EVENT_NOT_PUBLISHED`, ghi attempt `counted = false`, không trả metadata trừ lượt.
  - `concurrentFailedAttemptsAtBoundaryOnlyIncrementsToThree` (`QE-54`): Đã sai 2 lần, bắn đồng thời 10 request token lỗi -> nhờ `pg_advisory_xact_lock`, đúng 1 request nhận `422 QR_MALFORMED` (`attemptCount = 3`), 9 request còn lại nhận `409 ATTEMPT_LIMIT_REACHED`, DB giữ chính xác `counted = 3`.

### 2.6 Lịch sử Sinh viên & Kiểm toán (`HistoryIT` 11 + `AuditIT` 3 = 14 tests)
- **`HistoryIT` (11/11 Pass — `17.19 s`):**
  - `unauthenticatedCallFails` (`401`), `ownerRoleCallFails` (`403`), `studentWithoutRegistrationsReturnsEmptyList` (`Page` với `content = []`, `totalElements = 0`), `registeredEventShowsNotCheckedIn`, `registeredAndAttendedEventShowsAccepted`, `studentDataIsolation`.
  - `paginationWorksCorrectly` (`QE-72`): Hỗ trợ `page`, `size`, `totalElements`, `totalPages`, `number`.
  - `filterByAttendanceStatus` (`QE-72`): Lọc `attendanceStatus=ACCEPTED` và `NOT_CHECKED_IN`.
  - `filterByActivityType` (`QE-72`): Lọc theo `activityType`.
  - `filterByUpcoming` (`QE-72`): Lọc `upcoming=true` (tương lai) và `upcoming=false` (quá khứ).
  - `filterByAcademicYearAndSemester` (`QE-72`): Lọc theo `academicYear` và `semester` (`1`, `2` hoặc cả năm học).
- **`AuditIT` (3/3 Pass — `2.176 s`):**
  - `recordAuditLogSuccess`, `databaseRejectsUpdateOnAuditLogs`, `databaseRejectsDeleteOnAuditLogs`.

---

## 3. Chi tiết Bộ Kiểm thử Frontend (11 Vitest + 7 Playwright E2E)

### 3.1 Unit & Component Tests (`vitest run` — 5 files, 11/11 Pass, `4.93s`)
- `src/test/time.test.ts` (**2 tests**): Định dạng `formatDateTime` theo múi giờ `Asia/Ho_Chi_Minh` và fallback `'—'` cho `'invalid-date'` / `null`.
- `src/test/StatusBadge.test.tsx` (**3 tests**): Kiểm tra render nhãn chữ tiếng Việt cho `PUBLISHED` (`Đã công bố`), `DRAFT` (`Bản nháp`), `REGISTERED` (`Đã đăng ký`). *(Chưa có unit test riêng cho `ACCEPTED`, `NOT_CHECKED_IN` hoặc kiểm tra màu sắc/icon).*
- `src/test/Button.test.tsx` (**2 tests**): Render nhãn nút và trạng thái `disabled` khi `loading`.
- `src/test/RegistrationSlot.test.tsx` (**2 tests**): Hiển thị tiến trình chỉ tiêu `10/50 chỗ` + nút `Đăng ký giữ chỗ` cho sinh viên, và trạng thái hết chỗ khi `remainingQuota = 0`.
- `src/test/HistoryPage.test.tsx` (**2 tests**): Trạng thái rỗng (`Bạn chưa có lịch sử sự kiện nào`) và danh sách đã đăng ký / đã điểm danh cùng KPI (`2`, `1`, `+10 đ`).

### 3.2 End-to-End Tests (`e2e/*.spec.ts` — 2 files, 7/7 Pass với Chrome cục bộ, `14.1s`)
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
> **Trạng thái Issue Bug & Retest (theo R1, R8, R10, R12):** Các lỗi dưới đây được đối chiếu trực tiếp trên mã nguồn BE (`origin/develop` `488d32a`) và FE (`develop` `b24ad2f`) tại thời điểm `21/11/2026`. Tuy nhiên **chưa tạo được Jira Bug issue thật** (máy kiểm thử chưa đăng nhập `twg` / Atlassian Teamwork Graph CLI để tạo Bug) và các lỗi còn tồn đọng **chưa thực hiện sửa/retest** (do phần lớn nằm ngoài phạm vi module A2 theo bảng phân công `AGENTS.md` §2 hoặc cần chốt nhánh sửa). Riêng `BUG-06` đã được xác nhận khắc phục trên `origin/develop` (`488d32a`).

| Mã tạm | Phạm vi / File vị trí | Mô tả Bug thật đã phát hiện | Jira Bug Issue | Trạng thái sửa & Kết quả chạy lại |
|---|---|---|---|---|
| **BUG-01** *(một phần phạm vi A2)* | FE `src/features/registration/index.tsx` (L57-58, L118-140) & `src/features/events/types.ts` (L76-80) | Khi `event` chưa tải (`undefined`), `remainingQuota` mặc định `0` nên UI hiển thị nhầm `"Sự kiện đã hết suất tham gia."`, và khi sự kiện ở trạng thái `DRAFT`/`CANCELLED` nhưng còn suất lại hiển thị `"Đã hết thời hạn đăng ký sự kiện."`. Đồng thời `types.ts` khai báo `RegistrationResponse.cached: boolean` lệch với BE (`alreadyRegistered: boolean`). | *(Chưa tạo — thiếu TWG/Jira CLI)* | Chưa sửa / Chưa retest |
| **BUG-02** *(trong phạm vi A2)* | BE `src/main/java/vn/edu/qnu/events/registration/RegistrationService.java` (L58-146) | Các nhánh từ chối đăng ký ghi `audit_logs.action = "REGISTRATION_CREATE"` thay vì `"REGISTRATION_REJECTED"` (lệch `sprint1-rescue-plan.md` §6.8), `NOT_IN_AUDIENCE` ghi `outcome = "DENIED"` khác các lỗi khác ghi `"REJECTED"`, và `requestId` truyền `null`. | *(Chưa tạo — thiếu TWG/Jira CLI)* | Chưa sửa / Chưa retest |
| **BUG-03** *(ngoài phạm vi A2 — `common/`)* | BE `src/main/java/vn/edu/qnu/events/common/GlobalExceptionHandler.java` | Thiếu `@ExceptionHandler(MethodArgumentTypeMismatchException.class)`: gọi API với UUID sai định dạng (vd `/api/v1/events/not-a-uuid/registrations` hoặc `/events/not-a-uuid`) rơi vào handler chung trả **HTTP 500 `INTERNAL_ERROR`** thay vì `400 VALIDATION_FAILED`. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-04** *(ngoài phạm vi A2 — `event/`)* | BE `src/main/java/vn/edu/qnu/events/event/EventUpsert.java` (L47-48) | `activityType` thiếu `@Size(max = 50)` khớp cột DB `varchar(50)` (`V1`), gửi chuỗi > 50 ký tự lọt qua `@Valid` và ném lỗi DB **HTTP 500 `INTERNAL_ERROR`**. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-05** *(ngoài phạm vi A2 — FE `events/`)* | FE `src/features/events/EventFormPage.tsx` (L47-51) | Khi chỉnh sửa sự kiện, FE cắt trực tiếp chuỗi ISO UTC (`data.startAt.slice(0, 16)`) đưa vào `<input type="datetime-local">` thay vì chuyển sang `Asia/Ho_Chi_Minh`, làm sai lệch `-7 giờ` mỗi lần lưu chỉnh sửa. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-06** *(ngoài phạm vi A2 — `event/`)* | BE `src/main/java/vn/edu/qnu/events/event/EventService.java` (L234-237, L261-270) | Trước đây trên nhánh `main`, `myRegistrationStatus` bị gán cứng `null`. Trên `origin/develop` (`488d32a`, PR #8 `fb127c6`), BE đã truy vấn `myRegistrationStatus` từ `RegistrationRepository` trong cả `getEventDetail` và `listEvents`. | *(Đã xử lý trên `develop` — không mở issue mới)* | **Đã fix trên `origin/develop`** · Retest Pass qua `EventIT` #17 (`eventDetailDisplaysAllRequiredFieldsAndServerSideRegistrationStatus`) |
| **BUG-07** *(ngoài phạm vi A2 — `student/` & FE `history/`)* | BE `HistoryService.java` (L45-50) & FE `src/features/history/index.tsx` (L72-75, L80) | `HistoryService` không lọc `r.status` nên sinh viên đăng ký lại sau `CANCELLED` bị trả 2 dòng trùng `eventId` và cộng sai KPI; `HistoryPage` thiếu nút **"Thử lại"** khi API lỗi theo `TEST PLAN SPRINT 1.md` §4.6. | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
| **BUG-08** *(lệch hợp đồng BE ↔ FE — `student/` & FE `history/`)* | BE `HistoryController.java` (L27) ↔ FE `src/api/events.ts` (L86-88) & `src/features/history/index.tsx` (L17-22, L76-79) | Sau PR #5 (`QE-72`) trên BE `origin/develop`, `GET /api/v1/me/history` trả `Page<HistoryItem>` (`{ content: [...], totalElements, ... }`), trong khi FE `develop` (`b24ad2f`) vẫn gọi và giả lập trong test dạng mảng `HistoryItem[]` (`historyItems?.length`, `historyItems?.filter(...)`). | *(Chưa tạo — cần duyệt R8 + TWG)* | Chưa sửa / Chưa retest |
