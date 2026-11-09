package vn.edu.qnu.events.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.qnu.events.attendance.Attendance;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HistoryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OrgClassRepository orgClassRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private String student1Token;
    private String student2Token;
    private String student3Token;
    private String ownerToken;

    private UUID student1Id;
    private UUID student2Id;
    private UUID student3Id;
    private UUID ownerId;
    private UUID k49ClassId;

    @BeforeEach
    void setUp() {
        student1Token = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();
        student2Token = authService.login(new LoginRequest("sv002", "Demo@12345")).accessToken();
        student3Token = authService.login(new LoginRequest("sv003", "Demo@12345")).accessToken();
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();

        student1Id = appUserRepository.findByLoginId("sv001").orElseThrow().getId();
        student2Id = appUserRepository.findByLoginId("sv002").orElseThrow().getId();
        student3Id = appUserRepository.findByLoginId("sv003").orElseThrow().getId();
        ownerId = appUserRepository.findByLoginId("owner01").orElseThrow().getId();

        k49ClassId = orgClassRepository.findByCode("K49CNTT1").orElseThrow().getId();
    }

    private Event createEvent(String title, int points) {
        Instant now = Instant.now();
        Instant startAt = now.plus(2, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant regDeadline = startAt.minus(2, ChronoUnit.HOURS);
        Instant attOpen = startAt.minus(15, ChronoUnit.MINUTES);
        Instant attClose = endAt.plus(15, ChronoUnit.MINUTES);

        Event event = new Event(
                UUID.randomUUID(),
                ownerId,
                title,
                "Mô tả sự kiện",
                "Hội trường A",
                startAt,
                endAt,
                regDeadline,
                attOpen,
                attClose,
                AttendanceMethod.DYNAMIC_QR,
                10,
                3,
                100,
                "Hội thảo khoa học",
                points,
                Set.of(k49ClassId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(now);
        return eventRepository.saveAndFlush(event);
    }

    @Test
    @DisplayName("1. Chưa đăng nhập gọi GET /api/v1/me/history -> 401 UNAUTHENTICATED")
    void unauthenticatedCallFails() throws Exception {
        mockMvc.perform(get("/api/v1/me/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("2. Role EVENT_OWNER gọi GET /api/v1/me/history -> 403 FORBIDDEN")
    void ownerRoleCallFails() throws Exception {
        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("3. Sinh viên chưa đăng ký sự kiện nào -> 200 OK danh sách rỗng []")
    void studentWithoutRegistrationsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("4. Sinh viên đã đăng ký nhưng chưa điểm danh -> attendanceStatus: NOT_CHECKED_IN, attendanceAt: null")
    void registeredEventShowsNotCheckedIn() throws Exception {
        Event event = createEvent("Sự kiện chưa điểm danh", 10);
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), event.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("Sự kiện chưa điểm danh"))
                .andExpect(jsonPath("$[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$[0].attendanceStatus").value("NOT_CHECKED_IN"))
                .andExpect(jsonPath("$[0].attendanceAt").doesNotExist())
                .andExpect(jsonPath("$[0].activityPoints").value(10))
                .andExpect(jsonPath("$[0].method").value("DYNAMIC_QR"));
    }

    @Test
    @DisplayName("5. Sinh viên đã đăng ký và đã điểm danh -> attendanceStatus: ACCEPTED, attendanceAt không null")
    void registeredAndAttendedEventShowsAccepted() throws Exception {
        Event event = createEvent("Sự kiện đã điểm danh", 15);
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), event.getId(), student2Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        attendanceRepository.saveAndFlush(new Attendance(
                UUID.randomUUID(), event.getId(), student2Id, "DYNAMIC_QR", "ACCEPTED", Instant.now()
        ));

        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("Sự kiện đã điểm danh"))
                .andExpect(jsonPath("$[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$[0].attendanceStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].attendanceAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$[0].activityPoints").value(15));
    }

    @Test
    @DisplayName("6. Phân lập dữ liệu: Sinh viên A không nhìn thấy lịch sử của sinh viên B")
    void studentDataIsolation() throws Exception {
        Event event = createEvent("Sự kiện chỉ sinh viên 2 tham gia", 20);
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), event.getId(), student2Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Sinh viên 3 kiểm tra -> không có sự kiện này
        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventId == '%s')]", event.getId().toString()).doesNotExist());
    }
}

