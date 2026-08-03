package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateJobRequest(

        @Size(min = 3, max = 150)
        String title,

        @Size(min = 20, max = 20000)
        String description,

        @Size(max = 500)
        String requiredSkills,

        @Size(max = 120)
        String location,

        @DecimalMin("0.0") BigDecimal salaryMin,
        @DecimalMin("0.0") BigDecimal salaryMax,

        @Min(0) @Max(100) Integer referralSlots,

        @FutureOrPresent(message = "The deadline cannot be in the past")
        LocalDate applicationDeadline,

        @Pattern(regexp = "^(DRAFT|OPEN|CLOSED)$", message = "Status must be DRAFT, OPEN or CLOSED")
        String status) {
}
