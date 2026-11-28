package vn.edu.qnu.events.event;

import org.springframework.stereotype.Component;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EventValidator {

    private final OrgClassRepository orgClassRepository;

    public EventValidator(OrgClassRepository orgClassRepository) {
        this.orgClassRepository = orgClassRepository;
    }

    public void validate(EventUpsert upsert) {
        AttendanceMethod effectiveMethod = upsert.attendanceMethod() != null
                ? upsert.attendanceMethod()
                : AttendanceMethod.DYNAMIC_QR;
        validate(upsert, effectiveMethod);
    }

    public void validate(EventUpsert upsert, AttendanceMethod effectiveMethod) {
        Map<String, String> fieldErrors = new HashMap<>();

        if (upsert.startAt() != null && upsert.endAt() != null) {
            if (!upsert.endAt().isAfter(upsert.startAt())) {
                fieldErrors.put("endAt", "Thời gian kết thúc phải sau thời gian bắt đầu.");
            }
        }

        if (upsert.attendanceOpenAt() != null && upsert.attendanceCloseAt() != null) {
            if (!upsert.attendanceCloseAt().isAfter(upsert.attendanceOpenAt())) {
                fieldErrors.put("attendanceCloseAt", "Thời gian đóng điểm danh phải sau thời gian mở điểm danh.");
            }
        }

        if (upsert.registrationDeadline() != null && upsert.attendanceCloseAt() != null) {
            if (upsert.registrationDeadline().isAfter(upsert.attendanceCloseAt())) {
                fieldErrors.put("registrationDeadline", "Hạn chót đăng ký phải trước hoặc bằng thời gian đóng điểm danh.");
            }
        }

        if (upsert.quotaTotal() == null || upsert.quotaTotal() < 1) {
            fieldErrors.put("quotaTotal", "Số lượng chỗ tối thiểu là 1.");
        }

        if (upsert.activityPoints() == null || upsert.activityPoints() < 0) {
            fieldErrors.put("activityPoints", "Điểm rèn luyện phải lớn hơn hoặc bằng 0.");
        }

        if (upsert.activityType() != null && upsert.activityType().length() > 50) {
            fieldErrors.put("activityType", "Loại hoạt động không vượt quá 50 ký tự.");
        }

        populateGpsFieldErrors(
                effectiveMethod,
                upsert.gpsLat(),
                upsert.gpsLng(),
                upsert.allowedRadiusMeters(),
                false,
                fieldErrors
        );

        if (upsert.audienceClassIds() == null || upsert.audienceClassIds().isEmpty()) {
            fieldErrors.put("audienceClassIds", "Phải chọn ít nhất một lớp đối tượng tham gia.");
        } else {
            for (UUID classId : upsert.audienceClassIds()) {
                if (classId == null) {
                    fieldErrors.put("audienceClassIds", "Danh sách lớp đối tượng chứa giá trị rỗng không hợp lệ.");
                    break;
                }
                if (!orgClassRepository.existsById(classId)) {
                    fieldErrors.put("audienceClassIds", "Lớp học có ID " + classId + " không tồn tại.");
                    break;
                }
            }
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Thông tin sự kiện không hợp lệ.",
                    Map.of("fields", fieldErrors)
            );
        }
    }

    public void validateGpsConfiguration(Event event) {
        Map<String, String> fieldErrors = new HashMap<>();
        populateGpsFieldErrors(
                event.getAttendanceMethod(),
                event.getGpsLat(),
                event.getGpsLng(),
                event.getAllowedRadiusMeters(),
                true,
                fieldErrors
        );
        if (!fieldErrors.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Thông tin sự kiện không hợp lệ.",
                    Map.of("fields", fieldErrors)
            );
        }
    }

    public boolean hasValidGpsConfiguration(Event event) {
        if (event.getAttendanceMethod() != AttendanceMethod.GPS) {
            return true;
        }
        Map<String, String> fieldErrors = new HashMap<>();
        populateGpsFieldErrors(
                event.getAttendanceMethod(),
                event.getGpsLat(),
                event.getGpsLng(),
                event.getAllowedRadiusMeters(),
                true,
                fieldErrors
        );
        return fieldErrors.isEmpty();
    }

    private void populateGpsFieldErrors(
            AttendanceMethod method,
            Double gpsLat,
            Double gpsLng,
            Integer allowedRadiusMeters,
            boolean requireRadiusForGps,
            Map<String, String> fieldErrors
    ) {
        if (method == AttendanceMethod.GPS) {
            if (gpsLat == null || !Double.isFinite(gpsLat)
                    || gpsLat < -90.0 || gpsLat > 90.0) {
                fieldErrors.put("gpsLat", "Vĩ độ tâm sự kiện (gpsLat) phải là số hữu hạn trong khoảng [-90, 90].");
            }
            if (gpsLng == null || !Double.isFinite(gpsLng)
                    || gpsLng < -180.0 || gpsLng > 180.0) {
                fieldErrors.put("gpsLng", "Kinh độ tâm sự kiện (gpsLng) phải là số hữu hạn trong khoảng [-180, 180].");
            }
            if ((requireRadiusForGps && allowedRadiusMeters == null)
                    || (allowedRadiusMeters != null && (allowedRadiusMeters < 10 || allowedRadiusMeters > 1000))) {
                fieldErrors.put("allowedRadiusMeters", "Bán kính điểm danh GPS phải nằm trong khoảng từ 10 đến 1000 mét.");
            }
        } else {
            if (gpsLat != null && (!Double.isFinite(gpsLat)
                    || gpsLat < -90.0 || gpsLat > 90.0)) {
                fieldErrors.put("gpsLat", "Vĩ độ tâm sự kiện (gpsLat) phải nằm trong khoảng [-90, 90].");
            }
            if (gpsLng != null && (!Double.isFinite(gpsLng)
                    || gpsLng < -180.0 || gpsLng > 180.0)) {
                fieldErrors.put("gpsLng", "Kinh độ tâm sự kiện (gpsLng) phải nằm trong khoảng [-180, 180].");
            }
            if (allowedRadiusMeters != null
                    && (allowedRadiusMeters < 10 || allowedRadiusMeters > 1000)) {
                fieldErrors.put("allowedRadiusMeters", "Bán kính điểm danh GPS phải nằm trong khoảng từ 10 đến 1000 mét.");
            }
        }
    }
}

