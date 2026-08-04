package com.legacyloop.feed.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.time.Instant;

/** One row in the message list: who, the last thing said, and how much is unread. */
public record ConversationResponse(
        String conversationId,
        UserSummaryDto otherUser,
        String lastMessage,
        Long lastMessageSenderId,
        Instant lastMessageAt,
        long unreadCount,
        boolean connected) {
}
