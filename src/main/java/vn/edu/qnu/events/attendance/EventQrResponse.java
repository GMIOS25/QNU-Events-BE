package vn.edu.qnu.events.attendance;

import java.time.Instant;

public record EventQrResponse(
        String token,
        long epoch,
        Instant validUntil,
        int rotationSeconds,
        Instant serverTime
) {}

