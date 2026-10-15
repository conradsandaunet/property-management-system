package org.conrad.residentservice.dto;

public record ResidentUpdateRequest(
        String email,
        String phone
) {
}
