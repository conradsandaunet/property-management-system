package org.conrad.residentservice.dto;

import jakarta.validation.constraints.Size;

/** A field left out (null) is not changed, so these only limit what is sent. */
public record ApartmentUpdateRequest(
        @Size(max = 10) String apartmentNumber,
        Integer floor,
        Long buildingId
) {
}
