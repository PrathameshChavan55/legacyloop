package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * conversationId is the two user ids sorted and joined ("7:19"), so both participants
 * derive the same key without a lookup table (SRS REQ-8.2).
 */
@Document("chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "conversation_recent", def = "{'conversationId': 1, 'sentAt': -1}")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private Long senderId;

    private Long recipientId;

    /** Optional once an attachment is present - a file on its own is a valid message. */
    private String content;

    /** Storage key, not a URL. The URL is built at read time so the route can move. */
    private String attachmentKey;

    private String attachmentName;

    private String attachmentType;

    private Long attachmentSize;

    private boolean read;

    @CreatedDate
    private Instant sentAt;
}
