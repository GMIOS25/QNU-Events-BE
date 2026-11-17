package vn.edu.qnu.events.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final AuthSessionRepository authSessionRepository;
    private final AppUserRepository appUserRepository;
    private final Clock clock;

    public SessionJwtValidator(
            AuthSessionRepository authSessionRepository,
            AppUserRepository appUserRepository,
            Clock clock
    ) {
        this.authSessionRepository = authSessionRepository;
        this.appUserRepository = appUserRepository;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        if (sid == null || sid.isBlank()) {
            OAuth2Error error = new OAuth2Error("invalid_token", "Missing session ID in token", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sid);
        } catch (IllegalArgumentException e) {
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid session ID format", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        Optional<AuthSession> sessionOpt = authSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            OAuth2Error error = new OAuth2Error("session_revoked", "SESSION_REVOKED", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        AuthSession session = sessionOpt.get();
        if (session.getRevokedAt() != null) {
            OAuth2Error error = new OAuth2Error("session_revoked", "SESSION_REVOKED", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        if (session.getExpiresAt().isBefore(Instant.now(clock))) {
            OAuth2Error error = new OAuth2Error("session_expired", "SESSION_EXPIRED", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            OAuth2Error error = new OAuth2Error("invalid_token", "Invalid user ID format", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        Optional<AppUser> userOpt = appUserRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getStatus() != UserStatus.ACTIVE) {
            OAuth2Error error = new OAuth2Error("user_locked", "User is locked or deleted", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}

