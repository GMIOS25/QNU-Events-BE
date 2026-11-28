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
import vn.edu.qnu.events.audit.AuditLog;
import vn.edu.qnu.events.audit.AuditLogRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GpsAttendanceIT {

    // Tọa độ Hội trường A - Trường Đại học Quy Nhơn (170 An Dương Vương)
    private static final double QNU_LAT = 13.758900;
    private static final double QNU_LON = 109.218300;
    private static final int DEFAULT_RADIUS_METERS = 60;

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
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String ownerToken;
    private String student1Token;
    private UUID k49ClassId;
    private UUID ownerId;
    private UUID student1Id;

    @BeforeEach
    void setUp() {
        ownerToken = authService.login(new LoginRequest("owner01", "Demo@12345")).accessToken();
        student1Token = authService.login(new LoginRequest("sv001", "Demo@12345")).accessToken();

        OrgClass k49 = orgClassRepository.findByCode("K49CNTT1").orElseThrow();
        k49ClassId = k49.getId();

        ownerId = appUserRepository.findByLoginId("owner01").orElseThrow().getId();
        student1Id = appUserRepository.findByLoginId("sv001").orElseThrow().getId();
    }

    private Event createPublishedGpsEvent(
            Instant attOpen,
            Instant attClose,
            Double gpsLat,
            Double gpsLng,
            Integer radiusMeters
    ) {
        Instant now = Instant.now();
        Instant startAt = now.plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant regDeadline = startAt.minus(2, ChronoUnit.HOURS);

        Event event = new Event(
                UUID.randomUUID(),
                ownerId,
                "Sự kiện Điểm danh GPS",
                "Mô tả sự kiện GPS",
                "Sân vận động ĐH Quy Nhơn",
                startAt,
                endAt,
                regDeadline,
                attOpen,
                attClose,
                AttendanceMethod.GPS,
                10,
                3,
                50,
                "Hoạt động phong trào",
                5,
                Set.of(k49ClassId)
        );
        event.setGpsLat(gpsLat);
        event.setGpsLng(gpsLng);
        event.setAllowedRadiusMeters(radiusMeters);
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setPublishedBy(ownerId);
        return eventRepository.saveAndFlush(event);
    }

    private Event createOpenGpsEventAndRegisterStudent1() {
        Instant now = Instant.now();
        Event event = createPublishedGpsEvent(
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(30, ChronoUnit.MINUTES),
                QNU_LAT,
                QNU_LON,
                DEFAULT_RADIUS_METERS
        );
        registerStudent(event.getId(), student1Id);
        return event;
    }

    private void registerStudent(UUID eventId, UUID studentId) {
        Registration reg = new Registration(
                UUID.randomUUID(),
                eventId,
                studentId,
                RegistrationStatus.REGISTERED,
                Instant.now(),
                Instant.now()
        );
        registrationRepository.saveAndFlush(reg);
    }

    private String gpsJson(double lat, double lon, double accuracy) {
        return String.format(java.util.Locale.US,
                "{\"latitude\":%.8f,\"longitude\":%.8f,\"accuracyMeters\":%.2f}",
                lat, lon, accuracy);
    }

    @Test
    @DisplayName("1. Điểm danh GPS thành công trong bán kính (60m) với sai số tốt -> 200 ACCEPTED, lưu tọa độ & audit log")
    void checkInGps_withinRadiusAndGoodAccuracy_returns200AndPersistsAttendance() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();
        // Cách tâm ~11.1m (< 60m), accuracy = 14.5m (<= 100m)
        double studentLat = QNU_LAT + 0.0001;

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(studentLat, QNU_LON, 14.5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty())
                .andExpect(jsonPath("$.allowedRadiusMeters").value(60))
                .andExpect(jsonPath("$.distanceMeters").isNumber())
                .andExpect(jsonPath("$.suspicious").doesNotExist());

        Attendance saved = attendanceRepository
                .findByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED")
                .orElseThrow();
        assertThat(saved.getMethod()).isEqualTo("GPS");
        assertThat(saved.getGpsLat()).isCloseTo(studentLat, within(1e-7));
        assertThat(saved.getGpsLng()).isCloseTo(QNU_LON, within(1e-7));
        assertThat(saved.getGpsAccuracyMeters()).isEqualTo(14.5);
        assertThat(saved.getGpsDistanceMeters()).isCloseTo(11.12, within(0.2));
        assertThat(saved.isSuspiciousFlag()).isFalse();
        assertThat(saved.getSuspiciousReason()).isNull();

        List<AuditLog> logs = auditLogRepository.findByTargetTypeAndTargetIdOrderByOccurredAtAsc(
                "ATTENDANCE", saved.getId().toString()
        );
        assertThat(logs).isNotEmpty();
        assertThat(logs.getFirst().getAction()).isEqualTo("ATTENDANCE_GPS_ACCEPT");
        assertThat(logs.getFirst().getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("2. Điểm danh GPS trong bán kính nhưng sai số cao (accuracy = 250m > 100m) -> vẫn 200 ACCEPTED nhưng gắn suspicious_flag = true trong DB (BR-ATT-07)")
    void checkInGps_withinRadiusButLowAccuracy_acceptsAndFlagsSuspiciousInDatabase() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT + 0.0002, QNU_LON, 250.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.suspicious").doesNotExist());

        Attendance saved = attendanceRepository
                .findByEventIdAndStudentIdAndStatus(event.getId(), student1Id, "ACCEPTED")
                .orElseThrow();
        assertThat(saved.isSuspiciousFlag()).isTrue();
        assertThat(saved.getSuspiciousReason()).isEqualTo("LOW_GPS_ACCURACY");

        List<AuditLog> logs = auditLogRepository.findByTargetTypeAndTargetIdOrderByOccurredAtAsc(
                "ATTENDANCE", saved.getId().toString()
        );
        assertThat(logs).isNotEmpty();
        assertThat(logs.getFirst().getDetails())
                .containsEntry("suspicious", true)
                .containsEntry("suspiciousReason", "LOW_GPS_ACCURACY");
    }

    @Test
    @DisplayName("3. Điểm danh GPS sát ngoài biên (60.04m > 60m) -> 422 GPS_OUT_OF_RANGE và distanceMeters trả về > 60.0 (không làm tròn về 60.0)")
    void checkInGps_justOutsideBoundary60Point04Meters_returns422WithExactDistanceGreaterThan60() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();
        double deltaLatFor60Point04Meters = 60.04 / 111_194.9266;

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT + deltaLatFor60Point04Meters, QNU_LON, 10.0)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GPS_OUT_OF_RANGE"))
                .andExpect(jsonPath("$.details.method").value("GPS"))
                .andExpect(jsonPath("$.details.attemptCount").value(1))
                .andExpect(jsonPath("$.details.allowedRadiusMeters").value(60))
                .andExpect(jsonPath("$.details.distanceMeters", greaterThan(60.0)));
    }

    @Test
    @DisplayName("4. Điểm danh GPS ngoài bán kính 3 lần -> trừ đủ 3 lượt (422), lần thứ 4 bị chặn 409 ATTEMPT_LIMIT_REACHED (counted = false)")
    void checkInGps_outsideRadiusThreeTimes_locksOnFourthAttempt() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();
        double farLat = QNU_LAT + 0.0015; // ~166.8m (> 60m)

        for (int i = 1; i <= 3; i++) {
            boolean expectedCanRetry = i < 3;
            int expectedRemaining = 3 - i;
            mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                            .header("Authorization", "Bearer " + student1Token)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gpsJson(farLat, QNU_LON, 15.0)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("GPS_OUT_OF_RANGE"))
                    .andExpect(jsonPath("$.details.method").value("GPS"))
                    .andExpect(jsonPath("$.details.attemptCount").value(i))
                    .andExpect(jsonPath("$.details.maxAttempts").value(3))
                    .andExpect(jsonPath("$.details.remainingAttempts").value(expectedRemaining))
                    .andExpect(jsonPath("$.details.canRetry").value(expectedCanRetry));
        }

        // Lần thứ 4 ngay cả khi đứng đúng tâm sự kiện vẫn bị chặn 409 ATTEMPT_LIMIT_REACHED
        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_LIMIT_REACHED"))
                .andExpect(jsonPath("$.details.attemptCount").value(3))
                .andExpect(jsonPath("$.details.remainingAttempts").value(0))
                .andExpect(jsonPath("$.details.canRetry").value(false));

        long countedInDb = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(event.getId(), student1Id);
        assertThat(countedInDb).isEqualTo(3L);
    }

    @Test
    @DisplayName("5. Tọa độ sinh viên sai (ngoài biên [-90, 90], accuracy âm, thiếu trường) -> trả 400 VALIDATION_FAILED (không trả 500, không trừ lượt)")
    void checkInGps_invalidStudentCoordinates_returns400AndDoesNotCountAttempt() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(95.0, QNU_LON, 10.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, -5.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        long countedInDb = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(event.getId(), student1Id);
        assertThat(countedInDb).isZero();
    }

    @Test
    @DisplayName("6. Sự kiện GPS chưa được cấu hình tọa độ tâm -> trả 409 GPS_NOT_CONFIGURED (không trả 500, không trừ lượt)")
    void checkInGps_whenEventMissingGpsCoordinates_returns409GpsNotConfigured() throws Exception {
        Instant now = Instant.now();
        Event unconfiguredEvent = createPublishedGpsEvent(
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(30, ChronoUnit.MINUTES),
                null,
                null,
                null
        );
        registerStudent(unconfiguredEvent.getId(), student1Id);

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", unconfiguredEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GPS_NOT_CONFIGURED"));

        long countedInDb = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(
                unconfiguredEvent.getId(), student1Id
        );
        assertThat(countedInDb).isZero();
    }

    @Test
    @DisplayName("7. Gọi /attendance/gps trên sự kiện DYNAMIC_QR -> trả 400 VALIDATION_FAILED (counted = false)")
    void checkInGps_whenEventMethodIsDynamicQr_returns400ValidationFailed() throws Exception {
        Instant now = Instant.now();
        Event qrEvent = createPublishedGpsEvent(
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(30, ChronoUnit.MINUTES),
                QNU_LAT,
                QNU_LON,
                60
        );
        qrEvent.setAttendanceMethod(AttendanceMethod.DYNAMIC_QR);
        eventRepository.saveAndFlush(qrEvent);
        registerStudent(qrEvent.getId(), student1Id);

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", qrEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        long countedInDb = attemptRepository.countByEventIdAndStudentIdAndCountedTrue(qrEvent.getId(), student1Id);
        assertThat(countedInDb).isZero();
    }

    @Test
    @DisplayName("8. Kiểm tra cổng: ngoài giờ mở (409 ATTENDANCE_NOT_OPEN), DRAFT (409 EVENT_NOT_PUBLISHED), chưa đăng ký (403 NOT_REGISTERED)")
    void checkInGps_gateChecks_rejectWithoutConsumingAttempts() throws Exception {
        Instant now = Instant.now();

        // 8a. Ngoài giờ mở điểm danh
        Event closedWindowEvent = createPublishedGpsEvent(
                now.plus(1, ChronoUnit.HOURS),
                now.plus(2, ChronoUnit.HOURS),
                QNU_LAT, QNU_LON, 60
        );
        registerStudent(closedWindowEvent.getId(), student1Id);

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", closedWindowEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_NOT_OPEN"));

        // 8b. Sự kiện ở trạng thái DRAFT
        Event draftEvent = createPublishedGpsEvent(
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(30, ChronoUnit.MINUTES),
                QNU_LAT, QNU_LON, 60
        );
        draftEvent.setStatus(EventStatus.DRAFT);
        eventRepository.saveAndFlush(draftEvent);
        registerStudent(draftEvent.getId(), student1Id);

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", draftEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_PUBLISHED"));

        // 8c. Sinh viên chưa đăng ký sự kiện
        Event unregisteredEvent = createPublishedGpsEvent(
                now.minus(10, ChronoUnit.MINUTES),
                now.plus(30, ChronoUnit.MINUTES),
                QNU_LAT, QNU_LON, 60
        );

        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", unregisteredEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_REGISTERED"));
    }

    @Test
    @DisplayName("9. Idempotency: Gửi lại request GPS với cùng Idempotency-Key trả lại đúng kết quả cũ và không trừ thêm lượt")
    void checkInGps_retryWithSameIdempotencyKey_returnsCachedResponseWithoutExtraAttempt() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();
        double farLat = QNU_LAT + 0.0015;

        // 9a. Retry cho kết quả 422 GPS_OUT_OF_RANGE
        String failedKey = UUID.randomUUID().toString();
        MvcResult firstFail = mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", failedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(farLat, QNU_LON, 15.0)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.attemptCount").value(1))
                .andReturn();

        MvcResult replayedFail = mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", failedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(farLat, QNU_LON, 15.0)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.attemptCount").value(1))
                .andReturn();

        assertThat(replayedFail.getResponse().getContentAsString())
                .isEqualTo(firstFail.getResponse().getContentAsString());
        assertThat(attemptRepository.countByEventIdAndStudentIdAndCountedTrue(event.getId(), student1Id))
                .isEqualTo(1L);

        // 9b. Retry cho kết quả 200 ACCEPTED
        String successKey = UUID.randomUUID().toString();
        MvcResult firstSuccess = mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", successKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();

        MvcResult replayedSuccess = mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", successKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();

        assertThat(replayedSuccess.getResponse().getContentAsString())
                .isEqualTo(firstSuccess.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("10. Concurrency: 5 request GPS hợp lệ chạy đồng thời (khác Idempotency-Key) chỉ tạo đúng 1 bản ghi ACCEPTED")
    void checkInGps_concurrentValidRequests_createsExactlyOneAcceptedAttendance() throws Exception {
        Event event = createOpenGpsEventAndRegisterStudent1();
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Integer> task = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                MvcResult res = mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", event.getId())
                                .header("Authorization", "Bearer " + student1Token)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(gpsJson(QNU_LAT + 0.0001, QNU_LON, 12.0)))
                        .andReturn();
                return res.getResponse().getStatus();
            };
            futures.add(executor.submit(task));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statuses.add(f.get(15, TimeUnit.SECONDS));
        }
        executor.shutdownNow();

        long okCount = statuses.stream().filter(s -> s == 200).count();
        long conflictCount = statuses.stream().filter(s -> s == 409).count();

        assertThat(okCount).isEqualTo(1L);
        assertThat(conflictCount).isEqualTo(threads - 1L);
        assertThat(attendanceRepository.findByEventIdAndStudentIdAndStatus(
                event.getId(), student1Id, "ACCEPTED"
        )).isPresent();
    }

    @Test
    @DisplayName("11. Luồng End-to-End từ tạo Event GPS qua POST /api/v1/events (mặc định bán kính 60m) -> Preview -> Publish -> Check-in GPS -> GET /attendance/me")
    void fullEndToEndGpsEventCreationPublishAndCheckIn() throws Exception {
        Instant now = Instant.now();
        Map<String, Object> createPayload = new java.util.HashMap<>();
        createPayload.put("title", "Giải chạy Sinh viên QNU 2026");
        createPayload.put("description", "Điểm danh GPS tại cổng chính");
        createPayload.put("location", "Khuôn viên ĐH Quy Nhơn");
        createPayload.put("startAt", now.plus(1, ChronoUnit.DAYS).toString());
        createPayload.put("endAt", now.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).toString());
        createPayload.put("registrationDeadline", now.plus(30, ChronoUnit.MINUTES).toString());
        createPayload.put("attendanceOpenAt", now.minus(5, ChronoUnit.MINUTES).toString());
        createPayload.put("attendanceCloseAt", now.plus(1, ChronoUnit.HOURS).toString());
        createPayload.put("attendanceMethod", "GPS");
        createPayload.put("quotaTotal", 100);
        createPayload.put("audienceClassIds", List.of(k49ClassId.toString()));
        createPayload.put("activityType", "Thể thao");
        createPayload.put("activityPoints", 5);
        createPayload.put("gpsLat", QNU_LAT);
        createPayload.put("gpsLng", QNU_LON);

        MvcResult createdResult = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attendanceMethod").value("GPS"))
                .andExpect(jsonPath("$.gpsLat").value(QNU_LAT))
                .andExpect(jsonPath("$.gpsLng").value(QNU_LON))
                .andExpect(jsonPath("$.allowedRadiusMeters").value(60))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createdResult.getResponse().getContentAsString());
        UUID eventId = UUID.fromString(createdNode.get("id").asText());

        // Preview & Publish
        MvcResult previewResult = mockMvc.perform(get("/api/v1/events/{id}/preview", eventId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        int previewVersion = objectMapper.readTree(previewResult.getResponse().getContentAsString())
                .get("previewVersion").asInt();

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true,\"previewVersion\":" + previewVersion + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Sinh viên đăng ký giữ chỗ
        mockMvc.perform(post("/api/v1/events/{id}/registrations", eventId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated());

        // Sinh viên điểm danh GPS thành công
        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", eventId)
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Sinh viên F5 kiểm tra GET /attendance/me
        mockMvc.perform(get("/api/v1/events/{id}/attendance/me", eventId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.canRetry").value(false));
    }

    @Test
    @DisplayName("12. [P1] Cập nhật sự kiện GPS khi bỏ qua attendanceMethod mà thiếu tọa độ bị từ chối 400; sự kiện GPS thiếu tọa độ bị chặn publish 409 GPS_NOT_CONFIGURED")
    void updateGpsEventOmittingMethodAndPublishUnconfiguredGpsEvent_areRejected() throws Exception {
        Instant now = Instant.now();
        Map<String, Object> createPayload = new java.util.HashMap<>();
        createPayload.put("title", "Sự kiện GPS Kiểm tra P1");
        createPayload.put("description", "Mô tả");
        createPayload.put("location", "Sân vận động QNU");
        createPayload.put("startAt", now.plus(1, ChronoUnit.DAYS).toString());
        createPayload.put("endAt", now.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).toString());
        createPayload.put("registrationDeadline", now.plus(30, ChronoUnit.MINUTES).toString());
        createPayload.put("attendanceOpenAt", now.minus(5, ChronoUnit.MINUTES).toString());
        createPayload.put("attendanceCloseAt", now.plus(1, ChronoUnit.HOURS).toString());
        createPayload.put("attendanceMethod", "GPS");
        createPayload.put("quotaTotal", 100);
        createPayload.put("audienceClassIds", List.of(k49ClassId.toString()));
        createPayload.put("activityType", "Thể thao");
        createPayload.put("activityPoints", 5);
        createPayload.put("gpsLat", QNU_LAT);
        createPayload.put("gpsLng", QNU_LON);

        MvcResult createdResult = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createdResult.getResponse().getContentAsString());
        UUID eventId = UUID.fromString(createdNode.get("id").asText());
        int version = createdNode.get("version").asInt();

        // 12a. PUT cập nhật bỏ qua attendanceMethod và không gửi gpsLat/gpsLng -> phải trả 400 VALIDATION_FAILED
        Map<String, Object> updateWithoutCoords = new java.util.HashMap<>(createPayload);
        updateWithoutCoords.remove("attendanceMethod");
        updateWithoutCoords.remove("gpsLat");
        updateWithoutCoords.remove("gpsLng");
        updateWithoutCoords.put("version", version);

        mockMvc.perform(put("/api/v1/events/{id}", eventId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateWithoutCoords)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.gpsLat").isNotEmpty())
                .andExpect(jsonPath("$.details.fields.gpsLng").isNotEmpty());

        // Tọa độ trong DB vẫn được giữ nguyên
        Event stored = eventRepository.findById(eventId).orElseThrow();
        assertThat(stored.getGpsLat()).isEqualTo(QNU_LAT);
        assertThat(stored.getGpsLng()).isEqualTo(QNU_LON);

        // 12b. Giả lập bản nháp GPS bị thiếu tọa độ trong DB -> gọi publish phải bị chặn 409 GPS_NOT_CONFIGURED
        stored.setGpsLat(null);
        stored.setGpsLng(null);
        eventRepository.saveAndFlush(stored);

        MvcResult previewResult = mockMvc.perform(get("/api/v1/events/{id}/preview", eventId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        int previewVersion = objectMapper.readTree(previewResult.getResponse().getContentAsString())
                .get("previewVersion").asInt();

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true,\"previewVersion\":" + previewVersion + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GPS_NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("13. [P2] Dùng lại cùng Idempotency-Key giữa /attendance/gps và /attendance/qr bị chặn 400 VALIDATION_FAILED (cả 2 chiều)")
    void checkIn_reusingSameIdempotencyKeyAcrossGpsAndQr_returns400ValidationFailed() throws Exception {
        Event gpsEvent = createOpenGpsEventAndRegisterStudent1();
        String gpsFirstKey = UUID.randomUUID().toString();

        // Chiều 1: Gọi /attendance/gps trước (200 ACCEPTED), sau đó dùng lại cùng key ở /attendance/qr -> 400 VALIDATION_FAILED
        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", gpsEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", gpsFirstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", gpsEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", gpsFirstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"qr1.dummy.token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Chiều 2: Gọi /attendance/qr trước (trên sự kiện GPS sẽ ghi attempt DYNAMIC_QR 400), sau đó dùng lại cùng key ở /attendance/gps -> 400 VALIDATION_FAILED
        Event secondGpsEvent = createOpenGpsEventAndRegisterStudent1();
        String qrFirstKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/events/{id}/attendance/qr", secondGpsEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", qrFirstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"qr1.dummy.token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Nếu không kiểm tra method, lời gọi GPS dưới đây sẽ trả nhầm lỗi 400 cũ của QR thay vì cho điểm danh hoặc báo lỗi trùng key khác method;
        // và ngay cả khi gửi tọa độ hợp lệ, dùng lại qrFirstKey ở endpoint GPS phải bị từ chối 400 với thông điệp khác phương thức (không tạo bản ghi ACCEPTED).
        mockMvc.perform(post("/api/v1/events/{id}/attendance/gps", secondGpsEvent.getId())
                        .header("Authorization", "Bearer " + student1Token)
                        .header("Idempotency-Key", qrFirstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson(QNU_LAT, QNU_LON, 10.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key đã được sử dụng cho phương thức điểm danh khác."));

        assertThat(attendanceRepository.findByEventIdAndStudentIdAndStatus(
                secondGpsEvent.getId(), student1Id, "ACCEPTED"
        )).isEmpty();
    }
}
