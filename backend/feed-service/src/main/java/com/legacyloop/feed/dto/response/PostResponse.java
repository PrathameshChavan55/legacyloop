package com.legacyloop.feed.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.time.Instant;
import java.util.List;

public record PostResponse(
        String id,
        Long authorId,
        UserSummaryDto author,
        String content,
        List<String> mediaUrls,
        List<String> hashtags,
        String type,
        boolean pinned,
        int likeCount,
        int commentCount,
        boolean likedByMe,
        Instant createdAt) {

    public PostResponse withAuthor(UserSummaryDto summary) {
        return new PostResponse(id, authorId, summary, content, mediaUrls, hashtags, type,
                pinned, likeCount, commentCount, likedByMe, createdAt);
    }

    public PostResponse withLiked(boolean liked) {
        return new PostResponse(id, authorId, author, content, mediaUrls, hashtags, type,
                pinned, likeCount, commentCount, liked, createdAt);
    }
}
