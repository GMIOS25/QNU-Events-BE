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

        if (upsert.quotaTotal() < 1) {
            fieldErrors.put("quotaTotal", "Số lượng chỗ tối thiểu là 1.");
        }

        if (upsert.audienceClassIds() == null || upsert.audienceClassIds().isEmpty()) {
            fieldErrors.put("audienceClassIds", "Phải chọn ít nhất một lớp đối tượng tham gia.");
        } else {
            for (UUID classId : upsert.audienceClassIds()) {
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
}

