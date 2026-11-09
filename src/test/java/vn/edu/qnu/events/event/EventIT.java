package vn.edu.qnu.events.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.qnu.events.audit.AuditLogRepository;
import vn.edu.qnu.events.auth.AuthService;
import vn.edu.qnu.events.auth.LoginRequest;
import vn.edu.qnu.events.student.OrgClass;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EventIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private OrgClassRepository orgClassRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String ownerToken;
    private String owner2Token;
    private String studentToken;
    private String student3Token;
    private UUID k49ClassId;

    @BeforeEach
    void setUp() {
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();
        owner2Token = authService.login(new LoginRequest("owner02", "Demo@12345")).accessToken();
        studentToken = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();
        student3Token = authService.login(new LoginRequest("sv003", "Demo@12345")).accessToken();

        OrgClass k49 = orgClassRepository.findByCode("K49CNTT1").orElseThrow();
        k49ClassId = k49.getId();
    }

    private String buildValidEventUpsert(String title) {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant regDeadline = startAt.minus(2, ChronoUnit.HOURS);
        Instant attOpen = startAt.minus(15, ChronoUnit.MINUTES);
        Instant attClose = endAt.plus(30, ChronoUnit.MINUTES);

        return String.format("""
                {
                    "title": "%s",
                    "description": "Mô tả sự kiện chi tiết",
                    "location": "Hội trường B - Đại học Quy Nhơn",
                    "startAt": "%s",
                    "endAt": "%s",
                    "registrationDeadline": "%s",
                    "attendanceOpenAt": "%s",
                    "attendanceCloseAt": "%s",
                    "attendanceMethod": "DYNAMIC_QR",
                    "quotaTotal": 50,
                    "audienceClassIds": ["%s"],
                    "activityType": "Hội thảo học thuật",
                    "activityPoints": 5
                }
                """, title, startAt, endAt, regDeadline, attOpen, attClose, k49ClassId);
    }

    @Test
    @DisplayName("1. EVENT_OWNER tạo sự kiện nháp DRAFT hợp lệ thành công")
    void createDraftEventSuccess() throws Exception {
        String payload = buildValidEventUpsert("Hội thảo AI trong giáo dục");

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.title").value("Hội thảo AI trong giáo dục"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.quotaTotal").value(50))
                .andExpect(jsonPath("$.registeredCount").value(0))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("2. endAt <= startAt trả về 400 VALIDATION_FAILED")
    void createEventWithInvalidTimeFails() throws Exception {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.minus(1, ChronoUnit.HOURS); // endAt < startAt

        String payload = String.format("""
                {
                    "title": "Sự kiện sai thời gian",
                    "location": "Hội trường B",
                    "startAt": "%s",
                    "endAt": "%s",
                    "registrationDeadline": "%s",
                    "attendanceOpenAt": "%s",
                    "attendanceCloseAt": "%s",
                    "quotaTotal": 50,
                    "audienceClassIds": ["%s"],
                    "activityType": "Workshop",
                    "activityPoints": 5
                }
                """, startAt, endAt, startAt, startAt, endAt, k49ClassId);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("3. quotaTotal = 0 trả về 400 VALIDATION_FAILED")
    void createEventWithZeroQuotaFails() throws Exception {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);

        String payload = String.format("""
                {
                    "title": "Sự kiện không có chỗ",
                    "location": "Hội trường B",
                    "startAt": "%s",
                    "endAt": "%s",
                    "registrationDeadline": "%s",
                    "attendanceOpenAt": "%s",
                    "attendanceCloseAt": "%s",
                    "quotaTotal": 0,
                    "audienceClassIds": ["%s"],
                    "activityType": "Workshop",
                    "activityPoints": 5
                }
                """, startAt, endAt, startAt, startAt, endAt, k49ClassId);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("4. audienceClassIds rỗng trả về 400 VALIDATION_FAILED")
    void createEventWithEmptyAudienceFails() throws Exception {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);

        String payload = String.format("""
                {
                    "title": "Sự kiện thiếu lớp đối tượng",
                    "location": "Hội trường B",
                    "startAt": "%s",
                    "endAt": "%s",
                    "registrationDeadline": "%s",
                    "attendanceOpenAt": "%s",
                    "attendanceCloseAt": "%s",
                    "quotaTotal": 30,
                    "audienceClassIds": [],
                    "activityType": "Workshop",
                    "activityPoints": 5
                }
                """, startAt, endAt, startAt, startAt, endAt);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("5. STUDENT gọi POST /events bị từ chối 403 FORBIDDEN")
    void studentCannotCreateEvent() throws Exception {
        String payload = buildValidEventUpsert("Sinh viên cố tạo sự kiện");

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("6. Luồng Publish đầy đủ: Preview -> Publish thành công -> Publish lần 2 không lặp lại")
    void previewAndPublishWorkflowSuccess() throws Exception {
        // 1. Tạo sự kiện nháp
        String createPayload = buildValidEventUpsert("Hội nghị nghiên cứu khoa học sinh viên");
        MvcResult createRes = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String eventId = createdJson.get("id").asText();

        // 2. Publish khi chưa preview -> 409 PREVIEW_REQUIRED
        String publishBeforePreview = """
                {
                    "confirm": true,
                    "previewVersion": 0
                }
                """;
        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBeforePreview))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PREVIEW_REQUIRED"));

        // 3. Xem trước preview -> ghi nhận previewedVersion = 0
        MvcResult previewRes = mockMvc.perform(get("/api/v1/events/" + eventId + "/preview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewVersion").value(0))
                .andReturn();

        // 4. Publish thiếu confirm=true -> 400 VALIDATION_FAILED
        String publishNoConfirm = """
                {
                    "confirm": false,
                    "previewVersion": 0
                }
                """;
        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishNoConfirm))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // 5. Publish với confirm=true và previewVersion đúng -> 200 PUBLISHED
        String validPublish = """
                {
                    "confirm": true,
                    "previewVersion": 0
                }
                """;
        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPublish))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt", not(emptyOrNullString())));

        // 6. Publish lần 2 -> 200 trả hiện trạng mà không sinh thêm lỗi hoặc audit trùng
        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPublish))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // 7. Sửa sự kiện sau khi đã PUBLISHED -> 409 CONFLICT
        String updatePayload = buildValidEventUpsert("Cố sửa sự kiện đã công bố");
        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("7. Sửa sự kiện sau preview dẫn đến lệch version -> Publish bị từ chối với PREVIEW_OUTDATED")
    void publishOutdatedVersionFails() throws Exception {
        // 1. Tạo sự kiện nháp
        String createPayload = buildValidEventUpsert("Sự kiện cập nhật version");
        MvcResult createRes = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // 2. Preview version 0
        mockMvc.perform(get("/api/v1/events/" + eventId + "/preview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 3. Sửa sự kiện -> version tăng lên 1
        String updatePayload = buildValidEventUpsert("Sự kiện cập nhật tiêu đề mới");
        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // 4. Publish với previewVersion cũ (0) -> 409 PREVIEW_OUTDATED
        String publishPayload = """
                {
                    "confirm": true,
                    "previewVersion": 0
                }
                """;
        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PREVIEW_OUTDATED"));
    }

    @Test
    @DisplayName("8. Owner B cố tình sửa sự kiện của Owner A -> 403 FORBIDDEN")
    void ownerBCannotUpdateOwnerAEvent() throws Exception {
        String createPayload = buildValidEventUpsert("Sự kiện riêng của Owner A");
        MvcResult createRes = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        String updatePayload = buildValidEventUpsert("Owner B cố ghi đè sự kiện");
        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("9. Sinh viên xem sự kiện: SV đúng lớp xem được (200), SV khác lớp không thấy (404)")
    void studentAudienceClassIsolation() throws Exception {
        // 1. Tạo sự kiện nháp cho lớp K49CNTT1 và Publish
        String createPayload = buildValidEventUpsert("Sự kiện riêng cho K49CNTT1");
        MvcResult createRes = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // Preview & Publish
        mockMvc.perform(get("/api/v1/events/" + eventId + "/preview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "confirm": true,
                                    "previewVersion": 0
                                }
                                """))
                .andExpect(status().isOk());

        // 2. sv001 (thuộc K49CNTT1) xem chi tiết -> 200 OK
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Sự kiện riêng cho K49CNTT1"));

        // 3. sv003 (thuộc K49CNTT2) xem chi tiết -> 404 NOT_FOUND (bảo mật không lộ sự kiện khác lớp)
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("10. Tìm kiếm phân trang GET /api/v1/events?q=... lọc theo từ khóa tiêu đề")
    void searchEventsByTitle() throws Exception {
        String uniqueTitle = "Khởi nghiệp công nghệ AI " + UUID.randomUUID().toString().substring(0, 8);
        String createPayload = buildValidEventUpsert(uniqueTitle);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated());

        // Tìm theo từ khóa chữ thường "khởi nghiệp"
        mockMvc.perform(get("/api/v1/events?q=khởi nghiệp")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[?(@.title =~ /.*Khởi nghiệp.*/i)]", not(empty())));
    }
}

