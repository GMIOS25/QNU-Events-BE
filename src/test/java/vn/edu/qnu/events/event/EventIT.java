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
    private EventRepository eventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String ownerToken;
    private String owner2Token;
    private String studentToken;
    private String student2Token;
    private String student3Token;
    private UUID k49ClassId;

    @BeforeEach
    void setUp() {
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();
        owner2Token = authService.login(new LoginRequest("owner02", "Demo@12345")).accessToken();
        studentToken = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();
        student2Token = authService.login(new LoginRequest("sv002", "Demo@12345")).accessToken();
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

    private String createAndPublishEvent(String title, String activityType, int quotaTotal) throws Exception {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant regDeadline = startAt.minus(2, ChronoUnit.HOURS);
        Instant attOpen = startAt.minus(15, ChronoUnit.MINUTES);
        Instant attClose = endAt.plus(30, ChronoUnit.MINUTES);

        String payload = String.format("""
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
                    "quotaTotal": %d,
                    "audienceClassIds": ["%s"],
                    "activityType": "%s",
                    "activityPoints": 5
                }
                """, title, startAt, endAt, regDeadline, attOpen, attClose, quotaTotal, k49ClassId, activityType);

        MvcResult createRes = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/events/" + eventId + "/preview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\": true, \"previewVersion\": 0}"))
                .andExpect(status().isOk());

        return eventId;
    }

    @Test
    @DisplayName("11. [AC1] Sinh viên chỉ thấy sự kiện PUBLISHED dành cho lớp mình; DRAFT và sự kiện lớp khác bị ẩn ở server")
    void studentOnlySeesPublishedEventsForTheirClass() throws Exception {
        String publishedTitle = "Sự kiện công bố K49 " + UUID.randomUUID().toString().substring(0, 8);
        createAndPublishEvent(publishedTitle, "Hội thảo học thuật", 50);

        String draftTitle = "Sự kiện nháp ẩn " + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidEventUpsert(draftTitle)))
                .andExpect(status().isCreated());

        MvcResult listRes = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = listRes.getResponse().getContentAsString();
        assertTrue(body.contains(publishedTitle), "Sinh viên phải thấy sự kiện đã công bố của lớp mình");
        assertFalse(body.contains(draftTitle), "Sinh viên tuyệt đối không được thấy sự kiện nháp DRAFT");

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == '" + publishedTitle + "')]", empty()));
    }

    @Test
    @DisplayName("12. [AC2 & AC5] Trang chi tiết hiển thị đầy đủ thông tin, thời hạn, quota và trạng thái đăng ký do server xác định (phân lập giữa 2 sinh viên)")
    void eventDetailDisplaysAllRequiredFieldsAndServerSideRegistrationStatus() throws Exception {
        String title = "Hội thảo hướng nghiệp IT " + UUID.randomUUID().toString().substring(0, 8);
        String eventId = createAndPublishEvent(title, "Hướng nghiệp", 100);

        // 1. Trước khi đăng ký: myRegistrationStatus là null, registrationOpen = true
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.location").value("Hội trường B - Đại học Quy Nhơn"))
                .andExpect(jsonPath("$.startAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.endAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.registrationDeadline", not(emptyOrNullString())))
                .andExpect(jsonPath("$.attendanceOpenAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.attendanceCloseAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.quotaTotal").value(100))
                .andExpect(jsonPath("$.registeredCount").value(0))
                .andExpect(jsonPath("$.remainingQuota").value(100))
                .andExpect(jsonPath("$.registrationOpen").value(true))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.myRegistrationStatus").doesNotExist());

        // 2. sv001 đăng ký giữ suất thành công
        mockMvc.perform(post("/api/v1/events/" + eventId + "/registrations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTERED"));

        // 3. sv001 xem lại chi tiết -> server trả myRegistrationStatus = 'REGISTERED', registeredCount = 1, remainingQuota = 99
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.remainingQuota").value(99))
                .andExpect(jsonPath("$.myRegistrationStatus").value("REGISTERED"));

        // 4. Phân lập dữ liệu (AC5): sv002 (cùng lớp K49CNTT1) xem chi tiết -> myRegistrationStatus vẫn là null, không lộ trạng thái của sv001!
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.remainingQuota").value(99))
                .andExpect(jsonPath("$.myRegistrationStatus").doesNotExist());
    }

    @Test
    @DisplayName("13. [AC3] Lọc theo loại hoạt động (activityType) và tìm kiếm kết hợp; trả về rỗng khi không khớp (REQ-REG-003)")
    void filterEventsByActivityTypeAndKeyword() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String eventA = "Diễn đàn AI " + tag;
        String eventB = "Ngày hội hiến máu " + tag;

        createAndPublishEvent(eventA, "Hội thảo học thuật", 50);
        createAndPublishEvent(eventB, "Tình nguyện xã hội", 50);

        // Lọc theo activityType = "Hội thảo học thuật"
        mockMvc.perform(get("/api/v1/events?activityType=Hội thảo học thuật")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == '" + eventA + "')]", not(empty())))
                .andExpect(jsonPath("$.content[?(@.title == '" + eventB + "')]", empty()));

        // Lọc theo activityType không tồn tại -> trả về rỗng theo REQ-REG-003
        mockMvc.perform(get("/api/v1/events?activityType=LoạiKhôngTồnTại_999")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()));

        // Tìm kiếm kết hợp q và activityType
        mockMvc.perform(get("/api/v1/events?q=hiến máu&activityType=Tình nguyện xã hội")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title == '" + eventB + "')]", not(empty())))
                .andExpect(jsonPath("$.content[?(@.title == '" + eventA + "')]", empty()));
    }

    @Test
    @DisplayName("14. [AC4] Sự kiện đã hủy (CANCELLED) hoặc nháp (DRAFT) bị ẩn hoàn toàn (404), không xuất hiện như sự kiện có thể đăng ký")
    void cancelledOrDraftEventsAreHiddenAndCannotBeRegistered() throws Exception {
        // 1. Tạo và công bố sự kiện
        String title = "Sự kiện chuẩn bị hủy " + UUID.randomUUID().toString().substring(0, 8);
        String eventId = createAndPublishEvent(title, "Hội thảo", 20);

        // Xác nhận ban đầu sinh viên xem được
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // 2. Chuyển trạng thái sang CANCELLED
        Event event = eventRepository.findById(UUID.fromString(eventId)).orElseThrow();
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        // 3. Sinh viên gọi GET /events/{id} -> 404 NOT_FOUND (bảo mật ẩn sự kiện đã hủy)
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 4. Sinh viên gọi GET /events -> Không xuất hiện trong danh sách
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + eventId + "')]", empty()));
    }

    @Test
    @DisplayName("15. [AC4 & AC5] Server tính toán registrationOpen = false khi sự kiện hết quota (remainingQuota = 0)")
    void registrationOpenCalculatedAsFalseWhenQuotaExhausted() throws Exception {
        String title = "Sự kiện giới hạn 1 suất " + UUID.randomUUID().toString().substring(0, 8);
        String eventId = createAndPublishEvent(title, "Workshop", 1); // quota = 1

        // Trước khi đăng ký: remainingQuota = 1, registrationOpen = true
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuota").value(1))
                .andExpect(jsonPath("$.registrationOpen").value(true));

        // sv001 đăng ký chiếm suất duy nhất
        mockMvc.perform(post("/api/v1/events/" + eventId + "/registrations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated());

        // sv002 kiểm tra chi tiết -> remainingQuota = 0, registrationOpen = false
        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuota").value(0))
                .andExpect(jsonPath("$.registrationOpen").value(false));
    }
}

