package com.legacyloop.core.dto.response;

import java.util.List;

public record AlumniProfileResponse(
        Long id,
        Long userId,
        String fullName,
        Integer graduationYear,
        String companyName,
        String designation,
        List<String> expertise,
        boolean mentorshipAvailable,
        String linkedinUrl) {
}
