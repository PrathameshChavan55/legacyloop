package com.legacyloop.payment.service;

import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.payment.dto.request.CreateOrderRequest;
import com.legacyloop.payment.dto.request.VerifyPaymentRequest;
import com.legacyloop.payment.dto.response.OrderResponse;
import com.legacyloop.payment.dto.response.PaymentResponse;
import com.legacyloop.payment.dto.response.PlanResponse;
import com.legacyloop.payment.dto.response.SubscriptionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {

    List<PlanResponse> activePlans();

    OrderResponse createOrder(CreateOrderRequest request);

    /** Optimistic client-side confirmation. The webhook remains the source of truth. */
    PaymentResponse verifyFromClient(VerifyPaymentRequest request);

    PageResponse<PaymentResponse> myPayments(Pageable pageable);

    SubscriptionResponse mySubscription();

    /** Called by the webhook handler once the signature has been verified. */
    void capturePayment(String gatewayOrderId, String gatewayPaymentId);

    void failPayment(String gatewayOrderId, String reason);
}

