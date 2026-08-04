package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "referrals",
        uniqueConstraints = @UniqueConstraint(name = "uk_referral_app",
                columnNames = {"application_id"}),
        indexes = @Index(name = "idx_referral_alumni", columnList = "alumni_user_id"))
public class Referral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "alumni_user_id", nullable = false)
    private Long alumniUserId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "referral_code", length = 40)
    private String referralCode;

    @Column(length = 500)
    private String note;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
}
