package vn.edu.qnu.events.attendance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AttendanceAttemptService {

    private final AttendanceAttemptRepository attemptRepository;

    public AttendanceAttemptService(AttendanceAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                responseBody
        );
        return attemptRepository.save(attempt);
    }
}

