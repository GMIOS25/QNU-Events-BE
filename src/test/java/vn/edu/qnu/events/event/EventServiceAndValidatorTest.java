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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("update() trên sự kiện GPS khi request bỏ qua attendanceMethod (null) và thiếu tọa độ -> 400 VALIDATION_FAILED, không làm mất tọa độ cũ (QE-55 P1)")
    void updateGpsEventOmittingAttendanceMethodRequiresGpsCoordinates() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-24T12:00:00Z");
        when(orgClassRepository.existsById(classId)).thenReturn(true);

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("EVENT_OWNER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_EVENT_OWNER")))
        );

        Event existingGpsEvent = new Event(
                eventId, ownerId, "Sự kiện GPS", "Mô tả", "Sân vận động QNU",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.GPS, 10, 3, 50, "Workshop", 5, Set.of(classId)
        );
        existingGpsEvent.setVersion(0);
        existingGpsEvent.setGpsLat(13.7589);
        existingGpsEvent.setGpsLng(109.2183);
        existingGpsEvent.setAllowedRadiusMeters(60);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingGpsEvent));

        // Request update bỏ qua attendanceMethod (null) và bỏ qua gpsLat/gpsLng (null)
        EventUpsert upsertWithoutMethodAndCoords = new EventUpsert(
                "Sự kiện GPS (đã sửa tên)", "Mô tả mới", "Sân vận động QNU",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                null, 50, Set.of(classId), "Workshop", 5, 0,
                null, null, null
        );

        assertThatThrownBy(() -> eventService.update(eventId, upsertWithoutMethodAndCoords))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);

        // Đảm bảo tọa độ cũ trên entity không bị ghi đè thành null
        assertThat(existingGpsEvent.getGpsLat()).isEqualTo(13.7589);
        assertThat(existingGpsEvent.getGpsLng()).isEqualTo(109.2183);
        assertThat(existingGpsEvent.getAllowedRadiusMeters()).isEqualTo(60);
    }

    @Test
    @DisplayName("publish() chặn công bố sự kiện GPS nếu thiếu cấu hình tọa độ tâm hoặc bán kính -> 409 GPS_NOT_CONFIGURED (QE-55 P1)")
    void publishRejectsGpsEventMissingCoordinates() {
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

        Event unconfiguredGpsDraft = new Event(
                eventId, ownerId, "Sự kiện GPS thiếu tọa độ", "Mô tả", "Hội trường A",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.GPS, 10, 3, 50, "Workshop", 5, Set.of(UUID.randomUUID())
        );
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(unconfiguredGpsDraft));

        assertThatThrownBy(() -> eventService.publish(eventId, new PublishRequest(true, 0)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.GPS_NOT_CONFIGURED);

        assertThat(unconfiguredGpsDraft.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    @DisplayName("EventValidator từ chối tọa độ GPS không hợp lệ hoặc bán kính ngoài [10, 1000] (QE-55)")
    void validatorRejectsInvalidGpsCoordinatesAndRadiusBounds() {
        UUID classId = UUID.randomUUID();
        when(orgClassRepository.existsById(classId)).thenReturn(true);
        Instant start = Instant.parse("2026-12-01T08:00:00Z");

        EventUpsert badLat = new EventUpsert(
                "Sự kiện GPS", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                95.0, 109.2183, 60
        );
        EventUpsert nanLng = new EventUpsert(
                "Sự kiện GPS", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                13.7589, Double.NaN, 60
        );
        EventUpsert radiusTooSmall = new EventUpsert(
                "Sự kiện GPS", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                13.7589, 109.2183, 9
        );
        EventUpsert radiusTooLarge = new EventUpsert(
                "Sự kiện GPS", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                13.7589, 109.2183, 1001
        );

        for (EventUpsert invalid : List.of(badLat, nanLng, radiusTooSmall, radiusTooLarge)) {
            assertThatThrownBy(() -> eventValidator.validate(invalid))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Test
    @DisplayName("EventValidator chấp nhận bán kính GPS tại đúng hai biên 10m và 1000m (QE-55)")
    void validatorAcceptsGpsRadiusExactBoundaries10And1000() {
        UUID classId = UUID.randomUUID();
        when(orgClassRepository.existsById(classId)).thenReturn(true);
        Instant start = Instant.parse("2026-12-01T08:00:00Z");

        EventUpsert minRadius = new EventUpsert(
                "Sự kiện GPS 10m", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                13.7589, 109.2183, 10
        );
        EventUpsert maxRadius = new EventUpsert(
                "Sự kiện GPS 1000m", "Mô tả", "QNU",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 50, Set.of(classId), "Thể thao", 5, null,
                13.7589, 109.2183, 1000
        );

        assertThatCode(() -> eventValidator.validate(minRadius)).doesNotThrowAnyException();
        assertThatCode(() -> eventValidator.validate(maxRadius)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create() sự kiện GPS khi allowedRadiusMeters=null tự áp dụng bán kính mặc định 60m (QE-55)")
    void createGpsEventWithNullRadiusAppliesDefault60Meters() {
        UUID ownerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant start = Instant.parse("2026-12-01T08:00:00Z");
        when(orgClassRepository.existsById(classId)).thenReturn(true);
        when(orgClassRepository.findAllById(any())).thenReturn(List.of());
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("EVENT_OWNER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_EVENT_OWNER")))
        );

        EventUpsert upsert = new EventUpsert(
                "Giải chạy QNU", "Mô tả", "Sân vận động",
                start, start.plusSeconds(3600),
                start.minusSeconds(3600), start.minusSeconds(600), start.plusSeconds(4200),
                AttendanceMethod.GPS, 100, Set.of(classId), "Thể thao", 5, null,
                13.7589, 109.2183, null
        );

        EventDetail created = eventService.create(upsert);

        assertThat(created.attendanceMethod()).isEqualTo(AttendanceMethod.GPS);
        assertThat(created.gpsLat()).isEqualTo(13.7589);
        assertThat(created.gpsLng()).isEqualTo(109.2183);
        assertThat(created.allowedRadiusMeters()).isEqualTo(60);
    }

    @Test
    @DisplayName("update() chuyển từ DYNAMIC_QR sang GPS nhưng thiếu tọa độ bị từ chối 400 VALIDATION_FAILED (QE-55)")
    void updateSwitchingQrToGpsWithoutCoordinatesIsRejected() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-24T12:00:00Z");
        when(orgClassRepository.existsById(classId)).thenReturn(true);

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("EVENT_OWNER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_EVENT_OWNER")))
        );

        Event qrDraft = new Event(
                eventId, ownerId, "Sự kiện QR", "Mô tả", "Hội trường A",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.DYNAMIC_QR, 10, 3, 50, "Workshop", 5, Set.of(classId)
        );
        qrDraft.setVersion(0);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(qrDraft));

        EventUpsert switchToGpsWithoutCoords = new EventUpsert(
                "Sự kiện chuyển GPS", "Mô tả", "Sân vận động",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.GPS, 50, Set.of(classId), "Workshop", 5, 0,
                null, null, 60
        );

        assertThatThrownBy(() -> eventService.update(eventId, switchToGpsWithoutCoords))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("update() chuyển từ GPS sang DYNAMIC_QR hợp lệ không bắt buộc tọa độ GPS (QE-55)")
    void updateSwitchingGpsBackToDynamicQrSucceeds() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-24T12:00:00Z");
        when(orgClassRepository.existsById(classId)).thenReturn(true);
        when(orgClassRepository.findAllById(any())).thenReturn(List.of());
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("tok")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("EVENT_OWNER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of(new SimpleGrantedAuthority("ROLE_EVENT_OWNER")))
        );

        Event gpsDraft = new Event(
                eventId, ownerId, "Sự kiện GPS", "Mô tả", "Sân vận động",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.GPS, 10, 3, 50, "Workshop", 5, Set.of(classId)
        );
        gpsDraft.setVersion(0);
        gpsDraft.setGpsLat(13.7589);
        gpsDraft.setGpsLng(109.2183);
        gpsDraft.setAllowedRadiusMeters(60);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(gpsDraft));

        EventUpsert switchToQr = new EventUpsert(
                "Sự kiện chuyển QR", "Mô tả", "Hội trường A",
                now.plusSeconds(7200), now.plusSeconds(14400),
                now.plusSeconds(3600), now.plusSeconds(7000), now.plusSeconds(15000),
                AttendanceMethod.DYNAMIC_QR, 50, Set.of(classId), "Workshop", 5, 0,
                null, null, null
        );

        EventDetail updated = eventService.update(eventId, switchToQr);

        assertThat(updated.attendanceMethod()).isEqualTo(AttendanceMethod.DYNAMIC_QR);
        assertThat(updated.gpsLat()).isNull();
        assertThat(updated.gpsLng()).isNull();
        assertThat(updated.allowedRadiusMeters()).isNull();
    }
}
