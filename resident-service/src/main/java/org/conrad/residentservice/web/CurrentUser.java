package org.conrad.residentservice.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

final class CurrentUser {

    private CurrentUser() {
    }

    static String name() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "unknown";
    }

    static Long requireResidentId() {
        try {
            return Long.valueOf(name());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only residents can use this endpoint");
        }
    }
}
