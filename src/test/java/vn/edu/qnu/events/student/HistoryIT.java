package vn.edu.qnu.events.student;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.jayway.jsonpath.JsonPath;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

    @AfterEach
    void tearDown() {
        attendanceRepository.deleteAll();
        registrationRepository.deleteAll();
    }

    private Event createEvent(String title, int points) {
        Instant now = Instant.now();
        Instant startAt = now.plus(2, ChronoUnit.DAYS);
        return createEventWithCustom(title, points, "Hội thảo khoa học", startAt);
    }

    private Event createEventWithCustom(String title, int points, String activityType, Instant startAt) {
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
                activityType,
                points,
                Set.of(k49ClassId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
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
    @DisplayName("3. Sinh viên chưa đăng ký sự kiện nào -> 200 OK danh sách rỗng (content=[])")
    void studentWithoutRegistrationsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
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
                .andExpect(jsonPath("$.content[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện chưa điểm danh"))
                .andExpect(jsonPath("$.content[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("NOT_CHECKED_IN"))
                .andExpect(jsonPath("$.content[0].attendanceAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].activityPoints").value(10))
                .andExpect(jsonPath("$.content[0].method").value("DYNAMIC_QR"));
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
                .andExpect(jsonPath("$.content[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện đã điểm danh"))
                .andExpect(jsonPath("$.content[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.content[0].attendanceAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.content[0].activityPoints").value(15));
    }

    @Test
    @DisplayName("6. Phân lập dữ liệu: Sinh viên A không nhìn thấy lịch sử của sinh viên B")
    void studentDataIsolation() throws Exception {
        Event event = createEvent("Sự kiện chỉ sinh viên 2 tham gia", 20);
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), event.getId(), student2Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Sinh viên 3 kiểm tra -> không có sự kiện này trong content
        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.eventId == '%s')]", event.getId().toString()).doesNotExist());
    }

    @Test
    @DisplayName("7. Phân trang: Hỗ trợ page, size, totalElements, totalPages")
    void paginationWorksCorrectly() throws Exception {
        Event e1 = createEvent("Sự kiện trang 1", 10);
        Event e2 = createEvent("Sự kiện trang 2", 15);

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), e1.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), e2.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Lấy trang 0 với size 1
        mockMvc.perform(get("/api/v1/me/history?page=0&size=1")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1));

        // Lấy trang 1 với size 1
        mockMvc.perform(get("/api/v1/me/history?page=1&size=1")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("8. Bộ lọc: Lọc theo attendanceStatus (ACCEPTED và NOT_CHECKED_IN)")
    void filterByAttendanceStatus() throws Exception {
        Event attendedEvent = createEvent("Sự kiện ĐÃ điểm danh", 10);
        Event unattendedEvent = createEvent("Sự kiện CHƯA điểm danh", 10);

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), attendedEvent.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        attendanceRepository.saveAndFlush(new Attendance(
                UUID.randomUUID(), attendedEvent.getId(), student1Id, "DYNAMIC_QR", "ACCEPTED", Instant.now()
        ));

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), unattendedEvent.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Lọc chỉ sự kiện ACCEPTED
        mockMvc.perform(get("/api/v1/me/history?attendanceStatus=ACCEPTED")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventId").value(attendedEvent.getId().toString()))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("ACCEPTED"));

        // Lọc chỉ sự kiện NOT_CHECKED_IN
        mockMvc.perform(get("/api/v1/me/history?attendanceStatus=NOT_CHECKED_IN")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventId").value(unattendedEvent.getId().toString()))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("NOT_CHECKED_IN"));
    }

    @Test
    @DisplayName("9. Bộ lọc: Lọc theo activityType")
    void filterByActivityType() throws Exception {
        Event seminar = createEventWithCustom("Hội thảo AI", 10, "Hội thảo học thuật", Instant.now().plus(2, ChronoUnit.DAYS));
        Event volunteer = createEventWithCustom("Tiếp sức mùa thi", 15, "Hoạt động tình nguyện", Instant.now().plus(3, ChronoUnit.DAYS));

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), seminar.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), volunteer.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Lọc theo "Hoạt động tình nguyện"
        mockMvc.perform(get("/api/v1/me/history?activityType=Hoạt động tình nguyện")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Tiếp sức mùa thi"))
                .andExpect(jsonPath("$.content[0].activityType").value("Hoạt động tình nguyện"));
    }

    @Test
    @DisplayName("10. Bộ lọc: Lọc theo upcoming (sắp diễn ra vs đã diễn ra)")
    void filterByUpcoming() throws Exception {
        Event pastEvent = createEventWithCustom("Sự kiện quá khứ", 10, "Học thuật", Instant.now().minus(5, ChronoUnit.DAYS));
        Event futureEvent = createEventWithCustom("Sự kiện tương lai", 10, "Học thuật", Instant.now().plus(5, ChronoUnit.DAYS));

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), pastEvent.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), futureEvent.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // upcoming = true -> chỉ sự kiện tương lai
        mockMvc.perform(get("/api/v1/me/history?upcoming=true")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện tương lai"));

        // upcoming = false -> chỉ sự kiện quá khứ
        mockMvc.perform(get("/api/v1/me/history?upcoming=false")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện quá khứ"));
    }

    @Test
    @DisplayName("11. Bộ lọc: Lọc theo năm học và học kỳ (academicYear & semester)")
    void filterByAcademicYearAndSemester() throws Exception {
        // Sự kiện HK1 năm học 2026-2027 (Tháng 10/2026)
        Instant sem1Time = Instant.parse("2026-10-15T09:00:00Z");
        Event sem1Event = createEventWithCustom("Sự kiện HK1", 10, "Học thuật", sem1Time);

        // Sự kiện HK2 năm học 2026-2027 (Tháng 03/2027)
        Instant sem2Time = Instant.parse("2027-03-20T09:00:00Z");
        Event sem2Event = createEventWithCustom("Sự kiện HK2", 10, "Học thuật", sem2Time);

        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), sem1Event.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), sem2Event.getId(), student1Id, RegistrationStatus.REGISTERED, Instant.now(), Instant.now()
        ));

        // Lọc HK1 năm học 2026-2027
        mockMvc.perform(get("/api/v1/me/history?academicYear=2026-2027&semester=1")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện HK1"));

        // Lọc HK2 năm học 2026-2027
        mockMvc.perform(get("/api/v1/me/history?academicYear=2026-2027&semester=2")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Sự kiện HK2"));

        // Lọc cả năm học 2026-2027 (không truyền semester) -> cả 2 sự kiện
        mockMvc.perform(get("/api/v1/me/history?academicYear=2026-2027")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    private void saveRegistration(UUID eventId, UUID studentId, RegistrationStatus status, Instant createdAt) {
        registrationRepository.saveAndFlush(new Registration(
                UUID.randomUUID(), eventId, studentId, status, createdAt, createdAt
        ));
    }

    @Test
    @DisplayName("12. Hủy rồi đăng ký lại cùng sự kiện -> chỉ 1 item, registrationStatus REGISTERED, totalElements 1")
    void cancelledThenReRegisteredReturnsSingleItem() throws Exception {
        Event event = createEvent("Sự kiện đăng ký lại", 10);
        Instant now = Instant.now();
        saveRegistration(event.getId(), student1Id, RegistrationStatus.CANCELLED, now.minus(1, ChronoUnit.HOURS));
        saveRegistration(event.getId(), student1Id, RegistrationStatus.REGISTERED, now);

        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("NOT_CHECKED_IN"));
    }

    @Test
    @DisplayName("13. Hủy, đăng ký lại rồi điểm danh -> 1 item ACCEPTED, lọc attendanceStatus=ACCEPTED cũng 1 item")
    void cancelledThenReRegisteredAndAttendedReturnsSingleAcceptedItem() throws Exception {
        Event event = createEvent("Sự kiện đăng ký lại và điểm danh", 10);
        Instant now = Instant.now();
        saveRegistration(event.getId(), student1Id, RegistrationStatus.CANCELLED, now.minus(1, ChronoUnit.HOURS));
        saveRegistration(event.getId(), student1Id, RegistrationStatus.REGISTERED, now);
        attendanceRepository.saveAndFlush(new Attendance(
                UUID.randomUUID(), event.getId(), student1Id, "DYNAMIC_QR", "ACCEPTED", now
        ));

        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/me/history?attendanceStatus=ACCEPTED")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("14. Chỉ có 1 đăng ký đã CANCELLED -> vẫn hiện 1 item CANCELLED, NOT_CHECKED_IN")
    void onlyCancelledRegistrationStillShown() throws Exception {
        Event event = createEvent("Sự kiện đã hủy", 10);
        saveRegistration(event.getId(), student1Id, RegistrationStatus.CANCELLED, Instant.now());

        mockMvc.perform(get("/api/v1/me/history")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].registrationStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.content[0].attendanceStatus").value("NOT_CHECKED_IN"));
    }

    @Test
    @DisplayName("15. Phân trang sau khử trùng: 3 sự kiện (1 sự kiện có 2 registration), size=2 -> totalElements 3, totalPages 2, không lặp eventId")
    void paginationCountsEventsNotRegistrations() throws Exception {
        Instant base = Instant.now().plus(2, ChronoUnit.DAYS);
        Event e1 = createEventWithCustom("Sự kiện phân trang 1", 10, "Hội thảo khoa học", base);
        Event e2 = createEventWithCustom("Sự kiện phân trang 2", 10, "Hội thảo khoa học", base.plus(1, ChronoUnit.DAYS));
        Event e3 = createEventWithCustom("Sự kiện phân trang 3", 10, "Hội thảo khoa học", base.plus(2, ChronoUnit.DAYS));

        Instant now = Instant.now();
        saveRegistration(e1.getId(), student1Id, RegistrationStatus.REGISTERED, now);
        saveRegistration(e2.getId(), student1Id, RegistrationStatus.CANCELLED, now.minus(1, ChronoUnit.HOURS));
        saveRegistration(e2.getId(), student1Id, RegistrationStatus.REGISTERED, now);
        saveRegistration(e3.getId(), student1Id, RegistrationStatus.REGISTERED, now);

        String page0 = mockMvc.perform(get("/api/v1/me/history?page=0&size=2")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn().getResponse().getContentAsString();

        String page1 = mockMvc.perform(get("/api/v1/me/history?page=1&size=2")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn().getResponse().getContentAsString();

        List<String> ids0 = JsonPath.read(page0, "$.content[*].eventId");
        List<String> ids1 = JsonPath.read(page1, "$.content[*].eventId");
        Set<String> all = new HashSet<>(ids0);
        all.addAll(ids1);

        assertThat(all).hasSize(3)
                .containsExactlyInAnyOrder(e1.getId().toString(), e2.getId().toString(), e3.getId().toString());
    }
}
