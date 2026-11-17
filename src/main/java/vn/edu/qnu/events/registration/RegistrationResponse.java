package vn.edu.qnu.events.registration;

import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        String status,
        boolean alreadyRegistered
) {}

