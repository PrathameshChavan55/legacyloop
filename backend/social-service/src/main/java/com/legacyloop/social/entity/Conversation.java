package com.legacyloop.social.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A one-to-one thread.
 *
 * <p>The last message and the unread counts are kept on the conversation so the inbox is one query
 * — without them, listing ten threads means ten "latest message" lookups and ten counts.
 */
@Document("conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;

    @Indexed
    private List<Long> participantIds;

    private String lastMessagePreview;

    private Long lastMessageSenderId;

    private Instant lastMessageAt;

    /** User id to the number of messages they have not read. */
    @Builder.Default
    private Map<String, Integer> unread = new LinkedHashMap<>();

    public int unreadFor(Long userId) {
        return unread == null ? 0 : unread.getOrDefault(String.valueOf(userId), 0);
    }

    public Long otherParticipant(Long userId) {
        return participantIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(userId);
    }
}
