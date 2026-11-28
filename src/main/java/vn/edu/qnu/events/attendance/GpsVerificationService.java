package vn.edu.qnu.events.attendance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ miền xác thực vị trí điểm danh bằng GPS (QE-55 / UC016 / BR-ATT-07).
 *
 * <p>Trách nhiệm:
 * <ul>
 *   <li>Kiểm tra tính hợp lệ của tọa độ đầu vào (hữu hạn, nằm trong biên Trái Đất, accuracy >= 0).</li>
 *   <li>Tính khoảng cách thực tế đến tâm sự kiện bằng công thức Haversine (chuẩn cầu WGS-84).</li>
 *   <li>So sánh khoảng cách với bán kính cho phép theo từng sự kiện và giữ nguyên khoảng cách gốc
 *       trong kết quả xác thực (tránh nghịch lý làm tròn 60.04m -> 60.0m ở sát biên).</li>
 *   <li>Đánh dấu cờ đáng ngờ ({@code LOW_GPS_ACCURACY}) khi {@code accuracyMeters} vượt ngưỡng tin cậy
 *       nhưng vẫn cho phép điểm danh nếu nằm trong bán kính hợp lệ (theo BR-ATT-07).</li>
 * </ul>
 */
@Service
public class GpsVerificationService {

    /** Bán kính trung bình Trái Đất theo chuẩn WGS-84 (mét). */
    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;

    public static final double DEFAULT_MAX_TRUSTED_ACCURACY_METERS = 100.0;
    public static final String SUSPICIOUS_REASON_LOW_ACCURACY = "LOW_GPS_ACCURACY";

    private final double maxTrustedAccuracyMeters;

    public GpsVerificationService() {
        this(DEFAULT_MAX_TRUSTED_ACCURACY_METERS);
    }

    @Autowired
    public GpsVerificationService(
            @Value("${app.gps.max-trusted-accuracy-meters:100.0}") double maxTrustedAccuracyMeters
    ) {
        if (!Double.isFinite(maxTrustedAccuracyMeters) || maxTrustedAccuracyMeters <= 0) {
            throw new IllegalArgumentException("Ngưỡng maxTrustedAccuracyMeters phải là số dương hữu hạn.");
        }
        this.maxTrustedAccuracyMeters = maxTrustedAccuracyMeters;
    }

    /**
     * Xác thực yêu cầu điểm danh GPS của sinh viên so với tâm và bán kính sự kiện.
     *
     * @param request             tọa độ và độ sai số gửi từ thiết bị sinh viên
     * @param eventLatitude       vĩ độ tâm sự kiện
     * @param eventLongitude      kinh độ tâm sự kiện
     * @param allowedRadiusMeters bán kính cho phép của sự kiện (mét, phải > 0)
     * @return kết quả xác thực gồm trạng thái trong/ngoài bán kính, khoảng cách gốc và cờ đáng ngờ
     * @throws IllegalArgumentException nếu tọa độ sinh viên bất hợp lệ
     * @throws IllegalStateException    nếu cấu hình GPS của sự kiện chưa hợp lệ
     */
    public VerificationResult verify(
            GpsCheckInRequest request,
            Double eventLatitude,
            Double eventLongitude,
            Integer allowedRadiusMeters
    ) {
        validateEventConfiguration(eventLatitude, eventLongitude, allowedRadiusMeters);
        validateStudentRequest(request);

        double rawDistanceMeters = calculateHaversineDistanceMeters(
                request.latitude(),
                request.longitude(),
                eventLatitude,
                eventLongitude
        );

        // Giữ nguyên khoảng cách thực tế (rawDistanceMeters) trong kết quả xác thực để không bao giờ
        // xảy ra mâu thuẫn isWithinRadius = false nhưng distanceMeters sau làm tròn lại <= allowedRadiusMeters.
        boolean withinRadius = rawDistanceMeters <= allowedRadiusMeters;

        boolean suspicious = request.accuracyMeters() > maxTrustedAccuracyMeters;
        String suspiciousReason = suspicious ? SUSPICIOUS_REASON_LOW_ACCURACY : null;

        return new VerificationResult(
                withinRadius,
                rawDistanceMeters,
                allowedRadiusMeters,
                suspicious,
                suspiciousReason
        );
    }

