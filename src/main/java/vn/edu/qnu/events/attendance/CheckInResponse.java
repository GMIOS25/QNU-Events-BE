package vn.edu.qnu.events.attendance;

import java.time.Instant;

public record CheckInResponse(
        String status,
        Instant acceptedAt
) {}

