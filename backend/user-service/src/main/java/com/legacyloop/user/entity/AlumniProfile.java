package com.legacyloop.user.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/** An alumnus's working life: where they are now, and whether they will refer or mentor. */
@Entity
@Table(name = "alumni_profiles", indexes = {
        @Index(name = "idx_alumni_user", columnList = "user_id", unique = true),
        @Index(name = "idx_alumni_company", columnList = "current_company")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumniProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "program_id")
    private Long programId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "current_company", length = 160)
    private String currentCompany;

    @Column(name = "current_designation", length = 160)
    private String currentDesignation;

    @Column(name = "current_location", length = 120)
    private String currentLocation;

    @Column(length = 120)
    private String industry;

    /** Total experience in months; the label shown in the UI is derived, not stored. */
    @Column(name = "total_experience_months")
    private Integer totalExperienceMonths;

    @Column(name = "previous_companies", length = 500)
    private String previousCompanies;

    @Column(length = 160)
    private String headline;

    @Column(length = 2000)
    private String about;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    @ElementCollection
    @CollectionTable(name = "alumni_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", length = 80)
    @Builder.Default
    private Set<String> skills = new LinkedHashSet<>();

    @Column(name = "willing_to_refer", nullable = false)
    @Builder.Default
    private boolean willingToRefer = false;

    @Column(name = "available_for_mentorship", nullable = false)
    @Builder.Default
    private boolean availableForMentorship = false;

    @Column(name = "max_referrals_per_month")
    @Builder.Default
    private Integer maxReferralsPerMonth = 3;

    @Column(name = "mentorship_areas", length = 500)
    private String mentorshipAreas;

    @Column(name = "profile_visible", nullable = false)
    @Builder.Default
    private boolean profileVisible = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** "4 yrs 2 mos", or null when the alumnus has not said. */
    public String experienceLabel() {
        if (totalExperienceMonths == null || totalExperienceMonths <= 0) {
            return null;
        }
        int years = totalExperienceMonths / 12;
        int months = totalExperienceMonths % 12;
        if (years == 0) {
            return months + " mos";
        }
        return months == 0 ? years + " yrs" : years + " yrs " + months + " mos";
    }
}
