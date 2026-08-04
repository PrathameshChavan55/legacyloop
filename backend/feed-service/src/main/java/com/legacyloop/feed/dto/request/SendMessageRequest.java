package com.legacyloop.feed.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Content is no longer @NotBlank: a message can be a file with nothing typed alongside it,
 * which is how people actually send notes. "At least one of the two" cannot be expressed as
 * a field annotation, so the service enforces it.
 */
public record SendMessageRequest(

        @NotNull(message = "Recipient is required")
        Long recipientId,

        @Size(max = 4000)
        String content,

        /** Key returned by POST /chat/attachments. Null for a plain text message. */
        @Size(max = 300)
        String attachmentKey,

        @Size(max = 255)
        String attachmentName,

        @Size(max = 100)
        String attachmentType,

        Long attachmentSize) {

    /** Convenience for the common case and for existing callers. */
    public SendMessageRequest(Long recipientId, String content) {
        this(recipientId, content, null, null, null, null);
    }
}
