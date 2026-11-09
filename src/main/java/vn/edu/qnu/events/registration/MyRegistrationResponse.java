package vn.edu.qnu.events.registration;

import java.time.Instant;
import java.util.UUID;

public record MyRegistrationResponse(
        UUID id,
        String status,
        Instant registeredAt
) {}

