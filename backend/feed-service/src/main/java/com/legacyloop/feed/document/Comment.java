package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Threaded one level deep: parentCommentId null = top-level, set = a reply. */
@Document("comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "post_recent", def = "{'postId': 1, 'createdAt': 1}")
public class Comment {

    @Id
    private String id;

    private String postId;

    private String parentCommentId;

    private Long authorId;

    private String content;

    private boolean deleted;

    @CreatedDate
    private Instant createdAt;
}
