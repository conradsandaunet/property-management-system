package org.conrad.reservationservice.dto;

import java.time.LocalDateTime;

public record BlockResourceRequest(
        LocalDateTime from,
        LocalDateTime to
) {
}
