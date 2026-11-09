package vn.edu.qnu.events.auth;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        UserInfo user
) {}

