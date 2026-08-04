package com.legacyloop.payment.controller;

import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.payment.dto.request.CreateOrderRequest;
import com.legacyloop.payment.dto.request.VerifyPaymentRequest;
import com.legacyloop.payment.dto.response.OrderResponse;
import com.legacyloop.payment.dto.response.PaymentResponse;
import com.legacyloop.payment.dto.response.PlanResponse;
import com.legacyloop.payment.dto.response.SubscriptionResponse;
import com.legacyloop.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Premium plans, Razorpay checkout and subscriptions (SRS 3.7)")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/plans")
    @Operation(summary = "Available premium plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> plans() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.activePlans()));
    }

    @PostMapping("/payments/create-order")
    @Operation(summary = "Start a checkout",
            description = "Returns the Razorpay order id and public key for the checkout widget")
    @PreAuthorize("hasAnyRole('STUDENT','ALUMNI')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.createOrder(request)));
    }

    @PostMapping("/payments/verify")
    @Operation(summary = "Confirm from the browser",
            description = "Verifies the checkout signature for instant UI feedback. "
                    + "The webhook remains the authoritative grant.")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.verifyFromClient(request),
                "Payment verified. Premium is active - sign out and back in to refresh your token."));
    }

    @GetMapping("/payments/mine")
    @Operation(summary = "Your payment history")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> myPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.myPayments(pageable)));
    }

    @GetMapping("/subscriptions/mine")
    @Operation(summary = "Your current subscription", description = "null when not subscribed")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> mySubscription() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.mySubscription()));
    }
}

