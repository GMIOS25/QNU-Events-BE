package vn.edu.qnu.events.registration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.qnu.events.audit.AuditLogRepository;
import vn.edu.qnu.events.auth.AppUser;
import vn.edu.qnu.events.auth.AppUserRepository;
import vn.edu.qnu.events.auth.AuthService;
import vn.edu.qnu.events.auth.AuthSession;
import vn.edu.qnu.events.auth.AuthSessionRepository;
import vn.edu.qnu.events.auth.JwtService;
import vn.edu.qnu.events.auth.LoginRequest;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.student.OrgClass;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RegistrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private OrgClassRepository orgClassRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String ownerToken;
    private String student1Token;
    private String student2Token;
    private String student3Token; // in class 2
    private UUID k49Class1Id;
    private UUID k49Class2Id;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();
        student1Token = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();
        student2Token = authService.login(new LoginRequest("sv002", "Demo@12345")).accessToken();
        student3Token = authService.login(new LoginRequest("sv003", "Demo@12345")).accessToken();

        OrgClass class1 = orgClassRepository.findByCode("K49CNTT1").orElseThrow();
        OrgClass class2 = orgClassRepository.findByCode("K49CNTT2").orElseThrow();
        k49Class1Id = class1.getId();
        k49Class2Id = class2.getId();

        AppUser owner = appUserRepository.findByLoginId("owner01").orElseThrow();
        ownerId = owner.getId();
    }

    private Event createPublishedEvent(String title, int quota, Instant regDeadline, Set<UUID> audienceClassIds) {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant deadline = regDeadline != null ? regDeadline : startAt.minus(2, ChronoUnit.HOURS);
        Instant attOpen = startAt.minus(15, ChronoUnit.MINUTES);
        Instant attClose = endAt.plus(30, ChronoUnit.MINUTES);

        Event event = new Event(
                UUID.randomUUID(),
                ownerId,
                title,
                "Mô tả sự kiện",
                "Hội trường B",
                startAt,
                endAt,
                deadline,
                attOpen,
                attClose,
                AttendanceMethod.DYNAMIC_QR,
                10,
                3,
                quota,
                "Workshop",
                5,
                audienceClassIds
        );
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(now);
        return eventRepository.saveAndFlush(event);
    }

    private String createStudentToken(String loginId, UUID classId) {
        UUID userId = UUID.randomUUID();
        AppUser user = appUserRepository.save(new AppUser(
                userId,
                loginId,
                passwordEncoder.encode("Demo@12345"),
                "Student " + loginId,
                "SV_" + UUID.randomUUID().toString().substring(0, 8),
                classId,
                Set.of("STUDENT")
        ));
        AuthSession session = authSessionRepository.save(new AuthSession(
                null,
                userId,
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.DAYS)
        ));
        return jwtService.createToken(user.getId(), session.getId(), List.of("STUDENT"), classId, session.getExpiresAt());
    }

    @Test
    @DisplayName("1. Sinh viên đăng ký lần đầu thành công -> 201 Created và registered_count tăng")
    void singleStudentRegistrationSuccess() throws Exception {
        Event event = createPublishedEvent("Sự kiện Đăng ký thành công", 10, null, Set.of(k49Class1Id));

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.alreadyRegistered").value(false));

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("2. Sinh viên đăng ký lại lần 2 -> 200 OK idempotent, alreadyRegistered=true, không tăng registered_count")
    void duplicateRegistrationReturns200AlreadyRegistered() throws Exception {
        Event event = createPublishedEvent("Sự kiện Đăng ký lặp lại", 10, null, Set.of(k49Class1Id));

        // Lần 1: 201 Created
        MvcResult res1 = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alreadyRegistered").value(false))
                .andReturn();

        String regId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asText();

        // Lần 2: 200 OK idempotent với cùng id
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regId1))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.alreadyRegistered").value(true));

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. GET /events/{id}/registrations/me: 404 khi chưa đăng ký, 200 sau khi đăng ký")
    void getMyRegistrationSuccessAndNotFound() throws Exception {
        Event event = createPublishedEvent("Sự kiện Kiểm tra my registration", 10, null, Set.of(k49Class1Id));

        // Chưa đăng ký -> 404
        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations/me")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Đăng ký
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Đã đăng ký -> 200 OK
        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations/me")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.registeredAt", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("4. Sinh viên sai lớp đăng ký -> 403 NOT_IN_AUDIENCE")
    void wrongClassStudentCannotRegister() throws Exception {
        Event event = createPublishedEvent("Sự kiện dành riêng lớp CNTT1", 10, null, Set.of(k49Class1Id));

        // student3 thuộc K49CNTT2
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_IN_AUDIENCE"));
    }

    @Test
    @DisplayName("5. Sự kiện ở trạng thái DRAFT không thể đăng ký -> 409 EVENT_NOT_PUBLISHED")
    void draftEventCannotBeRegistered() throws Exception {
        Instant now = Instant.now();
        Event draftEvent = new Event(
                UUID.randomUUID(),
                ownerId,
                "Sự kiện bản nháp",
                "Mô tả",
                "Hội trường B",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.DAYS).minus(15, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                AttendanceMethod.DYNAMIC_QR,
                10,
                3,
                10,
                "Workshop",
                5,
                Set.of(k49Class1Id)
        );
        eventRepository.saveAndFlush(draftEvent);

        mockMvc.perform(post("/api/v1/events/" + draftEvent.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_PUBLISHED"));
    }

    @Test
    @DisplayName("6. Hết hạn đăng ký -> 409 REGISTRATION_CLOSED")
    void pastDeadlineCannotBeRegistered() throws Exception {
        Instant pastDeadline = Instant.now().minus(1, ChronoUnit.HOURS);
        Event event = createPublishedEvent("Sự kiện đã hết hạn đăng ký", 10, pastDeadline, Set.of(k49Class1Id));

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"));
    }

    @Test
    @DisplayName("7. Chưa đăng nhập không thể đăng ký -> 401 UNAUTHENTICATED")
    void unauthenticatedCannotRegister() throws Exception {
        Event event = createPublishedEvent("Sự kiện không đăng nhập", 10, null, Set.of(k49Class1Id));

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("8. Chủ sự kiện (EVENT_OWNER) gọi đăng ký -> 403 FORBIDDEN")
    void eventOwnerCannotRegister() throws Exception {
        Event event = createPublishedEvent("Sự kiện owner cố đăng ký", 10, null, Set.of(k49Class1Id));

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("9. Đăng ký thất bại CAPACITY_FULL không để lại dòng registrations mồ côi")
    void capacityFullRollbackLeavesNoOrphanRow() throws Exception {
        Event event = createPublishedEvent("Sự kiện 1 chỗ duy nhất", 1, null, Set.of(k49Class1Id));

        // SV1 đăng ký thành công suất duy nhất
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // SV2 đăng ký khi đã đầy -> 409 CAPACITY_FULL
        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_FULL"));

        // Kiểm tra số dòng registrations trong DB cho event này chính xác là 1 (không có dòng mồ côi của SV2)
        long count = registrationRepository.countByEventId(event.getId());
        assertThat(count).isEqualTo(1L);

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("10. Concurrency: quota=1, 2 sinh viên tranh suất đồng thời -> đúng 1 người được, người kia CAPACITY_FULL")
    void concurrencyRaceConditionQuota1() throws Exception {
        Event event = createPublishedEvent("Sự kiện Concurrency Quota 1", 1, null, Set.of(k49Class1Id));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullCount = new AtomicInteger(0);

        String[] tokens = {student1Token, student2Token};

        for (String token : tokens) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                                    .header("Authorization", "Bearer " + token))
                            .andReturn();
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        fullCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(fullCount.get()).isEqualTo(1);

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(1);
        assertThat(registrationRepository.countByEventId(event.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("11. Concurrency: quota=5, 30 sinh viên tranh suất đồng thời -> đúng 5 REGISTERED, 25 CAPACITY_FULL")
    void concurrencyRaceConditionQuota5With30Students() throws Exception {
        Event event = createPublishedEvent("Sự kiện Concurrency Quota 5 với 30 sinh viên", 5, null, Set.of(k49Class1Id));

        int totalStudents = 30;
        List<String> tokens = new ArrayList<>(totalStudents);
        for (int i = 0; i < totalStudents; i++) {
            tokens.add(createStudentToken("sim_sv_" + UUID.randomUUID().toString().substring(0, 8), k49Class1Id));
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalStudents);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullCount = new AtomicInteger(0);

        for (String token : tokens) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                                    .header("Authorization", "Bearer " + token))
                            .andReturn();
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        fullCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(fullCount.get()).isEqualTo(25);

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(5);
        assertThat(registrationRepository.countByEventId(event.getId())).isEqualTo(5L);
    }

    @Test
    @DisplayName("12. Cùng 1 sinh viên gửi 10 request song song -> đúng 1 dòng được tạo, 9 request trả alreadyRegistered=true")
    void sameStudentParallel10Requests() throws Exception {
        Event event = createPublishedEvent("Sự kiện 10 request trùng", 10, null, Set.of(k49Class1Id));

        int parallelRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(parallelRequests);

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger alreadyRegisteredCount = new AtomicInteger(0);

        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                                    .header("Authorization", "Bearer " + student1Token))
                            .andReturn();
                    int statusCode = result.getResponse().getStatus();
                    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                    if (statusCode == 201 && !node.get("alreadyRegistered").asBoolean()) {
                        createdCount.incrementAndGet();
                    } else if (statusCode == 200 && node.get("alreadyRegistered").asBoolean()) {
                        alreadyRegisteredCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(createdCount.get()).isEqualTo(1);
        assertThat(alreadyRegisteredCount.get()).isEqualTo(9);

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRegisteredCount()).isEqualTo(1);
        assertThat(registrationRepository.countByEventId(event.getId())).isEqualTo(1L);
    }
}

