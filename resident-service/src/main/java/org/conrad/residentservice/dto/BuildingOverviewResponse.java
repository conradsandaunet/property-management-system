package org.conrad.residentservice.dto;

public record BuildingOverviewResponse(
        long totalApartments,
        long occupiedApartments,
        long vacantApartments,
        long totalResidents
) {
}
