package com.legacyloop.feed.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        String id,
        String postId,
        String parentCommentId,
        Long authorId,
        UserSummaryDto author,
        String content,
        List<CommentResponse> replies,
        Instant createdAt) {

    public CommentResponse withAuthor(UserSummaryDto summary) {
        return new CommentResponse(id, postId, parentCommentId, authorId, summary, content,
                replies, createdAt);
    }

    public CommentResponse withReplies(List<CommentResponse> children) {
        return new CommentResponse(id, postId, parentCommentId, authorId, author, content,
                children, createdAt);
    }
}
