package org.conrad.residentservice.dto;

public record ApartmentResponse(
        Long id,
        String apartmentNumber,
        Integer floor,
        String buildingName,
        int residentCount
) {}
