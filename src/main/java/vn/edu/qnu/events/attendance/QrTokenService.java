package vn.edu.qnu.events.attendance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.qnu.events.common.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrTokenService {

    private final byte[] secretBytes;

    public QrTokenService(
            @Value("${app.qr.signing-secret:}") String signingSecret
    ) {
        String secret = (signingSecret != null && signingSecret.length() >= 32)
                ? signingSecret
                : "qnu-events-dev-qr-signing-secret-key-must-be-at-least-256-bits-long!";
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(UUID eventId, Instant now, int rotationSeconds) {
        long epoch = now.getEpochSecond() / rotationSeconds;
        String payload = "qr1." + eventId + "." + epoch;
        String signature = sign(payload);
        return payload + "." + signature;
    }

    public VerificationResult verify(String token, UUID expectedEventId, Instant now, int rotationSeconds, int graceSeconds) {
        if (token == null || token.isBlank()) {
            return VerificationResult.invalid(ErrorCode.QR_MALFORMED);
        }

        String[] parts = token.split("\\.");
        if (parts.length != 4 || !"qr1".equals(parts[0])) {
            return VerificationResult.invalid(ErrorCode.QR_MALFORMED);
        }

        UUID tokenEventId;
        try {
            tokenEventId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return VerificationResult.invalid(ErrorCode.QR_MALFORMED);
        }

        if (!tokenEventId.equals(expectedEventId)) {
            return VerificationResult.invalid(ErrorCode.QR_WRONG_EVENT);
        }

        long epoch;
        try {
            epoch = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return VerificationResult.invalid(ErrorCode.QR_MALFORMED);
        }

        String payload = "qr1." + tokenEventId + "." + epoch;
        String expectedSignature = sign(payload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[3].getBytes(StandardCharsets.UTF_8))) {
            return VerificationResult.invalid(ErrorCode.QR_BAD_SIGNATURE);
        }

        long currentEpoch = now.getEpochSecond() / rotationSeconds;
        if (epoch > currentEpoch) {
            return VerificationResult.invalid(ErrorCode.QR_FROM_FUTURE);
        }

        if (epoch == currentEpoch) {
            return VerificationResult.valid(epoch);
        }

        if (epoch == currentEpoch - 1) {
            long secondsIntoCurrentEpoch = now.getEpochSecond() - (currentEpoch * rotationSeconds);
            if (secondsIntoCurrentEpoch <= graceSeconds) {
                return VerificationResult.valid(epoch);
            }
            return VerificationResult.invalid(ErrorCode.QR_EXPIRED);
        }

        return VerificationResult.invalid(ErrorCode.QR_EXPIRED);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] sigBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA256", e);
        }
    }

    public record VerificationResult(boolean isValid, ErrorCode errorCode, Long epoch) {
        public static VerificationResult valid(long epoch) {
            return new VerificationResult(true, null, epoch);
        }

        public static VerificationResult invalid(ErrorCode errorCode) {
            return new VerificationResult(false, errorCode, null);
        }
    }
}

