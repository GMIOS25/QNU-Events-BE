package vn.edu.qnu.events.registration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            AppUserRepository appUserRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public RegistrationResponse register(UUID eventId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        if (!currentUser.hasRole("STUDENT")) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        UUID studentId = currentUser.userId();
        AppUser student = appUserRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            auditService.record(
                    studentId,
                    "STUDENT",
                    "REGISTRATION_CREATE",
                    "EVENT",
                    eventId.toString(),
                    "REJECTED",
                    "EVENT_NOT_PUBLISHED",
                    Map.of(),
                    null
            );
            throw new ApiException(ErrorCode.EVENT_NOT_PUBLISHED);
        }

        Instant now = Instant.now(clock);
        if (now.isAfter(event.getRegistrationDeadline())) {
            auditService.record(
                    studentId,
                    "STUDENT",
                    "REGISTRATION_CREATE",
                    "EVENT",
                    eventId.toString(),
                    "REJECTED",
                    "REGISTRATION_CLOSED",
                    Map.of(),
                    null
            );
            throw new ApiException(ErrorCode.REGISTRATION_CLOSED);
        }

        if (student.getClassId() == null || !event.getAudienceClassIds().contains(student.getClassId())) {
            auditService.record(
                    studentId,
                    "STUDENT",
                    "REGISTRATION_CREATE",
                    "EVENT",
                    eventId.toString(),
                    "DENIED",
                    "NOT_IN_AUDIENCE",
                    Map.of(),
                    null
            );
            throw new ApiException(ErrorCode.NOT_IN_AUDIENCE);
        }

        UUID newRegistrationId = UUID.randomUUID();
        int rowsInserted = registrationRepository.tryInsertRegistration(newRegistrationId, eventId, studentId, now);

        if (rowsInserted == 0) {
            // Already registered - return existing registration idempotently
            Registration existing = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
            return new RegistrationResponse(existing.getId(), existing.getStatus().name(), true);
        }

        // Try to atomically reserve quota seat
        int seatsReserved = registrationRepository.reserveSeat(eventId);
        if (seatsReserved == 0) {
            // Capacity full! Record audit rejection and throw to rollback the insert
            auditService.record(
                    studentId,
                    "STUDENT",
                    "REGISTRATION_CREATE",
                    "EVENT",
                    eventId.toString(),
                    "REJECTED",
                    "CAPACITY_FULL",
                    Map.of(),
                    null
            );
            throw new ApiException(ErrorCode.CAPACITY_FULL);
        }

        // Registration successful
        auditService.record(
                studentId,
                "STUDENT",
                "REGISTRATION_CREATE",
                "REGISTRATION",
                newRegistrationId.toString(),
                "SUCCESS",
                null,
                Map.of("eventId", eventId.toString()),
                null
        );

        return new RegistrationResponse(newRegistrationId, "REGISTERED", false);
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
