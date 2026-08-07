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
 * A comment, or a reply to one.
 *
 * <p>Comments stay in their own collection rather than being embedded in the post: a popular post
 * can gather hundreds, and a document that grows without limit is the one thing a document store
 * handles badly. Replies are the same shape with a parent, one level deep.
 */
@Document("comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    private String id;

    @Indexed
    private String postId;

    /** Null for a top-level comment; the comment id for a reply. */
    @Indexed
    private String parentId;

    @Indexed
    private Long authorId;

    private String content;

    @Builder.Default
    private int replyCount = 0;

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private Instant createdAt;
}
