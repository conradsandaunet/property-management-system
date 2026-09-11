package org.conrad.residentservice.dto;

public record MeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean manager
) {
}
