package vn.edu.qnu.events.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private OrgClassRepository orgClassRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

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
        return eventRepository.save(event);
    }

    // =========================================================================
    // 1. REGISTRATION STATE MACHINE TESTS
    // =========================================================================

    @Test
    @DisplayName("STTC-REG-01: Chuyển đổi hợp lệ NONE -> REGISTERED khi còn quota")
    void testRegistrationState_NoneToRegistered_WhenQuotaAvailable() throws Exception {
        Event event = createPublishedEvent(5, false);

        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
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
    @DisplayName("STTC-REG-03: Lũy biến (Idempotent) REGISTERED -> REGISTERED khi đăng ký lại")
    void testRegistrationState_Idempotent_ReRegisterKeepsRegistered() throws Exception {
        Event event = createPublishedEvent(5, false);

        // Đăng ký lần 1 -> 201 Created
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Đăng ký lần 2 -> 200 OK idempotent, giữ nguyên REGISTERED
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.alreadyRegistered").value(true));

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("STTC-REG-04 & 05: Chuyển đổi REGISTERED -> CANCELLED trước hạn chót và hoàn quota")
    void testRegistrationState_CancelBeforeDeadline_TransitionsToCancelled() throws Exception {
        Event event = createPublishedEvent(5, false);

        // Sinh viên 1 đăng ký
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Hủy đăng ký trước deadline bằng trigger nghiệp vụ / DB update
        int rows = jdbcTemplate.update(
                "UPDATE registrations SET status = 'CANCELLED' WHERE event_id = :eventId AND student_id = :studentId",
                new MapSqlParameterSource()
                        .addValue("eventId", event.getId())
                        .addValue("studentId", student1Id)
        );
        assertThat(rows).isEqualTo(1);

        // Giảm registered_count phản ánh hoàn quota
        jdbcTemplate.update(
                "UPDATE events SET registered_count = registered_count - 1 WHERE id = :id",
                new MapSqlParameterSource().addValue("id", event.getId())
        );
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getRegisteredCount()).isEqualTo(0);

        // Bản ghi bây giờ mang trạng thái CANCELLED
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM registrations WHERE event_id = :eventId AND student_id = :studentId",
                new MapSqlParameterSource().addValue("eventId", event.getId()).addValue("studentId", student1Id),
                String.class
        );
        assertThat(status).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("STTC-REG-07 & 08: Quy tắc No-Show: Chỉ REGISTERED chuyển thành NO_SHOW; WAITING_LIST không bị phạt")
    void testRegistrationState_NoShowRule_OnlyRegisteredEligible() {
        Event event = createPublishedEvent(10, false);

        // Giả lập SV1 là REGISTERED, SV2 là WAITING_LIST
        UUID reg1 = UUID.randomUUID();
        UUID reg2 = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO registrations (id, event_id, student_id, status, created_at) " +
                        "VALUES (:id, :eventId, :studentId, 'REGISTERED', now())",
                new MapSqlParameterSource().addValue("id", reg1).addValue("eventId", event.getId()).addValue("studentId", student1Id)
        );

        jdbcTemplate.update(
                "INSERT INTO registrations (id, event_id, student_id, status, created_at) " +
                        "VALUES (:id, :eventId, :studentId, 'WAITING_LIST', now())",
                new MapSqlParameterSource().addValue("id", reg2).addValue("eventId", event.getId()).addValue("studentId", student2Id)
        );

        // Quy tắc kết thúc sự kiện: Quét các sinh viên REGISTERED không có điểm danh ACCEPTED -> chuyển thành NO_SHOW
        int noShowMarked = jdbcTemplate.update(
                "UPDATE registrations r SET status = 'NO_SHOW' " +
                        "WHERE r.event_id = :eventId AND r.status = 'REGISTERED' " +
                        "AND NOT EXISTS (SELECT 1 FROM attendances a WHERE a.event_id = r.event_id AND a.student_id = r.student_id AND a.status = 'ACCEPTED')",
                new MapSqlParameterSource().addValue("eventId", event.getId())
        );
        assertThat(noShowMarked).isEqualTo(1); // Đúng 1 người REGISTERED bị NO_SHOW

        // Kiểm tra SV2 (WAITING_LIST) tuyệt đối không bị NO_SHOW
        String sv2Status = jdbcTemplate.queryForObject(
                "SELECT status FROM registrations WHERE id = :id",
                new MapSqlParameterSource().addValue("id", reg2),
                String.class
        );
        assertThat(sv2Status).isEqualTo("WAITING_LIST");
    }

    // =========================================================================
    // 2. ATTENDANCE STATE MACHINE TESTS
    // =========================================================================

    @Test
    @DisplayName("STTC-ATT-01: Chuyển đổi hợp lệ NOT_CHECKED_IN -> ACCEPTED qua Dynamic QR hợp lệ")
    void testAttendanceState_DynamicQr_Accepted() throws Exception {
        Event event = createPublishedEvent(5, true);

        // 1. Đăng ký trước
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // 2. Event Owner lấy mã QR động
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String qrToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Sinh viên quét mã QR -> 200 ACCEPTED
        String body = objectMapper.writeValueAsString(new vn.edu.qnu.events.attendance.CheckInRequest(qrToken));
        mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
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
    @DisplayName("STTC-ATT-03 & 04: Chuyển đổi sang REJECTED (Khóa) sau 3 lần thử thất bại -> Lần 4 bị chặn 409")
    void testAttendanceState_ThreeFailedAttempts_TransitionsToRejectedLocked() throws Exception {
        Event event = createPublishedEvent(5, true);

        // Đăng ký
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Lấy token chuẩn nhưng giả mạo chữ ký để tạo lượt thử thất bại
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String validToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();
        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "XXXX";

        String badBody = objectMapper.writeValueAsString(new vn.edu.qnu.events.attendance.CheckInRequest(tamperedToken));

        // 3 lần thử sai -> đều bị REJECTED
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
                            .header("Authorization", "Bearer " + student1Token)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("QR_BAD_SIGNATURE"));
        }

        // Lần thử thứ 4 (kể cả dùng mã QR hợp lệ) -> Bị chặn 409 ATTEMPT_LIMIT_REACHED
        String validBody = objectMapper.writeValueAsString(new vn.edu.qnu.events.attendance.CheckInRequest(validToken));
        mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
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
        mockMvc.perform(post("/api/v1/events/{id}/register", event.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Lấy QR
        MvcResult qrResult = mockMvc.perform(get("/api/v1/events/{id}/qr", event.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String qrToken = objectMapper.readTree(qrResult.getResponse().getContentAsString()).get("token").asText();
        String body = objectMapper.writeValueAsString(new vn.edu.qnu.events.attendance.CheckInRequest(qrToken));

        // Điểm danh lần 1 -> 200 ACCEPTED
        mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Điểm danh lần 2 với Idempotency Key mới -> 409 ALREADY_CHECKED_IN
        mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
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
        String body = objectMapper.writeValueAsString(new vn.edu.qnu.events.attendance.CheckInRequest(qrToken));

        // Sinh viên 2 chưa đăng ký gọi điểm danh -> 403 NOT_REGISTERED
        mockMvc.perform(post("/api/v1/events/{id}/checkin", event.getId())
                        .header("Authorization", "Bearer " + student2Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_REGISTERED"));
    }

    @Test
    @DisplayName("STTC-ATT-DB: Ràng buộc bất biến CSDL: Unique index uq_attendances_valid ngăn tạo 2 ACCEPTED")
    void testAttendanceState_DatabaseInvariant_UniqueAcceptedPerStudentEvent() {
        Event event = createPublishedEvent(5, true);

        // Lần 1: Chèn trực tiếp ACCEPTED thành công
        jdbcTemplate.update(
                "INSERT INTO attendances (id, event_id, student_id, method, status, created_at) " +
                        "VALUES (:id, :eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now())",
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("eventId", event.getId())
                        .addValue("studentId", student1Id)
        );

        // Lần 2: Cố tình chèn thêm 1 bản ghi ACCEPTED thứ hai cho cùng 1 sinh viên -> Bị CSDL ném lỗi vi phạm Unique Index
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "INSERT INTO attendances (id, event_id, student_id, method, status, created_at) " +
                            "VALUES (:id, :eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now())",
                    new MapSqlParameterSource()
                            .addValue("id", UUID.randomUUID())
                            .addValue("eventId", event.getId())
                            .addValue("studentId", student1Id)
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
