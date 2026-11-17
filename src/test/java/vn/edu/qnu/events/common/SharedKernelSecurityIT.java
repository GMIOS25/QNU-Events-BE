package vn.edu.qnu.events.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.events.auth.AuthService;
import vn.edu.qnu.events.auth.LoginRequest;
import vn.edu.qnu.events.auth.LoginResponse;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(SharedKernelSecurityIT.TestController.class)
class SharedKernelSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    private String studentToken;

    @BeforeEach
    void setUp() {
        LoginResponse response = authService.login(new LoginRequest("sv001", "Demo@12345"));
        studentToken = response.accessToken();
    }

    @RestController
    static class TestController {
        @PostMapping("/api/v1/test-validation")
        public String validate(@Valid @RequestBody TestRequest request) {
            return "ok";
        }

        @GetMapping("/api/v1/test-server-error")
        public String triggerServerError() {
            throw new RuntimeException("Sensitive database credentials leaked in exception message!");
        }

        @GetMapping("/api/v1/test-business-error")
        public String triggerBusinessError() {
            throw new ApiException(ErrorCode.CAPACITY_FULL, "Sự kiện đã hết suất.");
        }
    }

    record TestRequest(
            @NotBlank(message = "Tiêu đề không được để trống")
            String title
    ) {
    }

    @Test
    @DisplayName("401 trả về cấu trúc lỗi ApiError JSON chuẩn khi chưa xác thực")
    void unauthenticatedReturnsJsonErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Yêu cầu đăng nhập để thực hiện."))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("403 trả về cấu trúc lỗi ApiError JSON chuẩn khi sai vai trò")
    void forbiddenReturnsJsonErrorEnvelope() throws Exception {
        // Sinh viên gọi endpoint tạo sự kiện của EVENT_OWNER -> 403 FORBIDDEN
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("400 validation trả về cấu trúc lỗi VALIDATION_FAILED kèm details.fields")
    void validationFailureReturnsDetailsFields() throws Exception {
        mockMvc.perform(post("/api/v1/test-validation")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.title").value("Tiêu đề không được để trống"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("500 server error trả về INTERNAL_ERROR chung, không làm lộ stack trace hay thông tin nhạy cảm")
    void serverErrorDoesNotLeakStackTraceOrInternalMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-server-error")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Đã xảy ra lỗi hệ thống."))
                .andExpect(jsonPath("$.details", is(org.hamcrest.Matchers.anEmptyMap())))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("ApiException trả về đúng mã lỗi và HTTP status nghiệp vụ")
    void apiExceptionReturnsCorrectStatusAndCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-business-error")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_FULL"))
                .andExpect(jsonPath("$.message").value("Sự kiện đã hết suất."))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));
    }
}

