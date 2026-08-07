package com.legacyloop.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A membership period bought by one payment. */
@Entity
@Table(name = "subscriptions", indexes = @Index(name = "idx_subscription_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    public enum Status { ACTIVE, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    /** Cancelling stops renewal but leaves the paid-for period intact, so this stays true until it lapses. */
    public boolean isCurrent() {
        return status != Status.EXPIRED && expiresAt.isAfter(Instant.now());
    }
}

