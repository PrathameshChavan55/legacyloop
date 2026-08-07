package com.legacyloop.user.dto;

import com.legacyloop.user.entity.PaymentOrder;
import com.legacyloop.user.entity.Plan;
import com.legacyloop.user.entity.Subscription;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record PlanRequest(
            @NotBlank @Size(max = 48) @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,47}$",
                    message = "Code may contain only upper-case letters, digits and underscores") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull @Min(value = 100, message = "The minimum chargeable amount is 100 paise") Long amountPaise,
            @NotNull @Min(value = 1, message = "A plan must last at least one day") Integer durationDays,
            @Size(max = 15) List<@Size(max = 200) String> features,
            Boolean recommended,
            Integer displayOrder) {

        public PlanRequest {
            code = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
            features = features == null ? List.of() : List.copyOf(features);
        }
    }

    public record PlanResponse(Long id, String code, String name, String description, long amountPaise,
                               BigDecimal amountRupees, String priceLabel, String currency, int durationDays,
                               List<String> features, boolean recommended, boolean active) {

        public static PlanResponse from(Plan plan) {
            return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getDescription(),
                    plan.getAmountPaise(), plan.amountRupees(), plan.priceLabel(), "INR",
                    plan.getDurationDays(), List.copyOf(plan.getFeatures()), plan.isRecommended(),
                    plan.isActive());
        }
    }

    public record CreateOrderRequest(@NotNull(message = "Choose a plan") Long planId) {
    }

    /** Everything Razorpay's checkout widget needs, handed to the browser in one response. */
    public record CheckoutResponse(Long orderId, String gatewayOrderId, String keyId, long amountPaise,
                                   String currency, String planName, String userEmail, boolean stubMode) {
    }

    public record VerifyPaymentRequest(
            @NotBlank @Size(max = 64) String razorpayOrderId,
            @NotBlank @Size(max = 64) String razorpayPaymentId,
            @NotBlank @Size(max = 256) String razorpaySignature) {

        /** Never let a signature reach a log line. */
        @Override
        public String toString() {
            return "VerifyPaymentRequest[order=%s, payment=%s]".formatted(razorpayOrderId, razorpayPaymentId);
        }
    }

    public record PaymentResultResponse(Long orderId, String status, Long subscriptionId,
                                        Instant premiumUntil, String message) {
    }

    public record OrderResponse(Long id, Long planId, String planName, long amountPaise, String currency,
                                String status, String gatewayOrderId, String gatewayPaymentId,
                                Instant createdAt, Instant paidAt) {

        public static OrderResponse from(PaymentOrder order) {
            return new OrderResponse(order.getId(), order.getPlanId(), order.getPlanName(),
                    order.getAmountPaise(), order.getCurrency(), order.getStatus().name(),
                    order.getGatewayOrderId(), order.getGatewayPaymentId(), order.getCreatedAt(),
                    order.getPaidAt());
        }
    }

    public record SubscriptionResponse(Long id, Long planId, String planName, String status,
                                       Instant startedAt, Instant expiresAt, boolean current,
                                       long daysRemaining, Instant cancelledAt) {

        public static SubscriptionResponse from(Subscription subscription) {
            long days = Math.max(0, java.time.Duration.between(Instant.now(),
                    subscription.getExpiresAt()).toDays());
            return new SubscriptionResponse(subscription.getId(), subscription.getPlanId(),
                    subscription.getPlanName(), subscription.getStatus().name(), subscription.getStartedAt(),
                    subscription.getExpiresAt(), subscription.isCurrent(), days, subscription.getCancelledAt());
        }
    }
}
