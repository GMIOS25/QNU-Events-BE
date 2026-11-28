package vn.edu.qnu.events.attendance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.registration.Registration;
import vn.edu.qnu.events.registration.RegistrationRepository;
import vn.edu.qnu.events.registration.RegistrationStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceServiceTest {

    private EventRepository eventRepository;
    private AttendanceRepository attendanceRepository;
    private AttendanceAttemptRepository attemptRepository;
    private AttendanceService attendanceService;

    private final Instant now = Instant.parse("2026-11-26T12:00:00Z");
    private final UUID studentId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        attendanceRepository = mock(AttendanceRepository.class);
        attemptRepository = mock(AttendanceAttemptRepository.class);
        attendanceService = new AttendanceService(
                eventRepository,
                mock(RegistrationRepository.class),
                attendanceRepository,
                attemptRepository,
                mock(AttendanceAttemptService.class),
                new QrTokenService("test-secret-key-at-least-32-chars-long-1234567890!"),
                mock(AuditService.class),
                Clock.fixed(now, ZoneOffset.UTC),
                mock(EntityManager.class)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role, UUID userClassId) {
        Jwt.Builder builder = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(studentId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of(role));
        if (userClassId != null) {
            builder.claim("classId", userClassId.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        builder.build(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }

    private Event publishedEvent() {
        Event event = new Event(
                eventId, UUID.randomUUID(), "Hội thảo AI", "Mô tả", "Hội trường A",
                now.plusSeconds(3600), now.plusSeconds(7200),
                now.plusSeconds(1800), now.minusSeconds(600), now.plusSeconds(7200),
                AttendanceMethod.DYNAMIC_QR, 10, 3, 50, "Workshop", 5, Set.of(classId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    @Test
    @DisplayName("getMyAttendance trả về 2/3 lượt sai và canRetry=true khi chưa điểm danh (BUG-09)")
    void getMyAttendanceReturnsAttemptProgressWhenNotCheckedIn() {
        authenticateAs("STUDENT", classId);
        when(eventRepository.findPublishedByIdAndClass(eventId, classId)).thenReturn(Optional.of(publishedEvent()));
        when(attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId)).thenReturn(2L);
        when(attendanceRepository.findByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED"))
                .thenReturn(Optional.empty());

        MyAttendanceResponse res = attendanceService.getMyAttendance(eventId);

        assertThat(res.status()).isEqualTo("NOT_CHECKED_IN");
        assertThat(res.acceptedAt()).isNull();
        assertThat(res.attemptCount()).isEqualTo(2L);
        assertThat(res.maxAttempts()).isEqualTo(3);
        assertThat(res.remainingAttempts()).isEqualTo(1L);
        assertThat(res.canRetry()).isTrue();
    }

    @Test
    @DisplayName("getMyAttendance trả về canRetry=false khi chạm trần 3/3 lượt sai (BUG-09)")
    void getMyAttendanceReturnsLockedWhenLimitReached() {
        authenticateAs("STUDENT", classId);
        when(eventRepository.findPublishedByIdAndClass(eventId, classId)).thenReturn(Optional.of(publishedEvent()));
        when(attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId)).thenReturn(3L);
        when(attendanceRepository.findByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED"))
                .thenReturn(Optional.empty());

        MyAttendanceResponse res = attendanceService.getMyAttendance(eventId);

        assertThat(res.status()).isEqualTo("NOT_CHECKED_IN");
        assertThat(res.attemptCount()).isEqualTo(3L);
        assertThat(res.remainingAttempts()).isZero();
        assertThat(res.canRetry()).isFalse();
    }

    @Test
    @DisplayName("getMyAttendance trả về ACCEPTED cùng thời điểm ghi nhận sau khi điểm danh thành công (BUG-09)")
    void getMyAttendanceReturnsAcceptedWhenCheckedIn() {
        authenticateAs("STUDENT", classId);
        when(eventRepository.findPublishedByIdAndClass(eventId, classId)).thenReturn(Optional.of(publishedEvent()));
        when(attemptRepository.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId)).thenReturn(1L);
        Attendance accepted = new Attendance(UUID.randomUUID(), eventId, studentId, "DYNAMIC_QR", "ACCEPTED", now);
        when(attendanceRepository.findByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED"))
                .thenReturn(Optional.of(accepted));

        MyAttendanceResponse res = attendanceService.getMyAttendance(eventId);

        assertThat(res.status()).isEqualTo("ACCEPTED");
        assertThat(res.acceptedAt()).isEqualTo(now);
        assertThat(res.attemptCount()).isEqualTo(1L);
        assertThat(res.canRetry()).isFalse();
    }

    @Test
    @DisplayName("getMyAttendance trả 404 khi sự kiện không tồn tại hoặc không thuộc lớp sinh viên")
    void getMyAttendanceReturns404ForInaccessibleEvent() {
        authenticateAs("STUDENT", classId);
        when(eventRepository.findPublishedByIdAndClass(eventId, classId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getMyAttendance(eventId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getMyAttendance từ chối vai trò EVENT_OWNER với 403 FORBIDDEN")
    void getMyAttendanceRejectsNonStudent() {
        authenticateAs("EVENT_OWNER", null);

        assertThatThrownBy(() -> attendanceService.getMyAttendance(eventId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("checkInWithGps từ chối tọa độ không hợp lệ với 400 VALIDATION_FAILED thay vì 500 (QE-55)")
    void checkInWithGpsRejectsInvalidCoordinatesWith400() {
        authenticateAs("STUDENT", classId);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "idem-key-1", new GpsCheckInRequest(95.0, 109.2183, 10.0), "trace-1"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "idem-key-2", new GpsCheckInRequest(13.7589, 109.2183, -1.0), "trace-2"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "idem-key-3", null, "trace-3"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("checkInWithGps trả lại kết quả cached khi trùng Idempotency-Key (QE-55)")
    void checkInWithGpsReturnsCachedResultOnDuplicateIdempotencyKey() {
        authenticateAs("STUDENT", classId);
        AttendanceAttempt cached = new AttendanceAttempt(
                UUID.randomUUID(), eventId, studentId, "GPS", "idem-gps-1",
                "ACCEPTED", null, false, (short) 200, "{\"status\":\"ACCEPTED\"}", now
        );
        when(attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(eventId, studentId, "idem-gps-1"))
                .thenReturn(Optional.of(cached));

        CheckInResult result = attendanceService.checkInWithGps(
                eventId, "idem-gps-1", new GpsCheckInRequest(13.7589, 109.2183, 12.0), "trace-cached"
        );

        assertThat(result.httpStatus().value()).isEqualTo(200);
        assertThat(result.responseBody()).isEqualTo("{\"status\":\"ACCEPTED\"}");
    }

    @Test
    @DisplayName("Dùng lại cùng Idempotency-Key giữa QR và GPS bị từ chối 400 VALIDATION_FAILED thay vì trả nhầm kết quả cũ (QE-55 P2)")
    void checkInRejectsIdempotencyKeyReusedAcrossDifferentMethods() {
        authenticateAs("STUDENT", classId);

        // Key đã dùng ở DYNAMIC_QR nhưng gọi sang checkInWithGps
        AttendanceAttempt qrAttempt = new AttendanceAttempt(
                UUID.randomUUID(), eventId, studentId, "DYNAMIC_QR", "shared-idem-qr",
                "ACCEPTED", null, false, (short) 200, "{\"status\":\"ACCEPTED\"}", now
        );
        when(attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(eventId, studentId, "shared-idem-qr"))
                .thenReturn(Optional.of(qrAttempt));

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "shared-idem-qr", new GpsCheckInRequest(13.7589, 109.2183, 12.0), "trace-cross-1"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        // Key đã dùng ở GPS nhưng gọi sang checkInWithQr
        AttendanceAttempt gpsAttempt = new AttendanceAttempt(
                UUID.randomUUID(), eventId, studentId, "GPS", "shared-idem-gps",
                "ACCEPTED", null, false, (short) 200, "{\"status\":\"ACCEPTED\"}", now
        );
        when(attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(eventId, studentId, "shared-idem-gps"))
                .thenReturn(Optional.of(gpsAttempt));

        assertThatThrownBy(() -> attendanceService.checkInWithQr(
                eventId, "shared-idem-gps", new CheckInRequest("qr1.dummy.token"), "trace-cross-2"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("checkInWithGps từ chối vai trò EVENT_OWNER với 403 FORBIDDEN (QE-55)")
    void checkInWithGpsRejectsNonStudentWith403() {
        authenticateAs("EVENT_OWNER", null);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "idem-owner-1", new GpsCheckInRequest(13.7589, 109.2183, 10.0), "trace-owner"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("checkInWithGps từ chối Idempotency-Key rỗng hoặc vượt quá 100 ký tự với 400 VALIDATION_FAILED (QE-55)")
    void checkInWithGpsRejectsInvalidIdempotencyKey() {
        authenticateAs("STUDENT", classId);
        GpsCheckInRequest validReq = new GpsCheckInRequest(13.7589, 109.2183, 10.0);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(eventId, "   ", validReq, "trace-blank"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> attendanceService.checkInWithGps(eventId, "k".repeat(101), validReq, "trace-long"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("checkInWithGps trả 404 NOT_FOUND khi sự kiện không tồn tại (QE-55)")
    void checkInWithGpsReturns404WhenEventNotFound() {
        authenticateAs("STUDENT", classId);
        when(attemptRepository.findByEventIdAndStudentIdAndIdempotencyKey(eventId, studentId, "idem-missing-evt"))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkInWithGps(
                eventId, "idem-missing-evt", new GpsCheckInRequest(13.7589, 109.2183, 10.0), "trace-404"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getQrToken từ chối sự kiện dùng phương thức GPS với 400 VALIDATION_FAILED (QE-55)")
    void getQrTokenRejectsGpsEvent() {
        authenticateAs("EVENT_OWNER", null);
        Event gpsEvent = new Event(
                eventId, studentId, "Sự kiện GPS", "Mô tả", "Sân vận động",
                now.plusSeconds(3600), now.plusSeconds(7200),
                now.plusSeconds(1800), now.minusSeconds(600), now.plusSeconds(7200),
                AttendanceMethod.GPS, 10, 3, 50, "Thể thao", 5, Set.of(classId)
        );
        gpsEvent.setStatus(EventStatus.PUBLISHED);
        gpsEvent.setGpsLat(13.7589);
        gpsEvent.setGpsLng(109.2183);
        gpsEvent.setAllowedRadiusMeters(60);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(gpsEvent));

        assertThatThrownBy(() -> attendanceService.getQrToken(eventId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("checkInWithGps gọi auditService.record SAU KHI transaction điểm danh đã commit xong (cả luồng ACCEPT và REJECT) để tránh giữ 2 kết nối HikariCP (QE-55 P2)")
    void checkInWithGpsRecordsAuditStrictlyOutsideTransaction() {
        authenticateAs("STUDENT", classId);

        AtomicBoolean txActive = new AtomicBoolean(false);
        AtomicBoolean auditCalledWhileTxActive = new AtomicBoolean(false);
        AtomicBoolean txCommitted = new AtomicBoolean(false);

        PlatformTransactionManager trackingTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                txActive.set(true);
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                txActive.set(false);
                txCommitted.set(true);
            }

            @Override
            public void rollback(TransactionStatus status) {
                txActive.set(false);
            }
        };

        EventRepository localEventRepo = mock(EventRepository.class);
        RegistrationRepository localRegRepo = mock(RegistrationRepository.class);
        AttendanceRepository localAttRepo = mock(AttendanceRepository.class);
        AttendanceAttemptRepository localAttemptRepo = mock(AttendanceAttemptRepository.class);
        AttendanceAttemptService localAttemptService = mock(AttendanceAttemptService.class);
        AuditService localAuditService = mock(AuditService.class);
        EntityManager localEm = mock(EntityManager.class);
        Query lockQuery = mock(Query.class);

        when(localEm.createNativeQuery(anyString())).thenReturn(lockQuery);
        when(lockQuery.setParameter(anyInt(), any())).thenReturn(lockQuery);
        when(lockQuery.getSingleResult()).thenReturn(null);

        Event gpsEvent = new Event(
                eventId, UUID.randomUUID(), "Sự kiện GPS", "Mô tả", "QNU",
                now.plusSeconds(3600), now.plusSeconds(7200),
                now.plusSeconds(1800), now.minusSeconds(600), now.plusSeconds(7200),
                AttendanceMethod.GPS, 10, 3, 50, "Thể thao", 5, Set.of(classId)
        );
        gpsEvent.setStatus(EventStatus.PUBLISHED);
        gpsEvent.setGpsLat(13.7589);
        gpsEvent.setGpsLng(109.2183);
        gpsEvent.setAllowedRadiusMeters(60);

        when(localEventRepo.findById(eventId)).thenReturn(Optional.of(gpsEvent));
        when(localRegRepo.findActiveByEventAndStudent(eventId, studentId)).thenReturn(Optional.of(
                new Registration(UUID.randomUUID(), eventId, studentId, RegistrationStatus.REGISTERED, now, now)
        ));
        when(localAttRepo.existsByEventIdAndStudentIdAndStatus(eventId, studentId, "ACCEPTED")).thenReturn(false);
        when(localAttemptRepo.countByEventIdAndStudentIdAndCountedTrue(eventId, studentId)).thenReturn(0L);
        when(localAttemptRepo.findByEventIdAndStudentIdAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());

        when(localAttemptService.recordAttempt(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), any()))
                .thenAnswer(inv -> {
                    AttendanceAttempt a = new AttendanceAttempt(
                            UUID.randomUUID(),
                            inv.getArgument(0),
                            inv.getArgument(1),
                            inv.getArgument(2),
                            inv.getArgument(3),
                            inv.getArgument(4),
                            inv.getArgument(5),
                            inv.getArgument(6),
                            ((Integer) inv.getArgument(7)).shortValue(),
                            inv.getArgument(8),
                            now
                    );
                    when(localAttemptRepo.findById(a.getId())).thenReturn(Optional.of(a));
                    return a;
                });

        when(localAuditService.record(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    if (txActive.get()) {
                        auditCalledWhileTxActive.set(true);
                    }
                    return null;
                });

        AttendanceService serviceWithTx = new AttendanceService(
                localEventRepo,
                localRegRepo,
                localAttRepo,
                localAttemptRepo,
                localAttemptService,
                new QrTokenService("test-secret-key-at-least-32-chars-long-1234567890!"),
                new GpsVerificationService(),
                localAuditService,
                Clock.fixed(now, ZoneOffset.UTC),
                localEm,
                trackingTxManager
        );

        // 1) Luồng REJECT (ngoài bán kính)
        CheckInResult rejectRes = serviceWithTx.checkInWithGps(
                eventId, "idem-tx-reject", new GpsCheckInRequest(13.7610, 109.2183, 10.0), "trace-reject"
        );
        assertThat(rejectRes.httpStatus().value()).isEqualTo(422);
        assertThat(txCommitted.get()).isTrue();
        assertThat(auditCalledWhileTxActive.get()).isFalse();

        // 2) Luồng ACCEPT (trong bán kính)
        txCommitted.set(false);
        CheckInResult acceptRes = serviceWithTx.checkInWithGps(
                eventId, "idem-tx-accept", new GpsCheckInRequest(13.7589, 109.2183, 10.0), "trace-accept"
        );
        assertThat(acceptRes.httpStatus().value()).isEqualTo(200);
        assertThat(txCommitted.get()).isTrue();
        assertThat(auditCalledWhileTxActive.get()).isFalse();

        verify(localAuditService, org.mockito.Mockito.times(2))
                .record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
