package vn.edu.qnu.events.attendance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AttendanceAttemptService {

    private final AttendanceAttemptRepository attemptRepository;
    private final Clock clock;

    public AttendanceAttemptService(AttendanceAttemptRepository attemptRepository, Clock clock) {
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    @Transactional
    public AttendanceAttempt recordAttempt(
            UUID eventId,
            UUID studentId,
            String method,
            String idempotencyKey,
            String outcome,
            String reasonCode,
            boolean counted,
            int httpStatus,
            String responseBody
    ) {
        String resolvedMethod = (method != null && !method.isBlank()) ? method : "DYNAMIC_QR";
        AttendanceAttempt attempt = new AttendanceAttempt(
                UUID.randomUUID(),
                eventId,
                studentId,
                resolvedMethod,
                idempotencyKey,
                outcome,
                reasonCode,
                counted,
                (short) httpStatus,
                responseBody,
                Instant.now(clock)
        );
        return attemptRepository.save(attempt);
    }

    @Transactional
    public AttendanceAttempt recordAttempt(
            UUID eventId,
            UUID studentId,
            String idempotencyKey,
            String outcome,
            String reasonCode,
            boolean counted,
            int httpStatus,
            String responseBody
    ) {
        return recordAttempt(
                eventId,
                studentId,
                "DYNAMIC_QR",
                idempotencyKey,
                outcome,
                reasonCode,
                counted,
                httpStatus,
                responseBody
        );
    }
}

