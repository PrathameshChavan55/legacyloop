package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateApplicationStatusRequest(

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(UNDER_REVIEW|SHORTLISTED|REFERRED|INTERVIEW_SCHEDULED|SELECTED|REJECTED|WITHDRAWN)$",
                message = "Unsupported status")
        String status,

        @Size(max = 500)
        String note) {
}
