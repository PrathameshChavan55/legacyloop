package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * The domain profile. Identity (email, password, roles) stays in auth-service; this row is
 * created by the {@code user.registered} consumer, keyed on user id with no cross-database FK.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "students", indexes = @Index(name = "idx_student_user", columnList = "user_id", unique = true))
public class StudentProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 12)
    private String prn;

    @Column(name = "batch_id", length = 40)
    private String batchId;

    @Column(length = 1000)
    private String skills;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(name = "primary_resume_id")
    private Long primaryResumeId;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete;
}
