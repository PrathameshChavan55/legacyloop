package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.Size;

/** resumeId may be null - the student's primary resume is used then. */
public record ApplyRequest(

        Long resumeId,

        @Size(max = 2000, message = "Keep the cover note under 2000 characters")
        String coverNote) {
}
