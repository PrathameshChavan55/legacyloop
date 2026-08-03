package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "recipient_unread", def = "{'recipientUserId': 1, 'read': 1, 'createdAt': -1}")
public class Notification {

    @Id
    private String id;

    private Long recipientUserId;

    private String type;

    private String title;

    private String body;

    private String actionUrl;

    private Map<String, String> metadata;

    private boolean read;

    @CreatedDate
    private Instant createdAt;
}
