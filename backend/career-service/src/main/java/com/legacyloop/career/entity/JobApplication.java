package com.legacyloop.career.entity;

import com.legacyloop.career.entity.Enums.ApplicationStatus;
import jakarta.persistence.Column;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** One student's application to one job. The unique constraint is what stops double-applying. */
@Entity
@Table(name = "job_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_job_user",
                columnNames = {"job_id", "applicant_user_id"}),
        indexes = @Index(name = "idx_application_applicant", columnList = "applicant_user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "cover_letter", length = 5000)
    private String coverLetter;

    /** Visible to staff only; the applicant never sees this field. */
    @Column(name = "reviewer_notes", length = 2000)
    private String reviewerNotes;

    /** The last message a reviewer sent with a status change; this one the applicant does see. */
    @Column(name = "status_message", length = 500)
    private String statusMessage;

    @Column(name = "interview_at")
    private Instant interviewAt;

    @Column(name = "interview_location", length = 255)
    private String interviewLocation;

    @Column(name = "offered_package", precision = 12, scale = 2)
    private BigDecimal offeredPackage;

    @Column(name = "referral_count", nullable = false)
    @Builder.Default
    private int referralCount = 0;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private Instant appliedAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;
}

