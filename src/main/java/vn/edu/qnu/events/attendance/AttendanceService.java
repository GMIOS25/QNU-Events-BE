package vn.edu.qnu.events.attendance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.common.ApiError;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.registration.RegistrationRepository;
import vn.edu.qnu.events.registration.RegistrationStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AttendanceService {

    private static final int MAX_COUNTED_ATTEMPTS = 3;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceAttemptRepository attemptRepository;
    private final AttendanceAttemptService attemptService;
    private final QrTokenService qrTokenService;
    private final GpsVerificationService gpsVerificationService;
    private final AuditService auditService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public AttendanceService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            AttendanceRepository attendanceRepository,
            AttendanceAttemptRepository attemptRepository,
            AttendanceAttemptService attemptService,
            QrTokenService qrTokenService,
            AuditService auditService,
            Clock clock,
            EntityManager entityManager
    ) {
        this(
                eventRepository,
                registrationRepository,
                attendanceRepository,
                attemptRepository,
                attemptService,
                qrTokenService,
                new GpsVerificationService(),
                auditService,
                clock,
                entityManager,
                null
        );
    }

    public AttendanceService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            AttendanceRepository attendanceRepository,
            AttendanceAttemptRepository attemptRepository,
            AttendanceAttemptService attemptService,
            QrTokenService qrTokenService,
            GpsVerificationService gpsVerificationService,
            AuditService auditService,
            Clock clock,
            EntityManager entityManager
    ) {
        this(
                eventRepository,
                registrationRepository,
                attendanceRepository,
                attemptRepository,
                attemptService,
                qrTokenService,
                gpsVerificationService,
                auditService,
                clock,
                entityManager,
                null
        );
    }

    @Autowired
    public AttendanceService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            AttendanceRepository attendanceRepository,
            AttendanceAttemptRepository attemptRepository,
            AttendanceAttemptService attemptService,
            QrTokenService qrTokenService,
            GpsVerificationService gpsVerificationService,
            AuditService auditService,
            Clock clock,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
        this.qrTokenService = qrTokenService;
        this.gpsVerificationService = gpsVerificationService != null
                ? gpsVerificationService
                : new GpsVerificationService();
        this.auditService = auditService;
        this.clock = clock;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionManager != null
                ? new TransactionTemplate(transactionManager)
                : null;
    }

    @Transactional(readOnly = true)
    public EventQrResponse getQrToken(UUID eventId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("EVENT_OWNER")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!event.getOwnerId().equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.EVENT_NOT_PUBLISHED);
        }

        if (event.getAttendanceMethod() != AttendanceMethod.DYNAMIC_QR) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Sự kiện không sử dụng phương thức điểm danh QR động.");
        }

        Instant now = Instant.now(clock);
        if (now.isBefore(event.getAttendanceOpenAt()) || now.isAfter(event.getAttendanceCloseAt())) {
            throw new ApiException(ErrorCode.ATTENDANCE_NOT_OPEN);
        }

        int rotationSeconds = event.getQrRotationSeconds();
        long epoch = now.getEpochSecond() / rotationSeconds;
        Instant epochEnd = Instant.ofEpochSecond((epoch + 1) * rotationSeconds);
        Instant validUntil = epochEnd.isBefore(event.getAttendanceCloseAt())
                ? epochEnd
                : event.getAttendanceCloseAt();
        String token = qrTokenService.issue(eventId, now, rotationSeconds);

        return new EventQrResponse(token, epoch, validUntil, rotationSeconds, now);
    }

    @Transactional(readOnly = true)
    public MyAttendanceResponse getMyAttendance(UUID eventId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (currentUser.classId() == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        eventRepository.findPublishedByIdAndClass(eventId, currentUser.classId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        UUID studentId = currentUser.userId();
        long attemptCount = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
        long remainingAttempts = Math.max(0, MAX_COUNTED_ATTEMPTS - attemptCount);

        Optional<Attendance> accepted = attendanceRepository
                .findByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED");
        if (accepted.isPresent()) {
            return new MyAttendanceResponse(
                    "ACCEPTED",
                    accepted.get().getRecordedAt(),
                    attemptCount,
                    MAX_COUNTED_ATTEMPTS,
                    remainingAttempts,
                    false
            );
        }

        return new MyAttendanceResponse(
                "NOT_CHECKED_IN",
                null,
                attemptCount,
                MAX_COUNTED_ATTEMPTS,
                remainingAttempts,
                remainingAttempts > 0
        );
    }

    public CheckInResult checkInWithQr(
            UUID eventId,
            String idempotencyKey,
            CheckInRequest request,
            String traceId
    ) {
        return processCheckIn(eventId, "DYNAMIC_QR", idempotencyKey, request, traceId);
    }

    public CheckInResult checkInWithGps(
            UUID eventId,
            String idempotencyKey,
            GpsCheckInRequest request,
            String traceId
    ) {
        String method = "GPS";
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        validateIdempotencyKey(idempotencyKey);

        try {
            gpsVerificationService.validateStudentRequest(request);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ex.getMessage());
        }

        UUID studentId = currentUser.userId();

        // Chạy toàn bộ thao tác DB điểm danh trong TransactionTemplate riêng để giải phóng
        // connection #1 TRƯỚC khi gọi auditService.record (REQUIRES_NEW), tránh nghẽn
        // HikariCP pool khi đông sinh viên điểm danh đồng thời (A-QE8-01).
        CheckInExecution execution = executeInTransaction(() -> {
            Optional<AttendanceAttempt> cached = findCachedAttempt(eventId, studentId, idempotencyKey);
            if (cached.isPresent()) {
                return CheckInExecution.of(cachedResult(cached.get(), method));
            }

            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

            acquireStudentEventLock(eventId, studentId);

            cached = findCachedAttempt(eventId, studentId, idempotencyKey);
            if (cached.isPresent()) {
                return CheckInExecution.of(cachedResult(cached.get(), method));
            }

            entityManager.refresh(event);
            Instant now = Instant.now(clock);

            if (event.getStatus() != EventStatus.PUBLISHED) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.EVENT_NOT_PUBLISHED,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (event.getAttendanceMethod() != AttendanceMethod.GPS) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.VALIDATION_FAILED,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            try {
                gpsVerificationService.validateEventConfiguration(
                        event.getGpsLat(), event.getGpsLng(), event.getAllowedRadiusMeters()
                );
            } catch (IllegalStateException ex) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.GPS_NOT_CONFIGURED,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (now.isBefore(event.getAttendanceOpenAt()) || now.isAfter(event.getAttendanceCloseAt())) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ATTENDANCE_NOT_OPEN,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            boolean hasRegistration = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                    .map(registration -> registration.getStatus() == RegistrationStatus.REGISTERED)
                    .orElse(false);
            if (!hasRegistration) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.NOT_REGISTERED,
                        false, "DENIED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (attendanceRepository.existsByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED")) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ALREADY_CHECKED_IN,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            long failedAttempts = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
            if (failedAttempts >= MAX_COUNTED_ATTEMPTS) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ATTEMPT_LIMIT_REACHED,
                        false, "DENIED", now, traceId,
                        Map.of("eventId", eventId.toString(), "failedAttempts", failedAttempts)
                );
            }

            GpsVerificationService.VerificationResult verification = gpsVerificationService.verify(
                    request,
                    event.getGpsLat(),
                    event.getGpsLng(),
                    event.getAllowedRadiusMeters()
            );

            if (!verification.isWithinRadius()) {
                Map<String, Object> extraDetails = Map.of(
                        "distanceMeters", verification.distanceMeters(),
                        "allowedRadiusMeters", verification.allowedRadiusMeters()
                );
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("eventId", eventId.toString());
                auditDetails.put("distanceMeters", verification.distanceMeters());
                auditDetails.put("allowedRadiusMeters", verification.allowedRadiusMeters());
                auditDetails.put("accuracyMeters", request.accuracyMeters());
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.GPS_OUT_OF_RANGE,
                        true, "REJECTED", now, traceId, auditDetails, extraDetails
                );
            }

            Attendance attendance = new Attendance(
                    UUID.randomUUID(), eventId, studentId, method, "ACCEPTED", now
            );
            attendance.setGpsLat(request.latitude());
            attendance.setGpsLng(request.longitude());
            attendance.setGpsAccuracyMeters(request.accuracyMeters());
            attendance.setGpsDistanceMeters(verification.distanceMeters());
            attendance.setSuspiciousFlag(verification.isSuspicious());
            attendance.setSuspiciousReason(verification.suspiciousReason());
            attendance.setCreatedAt(now);
            attendanceRepository.saveAndFlush(attendance);

            GpsCheckInResponse response = new GpsCheckInResponse(
                    "ACCEPTED",
                    now,
                    verification.distanceMeters(),
                    verification.allowedRadiusMeters()
            );
            String responseBody = serialize(response);
            AttendanceAttempt attempt = attemptService.recordAttempt(
                    eventId, studentId, method, idempotencyKey, "ACCEPTED", null, false,
                    HttpStatus.OK.value(), responseBody
            );

            CheckInResult result = persistedResult(attempt);

            Map<String, Object> acceptAuditDetails = new HashMap<>();
            acceptAuditDetails.put("eventId", eventId.toString());
            acceptAuditDetails.put("distanceMeters", verification.distanceMeters());
            acceptAuditDetails.put("allowedRadiusMeters", verification.allowedRadiusMeters());
            acceptAuditDetails.put("accuracyMeters", request.accuracyMeters());
            acceptAuditDetails.put("suspicious", verification.isSuspicious());
            if (verification.suspiciousReason() != null) {
                acceptAuditDetails.put("suspiciousReason", verification.suspiciousReason());
            }

            AuditEntry auditEntry = new AuditEntry(
                    studentId,
                    "STUDENT",
                    "ATTENDANCE_GPS_ACCEPT",
                    "ATTENDANCE",
                    attendance.getId().toString(),
                    "SUCCESS",
                    null,
                    acceptAuditDetails,
                    traceId
            );

            return CheckInExecution.of(result, auditEntry);
        });

        recordAuditOutsideTransaction(execution.audit());
        return execution.result();
    }

    private CheckInResult processCheckIn(
            UUID eventId,
            String method,
            String idempotencyKey,
            CheckInRequest request,
            String traceId
    ) {
        if (!"DYNAMIC_QR".equals(method)) {
            throw new IllegalArgumentException("Phương thức điểm danh không được hỗ trợ: " + method);
        }

        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        validateIdempotencyKey(idempotencyKey);

        UUID studentId = currentUser.userId();

        CheckInExecution execution = executeInTransaction(() -> {
            Optional<AttendanceAttempt> cached = findCachedAttempt(eventId, studentId, idempotencyKey);
            if (cached.isPresent()) {
                return CheckInExecution.of(cachedResult(cached.get(), method));
            }

            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

            // Serialize only requests for the same event/student pair. This closes the
            // check-then-insert races for both idempotency and accepted attendance.
            acquireStudentEventLock(eventId, studentId);

            cached = findCachedAttempt(eventId, studentId, idempotencyKey);
            if (cached.isPresent()) {
                return CheckInExecution.of(cachedResult(cached.get(), method));
            }

            entityManager.refresh(event);
            Instant now = Instant.now(clock);
            String rawToken = request != null ? request.token() : null;

            if (event.getStatus() != EventStatus.PUBLISHED) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.EVENT_NOT_PUBLISHED,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (event.getAttendanceMethod() != AttendanceMethod.DYNAMIC_QR) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.VALIDATION_FAILED,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (now.isBefore(event.getAttendanceOpenAt()) || now.isAfter(event.getAttendanceCloseAt())) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ATTENDANCE_NOT_OPEN,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            boolean hasRegistration = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                    .map(registration -> registration.getStatus() == RegistrationStatus.REGISTERED)
                    .orElse(false);
            if (!hasRegistration) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.NOT_REGISTERED,
                        false, "DENIED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            if (attendanceRepository.existsByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED")) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ALREADY_CHECKED_IN,
                        false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
                );
            }

            long failedAttempts = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
            if (failedAttempts >= MAX_COUNTED_ATTEMPTS) {
                return reject(
                        eventId, studentId, method, idempotencyKey, ErrorCode.ATTEMPT_LIMIT_REACHED,
                        false, "DENIED", now, traceId,
                        Map.of("eventId", eventId.toString(), "failedAttempts", failedAttempts)
                );
            }

            QrTokenService.VerificationResult verification = qrTokenService.verify(
                    rawToken, eventId, now, event.getQrRotationSeconds(), event.getQrGraceSeconds()
            );
            if (!verification.isValid()) {
                return reject(
                        eventId, studentId, method, idempotencyKey, verification.errorCode(),
                        true, "REJECTED", now, traceId,
                        Map.of(
                                "eventId", eventId.toString(),
                                "tokenHash", qrTokenService.fingerprint(rawToken)
                        )
                );
            }

            Attendance attendance = new Attendance(
                    UUID.randomUUID(), eventId, studentId, method, "ACCEPTED", now
            );
            attendance.setCreatedAt(now);
            attendanceRepository.saveAndFlush(attendance);

            CheckInResponse response = new CheckInResponse("ACCEPTED", now);
            String responseBody = serialize(response);
            AttendanceAttempt attempt = attemptService.recordAttempt(
                    eventId, studentId, method, idempotencyKey, "ACCEPTED", null, false,
                    HttpStatus.OK.value(), responseBody
            );

            CheckInResult result = persistedResult(attempt);

            String auditAction = "DYNAMIC_QR".equals(method) ? "ATTENDANCE_QR_ACCEPT" : "ATTENDANCE_" + method + "_ACCEPT";
            AuditEntry auditEntry = new AuditEntry(
                    studentId,
                    "STUDENT",
                    auditAction,
                    "ATTENDANCE",
                    attendance.getId().toString(),
                    "SUCCESS",
                    null,
                    Map.of(
                            "eventId", eventId.toString(),
                            "epoch", verification.epoch(),
                            "tokenHash", qrTokenService.fingerprint(rawToken)
                    ),
                    traceId
            );

            return CheckInExecution.of(result, auditEntry);
        });

        recordAuditOutsideTransaction(execution.audit());
        return execution.result();
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Header Idempotency-Key là bắt buộc.");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Header Idempotency-Key không được vượt quá 100 ký tự."
            );
        }
    }

    private Optional<AttendanceAttempt> findCachedAttempt(
            UUID eventId,
            UUID studentId,
            String idempotencyKey
    ) {
        return attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(
                eventId, studentId, idempotencyKey
        );
    }

    private CheckInResult cachedResult(AttendanceAttempt attempt, String expectedMethod) {
        if (!expectedMethod.equals(attempt.getMethod())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Idempotency-Key đã được sử dụng cho phương thức điểm danh khác."
            );
        }
        HttpStatus status = HttpStatus.resolve(attempt.getHttpStatus());
        if (status == null) {
            throw new IllegalStateException("HTTP status đã lưu không hợp lệ: " + attempt.getHttpStatus());
        }
        return new CheckInResult(status, attempt.getResponseBody());
    }

    private CheckInExecution reject(
            UUID eventId,
            UUID studentId,
            String method,
            String idempotencyKey,
            ErrorCode errorCode,
            boolean counted,
            String auditOutcome,
            Instant now,
            String traceId,
            Map<String, Object> auditDetails
    ) {
        return reject(
                eventId, studentId, method, idempotencyKey, errorCode,
                counted, auditOutcome, now, traceId, auditDetails, Map.of()
        );
    }

    private CheckInExecution reject(
            UUID eventId,
            UUID studentId,
            String method,
            String idempotencyKey,
            ErrorCode errorCode,
            boolean counted,
            String auditOutcome,
            Instant now,
            String traceId,
            Map<String, Object> auditDetails,
            Map<String, Object> extraErrorDetails
    ) {
        long currentFailed = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
        long totalFailedAfterThis = counted ? currentFailed + 1 : currentFailed;
        long remainingAttempts = Math.max(0, MAX_COUNTED_ATTEMPTS - totalFailedAfterThis);
        boolean canRetry = remainingAttempts > 0
                && errorCode != ErrorCode.ATTEMPT_LIMIT_REACHED
                && errorCode != ErrorCode.ALREADY_CHECKED_IN;

        String resolvedMethod = (method != null && !method.isBlank()) ? method : "DYNAMIC_QR";

        Map<String, Object> errorDetails = new HashMap<>();
        if (counted || errorCode == ErrorCode.ATTEMPT_LIMIT_REACHED) {
            errorDetails.put("method", resolvedMethod);
            errorDetails.put("attemptCount", totalFailedAfterThis);
            errorDetails.put("maxAttempts", MAX_COUNTED_ATTEMPTS);
            errorDetails.put("remainingAttempts", remainingAttempts);
            errorDetails.put("canRetry", canRetry);
        }
        if (extraErrorDetails != null && !extraErrorDetails.isEmpty()) {
            errorDetails.putAll(extraErrorDetails);
        }

        ApiError error = new ApiError(
                errorCode.name(),
                errorCode.getDefaultMessage(),
                errorDetails,
                traceId,
                now
        );
        String responseBody = serialize(error);

        AttendanceAttempt attempt = attemptService.recordAttempt(
                eventId,
                studentId,
                resolvedMethod,
                idempotencyKey,
                "REJECTED",
                errorCode.name(),
                counted,
                errorCode.getHttpStatus().value(),
                responseBody
        );

        CheckInResult result = persistedResult(attempt);

        Map<String, Object> safeDetails = new HashMap<>(auditDetails);
        safeDetails.put("reasonCode", errorCode.name());
        String auditAction = "DYNAMIC_QR".equals(resolvedMethod) ? "ATTENDANCE_QR_REJECT" : "ATTENDANCE_" + resolvedMethod + "_REJECT";
        AuditEntry auditEntry = new AuditEntry(
                studentId,
                "STUDENT",
                auditAction,
                "EVENT",
                eventId.toString(),
                auditOutcome,
                errorCode.name(),
                safeDetails,
                traceId
        );

        return CheckInExecution.of(result, auditEntry);
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        if (transactionTemplate == null) {
            return action.get();
        }
        T value = transactionTemplate.execute(status -> action.get());
        if (value == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
        return value;
    }

    private void recordAuditOutsideTransaction(AuditEntry audit) {
        if (audit == null) {
            return;
        }
        auditService.record(
                audit.actorId(),
                audit.actorRole(),
                audit.action(),
                audit.targetType(),
                audit.targetId(),
                audit.outcome(),
                audit.reasonCode(),
                audit.details(),
                audit.requestId()
        );
    }

    private record AuditEntry(
            UUID actorId,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String outcome,
            String reasonCode,
            Map<String, Object> details,
            String requestId
    ) {}

    private record CheckInExecution(CheckInResult result, AuditEntry audit) {
        static CheckInExecution of(CheckInResult result) {
            return new CheckInExecution(result, null);
        }

        static CheckInExecution of(CheckInResult result, AuditEntry audit) {
            return new CheckInExecution(result, audit);
        }
    }

    private CheckInResult persistedResult(AttendanceAttempt attempt) {
        attemptRepository.flush();
        entityManager.detach(attempt);
        AttendanceAttempt stored = attemptRepository.findById(attempt.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy attempt vừa lưu"));
        return cachedResult(stored, stored.getMethod());
    }

    private void acquireStudentEventLock(UUID eventId, UUID studentId) {
        entityManager.createNativeQuery("""
                        SELECT pg_advisory_xact_lock(
                            hashtextextended(CAST(?1 AS text) || ':' || CAST(?2 AS text), 0)
                        )
                        """)
                .setParameter(1, eventId.toString())
                .setParameter(2, studentId.toString())
                .getSingleResult();
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể lưu response điểm danh", e);
        }
    }
}
