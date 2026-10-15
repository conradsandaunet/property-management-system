package org.conrad.residentservice.dto;

public record HouseholdMemberResponse(
        Long id,
        String firstName,
        String lastName
) {
}
