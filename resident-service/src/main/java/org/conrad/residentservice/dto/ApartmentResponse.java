package org.conrad.residentservice.dto;

public record ApartmentResponse(
        Long id,
        String apartmentNumber,
        Integer floor,
        Long buildingId,
        String buildingName,
        int residentCount
) {}
