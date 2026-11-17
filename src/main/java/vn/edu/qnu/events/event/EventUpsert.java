package vn.edu.qnu.events.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record EventUpsert(
        @NotBlank(message = "Tiêu đề sự kiện không được để trống")
        @Size(min = 3, max = 200, message = "Tiêu đề phải từ 3 đến 200 ký tự")
        String title,

        String description,

        @NotBlank(message = "Địa điểm không được để trống")
        @Size(max = 300, message = "Địa điểm không vượt quá 300 ký tự")
        String location,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        Instant startAt,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        Instant endAt,

        @NotNull(message = "Hạn chót đăng ký không được để trống")
        Instant registrationDeadline,

        @NotNull(message = "Thời gian mở điểm danh không được để trống")
        Instant attendanceOpenAt,

        @NotNull(message = "Thời gian đóng điểm danh không được để trống")
        Instant attendanceCloseAt,

        AttendanceMethod attendanceMethod,

        @Min(value = 1, message = "Số lượng chỗ tối thiểu là 1")
        int quotaTotal,

        @NotEmpty(message = "Phải chọn ít nhất một lớp đối tượng tham gia")
        Set<UUID> audienceClassIds,

        @NotBlank(message = "Loại hoạt động không được để trống")
        String activityType,

        @Min(value = 0, message = "Điểm rèn luyện phải lớn hơn hoặc bằng 0")
        int activityPoints,

        Integer version
) {}

