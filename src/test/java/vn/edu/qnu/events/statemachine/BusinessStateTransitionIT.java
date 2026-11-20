package vn.edu.qnu.events.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.qnu.events.attendance.AttendanceRepository;
import vn.edu.qnu.events.attendance.CheckInRequest;
import vn.edu.qnu.events.attendance.QrTokenService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QE-25: Kiểm thử tích hợp máy trạng thái nghiệp vụ (Business Status & State Transition IT).
 * <p>
 * Phạm vi Sprint 1: Kiểm thử toàn bộ chuyển trạng thái đăng ký và điểm danh qua REST API thật
 * và các ràng buộc toàn vẹn CSDL (Unique Partial Index), dọn dẹp dữ liệu cô lập sau mỗi ca kiểm thử.
 * Các chuyển trạng thái Hủy (QE-40), Waiting List (QE-39), No-Show (QE-14) thuộc phạm vi Sprint 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("QE-25: Business Status & State Transition Integration Test")
class BusinessStateTransitionIT {

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
    private QrTokenService qrTokenService;

    @Autowired
    private OrgClassRepository orgClassRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final List<UUID> createdEventIds = new ArrayList<>();

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

    @AfterEach
    void tearDown() {
        if (!createdEventIds.isEmpty()) {
            MapSqlParameterSource params = new MapSqlParameterSource("ids", createdEventIds);
            jdbcTemplate.update("DELETE FROM attendance_attempts WHERE event_id IN (:ids)", params);
            jdbcTemplate.update("DELETE FROM attendances WHERE event_id IN (:ids)", params);
            jdbcTemplate.update("DELETE FROM registrations WHERE event_id IN (:ids)", params);
            jdbcTemplate.update("DELETE FROM event_audience WHERE event_id IN (:ids)", params);
            jdbcTemplate.update("DELETE FROM events WHERE id IN (:ids)", params);
            createdEventIds.clear();
        }
    }

