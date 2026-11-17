package vn.edu.qnu.events.event;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetail create(@Valid @RequestBody EventUpsert upsert) {
        return eventService.create(upsert);
    }

    @PutMapping("/{id}")
    public EventDetail update(@PathVariable UUID id, @Valid @RequestBody EventUpsert upsert) {
        return eventService.update(id, upsert);
    }

    @GetMapping("/{id}/preview")
    public PreviewResponse preview(@PathVariable UUID id) {
        return eventService.preview(id);
    }

    @PostMapping("/{id}/publish")
    public EventDetail publish(@PathVariable UUID id, @Valid @RequestBody PublishRequest request) {
        return eventService.publish(id, request);
    }

    @GetMapping
    public Page<EventSummary> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventService.listEvents(q, pageable);
    }

    @GetMapping("/{id}")
    public EventDetail getById(@PathVariable UUID id) {
        return eventService.getEventDetail(id);
    }
}

