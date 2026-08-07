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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/** A student's academic record and public profile. Created empty when the account is verified. */
@Entity
@Table(name = "student_profiles", indexes = @Index(name = "idx_student_user", columnList = "user_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "program_id")
    private Long programId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "tenth_percentage", precision = 5, scale = 2)
    private BigDecimal tenthPercentage;

    @Column(name = "twelfth_percentage", precision = 5, scale = 2)
    private BigDecimal twelfthPercentage;

    @Column(name = "graduation_degree", length = 120)
    private String graduationDegree;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(nullable = false)
    @Builder.Default
    private Integer backlogs = 0;

    @Column(length = 160)
    private String headline;

    @Column(length = 2000)
    private String about;

    @Column(length = 120)
    private String location;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    @ElementCollection
    @CollectionTable(name = "student_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", length = 80)
    @Builder.Default
    private Set<String> skills = new LinkedHashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean placed = false;

    @Column(name = "placed_company", length = 160)
    private String placedCompany;

    @Column(name = "placed_package", precision = 12, scale = 2)
    private BigDecimal placedPackage;

    @Column(name = "profile_visible", nullable = false)
    @Builder.Default
    private boolean profileVisible = true;

    @Column(name = "open_to_work", nullable = false)
    @Builder.Default
    private boolean openToWork = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * How much of the profile is filled in, as a percentage of eight fields that matter to a
     * recruiter. Shown as a nudge on the profile page.
     */
    public int completeness() {
        int filled = 0;
        if (headline != null && !headline.isBlank()) filled++;
        if (about != null && !about.isBlank()) filled++;
        if (cgpa != null) filled++;
        if (batchId != null) filled++;
        if (!skills.isEmpty()) filled++;
        if (profilePhotoUrl != null && !profilePhotoUrl.isBlank()) filled++;
        if (linkedinUrl != null && !linkedinUrl.isBlank()) filled++;
        if (graduationYear != null) filled++;
        return filled * 100 / 8;
    }
}
