package org.conrad.residentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** A field left out (null) is not changed, so these only limit what is sent. */
public record ResidentBoardUpdateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        Boolean manager,
        Boolean active,
        Long apartmentId
) {
}
