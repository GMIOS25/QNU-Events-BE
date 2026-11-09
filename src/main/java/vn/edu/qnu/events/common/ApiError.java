package vn.edu.qnu.events.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, Object> details,
        String traceId,
        Instant timestamp
) {
    public static ApiError of(ErrorCode errorCode, String message, Map<String, Object> details, String traceId) {
        return new ApiError(
                errorCode.name(),
                message != null ? message : errorCode.getDefaultMessage(),
                details != null ? details : Map.of(),
                traceId,
                Instant.now()
        );
    }

    public static ApiError of(ErrorCode errorCode, String traceId) {
        return of(errorCode, errorCode.getDefaultMessage(), Map.of(), traceId);
    }
}

