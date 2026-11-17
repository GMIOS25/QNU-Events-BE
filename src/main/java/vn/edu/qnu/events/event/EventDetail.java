package vn.edu.qnu.events.event;

import java.time.Instant;
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
        Instant publishedAt
) {}

