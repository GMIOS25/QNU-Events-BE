package vn.edu.qnu.events.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionJwtValidatorTest {

    private AuthSessionRepository sessionRepository;
    private AppUserRepository userRepository;
    private SessionJwtValidator validator;
    private final Instant now = Instant.parse("2026-11-24T12:00:00Z");

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AuthSessionRepository.class);
        userRepository = mock(AppUserRepository.class);
        validator = new SessionJwtValidator(
                sessionRepository,
                userRepository,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("JWT thiếu claim sub trả về invalid_token thay vì NPE (BUG-07)")
    void missingSubjectReturnsInvalidToken() {
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(new AuthSession(sessionId, ownerId, now.minusSeconds(60), now.plusSeconds(3600))));

        Jwt jwtWithoutSub = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .claim("sid", sessionId.toString())
                .build();

        var result = validator.validate(jwtWithoutSub);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> "invalid_token".equals(e.getErrorCode()));
    }

    @Test
    @DisplayName("JWT có sub không khớp userId sở hữu session -> invalid_token (BUG-07)")
    void mismatchedSessionOwnerReturnsInvalidToken() {
        UUID sessionId = UUID.randomUUID();
        UUID realOwnerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(new AuthSession(sessionId, realOwnerId, now.minusSeconds(60), now.plusSeconds(3600))));

        Jwt forgedSubJwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(otherUserId.toString())
                .claim("sid", sessionId.toString())
                .build();

        var result = validator.validate(forgedSubJwt);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> "invalid_token".equals(e.getErrorCode()));
    }

    @Test
    @DisplayName("JWT hợp lệ với session còn hạn và user ACTIVE -> success")
    void validSessionAndActiveUserSucceeds() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(new AuthSession(sessionId, userId, now.minusSeconds(60), now.plusSeconds(3600))));
        AppUser user = new AppUser(userId, "sv001", "$2a$12$hash", "SV 1", "4651050001", null, Set.of("STUDENT"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Jwt validJwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .build();

        var result = validator.validate(validJwt);
        assertThat(result.hasErrors()).isFalse();
    }
}
