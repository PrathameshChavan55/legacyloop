package com.legacyloop.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(

        @NotBlank(message = "A comment cannot be empty")
        @Size(max = 2000)
        String content,

        /** Null for a top-level comment, set to reply to one. */
        String parentCommentId) {
}
