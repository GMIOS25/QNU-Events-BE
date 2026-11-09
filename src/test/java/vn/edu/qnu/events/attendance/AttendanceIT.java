package vn.edu.qnu.events.attendance;

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
import vn.edu.qnu.events.auth.AppUser;
import vn.edu.qnu.events.auth.AppUserRepository;
import vn.edu.qnu.events.auth.AuthService;
import vn.edu.qnu.events.auth.LoginRequest;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.registration.Registration;
import vn.edu.qnu.events.registration.RegistrationRepository;
import vn.edu.qnu.events.registration.RegistrationStatus;
import vn.edu.qnu.events.student.OrgClass;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AttendanceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceAttemptRepository attemptRepository;

    @Autowired
    private OrgClassRepository orgClassRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private QrTokenService qrTokenService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String ownerToken;
    private String student1Token;
    private String student2Token;
    private UUID k49ClassId;
    private UUID ownerId;
    private UUID student1Id;
    private UUID student2Id;

    @BeforeEach
    void setUp() {
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();
        student1Token = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();
        student2Token = authService.login(new LoginRequest("sv002", "Demo@12345")).accessToken();

        OrgClass k49 = orgClassRepository.findByCode("K49CNTT1").orElseThrow();
        k49ClassId = k49.getId();

        ownerId = appUserRepository.findByLoginId("owner01").orElseThrow().getId();
        student1Id = appUserRepository.findByLoginId("sv001").orElseThrow().getId();
        student2Id = appUserRepository.findByLoginId("sv002").orElseThrow().getId();
    }

    private Event createEventWithAttendanceWindow(Instant attOpen, Instant attClose) {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant regDeadline = startAt.minus(2, ChronoUnit.HOURS);

        Event event = new Event(
                UUID.randomUUID(),
                ownerId,
                "Sự kiện Điểm danh QR",
                "Mô tả",
                "Hội trường B",
                startAt,
                endAt,
                regDeadline,
                attOpen,
                attClose,
                AttendanceMethod.DYNAMIC_QR,
                10,
                3,
                50,
                "Workshop",
                5,
                Set.of(k49ClassId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(now);
        return eventRepository.saveAndFlush(event);
    }

    private void registerStudent(UUID eventId, UUID studentId) {
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(),
                eventId,
                studentId,
                RegistrationStatus.REGISTERED,
                Instant.now(),
                Instant.now()
        ));
    }

    @Test
    @DisplayName("1. EVENT_OWNER lấy mã QR động trong cửa sổ điểm danh -> 200 OK")
    void ownerCanGetQrTokenDuringAttendanceWindow() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/qr")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.epoch").isNumber())
                .andExpect(jsonPath("$.validUntil", not(emptyOrNullString())))
                .andExpect(jsonPath("$.rotationSeconds").value(10))
                .andExpect(jsonPath("$.serverTime", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("2. Sinh viên (STUDENT) gọi GET /events/{id}/qr bị từ chối 403 FORBIDDEN")
    void studentCannotGetQrToken() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/qr")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("3. Lấy QR ngoài cửa sổ điểm danh -> 409 ATTENDANCE_NOT_OPEN")
    void getQrTokenOutsideAttendanceWindowFails() throws Exception {
        Instant now = Instant.now();
        // Cửa sổ bắt đầu trong tương lai (1 giờ nữa)
        Event event = createEventWithAttendanceWindow(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/qr")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_NOT_OPEN"));
    }

    @Test
    @DisplayName("4. Sinh viên điểm danh thành công với QR hợp lệ -> 200 ACCEPTED")
    void checkInWithValidQrTokenSuccess() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        String token = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String idempotencyKey = UUID.randomUUID().toString();

        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, token);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt", not(emptyOrNullString())));

        boolean checkedIn = attendanceRepository.existsByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED");
        assertThat(checkedIn).isTrue();
    }

    @Test
    @DisplayName("5. Gửi lại cùng Idempotency-Key -> trả đúng response cũ mà không tạo thêm attendance")
    void checkInWithSameIdempotencyKeyReturnsCachedResponse() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        String token = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String idempotencyKey = UUID.randomUUID().toString();
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, token);

        // Lần 1: 200 ACCEPTED
        MvcResult res1 = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();

        String body1 = res1.getResponse().getContentAsString();

        // Lần 2 với cùng idempotency key -> trả về y hệt
        MvcResult res2 = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        String body2 = res2.getResponse().getContentAsString();
        assertThat(body1).isEqualTo(body2);

        // Vẫn chỉ có đúng 1 bản ghi attendance
        long count = attendanceRepository.countByEventIdAndStatus(event.getId(), "ACCEPTED");
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("6. Thiếu header Idempotency-Key -> 400 VALIDATION_FAILED")
    void checkInWithoutIdempotencyKeyFails() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        String token = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, token);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("7. Chưa đăng ký giữ suất mà điểm danh -> 403 NOT_REGISTERED")
    void checkInWithoutRegistrationFails() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        // Không gọi registerStudent!

        String token = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, token);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_REGISTERED"));
    }

    @Test
    @DisplayName("8. Đã điểm danh rồi điểm danh lại bằng key khác -> 409 ALREADY_CHECKED_IN")
    void checkInTwiceWithDifferentKeysFails() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        String token = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, token);

        // Lần 1: 200 ACCEPTED
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Lần 2 với key mới -> 409 ALREADY_CHECKED_IN
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));
    }

    @Test
    @DisplayName("9. Quét mã QR đã hết hạn (> grace 3s) -> 422 QR_EXPIRED")
    void checkInWithExpiredTokenFails() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        // Token tạo từ 50 giây trước (5 epoch trước)
        Instant past = now.minus(50, ChronoUnit.SECONDS);
        String expiredToken = qrTokenService.issue(event.getId(), past, event.getQrRotationSeconds());
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, expiredToken);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QR_EXPIRED"));
    }

    @Test
    @DisplayName("10. Quét mã QR chữ ký bị sửa -> 422 QR_BAD_SIGNATURE")
    void checkInWithBadSignatureFails() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student1Id);

        String validToken = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String tamperedToken = validToken.substring(0, validToken.length() - 3) + "xyz";
        String payload = String.format("""
                {
                    "token": "%s"
                }
                """, tamperedToken);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QR_BAD_SIGNATURE"));
    }

    @Test
    @DisplayName("11. Thử sai 3 lần, lần thứ 4 (kể cả với token đúng) bị từ chối với 409 ATTEMPT_LIMIT_REACHED")
    void checkInAttemptLimitReachedAfter3Failures() throws Exception {
        Instant now = Instant.now();
        Event event = createEventWithAttendanceWindow(now.minus(10, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));
        registerStudent(event.getId(), student2Id);

        // 3 lần thử sai với token giả
        for (int i = 1; i <= 3; i++) {
            String fakePayload = "{\"token\": \"qr1.fake.token.invalid\"}";
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                            .header("Authorization", "Bearer " + student2Token)
                            .header("Idempotency-Key", "fail-key-" + i + "-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fakePayload))
                    .andExpect(status().isUnprocessableEntity());
        }

        // Lần 4 thử với token hoàn toàn hợp lệ -> vẫn bị từ chối 409 ATTEMPT_LIMIT_REACHED
        String validToken = qrTokenService.issue(event.getId(), now, event.getQrRotationSeconds());
        String validPayload = String.format("""
                {
                    "token": "%s"
                }
                """, validToken);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/attendance/qr")
                        .header("Authorization", "Bearer " + student2Token)
                        .header("Idempotency-Key", "attempt-4-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_LIMIT_REACHED"));
    }
}

