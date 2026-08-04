package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateAlumniProfileRequest(

        @Size(max = 1000) String expertise,
        Boolean mentorshipAvailable,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 120) String companyName,
        @Size(max = 120) String designation) {
}
