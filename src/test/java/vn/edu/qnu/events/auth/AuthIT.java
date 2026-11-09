package vn.edu.qnu.events.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Tài khoản LOCKED bị từ chối đăng nhập với 403 FORBIDDEN")
    void loginWithLockedUserReturnsForbidden() throws Exception {
        AppUser lockedUser = new AppUser(
                java.util.UUID.randomUUID(),
                "locked_user_01",
                "$2a$12$e8x1KqIkn.wQnU4Bqg4K9.z9vA9oK5iQ8g5k7mZ4x1p2q3r4s5t6u",
                "Người Dùng Bị Khóa",
                null,
                null,
                java.util.Set.of("STUDENT")
        );
        lockedUser.setStatus(UserStatus.LOCKED);
        appUserRepository.save(lockedUser);

        String loginPayload = """
                {
                    "loginId": "locked_user_01",
                    "password": "AnyPassword"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa."));
    }

    @Test
    @DisplayName("Mật khẩu trong database phải là chuỗi băm BCrypt ($2) và không phải plaintext")
    void passwordsInDatabaseAreBcryptHashedNotPlaintext() {
        AppUser owner = appUserRepository.findByLoginId("owner01").orElseThrow();
        assertTrue(owner.getPasswordHash().startsWith("$2"), "Password hash phải bắt đầu bằng $2");
        assertFalse(owner.getPasswordHash().contains("Demo@12345"), "Password hash không được chứa plaintext");
    }

    @Test
    @DisplayName("STUDENT không có quyền gọi endpoint tạo sự kiện của EVENT_OWNER (403 FORBIDDEN)")
    void studentCallingPostEventsReturns403() throws Exception {
        String loginPayload = """
                {
                    "loginId": "sv001",
                    "password": "Demo@12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("EVENT_OWNER không có quyền gọi endpoint đăng ký sự kiện của STUDENT (403 FORBIDDEN)")
    void ownerCallingPostRegistrationsReturns403() throws Exception {
        String loginPayload = """
                {
                    "loginId": "owner01",
                    "password": "Demo@12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(post("/api/v1/events/" + java.util.UUID.randomUUID() + "/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản hợp lệ trả về JWT token và thông tin người dùng")
    void loginWithValidCredentialsReturnsJwtAndUserInfo() throws Exception {
        String loginPayload = """
                {
                    "loginId": "owner01",
                    "password": "Demo@12345"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiresAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.user.fullName").value("Nguyễn Văn Chủ Sự Kiện"))
                .andExpect(jsonPath("$.user.roles", hasItem("EVENT_OWNER")));
    }

    @Test
    @DisplayName("Sai mật khẩu và tài khoản không tồn tại trả về cùng cấu trúc lỗi INVALID_CREDENTIALS")
    void loginWithInvalidCredentialsReturnsIdenticalError() throws Exception {
        String wrongPasswordPayload = """
                {
                    "loginId": "owner01",
                    "password": "WrongPassword@123"
                }
                """;

        String nonExistentUserPayload = """
                {
                    "loginId": "non_existent_user_999",
                    "password": "AnyPassword@123"
                }
                """;

        MvcResult resultWrongPw = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Thông tin đăng nhập không chính xác."))
                .andReturn();

        MvcResult resultWrongUser = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonExistentUserPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Thông tin đăng nhập không chính xác."))
                .andReturn();

        JsonNode jsonWrongPw = objectMapper.readTree(resultWrongPw.getResponse().getContentAsString());
        JsonNode jsonWrongUser = objectMapper.readTree(resultWrongUser.getResponse().getContentAsString());

        org.junit.jupiter.api.Assertions.assertEquals(jsonWrongPw.get("code").asText(), jsonWrongUser.get("code").asText());
        org.junit.jupiter.api.Assertions.assertEquals(jsonWrongPw.get("message").asText(), jsonWrongUser.get("message").asText());
    }

    @Test
    @DisplayName("Đăng xuất thu hồi session và dùng lại token cũ sẽ bị từ chối với SESSION_REVOKED")
    void logoutRevokesSessionAndSubsequentCallFails() throws Exception {
        String loginPayload = """
                {
                    "loginId": "sv001",
                    "password": "Demo@12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Kiểm tra token hoạt động bình thường trên /auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Trần Sinh Viên 1"));

        // Gọi logout -> 204 No Content
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Dùng lại token đã bị thu hồi -> 401 SESSION_REVOKED
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }

    @Test
    @DisplayName("EVENT_OWNER lấy danh sách lớp học thành công qua GET /api/v1/classes")
    void ownerCanFetchClasses() throws Exception {
        String loginPayload = """
                {
                    "loginId": "owner01",
                    "password": "Demo@12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/classes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code", not(emptyOrNullString())))
                .andExpect(jsonPath("$[0].name", not(emptyOrNullString())));
    }
}

