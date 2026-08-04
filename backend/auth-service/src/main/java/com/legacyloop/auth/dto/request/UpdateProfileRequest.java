package com.legacyloop.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 2, max = 120, message = "Full name must be 2-120 characters")
        String fullName,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String contactNumber,

        @Size(max = 200)
        String headline,

        @Size(max = 500)
        String avatarUrl,

        @Size(max = 120)
        String companyName,

        @Size(max = 120)
        String designation) {
}
