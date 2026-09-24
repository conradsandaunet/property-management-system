package org.conrad.reservationservice.dto;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        Long resourceId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
