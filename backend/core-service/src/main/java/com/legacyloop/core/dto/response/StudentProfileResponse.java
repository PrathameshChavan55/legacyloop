package com.legacyloop.core.dto.response;

import java.util.List;

public record StudentProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String prn,
        String batchId,
        List<String> skills,
        String githubUrl,
        String linkedinUrl,
        String education,
        String projects,
        Long primaryResumeId,
        boolean profileComplete) {
}
