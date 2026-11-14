package vn.edu.qnu.events.attendance;

import org.springframework.http.HttpStatus;

/**
 * Internal result of a QR check-in. Rejections are returned instead of thrown so
 * the attendance attempt and its exact response can be committed atomically.
 */
public record CheckInResult(
        HttpStatus httpStatus,
        String responseBody
) {
}
