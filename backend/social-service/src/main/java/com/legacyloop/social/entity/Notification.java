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

/**
 * One line in somebody's inbox.
 *
 * <p>Created either directly (someone commented on your post) or from a RabbitMQ event published
 * by another service (your application moved on). Both paths end in the same document, which is
 * why notifications live here rather than in a service of their own — a fourth service whose only
 * job was to write this document would need the same broker, the same WebSocket and the same
 * database as this one.
 */
@Document("notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String type;

    private String title;

    private String body;

    /** Where clicking it should take the reader, e.g. {@code /applications/12}. */
    private String link;

    @Builder.Default
    private boolean read = false;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    private Instant readAt;
}
