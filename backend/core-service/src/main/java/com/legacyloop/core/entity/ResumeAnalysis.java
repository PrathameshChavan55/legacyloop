package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Output of the AI worker (SRS REQ-6.2). */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resume_analysis",
        indexes = @Index(name = "idx_analysis_student", columnList = "student_user_id"))
public class ResumeAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private Application application;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "matched_skills", length = 1000)
    private String matchedSkills;

    @Column(name = "missing_skills", length = 1000)
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "ai_provider", length = 20)
    private String aiProvider;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @Column(name = "completed_at")
    private Instant completedAt;
}
