package com.legacyloop.social.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** One message in a conversation. */
@Document("messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private Long senderId;

    private String content;

    private String attachmentUrl;

    private Instant readAt;

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private Instant createdAt;
}
