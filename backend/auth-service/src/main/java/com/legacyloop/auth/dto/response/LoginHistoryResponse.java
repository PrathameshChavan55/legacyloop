package com.legacyloop.auth.dto.response;

import java.time.Instant;

public record LoginHistoryResponse(
        Long id,
        boolean successful,
        String failureReason,
        String ipAddress,
        String userAgent,
        Instant loggedInAt) {
}
