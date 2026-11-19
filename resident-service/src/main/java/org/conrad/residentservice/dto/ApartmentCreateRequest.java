package org.conrad.residentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApartmentCreateRequest(
        @NotBlank @Size(max = 10) String apartmentNumber,
        @NotNull Integer floor,
        @NotNull Long buildingId
) {
}
