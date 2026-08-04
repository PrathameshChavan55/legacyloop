package com.legacyloop.payment.gateway;

import java.math.BigDecimal;

/** Abstracted so Stripe can be added later without touching the service layer. */
public interface PaymentGateway {

    record GatewayOrder(String orderId, int amountInPaise, String currency) {
    }

    GatewayOrder createOrder(BigDecimal amount, String currency, String receipt);

    /** Verifies the checkout callback signature (order_id|payment_id signed with the secret). */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /** Verifies the raw webhook body against the webhook secret. */
    boolean verifyWebhookSignature(String rawBody, String signature);

    String name();
}
