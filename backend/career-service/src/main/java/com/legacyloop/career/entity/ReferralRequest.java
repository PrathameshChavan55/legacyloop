package com.legacyloop.career.entity;

import com.legacyloop.career.entity.Enums.ReferralStatus;
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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** A student asking an alumnus at the company to put their name forward. */
@Entity
@Table(name = "referral_requests", indexes = {
        @Index(name = "idx_referral_referrer", columnList = "referrer_user_id"),
        @Index(name = "idx_referral_requester", columnList = "requester_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "referrer_user_id", nullable = false)
    private Long referrerUserId;

    @Column(length = 2000)
    private String message;

    @Column(length = 2000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.REQUESTED;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private Instant requestedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;
}
