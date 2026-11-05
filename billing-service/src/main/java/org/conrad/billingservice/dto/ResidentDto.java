package org.conrad.billingservice.dto;

public record ResidentDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String apartmentNumber
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
