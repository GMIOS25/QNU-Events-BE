package vn.edu.qnu.events.attendance;

/**
 * Payload yêu cầu điểm danh bằng vị trí GPS từ trình duyệt (QE-55 / UC016).
 * Không bao gồm cờ isMocked vì Web Geolocation API không cung cấp tín hiệu này đáng tin cậy.
 */
public record GpsCheckInRequest(
        Double latitude,
        Double longitude,
        Double accuracyMeters
) {}
