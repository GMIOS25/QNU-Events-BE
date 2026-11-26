package vn.edu.qnu.events.registration;

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
import vn.edu.qnu.events.auth.AppUser;
import vn.edu.qnu.events.auth.AppUserRepository;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.event.AttendanceMethod;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private RegistrationRepository registrationRepository;
    private EventRepository eventRepository;
    private AppUserRepository appUserRepository;
    private AuditService auditService;
    private List<String> lifecycleEvents;
    private RegistrationService registrationService;

    private final Instant now = Instant.parse("2026-11-24T12:00:00Z");
    private final UUID studentId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registrationRepository = mock(RegistrationRepository.class);
        eventRepository = mock(EventRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        auditService = mock(AuditService.class);
        lifecycleEvents = new ArrayList<>();

        PlatformTransactionManager recordingTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                lifecycleEvents.add("TX_BEGIN");
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                if (status.isRollbackOnly()) {
                    lifecycleEvents.add("TX_ROLLBACK");
                } else {
                    lifecycleEvents.add("TX_COMMIT");
                }
            }

            @Override
            public void rollback(TransactionStatus status) {
                lifecycleEvents.add("TX_ROLLBACK");
            }
        };

        registrationService = new RegistrationService(
                registrationRepository,
                eventRepository,
                appUserRepository,
                auditService,
                Clock.fixed(now, ZoneOffset.UTC),
                recordingTxManager
        );

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(studentId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("STUDENT"))
                .claim("classId", classId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );

        AppUser student = new AppUser(studentId, "sv001", "$2a$12$hash", "SV 1", "4651050001", classId, Set.of("STUDENT"));
        when(appUserRepository.findById(studentId)).thenReturn(Optional.of(student));

        Event event = new Event(
                eventId, UUID.randomUUID(), "Hội thảo AI", "Mô tả", "Hội trường A",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.DYNAMIC_QR, 10, 3, 5, "Workshop", 5, Set.of(classId)
        );
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        doAnswer(inv -> {
            lifecycleEvents.add("AUDIT:" + inv.getArgument(5) + ":" + inv.getArgument(6));
            return null;
        }).when(auditService).record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Đăng ký thành công: Audit ghi SAU KHI transaction DB đã commit (tránh cạn HikariCP pool TC-REG-20)")
    void registerCommitsDbTransactionBeforeCallingAudit() {
        when(registrationRepository.tryInsertRegistration(any(), eq(eventId), eq(studentId), eq(now))).thenReturn(1);
        when(registrationRepository.reserveSeat(eventId)).thenReturn(1);

        RegistrationResponse res = registrationService.register(eventId);

        assertThat(res.status()).isEqualTo("REGISTERED");
        assertThat(res.alreadyRegistered()).isFalse();
        assertThat(lifecycleEvents).containsExactly("TX_BEGIN", "TX_COMMIT", "AUDIT:SUCCESS:null");
    }

    @Test
    @DisplayName("Hết suất CAPACITY_FULL: Rollback DB transaction TRƯỚC KHI ghi audit rejection (không để dòng mồ côi & không deadlock pool)")
    void capacityFullRollsBackBeforeCallingAudit() {
        when(registrationRepository.tryInsertRegistration(any(), eq(eventId), eq(studentId), eq(now))).thenReturn(1);
        when(registrationRepository.reserveSeat(eventId)).thenReturn(0);

        assertThatThrownBy(() -> registrationService.register(eventId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CAPACITY_FULL);

        assertThat(lifecycleEvents).containsExactly("TX_BEGIN", "TX_ROLLBACK", "AUDIT:REJECTED:CAPACITY_FULL");
    }
}
