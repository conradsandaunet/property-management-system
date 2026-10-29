package org.conrad.residentservice.dto;

/** Fields left null are not changed. */
public record ApartmentUpdateRequest(
        String apartmentNumber,
        Integer floor,
        String buildingName
) {
}
