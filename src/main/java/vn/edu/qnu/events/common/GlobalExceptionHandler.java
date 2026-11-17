package vn.edu.qnu.events.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler() {
        this.clock = Clock.systemUTC();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GlobalExceptionHandler(org.springframework.beans.factory.ObjectProvider<Clock> clockProvider) {
        this.clock = clockProvider != null ? clockProvider.getIfAvailable(Clock::systemUTC) : Clock.systemUTC();
    }

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Business error [{}]: {} (traceId: {})", ex.getErrorCode(), ex.getMessage(), traceId);
        ApiError error = new ApiError(
                ex.getErrorCode().name(),
                ex.getMessage(),
                ex.getDetails(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> details = Map.of("fields", fieldErrors);
        ApiError error = new ApiError(
                ErrorCode.VALIDATION_FAILED.name(),
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                details,
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(error);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequestExceptions(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        ApiError error = new ApiError(
                ErrorCode.VALIDATION_FAILED.name(),
                "Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng.",
                Map.of(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        ApiError error = new ApiError(
                ErrorCode.FORBIDDEN.name(),
                ErrorCode.FORBIDDEN.getDefaultMessage(),
                Map.of(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        ApiError error = new ApiError(
                ErrorCode.UNAUTHENTICATED.name(),
                ErrorCode.UNAUTHENTICATED.getDefaultMessage(),
                Map.of(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getHttpStatus()).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        ApiError error = new ApiError(
                ErrorCode.NOT_FOUND.name(),
                ErrorCode.NOT_FOUND.getDefaultMessage(),
                Map.of(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Unhandled server exception (traceId: {}): {}", traceId, ex.getMessage(), ex);
        ApiError error = new ApiError(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                Map.of(),
                traceId,
                Instant.now(clock)
        );
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(error);
    }

    public static String resolveTraceId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        String headerTraceId = request.getHeader("X-Request-Id");
        if (headerTraceId == null || headerTraceId.isBlank()) {
            headerTraceId = request.getHeader("X-Trace-Id");
        }
        return (headerTraceId != null && !headerTraceId.isBlank()) ? headerTraceId : UUID.randomUUID().toString();
    }
}

