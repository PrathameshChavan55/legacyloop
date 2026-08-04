package com.legacyloop.auth.dto.response;

import java.time.Instant;
import java.util.Set;

/**
 * Both {@code studentIdentifier} and {@code prn} are serialised. The SPA reads the first,
 * anything built against 1.0 reads the second, and MapStruct fills both from the entity -
 * {@code prn} via the deprecated delegate. Drop {@code prn} once no client reads it.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String contactNumber,
        String status,
        boolean emailVerified,
        boolean premium,
        Instant premiumUntil,

        String studentIdentifier,
        String prn,

        Long institutionId,
        String batchId,
        Long batchRefId,

        Integer graduationYear,
        String companyName,
        String designation,
        String avatarUrl,
        String headline,
        Set<String> roles,
        Instant lastLoginAt,
        Instant createdAt) {
}
