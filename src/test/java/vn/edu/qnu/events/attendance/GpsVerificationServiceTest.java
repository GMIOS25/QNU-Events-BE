package vn.edu.qnu.events.attendance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GpsVerificationServiceTest {

    // Tọa độ tham chiếu: Hội trường A - Trường Đại học Quy Nhơn (170 An Dương Vương)
    private static final double QNU_HALL_A_LAT = 13.758900;
    private static final double QNU_HALL_A_LON = 109.218300;
    private static final int DEFAULT_RADIUS_METERS = 60;
    private static final double DEFAULT_ACCURACY_THRESHOLD_METERS = 100.0;

    private GpsVerificationService gpsVerificationService;

    @BeforeEach
    void setUp() {
        gpsVerificationService = new GpsVerificationService(DEFAULT_ACCURACY_THRESHOLD_METERS);
    }

    @Test
    @DisplayName("1. Tọa độ trùng đúng tâm sự kiện (0m) -> hợp lệ và không bị gắn cờ đáng ngờ")
    void verify_atExactEventCenter_returnsWithinRadiusAndNotSuspicious() {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT, QNU_HALL_A_LON, 12.5);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.distanceMeters()).isEqualTo(0.0);
        assertThat(result.allowedRadiusMeters()).isEqualTo(DEFAULT_RADIUS_METERS);
        assertThat(result.isSuspicious()).isFalse();
        assertThat(result.suspiciousReason()).isNull();
    }

    @Test
    @DisplayName("2. Vị trí nằm trong bán kính (~55.6m <= 60m) với sai số tốt (15m) -> hợp lệ bình thường")
    void verify_insideRadiusWithGoodAccuracy_returnsValidAndClean() {
        // Lệch +0.0005 độ vĩ độ ~ 55.6 mét (< 60m)
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT + 0.0005, QNU_HALL_A_LON, 15.0);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.distanceMeters()).isCloseTo(55.6, within(0.5));
        assertThat(result.isSuspicious()).isFalse();
        assertThat(result.suspiciousReason()).isNull();
    }

    @Test
    @DisplayName("3. Vị trí nằm sát ngay bên trong biên bán kính (59.9m <= 60m) -> hợp lệ")
    void verify_justInsideBoundary_returnsWithinRadius() {
        // 1 độ vĩ độ = 111,194.9266m -> 59.95m tương ứng deltaLat ~ 0.00053914 độ
        double deltaLatFor59Point95Meters = 59.95 / 111_194.9266;
        GpsCheckInRequest request = new GpsCheckInRequest(
                QNU_HALL_A_LAT + deltaLatFor59Point95Meters, QNU_HALL_A_LON, 10.0
        );

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.distanceMeters()).isLessThanOrEqualTo(60.0);
    }

    @Test
    @DisplayName("4. Vị trí vừa vượt quá bán kính cho phép (60.04m > 60m) -> từ chối và giữ nguyên distanceMeters gốc > 60.0 (không làm tròn về 60.0)")
    void verify_justOutsideBoundary60Point04Meters_returnsOutOfRangeAndDistanceStrictlyGreaterThanRadius() {
        double deltaLatFor60Point04Meters = 60.04 / 111_194.9266;
        GpsCheckInRequest request = new GpsCheckInRequest(
                QNU_HALL_A_LAT + deltaLatFor60Point04Meters, QNU_HALL_A_LON, 10.0
        );

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.distanceMeters()).isGreaterThan(60.0);
        assertThat(result.distanceMeters()).isCloseTo(60.04, within(0.005));
    }

    @Test
    @DisplayName("5. Vị trí cách xa sự kiện (~122m > 60m) -> từ chối ngoài vùng")
    void verify_farOutsideRadius_returnsOutOfRange() {
        // Lệch +0.0011 độ vĩ độ ~ 122.3 mét
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT + 0.0011, QNU_HALL_A_LON, 12.0);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.distanceMeters()).isCloseTo(122.3, within(0.5));
    }

    @Test
    @DisplayName("6. Vị trí trong bán kính và accuracy đúng bằng ngưỡng (100.0m) -> không gắn cờ đáng ngờ")
    void verify_accuracyExactlyAtThreshold_isNotSuspicious() {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT, QNU_HALL_A_LON, 100.0);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.isSuspicious()).isFalse();
        assertThat(result.suspiciousReason()).isNull();
    }

    @Test
    @DisplayName("7. Vị trí trong bán kính nhưng accuracy vượt ngưỡng (100.1m > 100m) -> vẫn trong vùng nhưng gắn cờ LOW_GPS_ACCURACY (BR-ATT-07)")
    void verify_insideRadiusButLowAccuracy_flagsSuspiciousWithoutRejecting() {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT + 0.0002, QNU_HALL_A_LON, 100.1);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.isSuspicious()).isTrue();
        assertThat(result.suspiciousReason()).isEqualTo(GpsVerificationService.SUSPICIOUS_REASON_LOW_ACCURACY);
    }

    @Test
    @DisplayName("8. Vị trí ngoài bán kính và accuracy vượt ngưỡng (250m) -> vẫn bị từ chối ngoài vùng")
    void verify_outsideRadiusWithLowAccuracy_isStillRejected() {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT + 0.0020, QNU_HALL_A_LON, 250.0);

        var result = gpsVerificationService.verify(
                request, QNU_HALL_A_LAT, QNU_HALL_A_LON, DEFAULT_RADIUS_METERS
        );

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.isSuspicious()).isTrue();
        assertThat(result.suspiciousReason()).isEqualTo(GpsVerificationService.SUSPICIOUS_REASON_LOW_ACCURACY);
    }

    @Test
    @DisplayName("9. Bán kính cấu hình động theo từng sự kiện (vd: sự kiện sân vận động 150m)")
    void verify_respectsPerEventConfiguredRadius() {
        // Khoảng cách ~111.2m: trượt với bán kính 60m nhưng đỗ với bán kính 150m
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT + 0.0010, QNU_HALL_A_LON, 20.0);

        var result60m = gpsVerificationService.verify(request, QNU_HALL_A_LAT, QNU_HALL_A_LON, 60);
        var result150m = gpsVerificationService.verify(request, QNU_HALL_A_LAT, QNU_HALL_A_LON, 150);

        assertThat(result60m.isWithinRadius()).isFalse();
        assertThat(result150m.isWithinRadius()).isTrue();
    }

    @Test
    @DisplayName("10. Từ chối khi request hoặc các trường latitude/longitude/accuracyMeters bị null")
    void verify_nullRequestOrFields_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> gpsVerificationService.verify(null, QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(
                new GpsCheckInRequest(null, QNU_HALL_A_LON, 10.0), QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(
                new GpsCheckInRequest(QNU_HALL_A_LAT, null, 10.0), QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(
                new GpsCheckInRequest(QNU_HALL_A_LAT, QNU_HALL_A_LON, null), QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.0001, 90.0001, -120.0, 150.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("11. Từ chối khi latitude nằm ngoài [-90, 90] hoặc là NaN/Infinity")
    void verify_invalidLatitude_throwsIllegalArgumentException(double invalidLat) {
        GpsCheckInRequest request = new GpsCheckInRequest(invalidLat, QNU_HALL_A_LON, 10.0);

        assertThatThrownBy(() -> gpsVerificationService.verify(request, QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.0001, 180.0001, -200.0, 360.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("12. Từ chối khi longitude nằm ngoài [-180, 180] hoặc là NaN/Infinity")
    void verify_invalidLongitude_throwsIllegalArgumentException(double invalidLon) {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT, invalidLon, 10.0);

        assertThatThrownBy(() -> gpsVerificationService.verify(request, QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -10.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("13. Từ chối khi accuracyMeters âm hoặc là NaN/Infinity")
    void verify_invalidAccuracy_throwsIllegalArgumentException(double invalidAccuracy) {
        GpsCheckInRequest request = new GpsCheckInRequest(QNU_HALL_A_LAT, QNU_HALL_A_LON, invalidAccuracy);

        assertThatThrownBy(() -> gpsVerificationService.verify(request, QNU_HALL_A_LAT, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("14. Chấp nhận tọa độ ở đúng các biên cực Trái Đất (±90, ±180) và accuracy = 0.0")
    void verify_exactPolarAndAntimeridianBoundaries_doesNotThrow() {
        GpsCheckInRequest northPole = new GpsCheckInRequest(90.0, 180.0, 0.0);
        var result = gpsVerificationService.verify(northPole, 90.0, -180.0, 10);

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.distanceMeters()).isCloseTo(0.0, within(1e-12));
    }

    @Test
    @DisplayName("15. Ném IllegalStateException khi sự kiện chưa cấu hình tọa độ tâm hoặc bán kính <= 0")
    void verify_invalidEventGpsConfig_throwsIllegalStateException() {
        GpsCheckInRequest validRequest = new GpsCheckInRequest(QNU_HALL_A_LAT, QNU_HALL_A_LON, 10.0);

        assertThatThrownBy(() -> gpsVerificationService.verify(validRequest, null, QNU_HALL_A_LON, 60))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(validRequest, QNU_HALL_A_LAT, null, 60))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(validRequest, QNU_HALL_A_LAT, QNU_HALL_A_LON, 0))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> gpsVerificationService.verify(validRequest, QNU_HALL_A_LAT, QNU_HALL_A_LON, -50))
                .isInstanceOf(IllegalStateException.class);
    }
}
