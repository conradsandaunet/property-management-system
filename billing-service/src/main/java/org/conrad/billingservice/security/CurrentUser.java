package org.conrad.billingservice.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long apartmentId(Authentication authentication) {
        if (authentication != null
                && authentication.getDetails() instanceof Claims claims
                && claims.get("apartmentId") instanceof Number apartmentId) {
            return apartmentId.longValue();
        }
        // Tokens issued before apartmentId was added to the JWT, or service tokens.
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Token has no apartmentId — log in again to get a new token");
    }
}