package org.conrad.reservationservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BlockResourceRequest(
        @NotNull LocalDateTime from,
        @NotNull LocalDateTime to
) {
}
