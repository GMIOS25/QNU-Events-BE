package vn.edu.qnu.events.event;

public record PreviewResponse(
        EventDetail event,
        Integer previewVersion
) {}

