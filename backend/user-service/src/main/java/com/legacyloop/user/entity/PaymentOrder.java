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
import org.hibernate.annotations.CreationTimestamp;

/**
 * One checkout attempt, from "order created" to "payment captured".
 *
 * <p>The original split this across a {@code payment_orders} table and a {@code payments} table
 * with a one-to-one between them, plus a {@code webhook_events} table. One row per attempt holds
 * the same information and makes "did this order get paid?" a field rather than a join.
 */
@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_gateway", columnList = "gateway_order_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    public enum Status { CREATED, PAID, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "INR";

    /** The id Razorpay gave us, or a generated stub id when running without gateway keys. */
    @Column(name = "gateway_order_id", nullable = false, length = 64)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 64)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.CREATED;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;
}

