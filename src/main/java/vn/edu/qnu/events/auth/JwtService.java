package vn.edu.qnu.events.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public JwtService(
            @Value("${app.security.jwt-secret:}") String jwtSecret,
            Clock clock
    ) {
        this.secret = (jwtSecret != null && jwtSecret.length() >= 32)
                ? jwtSecret
                : "qnu-events-dev-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!";
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

