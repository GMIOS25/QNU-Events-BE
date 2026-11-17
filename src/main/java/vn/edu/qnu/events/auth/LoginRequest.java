package vn.edu.qnu.events.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        String loginId,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {}

