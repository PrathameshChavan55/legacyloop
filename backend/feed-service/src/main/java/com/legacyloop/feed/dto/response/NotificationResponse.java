package com.legacyloop.feed.dto.response;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String body,
        String actionUrl,
        Map<String, String> metadata,
        boolean read,
        Instant createdAt) {
}