    private Event createPublishedEvent(int quota, boolean openForAttendance) {
        Instant now = Instant.now();
        Instant regDeadline = now.plus(1, ChronoUnit.HOURS);
        Instant startAt = now.plus(2, ChronoUnit.HOURS);
        Instant endAt = now.plus(4, ChronoUnit.HOURS);

        Instant attOpenAt = openForAttendance ? now.minus(5, ChronoUnit.MINUTES) : startAt;
        Instant attCloseAt = openForAttendance ? now.plus(30, ChronoUnit.MINUTES) : endAt;

        Event event = new Event(
                UUID.randomUUID(),
                ownerId,
                "State Transition Test Event " + UUID.randomUUID().toString().substring(0, 8),
                "Mo ta kiem thu chuyen trang thai nghiep vu",
                "Hoi truong A",
                startAt,
                endAt,
                regDeadline,
                attOpenAt,
                attCloseAt,
                AttendanceMethod.DYNAMIC_QR,
                10,
                3,
                quota,
                "Workshop",
                5,
                Set.of(k49ClassId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.saveAndFlush(event);
        createdEventIds.add(saved.getId());
        return saved;
    }

    // =========================================================================
    // 1. REGISTRATION STATE MACHINE TESTS (Sprint 1 API & DB Invariants)
    // =========================================================================

    @Test
    @DisplayName("STTC-REG-01: Chuyển đổi hợp lệ NONE -> REGISTERED khi còn quota")
    void testRegistrationState_NoneToRegistered_WhenQuotaAvailable() throws Exception {
        Event event = createPublishedEvent(5, false);

        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.alreadyRegistered").value(false));

        // Kiểm tra tính nhất quán CSDL
        Registration reg = registrationRepository.findActiveByEventAndStudent(event.getId(), student1Id).orElseThrow();
        assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("STTC-REG-02: Chuyển đổi khi hết quota: NONE -> Giữ nguyên NONE, từ chối 409 CAPACITY_FULL (Sprint 1 A-02)")
    void testRegistrationState_None_WhenQuotaFull_ThrowsCapacityFull() throws Exception {
        // Sự kiện chỉ có 1 suất duy nhất
        Event event = createPublishedEvent(1, false);

        // Sinh viên 1 đăng ký thành công suất duy nhất -> 201 Created
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Sinh viên 2 đăng ký khi đã hết quota -> Bị từ chối 409 CAPACITY_FULL (Sprint 1 chưa có waiting list)
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_FULL"));

        // Kiểm tra CSDL: Sinh viên 2 tuyệt đối không có bản ghi đăng ký nào
        assertThat(registrationRepository.findActiveByEventAndStudent(event.getId(), student2Id)).isEmpty();

        // Số lượng registered_count vẫn giữ nguyên là 1
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("STTC-REG-03: Lũy biến (Idempotent) REGISTERED -> REGISTERED khi đăng ký lại")
    void testRegistrationState_Idempotent_ReRegisterKeepsRegistered() throws Exception {
        Event event = createPublishedEvent(5, false);

        // Đăng ký lần 1 -> 201 Created
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Đăng ký lần 2 -> 200 OK idempotent, giữ nguyên REGISTERED
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.alreadyRegistered").value(true));

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("STTC-REG-DB: Ràng buộc bất biến CSDL: Unique index uq_registrations_active ngăn tạo 2 bản ghi active cho cùng 1 sinh viên")
    void testRegistrationState_DatabaseInvariant_UniqueActiveRegistrationPerStudentEvent() {
        Event event = createPublishedEvent(5, false);

        // Lần 1: Chèn bản ghi active REGISTERED
        Registration reg = new Registration(
                UUID.randomUUID(),
                event.getId(),
                student1Id,
                RegistrationStatus.REGISTERED,
                Instant.now(),
                Instant.now()
        );
        registrationRepository.saveAndFlush(reg);

        // Lần 2: Cố tình chèn thêm 1 bản ghi REGISTERED thứ hai cho cùng sinh viên -> Ném DataIntegrityViolationException
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "INSERT INTO registrations (id, event_id, student_id, status, created_at) " +
                            "VALUES (:id, :eventId, :studentId, 'REGISTERED', now())",
                    new MapSqlParameterSource()
                            .addValue("id", UUID.randomUUID())
                            .addValue("eventId", event.getId())
                            .addValue("studentId", student1Id)
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // 2. ATTENDANCE STATE MACHINE TESTS (Sprint 1 API & DB Invariants)
    // =========================================================================

    @Test
    @DisplayName("STTC-ATT-01: Chuyển đổi hợp lệ NOT_CHECKED_IN -> ACCEPTED qua Dynamic QR hợp lệ trong giờ")
    void testAttendanceState_DynamicQr_Accepted() throws Exception {
        Event event = createPublishedEvent(5, true);

        // 1. Đăng ký trước
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // 2. Event Owner lấy mã QR động
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String qrToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Sinh viên quét mã QR -> 200 ACCEPTED
        String body = objectMapper.writeValueAsString(new CheckInRequest(qrToken));
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Kiểm tra CSDL
        boolean hasAccepted = attendanceRepository.existsByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED");
        assertThat(hasAccepted).isTrue();
    }

    @Test
    @DisplayName("STTC-ATT-02: Chuyển đổi sang REJECTED khi quét mã QR đã hết hạn (> 3s grace) -> 422 QR_EXPIRED")
    void testAttendanceState_ExpiredQrToken_Returns422QrExpired() throws Exception {
        Event event = createPublishedEvent(5, true);

        // Đăng ký
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Tạo mã QR đã quá hạn (50s trước, vượt quá cả rotationSeconds=10s và graceSeconds=3s)
        Instant past = Instant.now().minus(50, ChronoUnit.SECONDS);
        String expiredToken = qrTokenService.issue(event.getId(), past, event.getQrRotationSeconds());
        String body = objectMapper.writeValueAsString(new CheckInRequest(expiredToken));

        // Quét mã hết hạn -> 422 QR_EXPIRED
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QR_EXPIRED"));

        // Xác nhận không tạo bản ghi ACCEPTED
        boolean hasAccepted = attendanceRepository.existsByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED");
        assertThat(hasAccepted).isFalse();
    }

    @Test
    @DisplayName("STTC-ATT-03 & 04: Chuyển đổi sang REJECTED (Khóa) sau 3 lần thử thất bại -> Lần 4 bị chặn 409 ATTEMPT_LIMIT_REACHED")
    void testAttendanceState_ThreeFailedAttempts_TransitionsToRejectedLocked() throws Exception {
        Event event = createPublishedEvent(5, true);

        // Đăng ký
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Lấy token chuẩn nhưng sửa chữ ký để tạo lượt thử thất bại
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String validToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();
        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "XXXX";

        String badBody = objectMapper.writeValueAsString(new CheckInRequest(tamperedToken));

        // 3 lần thử sai -> đều bị 422 QR_BAD_SIGNATURE
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                            .header("Authorization", "Bearer " + student1Token)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badBody))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("QR_BAD_SIGNATURE"));
        }

        // Lần thử thứ 4 (kể cả dùng mã QR hợp lệ) -> Bị chặn 409 ATTEMPT_LIMIT_REACHED
        String validBody = objectMapper.writeValueAsString(new CheckInRequest(validToken));
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_LIMIT_REACHED"));

        // Xác nhận trạng thái trong CSDL: Không có bản ghi ACCEPTED nào
        boolean hasAccepted = attendanceRepository.existsByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED");
        assertThat(hasAccepted).isFalse();
    }

    @Test
    @DisplayName("STTC-ATT-05: Chuyển đổi bất hợp lệ: ACCEPTED không thể quét lại mã mới -> 409 ALREADY_CHECKED_IN")
    void testAttendanceState_IllegalTransition_AlreadyAcceptedCannotCheckInAgain() throws Exception {
        Event event = createPublishedEvent(5, true);

        // Đăng ký
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Lấy QR
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String qrToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();
        String body = objectMapper.writeValueAsString(new CheckInRequest(qrToken));

        // Điểm danh lần 1 -> 200 ACCEPTED
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Điểm danh lần 2 với Idempotency Key mới -> 409 ALREADY_CHECKED_IN
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));
    }

