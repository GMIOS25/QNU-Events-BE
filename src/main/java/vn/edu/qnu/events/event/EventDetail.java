package vn.edu.qnu.events.event;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record EventDetail(
        UUID id,
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
        int registeredCount,
        int remainingQuota,
        boolean registrationOpen,
        boolean attendanceOpen,
        String myRegistrationStatus,
        EventStatus status,
        String activityType,
        int activityPoints,
        UUID ownerId,
        Set<UUID> audienceClassIds,
        Integer version,
        Integer previewedVersion,
        Instant publishedAt,
        List<EventAudienceClass> audienceClasses,
        boolean eligible,
        Double gpsLat,
        Double gpsLng,
        Integer allowedRadiusMeters
) {
    public EventDetail(
            UUID id,
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
            int registeredCount,
            int remainingQuota,
            boolean registrationOpen,
            boolean attendanceOpen,
            String myRegistrationStatus,
            EventStatus status,
            String activityType,
            int activityPoints,
            UUID ownerId,
            Set<UUID> audienceClassIds,
            Integer version,
            Integer previewedVersion,
            Instant publishedAt,
            List<EventAudienceClass> audienceClasses,
            boolean eligible
    ) {
        this(
                id, title, description, location, startAt, endAt, registrationDeadline,
                attendanceOpenAt, attendanceCloseAt, attendanceMethod, qrRotationSeconds,
                qrGraceSeconds, quotaTotal, registeredCount, remainingQuota,
                registrationOpen, attendanceOpen, myRegistrationStatus, status,
                activityType, activityPoints, ownerId, audienceClassIds, version,
                previewedVersion, publishedAt, audienceClasses, eligible,
                null, null, null
        );
    }

    /** QE-37: đối tượng được phép tham gia hiển thị được (mã/tên lớp) thay vì chỉ UUID. */
    public record EventAudienceClass(UUID id, String code, String name) {}
}
