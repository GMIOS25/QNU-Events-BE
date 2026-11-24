package vn.edu.qnu.events.event;

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
import vn.edu.qnu.events.registration.RegistrationRepository;
import vn.edu.qnu.events.student.OrgClassRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventServiceAndValidatorTest {

    private OrgClassRepository orgClassRepository;
    private EventValidator eventValidator;
    private EventRepository eventRepository;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        orgClassRepository = mock(OrgClassRepository.class);
        eventValidator = new EventValidator(orgClassRepository);
        eventRepository = mock(EventRepository.class);
        eventService = new EventService(
                eventRepository,
                eventValidator,
                mock(AuditService.class),
                Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC),
                mock(RegistrationRepository.class),
                orgClassRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("EventValidator từ chối audienceClassIds chứa phần tử null -> 400 VALIDATION_FAILED (BUG-05)")
    void validatorRejectsNullElementInAudienceClassIds() {
        Set<UUID> classIds = new HashSet<>();
        classIds.add(null);
        Instant start = Instant.parse("2026-12-01T08:00:00Z");
        EventUpsert upsert = new EventUpsert(
                "Hội thảo Test", "Mô tả", "Hội trường A",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.DYNAMIC_QR, 50, classIds, "Workshop", 5, null
        );

        assertThatThrownBy(() -> eventValidator.validate(upsert))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("EventValidator từ chối activityType dài hơn 50 ký tự -> 400 VALIDATION_FAILED (BUG-05)")
    void validatorRejectsOverlongActivityType() {
        UUID classId = UUID.randomUUID();
        when(orgClassRepository.existsById(classId)).thenReturn(true);
        Instant start = Instant.parse("2026-12-01T08:00:00Z");
        EventUpsert upsert = new EventUpsert(
                "Hội thảo Test", "Mô tả", "Hội trường A",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.DYNAMIC_QR, 50, Set.of(classId), "A".repeat(51), 5, null
        );

        assertThatThrownBy(() -> eventValidator.validate(upsert))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("escapeLikePattern escape ký tự %, _, ! an toàn (BUG-12)")
    void escapeLikePatternEscapesWildcards() {
        assertThat(EventService.escapeLikePattern(null)).isNull();
        assertThat(EventService.escapeLikePattern("   ")).isNull();
        assertThat(EventService.escapeLikePattern("100% AI_Workshop!"))
                .isEqualTo("100!% AI!_Workshop!!");
    }

    @Test
    @DisplayName("preview() và publish() từ chối sự kiện đã CANCELLED -> 409 VERSION_CONFLICT (BUG-03)")
    void previewAndPublishRejectCancelledEvent() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-24T12:00:00Z");

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("EVENT_OWNER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_EVENT_OWNER")))
        );

        Event cancelledEvent = new Event(
                eventId, ownerId, "Sự kiện đã hủy", "Mô tả", "Hội trường A",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.DYNAMIC_QR, 10, 3, 50, "Workshop", 5, Set.of(UUID.randomUUID())
        );
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(cancelledEvent));

        assertThatThrownBy(() -> eventService.preview(eventId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VERSION_CONFLICT);

        assertThatThrownBy(() -> eventService.publish(eventId, new PublishRequest(true, 0)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VERSION_CONFLICT);
    }
}
