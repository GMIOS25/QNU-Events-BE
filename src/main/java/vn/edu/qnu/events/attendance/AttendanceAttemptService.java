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
            String idempotencyKey,
            String outcome,
            String reasonCode,
            boolean counted,
            int httpStatus,
            String responseBody
    ) {
        AttendanceAttempt attempt = new AttendanceAttempt(
                UUID.randomUUID(),
                eventId,
                studentId,
                "DYNAMIC_QR",
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
}

