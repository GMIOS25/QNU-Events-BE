package vn.edu.qnu.events.attendance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.common.ApiError;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;
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
    private final AuditService auditService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final EntityManager entityManager;

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
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
        this.qrTokenService = qrTokenService;
        this.auditService = auditService;
        this.clock = clock;
        this.entityManager = entityManager;
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

    @Transactional(rollbackFor = Exception.class)
    public CheckInResult checkInWithQr(
            UUID eventId,
            String idempotencyKey,
            CheckInRequest request,
            String traceId
    ) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        validateIdempotencyKey(idempotencyKey);

        UUID studentId = currentUser.userId();
        Optional<AttendanceAttempt> cached = findCachedAttempt(eventId, studentId, idempotencyKey);
        if (cached.isPresent()) {
            return cachedResult(cached.get());
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // Serialize only requests for the same event/student pair. This closes the
        // check-then-insert races for both idempotency and accepted attendance.
        acquireStudentEventLock(eventId, studentId);

        cached = findCachedAttempt(eventId, studentId, idempotencyKey);
        if (cached.isPresent()) {
            return cachedResult(cached.get());
        }

        entityManager.refresh(event);
        Instant now = Instant.now(clock);
        String rawToken = request != null ? request.token() : null;

        if (event.getStatus() != EventStatus.PUBLISHED
                || now.isBefore(event.getAttendanceOpenAt())
                || now.isAfter(event.getAttendanceCloseAt())) {
            return reject(
                    eventId, studentId, idempotencyKey, ErrorCode.ATTENDANCE_NOT_OPEN,
                    false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
            );
        }

        boolean hasRegistration = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                .map(registration -> registration.getStatus() == RegistrationStatus.REGISTERED)
                .orElse(false);
        if (!hasRegistration) {
            return reject(
                    eventId, studentId, idempotencyKey, ErrorCode.NOT_REGISTERED,
                    false, "DENIED", now, traceId, Map.of("eventId", eventId.toString())
            );
        }

        if (attendanceRepository.existsByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED")) {
            return reject(
                    eventId, studentId, idempotencyKey, ErrorCode.ALREADY_CHECKED_IN,
                    false, "REJECTED", now, traceId, Map.of("eventId", eventId.toString())
            );
        }

        long failedAttempts = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
        if (failedAttempts >= MAX_COUNTED_ATTEMPTS) {
            return reject(
                    eventId, studentId, idempotencyKey, ErrorCode.ATTEMPT_LIMIT_REACHED,
                    false, "DENIED", now, traceId,
                    Map.of("eventId", eventId.toString(), "failedAttempts", failedAttempts)
            );
        }

        QrTokenService.VerificationResult verification = qrTokenService.verify(
                rawToken, eventId, now, event.getQrRotationSeconds(), event.getQrGraceSeconds()
        );
        if (!verification.isValid()) {
            return reject(
                    eventId, studentId, idempotencyKey, verification.errorCode(),
                    true, "REJECTED", now, traceId,
                    Map.of(
                            "eventId", eventId.toString(),
                            "tokenHash", qrTokenService.fingerprint(rawToken)
                    )
            );
        }

        Attendance attendance = new Attendance(
                UUID.randomUUID(), eventId, studentId, "DYNAMIC_QR", "ACCEPTED", now
        );
        attendance.setCreatedAt(now);
        attendanceRepository.saveAndFlush(attendance);

        CheckInResponse response = new CheckInResponse("ACCEPTED", now);
        String responseBody = serialize(response);
        AttendanceAttempt attempt = attemptService.recordAttempt(
                eventId, studentId, idempotencyKey, "ACCEPTED", null, false,
                HttpStatus.OK.value(), responseBody
        );

        CheckInResult result = persistedResult(attempt);

        auditService.record(
                studentId,
                "STUDENT",
                "ATTENDANCE_QR_ACCEPT",
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

        return result;
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

    private CheckInResult cachedResult(AttendanceAttempt attempt) {
        HttpStatus status = HttpStatus.resolve(attempt.getHttpStatus());
        if (status == null) {
            throw new IllegalStateException("HTTP status đã lưu không hợp lệ: " + attempt.getHttpStatus());
        }
        return new CheckInResult(status, attempt.getResponseBody());
    }

    private CheckInResult reject(
            UUID eventId,
            UUID studentId,
            String idempotencyKey,
            ErrorCode errorCode,
            boolean counted,
            String auditOutcome,
            Instant now,
            String traceId,
            Map<String, Object> auditDetails
    ) {
        ApiError error = new ApiError(
                errorCode.name(),
                errorCode.getDefaultMessage(),
                Map.of(),
                traceId,
                now
        );
        String responseBody = serialize(error);

        AttendanceAttempt attempt = attemptService.recordAttempt(
                eventId,
                studentId,
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
        auditService.record(
                studentId,
                "STUDENT",
                "ATTENDANCE_QR_REJECT",
                "EVENT",
                eventId.toString(),
                auditOutcome,
                errorCode.name(),
                safeDetails,
                traceId
        );

        return result;
    }

    private CheckInResult persistedResult(AttendanceAttempt attempt) {
        attemptRepository.flush();
        entityManager.detach(attempt);
        AttendanceAttempt stored = attemptRepository.findById(attempt.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy attempt vừa lưu"));
        return cachedResult(stored);
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
