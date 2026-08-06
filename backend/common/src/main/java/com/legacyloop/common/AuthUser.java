package com.legacyloop.common;

import java.util.Set;

/**
 * The signed-in caller, rebuilt from the JWT on every request. Controllers receive it with
 * {@code @AuthenticationPrincipal AuthUser user} — Spring's own annotation, so the original's
 * custom {@code @CurrentUser} annotation and its argument resolver are gone.
 */
public record AuthUser(Long id, String email, String fullName, Set<String> roles,
                       Long institutionId, boolean premium) {

    public boolean hasRole(String role) {
        return roles.contains(Roles.prefixed(role));
    }

    public boolean isAdmin() {
        return hasRole(Roles.ROLE_ADMIN);
    }

    public boolean isStaff() {
        return hasRole(Roles.ROLE_STAFF) || isAdmin();
    }
}
