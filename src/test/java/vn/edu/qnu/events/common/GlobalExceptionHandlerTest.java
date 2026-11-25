package vn.edu.qnu.events.common;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    @DisplayName("MethodArgumentTypeMismatchException (vd: UUID không hợp lệ) -> 400 VALIDATION_FAILED")
    void handleTypeMismatchReturns400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "id", null, new IllegalArgumentException("Invalid UUID")
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "trace-123");

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().traceId()).isEqualTo("trace-123");
        assertThat(response.getBody().details()).containsKey("fields");
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) response.getBody().details().get("fields");
        assertThat(fields).containsKey("id");
    }

    @Test
    @DisplayName("InvalidDataAccessApiUsageException (sort field sai) -> 400 VALIDATION_FAILED")
    void handleInvalidQueryUsageReturns400() {
        InvalidDataAccessApiUsageException ex = new InvalidDataAccessApiUsageException("No property 'invalidField' found");
        ResponseEntity<ApiError> response = handler.handleInvalidQueryUsage(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("OptimisticLockingFailure -> 409 VERSION_CONFLICT")
    void handleOptimisticLockReturns409() {
        ObjectOptimisticLockingFailureException springEx =
                new ObjectOptimisticLockingFailureException("Event", UUID.randomUUID());
        ResponseEntity<ApiError> r1 = handler.handleOptimisticLockingFailure(springEx, new MockHttpServletRequest());
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r1.getBody()).isNotNull();
        assertThat(r1.getBody().code()).isEqualTo("VERSION_CONFLICT");

        OptimisticLockException jpaEx = new OptimisticLockException("Concurrent edit");
        ResponseEntity<ApiError> r2 = handler.handleOptimisticLockingFailure(jpaEx, new MockHttpServletRequest());
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r2.getBody()).isNotNull();
        assertThat(r2.getBody().code()).isEqualTo("VERSION_CONFLICT");
    }
}
