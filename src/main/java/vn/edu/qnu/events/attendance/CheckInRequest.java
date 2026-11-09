package vn.edu.qnu.events.attendance;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(
        @NotBlank(message = "Mã token điểm danh không được để trống")
        String token
) {}

