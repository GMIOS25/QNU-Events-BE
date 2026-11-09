package vn.edu.qnu.events.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.qnu.events.common.ErrorCode;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QrTokenServiceTest {

    private QrTokenService qrTokenService;
    private final UUID eventId = UUID.randomUUID();
    private final int rotationSeconds = 10;
    private final int graceSeconds = 3;

    @BeforeEach
    void setUp() {
        qrTokenService = new QrTokenService("test-secret-key-at-least-32-chars-long-1234567890!");
    }

    @Test
    @DisplayName("1. Issue token định dạng đúng: qr1.<eventId>.<epoch>.<sig>")
    void issueTokenFormat() {
        Instant now = Instant.ofEpochSecond(1005); // epoch = 100
        String token = qrTokenService.issue(eventId, now, rotationSeconds);

        assertThat(token).startsWith("qr1." + eventId + ".100.");
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(4);
    }

    @Test
    @DisplayName("2. Verify thành công khi token thuộc epoch hiện tại")
    void verifyCurrentEpochSuccess() {
        Instant now = Instant.ofEpochSecond(1005);
        String token = qrTokenService.issue(eventId, now, rotationSeconds);

        var result = qrTokenService.verify(token, eventId, now, rotationSeconds, graceSeconds);
        assertThat(result.isValid()).isTrue();
        assertThat(result.epoch()).isEqualTo(100L);
    }

    @Test
    @DisplayName("3. Verify thành công khi token thuộc epoch trước nhưng vẫn trong grace (3s)")
    void verifyPreviousEpochWithinGraceSuccess() {
        Instant tokenTime = Instant.ofEpochSecond(1009); // epoch = 100
        String token = qrTokenService.issue(eventId, tokenTime, rotationSeconds);

        // Giờ hiện tại là 1012s -> epoch 101, trôi qua 2s (<= 3s grace)
        Instant verifyTime = Instant.ofEpochSecond(1012);
        var result = qrTokenService.verify(token, eventId, verifyTime, rotationSeconds, graceSeconds);

        assertThat(result.isValid()).isTrue();
        assertThat(result.epoch()).isEqualTo(100L);
    }

    @Test
    @DisplayName("4. Verify thất bại khi token thuộc epoch trước nhưng đã quá grace (3s) -> QR_EXPIRED")
    void verifyPreviousEpochPastGraceFails() {
        Instant tokenTime = Instant.ofEpochSecond(1009); // epoch = 100
        String token = qrTokenService.issue(eventId, tokenTime, rotationSeconds);

        // Giờ hiện tại là 1014s -> epoch 101, trôi qua 4s (> 3s grace)
        Instant verifyTime = Instant.ofEpochSecond(1014);
        var result = qrTokenService.verify(token, eventId, verifyTime, rotationSeconds, graceSeconds);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.QR_EXPIRED);
    }

    @Test
    @DisplayName("5. Token từ tương lai -> QR_FROM_FUTURE")
    void verifyFutureTokenFails() {
        Instant tokenTime = Instant.ofEpochSecond(1050); // epoch = 105
        String token = qrTokenService.issue(eventId, tokenTime, rotationSeconds);

        Instant verifyTime = Instant.ofEpochSecond(1000); // epoch = 100
        var result = qrTokenService.verify(token, eventId, verifyTime, rotationSeconds, graceSeconds);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.QR_FROM_FUTURE);
    }

    @Test
    @DisplayName("6. Chữ ký bị giả mạo / sửa đổi -> QR_BAD_SIGNATURE")
    void verifyTamperedSignatureFails() {
        Instant now = Instant.ofEpochSecond(1005);
        String token = qrTokenService.issue(eventId, now, rotationSeconds);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        var result = qrTokenService.verify(tamperedToken, eventId, now, rotationSeconds, graceSeconds);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.QR_BAD_SIGNATURE);
    }

    @Test
    @DisplayName("7. Sai event ID -> QR_WRONG_EVENT")
    void verifyWrongEventFails() {
        Instant now = Instant.ofEpochSecond(1005);
        String token = qrTokenService.issue(eventId, now, rotationSeconds);

        UUID anotherEventId = UUID.randomUUID();
        var result = qrTokenService.verify(token, anotherEventId, now, rotationSeconds, graceSeconds);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.QR_WRONG_EVENT);
    }

    @Test
    @DisplayName("8. Token sai định dạng / rỗng -> QR_MALFORMED")
    void verifyMalformedTokenFails() {
        Instant now = Instant.ofEpochSecond(1005);

        assertThat(qrTokenService.verify("", eventId, now, rotationSeconds, graceSeconds).errorCode())
                .isEqualTo(ErrorCode.QR_MALFORMED);
        assertThat(qrTokenService.verify("invalid.token", eventId, now, rotationSeconds, graceSeconds).errorCode())
                .isEqualTo(ErrorCode.QR_MALFORMED);
        assertThat(qrTokenService.verify("qr2." + eventId + ".100.sig", eventId, now, rotationSeconds, graceSeconds).errorCode())
                .isEqualTo(ErrorCode.QR_MALFORMED);
        assertThat(qrTokenService.verify("qr1.not-a-uuid.100.sig", eventId, now, rotationSeconds, graceSeconds).errorCode())
                .isEqualTo(ErrorCode.QR_MALFORMED);
        assertThat(qrTokenService.verify("qr1." + eventId + ".not-a-number.sig", eventId, now, rotationSeconds, graceSeconds).errorCode())
                .isEqualTo(ErrorCode.QR_MALFORMED);
    }
}

