package com.legacyloop.career.entity;

import com.legacyloop.career.entity.Enums.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One AI pass over a resume, optionally against a job.
 *
 * <p>The model's findings are stored as the JSON it returned rather than shredded into three
 * child tables. Nothing queries inside them — the UI renders the lists whole — so a column is
 * enough, and the shape can change without a migration.
 */
@Entity
@Table(name = "resume_analyses", indexes = @Index(name = "idx_analysis_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_title", length = 160)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    /** 0–100. Null until the analysis finishes. */
    private Integer score;

    @Column(length = 2000)
    private String summary;

    @Lob
    @Column(name = "strengths_json", columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Lob
    @Column(name = "gaps_json", columnDefinition = "LONGTEXT")
    private String gapsJson;

    @Lob
    @Column(name = "suggestions_json", columnDefinition = "LONGTEXT")
    private String suggestionsJson;

    @Lob
    @Column(name = "matched_skills_json", columnDefinition = "LONGTEXT")
    private String matchedSkillsJson;

    @Lob
    @Column(name = "missing_skills_json", columnDefinition = "LONGTEXT")
    private String missingSkillsJson;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

