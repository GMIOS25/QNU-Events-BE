package SeleniumTest.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic event payload builders for Selenium UI test fixtures.
 */
public final class SeleniumTestDataFactory {

    private SeleniumTestDataFactory() {
    }

    public static String uniqueTitle(String prefix) {
        return prefix + " [" + UUID.randomUUID().toString().substring(0, 8) + "]";
    }

    public static Map<String, Object> validFutureEventPayload(String title, int quotaTotal, List<String> classIds) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(3, ChronoUnit.HOURS);
        return buildEventPayload(
                title,
                "Mô tả chi tiết sự kiện kiểm thử tự động Selenium",
                "Hội trường A - Trường ĐH Quy Nhơn",
                startAt,
                endAt,
                startAt.minus(30, ChronoUnit.MINUTES),
                startAt.minus(15, ChronoUnit.MINUTES),
                endAt.plus(30, ChronoUnit.MINUTES),
                "DYNAMIC_QR",
                quotaTotal,
                classIds,
                "Hội thảo khoa học",
                5
        );
    }

    public static Map<String, Object> activeNowEventPayload(String title, int quotaTotal, List<String> classIds) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return buildEventPayload(
                title,
                "Sự kiện đang mở đăng ký và điểm danh Dynamic QR",
                "Hội trường B - ĐH Quy Nhơn",
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS),
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS),
                "DYNAMIC_QR",
                quotaTotal,
                classIds,
                "Hội thảo khoa học",
                10
        );
    }

    public static Map<String, Object> registrationExpiredEventPayload(String title, int quotaTotal, List<String> classIds) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return buildEventPayload(
                title,
                "Sự kiện đã hết hạn đăng ký",
                "Phòng C201 - ĐH Quy Nhơn",
                now.plus(1, ChronoUnit.HOURS),
                now.plus(3, ChronoUnit.HOURS),
                now.minus(5, ChronoUnit.MINUTES),
                now.plus(45, ChronoUnit.MINUTES),
                now.plus(3, ChronoUnit.HOURS),
                "DYNAMIC_QR",
                quotaTotal,
                classIds,
                "Hoạt động phong trào",
                5
        );
    }

    public static Map<String, Object> buildEventPayload(
            String title,
            String description,
            String location,
            Instant startAt,
            Instant endAt,
            Instant registrationDeadline,
            Instant attendanceOpenAt,
            Instant attendanceCloseAt,
            String attendanceMethod,
            int quotaTotal,
            List<String> audienceClassIds,
            String activityType,
            int activityPoints
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        payload.put("location", location);
        payload.put("startAt", startAt != null ? startAt.toString() : null);
        payload.put("endAt", endAt != null ? endAt.toString() : null);
        payload.put("registrationDeadline", registrationDeadline != null ? registrationDeadline.toString() : null);
        payload.put("attendanceOpenAt", attendanceOpenAt != null ? attendanceOpenAt.toString() : null);
        payload.put("attendanceCloseAt", attendanceCloseAt != null ? attendanceCloseAt.toString() : null);
        if (attendanceMethod != null) {
            payload.put("attendanceMethod", attendanceMethod);
        }
        payload.put("quotaTotal", quotaTotal);
        payload.put("audienceClassIds", audienceClassIds);
        payload.put("activityType", activityType);
        payload.put("activityPoints", activityPoints);
        return payload;
    }
}
