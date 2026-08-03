package com.legacyloop.core.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,
        Long studentUserId,
        UserSummaryDto student,
        Long resumeId,
        String status,
        String coverNote,
        Instant appliedAt,
        Instant statusChangedAt,
        ResumeAnalysisResponse analysis) {

    public ApplicationResponse withStudent(UserSummaryDto summary) {
        return new ApplicationResponse(id, jobId, jobTitle, companyName, studentUserId, summary,
                resumeId, status, coverNote, appliedAt, statusChangedAt, analysis);
    }
}
