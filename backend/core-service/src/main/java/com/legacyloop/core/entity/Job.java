package com.legacyloop.core.entity;

import com.legacyloop.common.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Posted by an alumni or a placement head. {@code postedByUserId} is a plain Long, not a
 * foreign key - users live in another service and another database.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_posted_by", columnList = "posted_by_user_id"),
        @Index(name = "idx_job_deadline", columnList = "application_deadline")
})
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "posted_by_user_id", nullable = false)
    private Long postedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Comma-separated tags: "Java,Spring Boot,React" (SRS REQ-3.2 / REQ-4.1). */
    @Column(name = "required_skills", nullable = false, length = 500)
    private String requiredSkills;

    @Column(length = 120)
    private String location;

    @Column(name = "employment_type", length = 40)
    private String employmentType;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "referral_slots", nullable = false)
    private int referralSlots;

    @Column(name = "referral_slots_used", nullable = false)
    private int referralSlotsUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "application_count", nullable = false)
    private int applicationCount;

    @Version
    private Long version;

    public boolean isOpen() {
        return status == JobStatus.OPEN
                && (applicationDeadline == null || !applicationDeadline.atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC).isBefore(Instant.now().minusSeconds(86400)));
    }

    public boolean hasReferralSlot() {
        return referralSlotsUsed < referralSlots;
    }
}
