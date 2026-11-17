package vn.edu.qnu.events.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_attempts")
@Immutable
public class AttendanceAttempt {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, length = 20)
    private String method = "DYNAMIC_QR";

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(nullable = false)
    private boolean counted;

    @Column(name = "http_status", nullable = false)
    private short httpStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb", nullable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AttendanceAttempt() {}

    public AttendanceAttempt(
            UUID id,
            UUID eventId,
            UUID studentId,
            String method,
            String idempotencyKey,
            String outcome,
            String reasonCode,
            boolean counted,
            short httpStatus,
            String responseBody,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.eventId = eventId;
        this.studentId = studentId;
        this.method = method != null ? method : "DYNAMIC_QR";
        this.idempotencyKey = idempotencyKey;
        this.outcome = outcome;
        this.reasonCode = reasonCode;
        this.counted = counted;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody != null ? responseBody : "{}";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getStudentId() { return studentId; }
    public String getMethod() { return method; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOutcome() { return outcome; }
    public String getReasonCode() { return reasonCode; }
    public boolean isCounted() { return counted; }
    public short getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }
    public Instant getCreatedAt() { return createdAt; }
}

