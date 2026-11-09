package vn.edu.qnu.events.attendance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.events.audit.AuditService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceAttemptRepository attemptRepository;
    private final AttendanceAttemptService attemptService;
    private final QrTokenService qrTokenService;
    private final AuditService auditService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AttendanceService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            AttendanceRepository attendanceRepository,
            AttendanceAttemptRepository attemptRepository,
            AttendanceAttemptService attemptService,
            QrTokenService qrTokenService,
            AuditService auditService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
        this.qrTokenService = qrTokenService;
        this.auditService = auditService;
        this.clock = clock;
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
        Instant validUntil = Instant.ofEpochSecond((epoch + 1) * rotationSeconds);
        String token = qrTokenService.issue(eventId, now, rotationSeconds);

        return new EventQrResponse(token, epoch, validUntil, rotationSeconds, now);
    }

    @Transactional(rollbackFor = Exception.class)
    public CheckInResponse checkInWithQr(UUID eventId, String idempotencyKey, CheckInRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Header Idempotency-Key là bắt buộc.");
        }

        UUID studentId = currentUser.userId();
        Instant now = Instant.now(clock);

        // 1. Kiểm tra Idempotency
        Optional<AttendanceAttempt> cachedAttempt = attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(
                eventId, studentId, idempotencyKey
        );
        if (cachedAttempt.isPresent()) {
            AttendanceAttempt attempt = cachedAttempt.get();
            if ("ACCEPTED".equals(attempt.getOutcome())) {
                try {
                    return objectMapper.readValue(attempt.getResponseBody(), CheckInResponse.class);
                } catch (JsonProcessingException e) {
                    return new CheckInResponse("ACCEPTED", attempt.getCreatedAt());
                }
            } else {
                ErrorCode err = ErrorCode.valueOf(attempt.getReasonCode());
                throw new ApiException(err);
            }
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 2. Cửa sổ điểm danh và trạng thái PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED || now.isBefore(event.getAttendanceOpenAt()) || now.isAfter(event.getAttendanceCloseAt())) {
            attemptService.recordAttempt(
                    eventId, studentId, idempotencyKey, "REJECTED", "ATTENDANCE_NOT_OPEN", false, 409,
                    "{\"code\":\"ATTENDANCE_NOT_OPEN\"}"
            );
            auditService.record(
                    studentId, "STUDENT", "ATTENDANCE_DENIED", "EVENT", eventId.toString(),
                    "REJECTED", "ATTENDANCE_NOT_OPEN", Map.of(), null
            );
            throw new ApiException(ErrorCode.ATTENDANCE_NOT_OPEN);
        }

        // 3. Đã đăng ký (REGISTERED)
        boolean hasRegistration = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                .map(r -> r.getStatus() == RegistrationStatus.REGISTERED)
                .orElse(false);

        if (!hasRegistration) {
            attemptService.recordAttempt(
                    eventId, studentId, idempotencyKey, "REJECTED", "NOT_REGISTERED", false, 403,
                    "{\"code\":\"NOT_REGISTERED\"}"
            );
            auditService.record(
                    studentId, "STUDENT", "ATTENDANCE_DENIED", "EVENT", eventId.toString(),
                    "DENIED", "NOT_REGISTERED", Map.of(), null
            );
            throw new ApiException(ErrorCode.NOT_REGISTERED);
        }

        // 4. Đã điểm danh trước đó
        if (attendanceRepository.existsByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED")) {
            attemptService.recordAttempt(
                    eventId, studentId, idempotencyKey, "REJECTED", "ALREADY_CHECKED_IN", false, 409,
                    "{\"code\":\"ALREADY_CHECKED_IN\"}"
            );
            auditService.record(
                    studentId, "STUDENT", "ATTENDANCE_DENIED", "EVENT", eventId.toString(),
                    "REJECTED", "ALREADY_CHECKED_IN", Map.of(), null
            );
            throw new ApiException(ErrorCode.ALREADY_CHECKED_IN);
        }

        // 5. Kiểm tra giới hạn 3 lần thử sai (counted = true)
        long failedAttempts = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId);
        if (failedAttempts >= 3) {
            attemptService.recordAttempt(
                    eventId, studentId, idempotencyKey, "REJECTED", "ATTEMPT_LIMIT_REACHED", true, 409,
                    "{\"code\":\"ATTEMPT_LIMIT_REACHED\"}"
            );
            auditService.record(
                    studentId, "STUDENT", "ATTENDANCE_DENIED", "EVENT", eventId.toString(),
                    "DENIED", "ATTEMPT_LIMIT_REACHED", Map.of("failedAttempts", String.valueOf(failedAttempts)), null
            );
            throw new ApiException(ErrorCode.ATTEMPT_LIMIT_REACHED);
        }

        // 6. Xác thực mã QR
        String rawToken = request != null ? request.token() : null;
        var verifyResult = qrTokenService.verify(
                rawToken, eventId, now, event.getQrRotationSeconds(), event.getQrGraceSeconds()
        );

        String maskedToken = rawToken != null && rawToken.length() > 8
                ? rawToken.substring(0, 8) + "***"
                : "***";

        if (!verifyResult.isValid()) {
            ErrorCode err = verifyResult.errorCode();
            attemptService.recordAttempt(
                    eventId, studentId, idempotencyKey, "REJECTED", err.name(), true,
                    err.getHttpStatus().value(), "{\"code\":\"" + err.name() + "\"}"
            );
            auditService.record(
                    studentId, "STUDENT", "ATTENDANCE_DENIED", "EVENT", eventId.toString(),
                    "REJECTED", err.name(), Map.of("tokenMasked", maskedToken), null
            );
            throw new ApiException(err);
        }

        // 7. Điểm danh thành công
        Attendance attendance = new Attendance(
                UUID.randomUUID(), eventId, studentId, "DYNAMIC_QR", "ACCEPTED", now
        );
        attendanceRepository.save(attendance);

        CheckInResponse response = new CheckInResponse("ACCEPTED", now);
        String responseBody;
        try {
            responseBody = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            responseBody = "{\"status\":\"ACCEPTED\"}";
        }

        attemptService.recordAttempt(
                eventId, studentId, idempotencyKey, "ACCEPTED", null, false, 200, responseBody
        );

        auditService.record(
                studentId, "STUDENT", "ATTENDANCE_CHECKIN", "ATTENDANCE", attendance.getId().toString(),
                "SUCCESS", null, Map.of("eventId", eventId.toString(), "epoch", String.valueOf(verifyResult.epoch())), null
        );

        return response;
    }
}
