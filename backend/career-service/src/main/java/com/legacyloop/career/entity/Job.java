package com.legacyloop.career.entity;

import com.legacyloop.career.entity.Enums.JobStatus;
import com.legacyloop.career.entity.Enums.JobType;
import com.legacyloop.career.entity.Enums.WorkMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A posting.
 *
 * <p>The eligibility rules (CGPA, backlogs, eligible batches) live here rather than in a separate
 * criteria entity: they are three columns and a set, and keeping them on the job means checking
 * eligibility never needs a second load.
 */
@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_company", columnList = "company_id"),
        @Index(name = "idx_job_posted_by", columnList = "posted_by_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 8000)
    private String description;

    @Column(length = 4000)
    private String responsibilities;

    @Column(length = 4000)
    private String requirements;

    /**
     * Eagerly fetched: a job is never shown without its company name, so a lazy proxy would only
     * turn one query into two.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    @Builder.Default
    private WorkMode workMode = WorkMode.ONSITE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    @Column(length = 160)
    private String location;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "min_experience_months")
    private Integer minExperienceMonths;

    @Column(name = "min_cgpa", precision = 4, scale = 2)
    private BigDecimal minCgpa;

    @Column(name = "max_backlogs")
    private Integer maxBacklogs;

    @ElementCollection
    @CollectionTable(name = "job_batches", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "batch_id")
    @Builder.Default
    private Set<Long> eligibleBatchIds = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", length = 80)
    @Builder.Default
    private Set<String> requiredSkills = new LinkedHashSet<>();

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "expected_joining_date")
    private LocalDate expectedJoiningDate;

    private Integer vacancies;

    @Column(name = "referrals_enabled", nullable = false)
    @Builder.Default
    private boolean referralsEnabled = true;

    @Column(name = "posted_by_user_id", nullable = false)
    private Long postedByUserId;

    @Column(name = "institution_id")
    private Long institutionId;

    /** Denormalised counters: every list view shows them, and counting rows per card is N+1. */
    @Column(name = "application_count", nullable = false)
    @Builder.Default
    private int applicationCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isAcceptingApplications() {
        return status == JobStatus.OPEN
                && (applicationDeadline == null || !applicationDeadline.isBefore(LocalDate.now()));
    }

    public String salaryLabel() {
        if (salaryMin == null && salaryMax == null) {
            return "Not disclosed";
        }
        if (salaryMax == null) {
            return "From ₹%s".formatted(salaryMin.toPlainString());
        }
        if (salaryMin == null) {
            return "Up to ₹%s".formatted(salaryMax.toPlainString());
        }
        return "₹%s – ₹%s".formatted(salaryMin.toPlainString(), salaryMax.toPlainString());
    }
}
