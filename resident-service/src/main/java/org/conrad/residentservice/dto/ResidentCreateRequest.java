package org.conrad.residentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The limits match the database columns (names 100, email 255, phone 20). BCrypt only uses the first
 * 72 bytes of a password, so longer ones are rejected instead of being silently cut.
 */
public record ResidentCreateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        boolean manager,
        @NotNull Long apartmentId
) {
}
