package com.legacyloop.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacyloop.payment.constant.PaymentConstants;
import com.legacyloop.payment.entity.WebhookEvent;
import com.legacyloop.payment.gateway.PaymentGateway;
import com.legacyloop.payment.repository.WebhookEventRepository;
import com.legacyloop.payment.service.PaymentService;
import com.legacyloop.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * SRS REQ-7.2. Order of operations matters here:
 * verify signature → record the event → act on it. Acting first would mean a forged request
 * could grant premium, and recording second would mean a retry could grant it twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean handle(String rawBody, String signature, String eventId) {
        if (signature == null || !paymentGateway.verifyWebhookSignature(rawBody, signature)) {
            log.error("Rejected webhook with an invalid signature");
            return false;
        }

        String resolvedEventId = (eventId == null || eventId.isBlank())
                ? UUID.nameUUIDFromBytes(rawBody.getBytes()).toString()
                : eventId;

        if (webhookEventRepository.existsByGatewayEventId(resolvedEventId)) {
            log.info("Webhook {} already processed - Razorpay retry", resolvedEventId);
            return true;      // 200 so Razorpay stops retrying
        }

        String eventType = "unknown";
        WebhookEvent record = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            eventType = root.path("event").asText("unknown");

            record = webhookEventRepository.save(WebhookEvent.builder()
                    .gatewayEventId(resolvedEventId)
                    .eventType(eventType)
                    .rawPayload(rawBody.length() > 60000 ? rawBody.substring(0, 60000) : rawBody)
                    .processed(false)
                    .receivedAt(Instant.now())
                    .build());

            JsonNode payment = root.path("payload").path("payment").path("entity");
            String orderId = payment.path("order_id").asText(null);
            String paymentId = payment.path("id").asText(null);

            switch (eventType) {
                case PaymentConstants.EVENT_PAYMENT_CAPTURED ->
                        paymentService.capturePayment(orderId, paymentId);
                case PaymentConstants.EVENT_PAYMENT_FAILED ->
                        paymentService.failPayment(orderId,
                                payment.path("error_description").asText("Payment failed"));
                default -> log.info("Ignoring unhandled webhook event {}", eventType);
            }

            record.setProcessed(true);
            webhookEventRepository.save(record);
            log.info("Processed webhook {} ({})", resolvedEventId, eventType);
            return true;

        } catch (Exception ex) {
            log.error("Webhook {} ({}) failed to process", resolvedEventId, eventType, ex);
            if (record != null) {
                record.setProcessingError(ex.getMessage() == null ? "unknown"
                        : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())));
                webhookEventRepository.save(record);
            }
            // Still 200: the event is recorded and can be replayed. Returning an error would
            // make Razorpay hammer the endpoint for hours.
            return true;
        }
    }
}