    @Test
    @DisplayName("STTC-ATT-06: Chuyển đổi bất hợp lệ: Chưa đăng ký không được phép điểm danh -> 403 NOT_REGISTERED")
    void testAttendanceState_IllegalTransition_NotRegisteredCannotAttend() throws Exception {
        Event event = createPublishedEvent(5, true);

        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String qrToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();
        String body = objectMapper.writeValueAsString(new CheckInRequest(qrToken));

        // Sinh viên 2 chưa đăng ký gọi điểm danh -> 403 NOT_REGISTERED
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student2Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_REGISTERED"));
    }

    @Test
    @DisplayName("STTC-ATT-07: Chuyển đổi bất hợp lệ: Quét mã QR ngoài khung giờ điểm danh -> 409 ATTENDANCE_NOT_OPEN")
    void testAttendanceState_IllegalTransition_OutsideWindow_Returns409AttendanceNotOpen() throws Exception {
        // Sự kiện chưa mở điểm danh (attendanceOpenAt = startAt = 2h nữa)
        Event event = createPublishedEvent(5, false);

        // Đăng ký thành công
        mockMvc.perform(post("/api/v1/events/{id}/registrations", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Lấy QR hợp lệ (token được tạo bình thường)
        String validToken = qrTokenService.issue(event.getId(), Instant.now(), event.getQrRotationSeconds());
        String body = objectMapper.writeValueAsString(new CheckInRequest(validToken));

        // Quét khi chưa đến giờ mở điểm danh -> 409 ATTENDANCE_NOT_OPEN
        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_NOT_OPEN"));

        boolean hasAccepted = attendanceRepository.existsByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED");
        assertThat(hasAccepted).isFalse();
    }

    @Test
    @DisplayName("STTC-ATT-DB: Ràng buộc bất biến CSDL: Unique index uq_attendances_valid ngăn tạo 2 ACCEPTED")
    void testAttendanceState_DatabaseInvariant_UniqueAcceptedPerStudentEvent() {
        Event event = createPublishedEvent(5, true);

        // Lần 1: Chèn trực tiếp ACCEPTED thành công
        jdbcTemplate.update(
                "INSERT INTO attendances (id, event_id, student_id, method, status, recorded_at, created_at) " +
                        "VALUES (:id, :eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now(), now())",
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("eventId", event.getId())
                        .addValue("studentId", student1Id)
        );

        // Lần 2: Cố tình chèn thêm 1 bản ghi ACCEPTED thứ hai cho cùng 1 sinh viên -> Bị CSDL ném lỗi vi phạm Unique Index
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "INSERT INTO attendances (id, event_id, student_id, method, status, recorded_at, created_at) " +
                            "VALUES (:id, :eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now(), now())",
                    new MapSqlParameterSource()
                            .addValue("id", UUID.randomUUID())
                            .addValue("eventId", event.getId())
                            .addValue("studentId", student1Id)
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
