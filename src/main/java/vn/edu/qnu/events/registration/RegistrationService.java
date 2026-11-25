package vn.edu.qnu.events.registration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.auth.AppUser;
import vn.edu.qnu.events.auth.AppUserRepository;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.event.Event;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            AppUserRepository appUserRepository,
            AuditService auditService,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RegistrationResponse register(UUID eventId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        UUID studentId = currentUser.userId();

        // Chạy phần thao tác DB trong TransactionTemplate riêng để giải phóng connection #1
        // TRƯỚC khi gọi auditService.record (REQUIRES_NEW), tránh nghẽn HikariCP pool khi
        // nhiều sinh viên đăng ký đồng thời (TC-REG-20).
        RegisterOutcome outcome = transactionTemplate.execute(status -> {
            AppUser student = appUserRepository.findById(studentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

            if (event.getStatus() != EventStatus.PUBLISHED) {
                return RegisterOutcome.rejected(
                        new ApiException(ErrorCode.EVENT_NOT_PUBLISHED),
                        new AuditPayload(
                                "REGISTRATION_CREATE",
                                "EVENT",
                                eventId.toString(),
                                "REJECTED",
                                "EVENT_NOT_PUBLISHED",
                                Map.of()
                        )
                );
            }

            Instant now = Instant.now(clock);
            if (now.isAfter(event.getRegistrationDeadline())) {
                return RegisterOutcome.rejected(
                        new ApiException(ErrorCode.REGISTRATION_CLOSED),
                        new AuditPayload(
                                "REGISTRATION_CREATE",
                                "EVENT",
                                eventId.toString(),
                                "REJECTED",
                                "REGISTRATION_CLOSED",
                                Map.of()
                        )
                );
            }

            if (student.getClassId() == null || !event.getAudienceClassIds().contains(student.getClassId())) {
                return RegisterOutcome.rejected(
                        new ApiException(ErrorCode.NOT_IN_AUDIENCE),
                        new AuditPayload(
                                "REGISTRATION_CREATE",
                                "EVENT",
                                eventId.toString(),
                                "DENIED",
                                "NOT_IN_AUDIENCE",
                                Map.of()
                        )
                );
            }

            UUID newRegistrationId = UUID.randomUUID();
            int rowsInserted = registrationRepository.tryInsertRegistration(newRegistrationId, eventId, studentId, now);

            if (rowsInserted == 0) {
                // Already registered - return existing registration idempotently
                Registration existing = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
                return RegisterOutcome.succeeded(
                        new RegistrationResponse(existing.getId(), existing.getStatus().name(), true),
                        null
                );
            }

            // Try to atomically reserve quota seat
            int seatsReserved = registrationRepository.reserveSeat(eventId);
            if (seatsReserved == 0) {
                // Capacity full! Roll back the inserted registration row cleanly, then record audit outside tx
                status.setRollbackOnly();
                return RegisterOutcome.rejected(
                        new ApiException(ErrorCode.CAPACITY_FULL),
                        new AuditPayload(
                                "REGISTRATION_CREATE",
                                "EVENT",
                                eventId.toString(),
                                "REJECTED",
                                "CAPACITY_FULL",
                                Map.of()
                        )
                );
            }

            return RegisterOutcome.succeeded(
                    new RegistrationResponse(newRegistrationId, "REGISTERED", false),
                    new AuditPayload(
                            "REGISTRATION_CREATE",
                            "REGISTRATION",
                            newRegistrationId.toString(),
                            "SUCCESS",
                            null,
                            Map.of("eventId", eventId.toString())
                    )
            );
        });

        if (outcome == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        if (outcome.audit() != null) {
            AuditPayload audit = outcome.audit();
            auditService.record(
                    studentId,
                    "STUDENT",
                    audit.action(),
                    audit.targetType(),
                    audit.targetId(),
                    audit.outcome(),
                    audit.reasonCode(),
                    audit.details(),
                    null
            );
        }

        if (outcome.error() != null) {
            throw outcome.error();
        }

        return outcome.response();
    }

    private record AuditPayload(
            String action,
            String targetType,
            String targetId,
            String outcome,
            String reasonCode,
            Map<String, Object> details
    ) {}

    private record RegisterOutcome(
            RegistrationResponse response,
            ApiException error,
            AuditPayload audit
    ) {
        static RegisterOutcome succeeded(RegistrationResponse response, AuditPayload audit) {
            return new RegisterOutcome(response, null, audit);
        }

        static RegisterOutcome rejected(ApiException error, AuditPayload audit) {
            return new RegisterOutcome(null, error, audit);
        }
    }

    @Transactional(readOnly = true)
    public MyRegistrationResponse getMyRegistration(UUID eventId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Registration reg = registrationRepository.findActiveByEventAndStudent(eventId, currentUser.userId())
                .or(() -> registrationRepository.findLatestByEventAndStudent(eventId, currentUser.userId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        return new MyRegistrationResponse(reg.getId(), reg.getStatus().name(), reg.getCreatedAt());
    }
}
