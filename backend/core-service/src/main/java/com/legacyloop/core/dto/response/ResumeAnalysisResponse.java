package com.legacyloop.core.dto.response;

import java.time.Instant;
import java.util.List;

public record ResumeAnalysisResponse(
        Long id,
        Long applicationId,
        Integer atsScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String suggestions,
        String aiProvider,
        String status,
        String failureReason,
        Instant completedAt) {
}
