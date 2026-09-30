package org.conrad.reservationservice.dto;

import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.model.ResourceType;

public record ResourceResponse(
        Long id,
        String name,
        ResourceType type,
        ResourceStatus status
) {
}
