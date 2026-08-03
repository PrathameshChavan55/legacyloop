package com.legacyloop.feed.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.time.Instant;

/**
 * Always described from the point of view of whoever asked.
 *
 * <p>{@code otherUser} is the person you are not, and {@code outgoing} says which way the
 * request went - the two facts the UI needs to decide between "Accept / Ignore" and
 * "Request sent". Returning raw requesterId/recipientId and making the client work it out is
 * how you end up with an Accept button on your own outgoing request.</p>
 */
public record ConnectionResponse(
        String id,
        String status,
        boolean outgoing,
        UserSummaryDto otherUser,
        Instant createdAt,
        Instant respondedAt) {
}
