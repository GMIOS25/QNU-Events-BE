package vn.edu.qnu.events.attendance;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.registration.RegistrationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
}
