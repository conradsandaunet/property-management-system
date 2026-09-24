package org.conrad.reservationservice.dto;

import org.conrad.reservationservice.model.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long residentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status
) {
}
