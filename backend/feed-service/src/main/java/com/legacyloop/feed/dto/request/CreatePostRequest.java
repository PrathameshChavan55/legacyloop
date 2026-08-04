package com.legacyloop.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(

        @NotBlank(message = "A post cannot be empty")
        @Size(max = 5000, message = "Keep posts under 5000 characters")
        String content,

        @Size(max = 4, message = "At most 4 attachments per post")
        List<String> mediaUrls,

        /** Announcements can only be created by a placement head; checked in the service. */
        Boolean announcement,

        Boolean pinned) {
}
