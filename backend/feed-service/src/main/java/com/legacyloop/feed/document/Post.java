package com.legacyloop.feed.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Pinned announcements sort above everything else - hence the compound index. */
@Document("posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "pinned_recent", def = "{'pinned': -1, 'createdAt': -1}")
public class Post {

    @Id
    private String id;

    @Indexed
    private Long authorId;

    @TextIndexed
    private String content;

    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    @Builder.Default
    private List<String> hashtags = new ArrayList<>();

    /** POST or ANNOUNCEMENT. */
    @Builder.Default
    private String type = "POST";

    private boolean pinned;

    /** Denormalised counters: a feed page must not run a count query per post. */
    @Builder.Default
    private int likeCount = 0;

    @Builder.Default
    private int commentCount = 0;

    private boolean deleted;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
