package vn.edu.qnu.events.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DUMMY_HASH = "$2a$12$e8x1KqIkn.wQnU4Bqg4K9.z9vA9oK5iQ8g5k7mZ4x1p2q3r4s5t6u";

    private final AppUserRepository appUserRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(
            AppUserRepository appUserRepository,
            AuthSessionRepository authSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<AppUser> userOpt = appUserRepository.findByLoginId(request.loginId());
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        AppUser user = userOpt.get();
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(8, ChronoUnit.HOURS);
        AuthSession session = new AuthSession(UUID.randomUUID(), user.getId(), now, expiresAt);
        authSessionRepository.save(session);

        String token = jwtService.createToken(
                user.getId(),
                session.getId(),
                user.getRoles().stream().toList(),
                user.getClassId(),
                expiresAt
        );

        return new LoginResponse(token, expiresAt, UserInfo.from(user));
    }

    @Transactional
    public void logout() {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (currentUser.sessionId() != null) {
            authSessionRepository.findById(currentUser.sessionId())
                    .ifPresent(session -> {
                        session.setRevokedAt(Instant.now(clock));
                        authSessionRepository.save(session);
                    });
        }
    }

    @Transactional(readOnly = true)
    public UserInfo getMe() {
        CurrentUser currentUser = CurrentUserProvider.require();
        AppUser user = appUserRepository.findById(currentUser.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        return UserInfo.from(user);
    }
}

