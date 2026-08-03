package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateStudentProfileRequest(

        @Size(max = 1000, message = "Skills list is too long")
        String skills,

        @Size(max = 300) String githubUrl,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 5000) String education,
        @Size(max = 5000) String projects) {
}
