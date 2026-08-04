package com.legacyloop.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Every webhook Razorpay sends is recorded before it is acted on. Razorpay retries
 * aggressively, so the unique gateway_event_id is what stops a retry from granting premium
 * twice.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway_event_id", nullable = false, unique = true, length = 100)
    private String gatewayEventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Lob
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "processing_error", length = 500)
    private String processingError;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}

