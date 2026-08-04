package com.legacyloop.core.dto.response;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        boolean primaryResume,
        String downloadUrl,
        int extractedCharacters,
        Instant uploadedAt) {
}
