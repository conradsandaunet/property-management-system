package org.conrad.residentservice.dto;

public record ApartmentCreateRequest(
        String apartmentNumber,
        Integer floor,
        Long buildingId
) {
}
