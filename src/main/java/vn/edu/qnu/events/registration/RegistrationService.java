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
import vn.edu.qnu.events.event.EventPool;
import vn.edu.qnu.events.event.EventPoolRepository;
import vn.edu.qnu.events.event.EventRepository;
import vn.edu.qnu.events.event.EventStatus;
import vn.edu.qnu.events.student.OrgClass;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final EventPoolRepository eventPoolRepository;
    private final OrgClassRepository orgClassRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final Clock clock;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            EventPoolRepository eventPoolRepository,
            OrgClassRepository orgClassRepository,
            AppUserRepository appUserRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.eventPoolRepository = eventPoolRepository;
        this.orgClassRepository = orgClassRepository;
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
        List<EventPool> pools = eventPoolRepository.findByEventIdOrderByPriorityAsc(eventId);

        if (pools.isEmpty()) {
            // Sự kiện không cấu hình pool: giữ nguyên luồng quota tổng của QE-71
            return registerWithoutPools(eventId, studentId, newRegistrationId, now);
        }

        // Phân bổ suất theo pool (QE-39, Assumption A-003):
        // duyệt pool theo priority tăng dần, chỉ nhận pool mà sinh viên thuộc scope.
        UUID studentClassId = student.getClassId();
        UUID allocatedPoolId = null;
        for (EventPool pool : pools) {
            if (!matchesScope(pool, studentClassId)) {
                continue;
            }
            if (eventPoolRepository.claimSeat(pool.getId()) == 1) {
                allocatedPoolId = pool.getId();
                break;
            }
            // Pool đầy: pool không bắt buộc (allowOverflow mặc định) -> thử pool kế tiếp theo priority
        }

        if (allocatedPoolId != null) {
            int rowsInserted = registrationRepository.tryInsertRegistration(newRegistrationId, eventId, studentId, now);
            if (rowsInserted == 0) {
                // Đã đăng ký từ trước -> hoàn suất vừa giữ và trả idempotent
                eventPoolRepository.releaseSeat(allocatedPoolId);
                Registration existing = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
                return new RegistrationResponse(existing.getId(), existing.getStatus().name(), true);
            }
            registrationRepository.setPool(newRegistrationId, allocatedPoolId);
            // Giữ trần quotaTotal (A-003) và cho summary hiển thị đúng chỗ còn lại
            int totalReserved = registrationRepository.reserveSeat(eventId);
            if (totalReserved == 0) {
                eventPoolRepository.releaseSeat(allocatedPoolId);
                throw new ApiException(ErrorCode.CAPACITY_FULL);
            }
            auditService.record(
                    studentId,
                    "STUDENT",
                    "REGISTRATION_CREATE",
                    "REGISTRATION",
                    newRegistrationId.toString(),
                    "SUCCESS",
                    null,
                    Map.of("eventId", eventId.toString(), "poolId", allocatedPoolId.toString()),
                    null
            );
            return new RegistrationResponse(newRegistrationId, "REGISTERED", false);
        }

        // Hết suất hợp lệ -> WAITING_LIST ở pool ưu tiên cao nhất mà sinh viên thuộc scope (A-003)
        EventPool targetPool = pools.stream()
                .filter(p -> matchesScope(p, studentClassId))
                .findFirst()
                .orElse(null);
        if (targetPool == null) {
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

        UUID waitingId = UUID.randomUUID();
        int waitingInserted = registrationRepository.tryInsertWaitingRegistration(
                waitingId, eventId, studentId, targetPool.getId(), now);
        if (waitingInserted == 0) {
            Registration existing = registrationRepository.findActiveByEventAndStudent(eventId, studentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
            return new RegistrationResponse(existing.getId(), existing.getStatus().name(), true);
        }
        auditService.record(
                studentId,
                "STUDENT",
                "REGISTRATION_CREATE",
                "REGISTRATION",
                waitingId.toString(),
                "SUCCESS",
                "WAITING_LIST",
                Map.of("eventId", eventId.toString(), "poolId", targetPool.getId().toString()),
                null
        );
        return new RegistrationResponse(waitingId, "WAITING_LIST", false);
    }

    private RegistrationResponse registerWithoutPools(UUID eventId, UUID studentId, UUID newRegistrationId, Instant now) {
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

    private boolean matchesScope(EventPool pool, UUID studentClassId) {
        return switch (pool.getScopeType()) {
            case ALL -> true;
            case LOP -> studentClassId != null && pool.getScopeRef().equals(studentClassId.toString());
            case KHOA -> studentClassId != null && orgClassRepository.findById(studentClassId)
                    .map(OrgClass::getFacultyName)
                    .filter(f -> f != null && f.equalsIgnoreCase(pool.getScopeRef()))
                    .isPresent();
            // KHOI/CLB: chờ chuẩn hóa mã khối/CLB trong danh mục (xem A-003) - tạm không match
            default -> false;
        };
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
