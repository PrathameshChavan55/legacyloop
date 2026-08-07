package com.legacyloop.career.dto;

import com.legacyloop.career.dto.CompanyDtos.CompanySummary;
import com.legacyloop.career.entity.Job;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public final class JobDtos {

    private JobDtos() {
    }

    public record JobRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 8000) String description,
            @Size(max = 4000) String responsibilities,
            @Size(max = 4000) String requirements,
            @NotNull(message = "Choose a company") Long companyId,
            @NotBlank String jobType,
            String workMode,
            @Size(max = 160) String location,
            @DecimalMin("0.0") BigDecimal salaryMin,
            @DecimalMin("0.0") BigDecimal salaryMax,
            @Min(0) Integer minExperienceMonths,
            @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal minCgpa,
            @Min(0) Integer maxBacklogs,
            Set<Long> eligibleBatchIds,
            Set<String> requiredSkills,
            LocalDate applicationDeadline,
            LocalDate expectedJoiningDate,
            @Min(1) Integer vacancies,
            Boolean referralsEnabled) {

        public JobRequest {
            eligibleBatchIds = eligibleBatchIds == null ? Set.of() : new LinkedHashSet<>(eligibleBatchIds);
            requiredSkills = requiredSkills == null ? Set.of() : new LinkedHashSet<>(requiredSkills);
        }
    }

    /** The card shown in a list. Deliberately smaller than the detail view. */
    public record JobSummary(Long id, String title, CompanySummary company, String jobType, String jobTypeLabel,
                             String workMode, String status, String location, String salaryLabel,
                             LocalDate applicationDeadline, int applicationCount, boolean acceptingApplications,
                             Set<String> requiredSkills, Instant publishedAt) {

        public static JobSummary from(Job job) {
            return new JobSummary(job.getId(), job.getTitle(), CompanySummary.from(job.getCompany()),
                    job.getJobType().name(), job.getJobType().label(), job.getWorkMode().name(),
                    job.getStatus().name(), job.getLocation(), job.salaryLabel(),
                    job.getApplicationDeadline(), job.getApplicationCount(), job.isAcceptingApplications(),
                    Set.copyOf(job.getRequiredSkills()), job.getPublishedAt());
        }
    }

    /**
     * The detail view. {@code eligibility} and {@code myApplicationId} are filled in per viewer,
     * which is why they are not on the entity.
     */
    public record JobDetail(Long id, String title, String description, String responsibilities,
                            String requirements, CompanyDtos.CompanyResponse company, String jobType,
                            String jobTypeLabel, String workMode, String status, String location,
                            BigDecimal salaryMin, BigDecimal salaryMax, String salaryLabel,
                            Integer minExperienceMonths, BigDecimal minCgpa, Integer maxBacklogs,
                            Set<Long> eligibleBatchIds, Set<String> requiredSkills,
                            LocalDate applicationDeadline, LocalDate expectedJoiningDate, Integer vacancies,
                            boolean referralsEnabled, Long postedByUserId, String postedByName,
                            int applicationCount, int viewCount, boolean acceptingApplications,
                            Instant publishedAt, Instant createdAt, EligibilityCheck eligibility,
                            Long myApplicationId) {

        public static JobDetail from(Job job, String postedByName, EligibilityCheck eligibility,
                                     Long myApplicationId) {
            return new JobDetail(job.getId(), job.getTitle(), job.getDescription(), job.getResponsibilities(),
                    job.getRequirements(), CompanyDtos.CompanyResponse.from(job.getCompany()),
                    job.getJobType().name(), job.getJobType().label(), job.getWorkMode().name(),
                    job.getStatus().name(), job.getLocation(), job.getSalaryMin(), job.getSalaryMax(),
                    job.salaryLabel(), job.getMinExperienceMonths(), job.getMinCgpa(), job.getMaxBacklogs(),
                    Set.copyOf(job.getEligibleBatchIds()), Set.copyOf(job.getRequiredSkills()),
                    job.getApplicationDeadline(), job.getExpectedJoiningDate(), job.getVacancies(),
                    job.isReferralsEnabled(), job.getPostedByUserId(), postedByName,
                    job.getApplicationCount(), job.getViewCount(), job.isAcceptingApplications(),
                    job.getPublishedAt(), job.getCreatedAt(), eligibility, myApplicationId);
        }
    }

    /**
     * Why a student can or cannot apply. Returned with the job so the button and the explanation
     * come from one source rather than being re-derived in the browser.
     */
    public record EligibilityCheck(boolean eligible, java.util.List<String> reasons) {

        public static EligibilityCheck ok() {
            return new EligibilityCheck(true, java.util.List.of());
        }

        public static EligibilityCheck notApplicable() {
            return new EligibilityCheck(false, java.util.List.of());
        }
    }
}
