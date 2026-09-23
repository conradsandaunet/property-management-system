package org.conrad.reservationservice.dto;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        Long resourceId,
        Long residentId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
