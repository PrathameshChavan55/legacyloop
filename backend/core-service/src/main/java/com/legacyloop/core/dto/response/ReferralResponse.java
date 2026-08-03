package com.legacyloop.core.dto.response;

import java.time.Instant;

public record ReferralResponse(
        Long id,
        Long applicationId,
        Long jobId,
        String jobTitle,
        Long alumniUserId,
        Long studentUserId,
        String referralCode,
        String note,
        Instant grantedAt) {
}
