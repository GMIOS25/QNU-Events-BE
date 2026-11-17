package vn.edu.qnu.events.event;

import jakarta.validation.constraints.NotNull;

public record PublishRequest(
        boolean confirm,

        @NotNull(message = "previewVersion không được để trống")
        Integer previewVersion
) {}

