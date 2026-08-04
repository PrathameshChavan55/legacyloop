package com.legacyloop.auth.dto.response;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String action,
        String targetType,
        String targetId,
        String details,
        String correlationId,
        Instant occurredAt) {
}
