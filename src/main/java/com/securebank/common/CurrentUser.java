package com.securebank.common;

import com.securebank.auth.Role;

/**
 * The logged-in user, read from the JWT by JwtInterceptor
 * and stored on the request so controllers can use it.
 */
public record CurrentUser(Long id, String email, Role role) {

    public static final String REQUEST_ATTRIBUTE = "currentUser";

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
