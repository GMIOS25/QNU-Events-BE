package vn.edu.qnu.events.attendance;

import java.time.Instant;

/**
 * Phản hồi khi điểm danh GPS thành công (QE-55 / UC016 Bước 8a & 10).
 * Lưu ý bảo mật: không trả cờ suspicious_flag về phía sinh viên để tránh lộ ngưỡng phát hiện gian lận.
 */
public record GpsCheckInResponse(
        String status,
        Instant acceptedAt,
        double distanceMeters,
        int allowedRadiusMeters
) {}
