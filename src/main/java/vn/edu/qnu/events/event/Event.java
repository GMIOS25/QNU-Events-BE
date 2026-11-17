package vn.edu.qnu.events.event;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 300)
    private String location;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "registration_deadline", nullable = false)
    private Instant registrationDeadline;

    @Column(name = "attendance_open_at", nullable = false)
    private Instant attendanceOpenAt;

    @Column(name = "attendance_close_at", nullable = false)
    private Instant attendanceCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_method", nullable = false, length = 20)
    private AttendanceMethod attendanceMethod = AttendanceMethod.DYNAMIC_QR;

    @Column(name = "qr_rotation_seconds", nullable = false)
    private int qrRotationSeconds = 10;

    @Column(name = "qr_grace_seconds", nullable = false)
    private int qrGraceSeconds = 3;

    @Column(name = "quota_total", nullable = false)
    private int quotaTotal;

    @Column(name = "registered_count", nullable = false)
    private int registeredCount = 0;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "activity_points", nullable = false)
    private int activityPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "previewed_version")
    private Integer previewedVersion;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_audience", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "class_id", nullable = false)
    private Set<UUID> audienceClassIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Event() {}

    public Event(
            UUID id,
            UUID ownerId,
            String title,
            String description,
            String location,
            Instant startAt,
            Instant endAt,
            Instant registrationDeadline,
            Instant attendanceOpenAt,
            Instant attendanceCloseAt,
            AttendanceMethod attendanceMethod,
            int qrRotationSeconds,
            int qrGraceSeconds,
            int quotaTotal,
            String activityType,
            int activityPoints,
            Set<UUID> audienceClassIds
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
        this.registrationDeadline = registrationDeadline;
        this.attendanceOpenAt = attendanceOpenAt;
        this.attendanceCloseAt = attendanceCloseAt;
        this.attendanceMethod = attendanceMethod != null ? attendanceMethod : AttendanceMethod.DYNAMIC_QR;
        this.qrRotationSeconds = qrRotationSeconds > 0 ? qrRotationSeconds : 10;
        this.qrGraceSeconds = qrGraceSeconds >= 0 ? qrGraceSeconds : 3;
        this.quotaTotal = quotaTotal;
        this.registeredCount = 0;
        this.activityType = activityType;
        this.activityPoints = activityPoints;
        this.status = EventStatus.DRAFT;
        if (audienceClassIds != null) {
            this.audienceClassIds.addAll(audienceClassIds);
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Instant getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(Instant registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public Instant getAttendanceOpenAt() { return attendanceOpenAt; }
    public void setAttendanceOpenAt(Instant attendanceOpenAt) { this.attendanceOpenAt = attendanceOpenAt; }
    public Instant getAttendanceCloseAt() { return attendanceCloseAt; }
    public void setAttendanceCloseAt(Instant attendanceCloseAt) { this.attendanceCloseAt = attendanceCloseAt; }
    public AttendanceMethod getAttendanceMethod() { return attendanceMethod; }
    public void setAttendanceMethod(AttendanceMethod attendanceMethod) { this.attendanceMethod = attendanceMethod; }
    public int getQrRotationSeconds() { return qrRotationSeconds; }
    public void setQrRotationSeconds(int qrRotationSeconds) { this.qrRotationSeconds = qrRotationSeconds; }
    public int getQrGraceSeconds() { return qrGraceSeconds; }
    public void setQrGraceSeconds(int qrGraceSeconds) { this.qrGraceSeconds = qrGraceSeconds; }
    public int getQuotaTotal() { return quotaTotal; }
    public void setQuotaTotal(int quotaTotal) { this.quotaTotal = quotaTotal; }
    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public int getActivityPoints() { return activityPoints; }
    public void setActivityPoints(int activityPoints) { this.activityPoints = activityPoints; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getPreviewedVersion() { return previewedVersion; }
    public void setPreviewedVersion(Integer previewedVersion) { this.previewedVersion = previewedVersion; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public UUID getPublishedBy() { return publishedBy; }
    public void setPublishedBy(UUID publishedBy) { this.publishedBy = publishedBy; }
    public Set<UUID> getAudienceClassIds() { return audienceClassIds; }
    public void setAudienceClassIds(Set<UUID> audienceClassIds) { this.audienceClassIds = audienceClassIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

