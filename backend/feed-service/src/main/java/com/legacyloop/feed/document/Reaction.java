package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** The unique index is what makes "like" idempotent - a double tap cannot double count. */
@Document("reactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "post_user_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
public class Reaction {

    @Id
    private String id;

    private String postId;

    private Long userId;

    @Builder.Default
    private String type = "LIKE";

    @CreatedDate
    private Instant createdAt;
}
