package com.legacyloop.common.dto;

import java.io.Serializable;

/**
 * The only shape of "another service's user" that core-, feed- and payment-service ever see.
 * Returned by auth-service on /internal/v1/users/**, cached in Redis for 10 minutes.
 *
 * <p>Serializable because it is stored in the Redis cache.</p>
 */
public record UserSummaryDto(
        Long userId,
        String fullName,
        String email,
        String role,
        String avatarUrl,
        String headline,
        boolean premium,
        boolean active) implements Serializable {

    /**
     * Degraded value returned by the Feign fallback when auth-service is unreachable.
     * A feed page must still render when the user service is briefly down.
     */
    public static UserSummaryDto unknown(Long userId) {
        return new UserSummaryDto(userId, "Unknown user", null, null, null, null, false, false);
    }
}
