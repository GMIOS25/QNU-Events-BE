package vn.edu.qnu.events.student;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

public record HistoryItem(
        UUID eventId,
        String title,
        Instant startAt,
        String location,
        String registrationStatus,
        Instant registeredAt,
        String attendanceStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant attendanceAt,
        String method,
        String activityType,
        Integer activityPoints
) {}

