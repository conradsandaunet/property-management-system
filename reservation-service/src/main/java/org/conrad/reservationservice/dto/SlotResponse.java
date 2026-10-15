package org.conrad.reservationservice.dto;

import java.time.LocalDateTime;

public record SlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean available
) {
}