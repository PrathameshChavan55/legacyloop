package com.legacyloop.core.entity;

import com.legacyloop.common.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_job_student",
                columnNames = {"job_id", "student_user_id"}),
        indexes = {
                @Index(name = "idx_app_student", columnList = "student_user_id"),
                @Index(name = "idx_app_status", columnList = "status")
        })
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private ResumeMetadata resume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "cover_note", columnDefinition = "TEXT")
    private String coverNote;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_changed_by")
    private Long statusChangedBy;

    @OneToOne(mappedBy = "application", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ResumeAnalysis analysis;

    @Version
    private Long version;
}
