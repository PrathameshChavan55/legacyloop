package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.BillingDtos.CheckoutResponse;
import com.legacyloop.user.dto.BillingDtos.CreateOrderRequest;
import com.legacyloop.user.dto.BillingDtos.OrderResponse;
import com.legacyloop.user.dto.BillingDtos.PaymentResultResponse;
import com.legacyloop.user.dto.BillingDtos.PlanRequest;
import com.legacyloop.user.dto.BillingDtos.PlanResponse;
import com.legacyloop.user.dto.BillingDtos.SubscriptionResponse;
import com.legacyloop.user.dto.BillingDtos.VerifyPaymentRequest;
import com.legacyloop.user.dto.UserDtos.ReasonRequest;
import com.legacyloop.user.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plans, checkout and subscriptions in one controller, because they are one screen flow:
 * pick a plan, pay, see the receipt.
 */
@Tag(name = "Billing", description = "Premium plans, Razorpay checkout and subscriptions")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillingController { 

    private final BillingService billingService;

    @GetMapping("/plans")
    @Operation(summary = "Plans on sale — public, so the pricing page renders signed out")
    public ApiResponse<List<PlanResponse>> plans() {
        return ApiResponse.ok(billingService.activePlans());
    }

    @GetMapping("/plans/{planId}")
    @Operation(summary = "One plan")
    public ApiResponse<PlanResponse> plan(@PathVariable Long planId) {
        return ApiResponse.ok(billingService.plan(planId));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create a plan")
    public ApiResponse<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(billingService.createPlan(request), "Plan created");
    }

    @PutMapping("/plans/{planId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update a plan")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable Long planId,
                                                @Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(billingService.updatePlan(planId, request), "Plan updated");
    }

    @PatchMapping("/plans/{planId}/withdraw")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Take a plan off sale without deleting it")
    public ApiResponse<PlanResponse> withdrawPlan(@PathVariable Long planId) {
        return ApiResponse.ok(billingService.withdrawPlan(planId), "Plan withdrawn");
    }

    @PostMapping("/orders")
    @Operation(summary = "Start a checkout and get everything the Razorpay widget needs")
    public ApiResponse<CheckoutResponse> createOrder(@AuthenticationPrincipal AuthUser user,
                                                     @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(billingService.createOrder(user.id(), request.planId()));
    }

    @GetMapping("/orders")
    @Operation(summary = "Your payment history")
    public ApiResponse<PageResponse<OrderResponse>> orders(@AuthenticationPrincipal AuthUser user,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(billingService.orders(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @PostMapping("/payments/verify")
    @Operation(summary = "Confirm a payment's signature and activate premium")
    public ApiResponse<PaymentResultResponse> verify(@AuthenticationPrincipal AuthUser user,
                                                     @Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(billingService.verify(user.id(), request), "Payment confirmed");
    }

    @GetMapping("/subscriptions/current")
    @Operation(summary = "Your membership, or null when you have never subscribed")
    public ApiResponse<SubscriptionResponse> current(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(billingService.currentSubscription(user.id()));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Every membership period you have had")
    public ApiResponse<PageResponse<SubscriptionResponse>> subscriptions(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(billingService.history(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @DeleteMapping("/subscriptions/current")
    @Operation(summary = "Stop renewal; the period already paid for is kept")
    public ApiResponse<SubscriptionResponse> cancel(@AuthenticationPrincipal AuthUser user,
                                                    @RequestBody(required = false) ReasonRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(billingService.cancel(user.id(), reason), "Membership cancelled");
    }
}
