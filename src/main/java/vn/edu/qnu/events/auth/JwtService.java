package vn.edu.qnu.events.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import vn.edu.qnu.events.config.SecurityConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final String secret;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${app.security.jwt-secret:}") String jwtSecret,
            Environment environment,
            Clock clock
    ) {
        this.secret = SecurityConfiguration.resolveJwtSecret(jwtSecret, environment);
        this.clock = clock;
    }

    public JwtService(String jwtSecret, Clock clock) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret phải có ít nhất 32 byte.");
        }
        this.secret = jwtSecret;
        this.clock = clock;
    }

    public String createToken(UUID userId, UUID sessionId, List<String> roles, UUID classId, Instant expiresAt) {
        try {
            Instant now = Instant.now(clock);
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("sid", sessionId.toString())
                    .claim("roles", roles)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt));

            if (classId != null) {
                builder.claim("classId", classId.toString());
            }

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    builder.build()
            );

            MACSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo JWT token", e);
        }
    }
}

