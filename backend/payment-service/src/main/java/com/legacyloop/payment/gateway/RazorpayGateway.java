package com.legacyloop.payment.gateway;

import com.legacyloop.common.exception.BusinessException;
import com.legacyloop.common.exception.ErrorCode;
import com.legacyloop.payment.constant.PaymentConstants;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class RazorpayGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private RazorpayClient client;

    public RazorpayGateway(@Value("${legacyloop.payment.razorpay.key-id:}") String keyId,
                           @Value("${legacyloop.payment.razorpay.key-secret:}") String keySecret,
                           @Value("${legacyloop.payment.razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    private RazorpayClient client() {
        if (client == null) {
            if (keyId.isBlank() || keySecret.isBlank()) {
                throw new BusinessException(ErrorCode.ORDER_CREATION_FAILED,
                        "Razorpay keys are not configured");
            }
            try {
                client = new RazorpayClient(keyId, keySecret);
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.ORDER_CREATION_FAILED,
                        "Could not initialise the payment gateway", null, ex);
            }
        }
        return client;
    }

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) {
        // Razorpay takes paise. Rounding here, not at display time, keeps the charged amount
        // and the stored amount identical.
        int paise = amount.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(PaymentConstants.PAISE_PER_RUPEE))
                .intValueExact();

        JSONObject request = new JSONObject();
        request.put("amount", paise);
        request.put("currency", currency);
        request.put("receipt", receipt);
        request.put("payment_capture", 1);

        try {
            Order order = client().orders.create(request);
            String orderId = order.get("id");
            log.info("Created Razorpay order {} for {} {}", orderId, amount, currency);
            return new GatewayOrder(orderId, paise, currency);
        } catch (Exception ex) {
            log.error("Razorpay order creation failed", ex);
            throw new BusinessException(ErrorCode.ORDER_CREATION_FAILED,
                    "Could not create the payment order", null, ex);
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (Exception ex) {
            log.warn("Payment signature verification threw: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("RAZORPAY_WEBHOOK_SECRET is not set - refusing to trust the webhook");
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
        } catch (Exception ex) {
            log.warn("Webhook signature verification threw: {}", ex.getMessage());
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }

    @Override
    public String name() {
        return PaymentConstants.GATEWAY_RAZORPAY;
    }
}

