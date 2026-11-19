package org.conrad.reservationservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        @NotNull Long resourceId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {
}
