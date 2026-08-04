package com.legacyloop.feed.dto.response;

import java.time.Instant;

public record ChatMessageResponse(
        String id,
        String conversationId,
        Long senderId,
        Long recipientId,
        String content,
        String attachmentUrl,
        String attachmentName,
        String attachmentType,
        Long attachmentSize,
        boolean read,
        Instant sentAt) {
}