    /**
     * Kiểm tra tính hợp lệ của payload tọa độ sinh viên gửi lên.
     *
     * @throws IllegalArgumentException nếu request hoặc bất kỳ trường tọa độ/sai số nào không hợp lệ
     */
    public void validateStudentRequest(GpsCheckInRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu tọa độ GPS không được để trống.");
        }
        if (!isValidLatitude(request.latitude())) {
            throw new IllegalArgumentException("Vĩ độ (latitude) phải là số hữu hạn trong khoảng [-90, 90].");
        }
        if (!isValidLongitude(request.longitude())) {
            throw new IllegalArgumentException("Kinh độ (longitude) phải là số hữu hạn trong khoảng [-180, 180].");
        }
        if (request.accuracyMeters() == null
                || !Double.isFinite(request.accuracyMeters())
                || request.accuracyMeters() < 0.0) {
            throw new IllegalArgumentException("Độ sai số (accuracyMeters) phải là số hữu hạn không âm.");
        }
    }

    /**
     * Kiểm tra cấu hình tọa độ tâm và bán kính của sự kiện.
     *
     * @throws IllegalStateException nếu sự kiện thiếu tọa độ tâm hoặc bán kính không hợp lệ
     */
    public void validateEventConfiguration(
            Double eventLatitude,
            Double eventLongitude,
            Integer allowedRadiusMeters
    ) {
        if (!isValidLatitude(eventLatitude) || !isValidLongitude(eventLongitude)) {
            throw new IllegalStateException("Sự kiện chưa được cấu hình tọa độ tâm GPS hợp lệ.");
        }
        if (allowedRadiusMeters == null || allowedRadiusMeters <= 0) {
            throw new IllegalStateException("Bán kính điểm danh GPS của sự kiện phải lớn hơn 0.");
        }
    }

    /**
     * Tính khoảng cách đường chim bay giữa hai tọa độ địa lý theo công thức Haversine (mét).
     */
    public double calculateHaversineDistanceMeters(
            double lat1Degrees,
            double lon1Degrees,
            double lat2Degrees,
            double lon2Degrees
    ) {
        double lat1Rad = Math.toRadians(lat1Degrees);
        double lat2Rad = Math.toRadians(lat2Degrees);
        double deltaLatRad = Math.toRadians(lat2Degrees - lat1Degrees);
        double deltaLonRad = Math.toRadians(lon2Degrees - lon1Degrees);

        double sinHalfDeltaLat = Math.sin(deltaLatRad / 2.0);
        double sinHalfDeltaLon = Math.sin(deltaLonRad / 2.0);

        double haversineA = (sinHalfDeltaLat * sinHalfDeltaLat)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * (sinHalfDeltaLon * sinHalfDeltaLon);

        // Kẹp haversineA trong [0.0, 1.0] để phòng sai số dấu phẩy động ở hai điểm đối cực
        double clampedA = Math.min(1.0, Math.max(0.0, haversineA));
        double centralAngleRad = 2.0 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1.0 - clampedA));

        return EARTH_RADIUS_METERS * centralAngleRad;
    }

    public double getMaxTrustedAccuracyMeters() {
        return maxTrustedAccuracyMeters;
    }

    public static boolean isValidLatitude(Double latitude) {
        return latitude != null
                && Double.isFinite(latitude)
                && latitude >= MIN_LATITUDE
                && latitude <= MAX_LATITUDE;
    }

    public static boolean isValidLongitude(Double longitude) {
        return longitude != null
                && Double.isFinite(longitude)
                && longitude >= MIN_LONGITUDE
                && longitude <= MAX_LONGITUDE;
    }

    public record VerificationResult(
            boolean isWithinRadius,
            double distanceMeters,
            int allowedRadiusMeters,
            boolean isSuspicious,
            String suspiciousReason
    ) {}
}
