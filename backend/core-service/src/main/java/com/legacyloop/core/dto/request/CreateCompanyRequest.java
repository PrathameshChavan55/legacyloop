package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(

        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 150)
        String name,

        @Size(max = 100) String industry,
        @Size(max = 300) String website,
        @Size(max = 500) String logoUrl,
        @Size(max = 5000) String description,
        @Size(max = 120) String location) {
}
