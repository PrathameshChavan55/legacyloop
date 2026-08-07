package com.legacyloop.user.dto;

import com.legacyloop.user.entity.AlumniProfile;
import com.legacyloop.user.entity.Skill;
import com.legacyloop.user.entity.StudentProfile;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record StudentProfileRequest(
            Long departmentId, Long programId, Long branchId, Long batchId,
            @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal cgpa,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal tenthPercentage,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal twelfthPercentage,
            @Size(max = 120) String graduationDegree,
            @Min(1950) @Max(2100) Integer graduationYear,
            @Min(0) @Max(50) Integer backlogs,
            @Size(max = 160) String headline,
            @Size(max = 2000) String about,
            @Size(max = 120) String location,
            @Size(max = 500) String profilePhotoUrl,
            @Size(max = 300) String linkedinUrl,
            @Size(max = 300) String githubUrl,
            @Size(max = 300) String portfolioUrl,
            Set<String> skills,
            Boolean profileVisible,
            Boolean openToWork) {

        public StudentProfileRequest {
            skills = skills == null ? Set.of() : new LinkedHashSet<>(skills);
        }
    }

    public record AlumniProfileRequest(
            Long programId, Long batchId,
            @Min(1950) @Max(2100) Integer graduationYear,
            @Size(max = 160) String currentCompany,
            @Size(max = 160) String currentDesignation,
            @Size(max = 120) String currentLocation,
            @Size(max = 120) String industry,
            @Min(0) @Max(720) Integer totalExperienceMonths,
            @Size(max = 500) String previousCompanies,
            @Size(max = 160) String headline,
            @Size(max = 2000) String about,
            @Size(max = 500) String profilePhotoUrl,
            @Size(max = 300) String linkedinUrl,
            @Size(max = 300) String githubUrl,
            @Size(max = 300) String portfolioUrl,
            Set<String> skills,
            Boolean willingToRefer,
            Boolean availableForMentorship,
            @Min(0) @Max(50) Integer maxReferralsPerMonth,
            @Size(max = 500) String mentorshipAreas,
            Boolean profileVisible) {

        public AlumniProfileRequest {
            skills = skills == null ? Set.of() : new LinkedHashSet<>(skills);
        }
    }

    /**
     * The names of the academic units are resolved by the service and passed in, so the record
     * itself never triggers a lazy load or an extra query.
     */
    public record StudentProfileResponse(Long id, Long userId, String fullName, String email,
                                         String studentIdentifier, Long institutionId,
                                         Long departmentId, String departmentName,
                                         Long programId, String programName,
                                         Long branchId, String branchName,
                                         Long batchId, String batchName,
                                         BigDecimal cgpa, BigDecimal tenthPercentage,
                                         BigDecimal twelfthPercentage, String graduationDegree,
                                         Integer graduationYear, Integer backlogs, String headline,
                                         String about, String location, String profilePhotoUrl,
                                         String linkedinUrl, String githubUrl, String portfolioUrl,
                                         Set<String> skills, boolean placed, String placedCompany,
                                         BigDecimal placedPackage, boolean profileVisible,
                                         boolean openToWork, int completenessPercentage, Instant updatedAt) {
    }

    public record AlumniProfileResponse(Long id, Long userId, String fullName, String email,
                                        Long institutionId, Long programId, String programName,
                                        Long batchId, String batchName, Integer graduationYear,
                                        String currentCompany, String currentDesignation,
                                        String currentLocation, String industry,
                                        Integer totalExperienceMonths, String experienceLabel,
                                        String previousCompanies, String headline, String about,
                                        String profilePhotoUrl, String linkedinUrl, String githubUrl,
                                        String portfolioUrl, Set<String> skills, boolean willingToRefer,
                                        boolean availableForMentorship, Integer maxReferralsPerMonth,
                                        String mentorshipAreas, boolean profileVisible, Instant updatedAt) {
    }

    /** The card shape used by the directory, mentor list and referrer picker. */
    public record ProfileSummary(Long userId, String fullName, String headline, String photoUrl,
                                 String subtitle, Set<String> skills, boolean alumni, boolean openToWork,
                                 boolean willingToRefer) {

        public static ProfileSummary ofStudent(StudentProfile profile, String fullName, String subtitle) {
            return new ProfileSummary(profile.getUserId(), fullName, profile.getHeadline(),
                    profile.getProfilePhotoUrl(), subtitle,
                    profile.getSkills() == null ? Set.of() : Set.copyOf(profile.getSkills()), false,
                    profile.isOpenToWork(), false);
        }

        public static ProfileSummary ofAlumni(AlumniProfile profile, String fullName) {
            String subtitle = profile.getCurrentDesignation() == null ? profile.getCurrentCompany()
                    : profile.getCurrentDesignation() + " at " + profile.getCurrentCompany();
            return new ProfileSummary(profile.getUserId(), fullName, profile.getHeadline(),
                    profile.getProfilePhotoUrl(), subtitle,
                    profile.getSkills() == null ? Set.of() : Set.copyOf(profile.getSkills()), true, false,
                    profile.isWillingToRefer());
        }
    }

    public record SkillResponse(Long id, String name, String category, long usageCount, boolean approved) {

        public static SkillResponse from(Skill skill) {
            return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory(),
                    skill.getUsageCount(), skill.isApproved());
        }
    }
}
