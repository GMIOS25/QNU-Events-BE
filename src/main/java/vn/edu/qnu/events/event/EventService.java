package vn.edu.qnu.events.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.common.ApiException;
import vn.edu.qnu.events.common.CurrentUser;
import vn.edu.qnu.events.common.CurrentUserProvider;
import vn.edu.qnu.events.common.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventValidator eventValidator;
    private final AuditService auditService;
    private final Clock clock;

    public EventService(
            EventRepository eventRepository,
            EventValidator eventValidator,
            AuditService auditService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.eventValidator = eventValidator;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public EventDetail create(EventUpsert upsert) {
        CurrentUser currentUser = CurrentUserProvider.require();
        eventValidator.validate(upsert);

        Event event = new Event(
                UUID.randomUUID(),
                currentUser.userId(),
                upsert.title(),
                upsert.description(),
                upsert.location(),
                upsert.startAt(),
                upsert.endAt(),
                upsert.registrationDeadline(),
                upsert.attendanceOpenAt(),
                upsert.attendanceCloseAt(),
                upsert.attendanceMethod(),
                10,
                3,
                upsert.quotaTotal(),
                upsert.activityType(),
                upsert.activityPoints(),
                upsert.audienceClassIds()
        );

        Event saved = eventRepository.saveAndFlush(event);
        return toDetail(saved);
    }

    @Transactional
    public EventDetail update(UUID id, EventUpsert upsert) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!event.getOwnerId().equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT, "Chỉ có thể chỉnh sửa sự kiện ở trạng thái bản nháp.");
        }

        if (upsert.version() != null && !upsert.version().equals(event.getVersion())) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT);
        }

        eventValidator.validate(upsert);

        event.setTitle(upsert.title());
        event.setDescription(upsert.description());
        event.setLocation(upsert.location());
        event.setStartAt(upsert.startAt());
        event.setEndAt(upsert.endAt());
        event.setRegistrationDeadline(upsert.registrationDeadline());
        event.setAttendanceOpenAt(upsert.attendanceOpenAt());
        event.setAttendanceCloseAt(upsert.attendanceCloseAt());
        if (upsert.attendanceMethod() != null) {
            event.setAttendanceMethod(upsert.attendanceMethod());
        }
        event.setQuotaTotal(upsert.quotaTotal());
        event.setActivityType(upsert.activityType());
        event.setActivityPoints(upsert.activityPoints());
        event.getAudienceClassIds().clear();
        if (upsert.audienceClassIds() != null) {
            event.getAudienceClassIds().addAll(upsert.audienceClassIds());
        }
        event.setUpdatedAt(Instant.now(clock));

        eventRepository.saveAndFlush(event);
        return toDetail(event);
    }

    @Transactional
    public PreviewResponse preview(UUID id) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!event.getOwnerId().equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        eventRepository.updatePreviewedVersion(id);
        Event reloaded = eventRepository.findById(id).orElseThrow();

        return new PreviewResponse(toDetail(reloaded), reloaded.getVersion());
    }

    @Transactional
    public EventDetail publish(UUID id, PublishRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!event.getOwnerId().equals(currentUser.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // Idempotent: already published -> return current state without re-auditing
        if (event.getStatus() == EventStatus.PUBLISHED) {
            return toDetail(event);
        }

        if (!request.confirm()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Cần xác nhận trước khi công bố sự kiện.");
        }

        if (event.getPreviewedVersion() == null) {
            auditService.record(
                    currentUser.userId(),
                    "EVENT_OWNER",
                    "EVENT_PUBLISH_DENIED",
                    "EVENT",
                    event.getId().toString(),
                    "DENIED",
                    "PREVIEW_REQUIRED",
                    Map.of("title", event.getTitle(), "version", event.getVersion()),
                    UUID.randomUUID().toString()
            );
            throw new ApiException(ErrorCode.PREVIEW_REQUIRED);
        }

        if (!event.getPreviewedVersion().equals(event.getVersion())
                || (request.previewVersion() != null && !request.previewVersion().equals(event.getVersion()))) {
            auditService.record(
                    currentUser.userId(),
                    "EVENT_OWNER",
                    "EVENT_PUBLISH_DENIED",
                    "EVENT",
                    event.getId().toString(),
                    "DENIED",
                    "PREVIEW_OUTDATED",
                    Map.of("title", event.getTitle(), "version", event.getVersion()),
                    UUID.randomUUID().toString()
            );
            throw new ApiException(ErrorCode.PREVIEW_OUTDATED);
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now(clock));
        event.setPublishedBy(currentUser.userId());
        event.setUpdatedAt(Instant.now(clock));
        eventRepository.save(event);

        auditService.record(
                currentUser.userId(),
                "EVENT_OWNER",
                "EVENT_PUBLISH",
                "EVENT",
                event.getId().toString(),
                "SUCCESS",
                null,
                Map.of("title", event.getTitle(), "version", event.getVersion()),
                UUID.randomUUID().toString()
        );

        return toDetail(event);
    }

    @Transactional(readOnly = true)
    public EventDetail getEventDetail(UUID id) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Event event;

        if (currentUser.roles().contains("EVENT_OWNER")) {
            event = eventRepository.findById(id)
                    .filter(e -> e.getOwnerId().equals(currentUser.userId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        } else if (currentUser.roles().contains("STUDENT")) {
            if (currentUser.classId() == null) {
                throw new ApiException(ErrorCode.NOT_FOUND);
            }
            event = eventRepository.findPublishedByIdAndClass(id, currentUser.classId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        } else {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        return toDetail(event);
    }

    @Transactional(readOnly = true)
    public Page<EventSummary> listEvents(String q, Pageable pageable) {
        CurrentUser currentUser = CurrentUserProvider.require();

        if (currentUser.roles().contains("EVENT_OWNER")) {
            return eventRepository.findByOwnerId(currentUser.userId(), q, pageable)
                    .map(this::toSummary);
        } else if (currentUser.roles().contains("STUDENT")) {
            if (currentUser.classId() == null) {
                return Page.empty(pageable);
            }
            return eventRepository.findPublishedForClass(currentUser.classId(), q, pageable)
                    .map(this::toSummary);
        } else {
            return Page.empty(pageable);
        }
    }

    public EventDetail toDetail(Event e) {
        Instant now = Instant.now(clock);
        int remainingQuota = Math.max(0, e.getQuotaTotal() - e.getRegisteredCount());
        boolean registrationOpen = (e.getStatus() == EventStatus.PUBLISHED)
                && !now.isAfter(e.getRegistrationDeadline())
                && remainingQuota > 0;
        boolean attendanceOpen = (e.getStatus() == EventStatus.PUBLISHED)
                && !now.isBefore(e.getAttendanceOpenAt())
                && !now.isAfter(e.getAttendanceCloseAt());

        return new EventDetail(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                e.getStartAt(),
                e.getEndAt(),
                e.getRegistrationDeadline(),
                e.getAttendanceOpenAt(),
                e.getAttendanceCloseAt(),
                e.getAttendanceMethod(),
                e.getQrRotationSeconds(),
                e.getQrGraceSeconds(),
                e.getQuotaTotal(),
                e.getRegisteredCount(),
                remainingQuota,
                registrationOpen,
                attendanceOpen,
                null,
                e.getStatus(),
                e.getActivityType(),
                e.getActivityPoints(),
                e.getOwnerId(),
                e.getAudienceClassIds(),
                e.getVersion(),
                e.getPreviewedVersion(),
                e.getPublishedAt()
        );
    }

    public EventSummary toSummary(Event e) {
        Instant now = Instant.now(clock);
        int remainingQuota = Math.max(0, e.getQuotaTotal() - e.getRegisteredCount());
        boolean registrationOpen = (e.getStatus() == EventStatus.PUBLISHED)
                && !now.isAfter(e.getRegistrationDeadline())
                && remainingQuota > 0;
        boolean attendanceOpen = (e.getStatus() == EventStatus.PUBLISHED)
                && !now.isBefore(e.getAttendanceOpenAt())
                && !now.isAfter(e.getAttendanceCloseAt());

        return new EventSummary(
                e.getId(),
                e.getTitle(),
                e.getLocation(),
                e.getStartAt(),
                e.getEndAt(),
                e.getQuotaTotal(),
                e.getRegisteredCount(),
                remainingQuota,
                registrationOpen,
                attendanceOpen,
                null,
                e.getStatus(),
                e.getActivityType(),
                e.getActivityPoints()
        );
    }
}

