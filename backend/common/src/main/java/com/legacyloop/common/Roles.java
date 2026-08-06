package com.legacyloop.common;

import java.util.List;

/** The four roles. Spring's {@code hasRole('X')} matches an authority named {@code ROLE_X}. */
public final class Roles {

    public static final String STUDENT = "STUDENT";
    public static final String ALUMNI = "ALUMNI";
    public static final String STAFF = "INSTITUTION_STAFF";
    public static final String ADMIN = "PLATFORM_ADMIN";

    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_ALUMNI = "ROLE_ALUMNI";
    public static final String ROLE_STAFF = "ROLE_INSTITUTION_STAFF";
    public static final String ROLE_ADMIN = "ROLE_PLATFORM_ADMIN";

    /** Roles a visitor may choose at sign-up; staff and admin accounts are created by an admin. */
    public static final List<String> SELF_REGISTERABLE = List.of(ROLE_STUDENT, ROLE_ALUMNI);

    public static final List<String> ALL = List.of(ROLE_STUDENT, ROLE_ALUMNI, ROLE_STAFF, ROLE_ADMIN);

    private Roles() {
    }

    /** Accepts "STUDENT" or "ROLE_STUDENT" and always returns the prefixed form. */
    public static String prefixed(String role) {
        if (role == null) {
            return null;
        }
        String upper = role.trim().toUpperCase();
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }
}
