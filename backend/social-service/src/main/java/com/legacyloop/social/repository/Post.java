package com.legacyloop.social.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * A feed post.
 *
 * <p>Reactions and saves are embedded here rather than kept in their own collections. The original
 * had {@code reactions}, {@code saved_posts} and {@code hashtags} collections, which meant three
 * extra queries to render one card and a counter to keep in step. A reaction is a value that only
 * ever exists inside a post, and Mongo can index into a map, so it lives in the document — and the
 * counts are then just map sizes rather than numbers that can drift.
 */
@Document("posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    private String id;

    @Indexed
    private Long authorId;

    private String content;

    private List<String> imageUrls;

    private String linkUrl;

    /** Lower-case, without the hash, extracted from the content when the post is written. */
    @Indexed
    private Set<String> hashtags;

    /** User id to reaction type (LIKE, CELEBRATE, SUPPORT, INSIGHTFUL). */
    @Builder.Default
    private Map<String, String> reactions = new LinkedHashMap<>();

    /** Ids of the people who saved this post. */
    @Builder.Default
    private Set<Long> savedBy = new LinkedHashSet<>();

    @Builder.Default
    private int commentCount = 0;

    @Builder.Default
    private boolean edited = false;

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    public int reactionCount() {
        return reactions == null ? 0 : reactions.size();
    }

    /** What this viewer reacted with, or null. */
    public String reactionOf(Long userId) {
        return reactions == null ? null : reactions.get(String.valueOf(userId));
    }

    public boolean isSavedBy(Long userId) {
        return savedBy != null && savedBy.contains(userId);
    }

    /** Counts per reaction type, for the breakdown under a post. */
    public Map<String, Long> reactionBreakdown() {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (reactions != null) {
            reactions.values().forEach(type -> counts.merge(type, 1L, Long::sum));
        }
        return counts;
    }
}
