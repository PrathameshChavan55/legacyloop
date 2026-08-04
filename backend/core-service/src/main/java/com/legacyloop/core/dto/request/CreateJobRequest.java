package com.legacyloop.core.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateJobRequest(

        @NotBlank(message = "Job title is required")
        @Size(min = 3, max = 150)
        String title,

        @NotNull(message = "Company is required")
        Long companyId,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 20000)
        String description,

        @NotBlank(message = "At least one required skill is needed")
        @Size(max = 500)
        String requiredSkills,

        @Size(max = 120)
        String location,

        @Pattern(regexp = "^(FULL_TIME|PART_TIME|INTERNSHIP|CONTRACT)$",
                message = "Employment type must be FULL_TIME, PART_TIME, INTERNSHIP or CONTRACT")
        String employmentType,

        @DecimalMin(value = "0.0", message = "Salary cannot be negative")
        BigDecimal salaryMin,

        @DecimalMin(value = "0.0", message = "Salary cannot be negative")
        BigDecimal salaryMax,

        @Min(0) @Max(50)
        Integer experienceYears,

        @Min(value = 0, message = "Referral slots cannot be negative")
        @Max(value = 100, message = "That is an unrealistic number of referral slots")
        Integer referralSlots,

        @FutureOrPresent(message = "The deadline cannot be in the past")
        LocalDate applicationDeadline) {
}
