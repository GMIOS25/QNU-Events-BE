package vn.edu.qnu.events.event;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        String location,
        Instant startAt,
        Instant endAt,
        int quotaTotal,
        int registeredCount,
        int remainingQuota,
        boolean registrationOpen,
        boolean attendanceOpen,
        String myRegistrationStatus,
        EventStatus status,
        String activityType,
        int activityPoints
) {}

