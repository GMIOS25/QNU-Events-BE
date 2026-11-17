package vn.edu.qnu.events.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestAuth {

    public static final String DEFAULT_TEST_SECRET =
            "qnu-events-dev-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!";

    private TestAuth() {
    }

    public static String createToken(
            UUID userId,
            UUID sessionId,
            List<String> roles,
            UUID classId,
            String secret,
            Duration validity
    ) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(validity != null ? validity : Duration.ofHours(8));

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt));

            if (sessionId != null) {
                builder.claim("sid", sessionId.toString());
            }
            if (roles != null) {
                builder.claim("roles", roles);
            }
            if (classId != null) {
                builder.claim("classId", classId.toString());
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test JWT token", e);
        }
    }

    public static String createToken(UUID userId, UUID sessionId, List<String> roles, UUID classId) {
        return createToken(userId, sessionId, roles, classId, DEFAULT_TEST_SECRET, Duration.ofHours(8));
    }

    public static String createStudentToken(UUID userId, UUID classId) {
        return createToken(userId, UUID.randomUUID(), List.of("STUDENT"), classId);
    }

    public static String createOwnerToken(UUID userId) {
        return createToken(userId, UUID.randomUUID(), List.of("EVENT_OWNER"), null);
    }
}

