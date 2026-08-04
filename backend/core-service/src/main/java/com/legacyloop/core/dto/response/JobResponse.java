package com.legacyloop.core.dto.response;

import com.legacyloop.common.dto.UserSummaryDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record JobResponse(
        Long id,
        String title,
        CompanyResponse company,
        Long postedByUserId,
        UserSummaryDto postedBy,
        String description,
        List<String> requiredSkills,
        String location,
        String employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Integer experienceYears,
        int referralSlots,
        int referralSlotsUsed,
        String status,
        LocalDate applicationDeadline,
        int applicationCount,
        boolean alreadyApplied,
        Instant createdAt) {

    public JobResponse withPoster(UserSummaryDto poster) {
        return new JobResponse(id, title, company, postedByUserId, poster, description,
                requiredSkills, location, employmentType, salaryMin, salaryMax, experienceYears,
                referralSlots, referralSlotsUsed, status, applicationDeadline, applicationCount,
                alreadyApplied, createdAt);
    }
}
