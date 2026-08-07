package com.legacyloop.career.dto;

import com.legacyloop.career.entity.Resume;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ResumeDtos {

    private ResumeDtos() {
    }

    public record RenameRequest(@NotBlank @Size(max = 160) String label) {
    }

    public record ResumeResponse(Long id, String label, String originalFilename, String contentType,
                                 long sizeBytes, String sizeLabel, boolean primary, boolean textExtracted,
                                 Instant uploadedAt) {

        public static ResumeResponse from(Resume resume) {
            return new ResumeResponse(resume.getId(), resume.getLabel(), resume.getOriginalFilename(),
                    resume.getContentType(), resume.getSizeBytes(), sizeLabel(resume.getSizeBytes()),
                    resume.isPrimary(),
                    resume.getExtractedText() != null && !resume.getExtractedText().isBlank(),
                    resume.getUploadedAt());
        }

        private static String sizeLabel(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            }
            return bytes < 1024 * 1024
                    ? "%.0f KB".formatted(bytes / 1024.0)
                    : "%.1f MB".formatted(bytes / (1024.0 * 1024));
        }
    }
}

