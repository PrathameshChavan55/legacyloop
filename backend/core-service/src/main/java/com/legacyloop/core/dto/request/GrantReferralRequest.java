package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GrantReferralRequest(

        @NotNull(message = "Application id is required")
        Long applicationId,

        @Size(max = 40) String referralCode,
        @Size(max = 500) String note) {
}
