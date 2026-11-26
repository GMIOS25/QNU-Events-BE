package vn.edu.qnu.events.attendance;

import java.time.Instant;

public record MyAttendanceResponse(
        String status,
        Instant acceptedAt,
        long attemptCount,
        int maxAttempts,
        long remainingAttempts,
        boolean canRetry
) {}
