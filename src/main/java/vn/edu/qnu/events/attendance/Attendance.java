package vn.edu.qnu.events.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendances")
public class Attendance {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, length = 20)
    private String method = "DYNAMIC_QR";

    @Column(nullable = false, length = 20)
    private String status = "ACCEPTED";

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Attendance() {}

    public Attendance(UUID id, UUID eventId, UUID studentId, String method, String status, Instant recordedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.eventId = eventId;
        this.studentId = studentId;
        this.method = method != null ? method : "DYNAMIC_QR";
        this.status = status != null ? status : "ACCEPTED";
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getStudentId() { return studentId; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public void setMethod(String method) { this.method = method; }
    public void setStatus(String status) { this.status = status; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

