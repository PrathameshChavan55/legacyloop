package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.EventPublisher;
import com.legacyloop.common.Events;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.BillingDtos.CheckoutResponse;
import com.legacyloop.user.dto.BillingDtos.OrderResponse;
import com.legacyloop.user.dto.BillingDtos.PaymentResultResponse;
import com.legacyloop.user.dto.BillingDtos.PlanRequest;
import com.legacyloop.user.dto.BillingDtos.PlanResponse;
import com.legacyloop.user.dto.BillingDtos.SubscriptionResponse;
import com.legacyloop.user.dto.BillingDtos.VerifyPaymentRequest;
import com.legacyloop.user.entity.AuditLog;
import com.legacyloop.user.entity.PaymentOrder;
import com.legacyloop.user.entity.Plan;
import com.legacyloop.user.entity.Subscription;
import com.legacyloop.user.entity.User;
import com.legacyloop.user.repository.PaymentOrderRepository;
import com.legacyloop.user.repository.PlanRepository;
import com.legacyloop.user.repository.SubscriptionRepository;
import com.legacyloop.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Premium membership: plans, checkout, signature verification and subscriptions.
 *
 * <p>Two things about this class are the whole reason payments live in user-service.
 *
 * <p>First, activating premium is now a field update in the same transaction as the payment.
 * In the original, payment-service published a {@code UserPremiumChangedEvent} which auth-service
 * consumed to flip a flag — a broker round trip, an idempotency table and an eventual-consistency
 * window, to set a boolean on a row the same database already held.
 *
 * <p>Second, the original had a {@code PaymentGateway} interface with a Razorpay implementation
 * and a stub implementation chosen by a profile, plus a separate signature verifier and a webhook
 * controller. Here the stub is a branch on whether a key is configured, because that is all the
 * abstraction was ever deciding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final PlanRepository plans;
    private final PaymentOrderRepository orders;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final AuditService audit;
    private final EventPublisher events;

    @Value("${legacyloop.razorpay.key-id:}")
    private String keyId;

    @Value("${legacyloop.razorpay.key-secret:}")
    private String keySecret;

    /** With no key configured the flow runs end to end without contacting Razorpay. */
    private boolean stubMode() {
        return keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank();
    }

    /* ---------------------------------------------------------------------------- plans */

    @Transactional(readOnly = true)
    public List<PlanResponse> activePlans() {
        return plans.findByActiveTrueOrderByDisplayOrderAscAmountPaiseAsc().stream()
                .map(PlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse plan(Long planId) {
        return PlanResponse.from(loadPlan(planId));
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        if (plans.existsByCode(request.code())) {
            throw ApiException.conflict("A plan with that code already exists");
        }
        Plan plan = Plan.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .amountPaise(request.amountPaise())
                .durationDays(request.durationDays())
                .features(new java.util.ArrayList<>(request.features()))
                .recommended(Boolean.TRUE.equals(request.recommended()))
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();
        return PlanResponse.from(plans.save(plan));
    }

    @Transactional
    public PlanResponse updatePlan(Long planId, PlanRequest request) {
        Plan plan = loadPlan(planId);
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setAmountPaise(request.amountPaise());
        plan.setDurationDays(request.durationDays());
        plan.setFeatures(new java.util.ArrayList<>(request.features()));
        plan.setRecommended(Boolean.TRUE.equals(request.recommended()));
        if (request.displayOrder() != null) {
            plan.setDisplayOrder(request.displayOrder());
        }
        return PlanResponse.from(plan);
    }

    /** Withdrawn, not deleted: existing subscriptions still point at it. */
    @Transactional
    public PlanResponse withdrawPlan(Long planId) {
        Plan plan = loadPlan(planId);
        plan.setActive(false);
        return PlanResponse.from(plan);
    }

    /* ------------------------------------------------------------------------- checkout */

    @Transactional
    public CheckoutResponse createOrder(Long userId, Long planId) {
        Plan plan = loadPlan(planId);
        if (!plan.isActive()) {
            throw ApiException.badRequest("That plan is no longer on sale");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));

        // Save order with placeholder ID first
        PaymentOrder order = orders.save(PaymentOrder.builder()
                .userId(userId)
                .planId(plan.getId())
                .planName(plan.getName())
                .amountPaise(plan.getAmountPaise())
                .gatewayOrderId("pending")
                .build());

        String gatewayOrderId;
        if (stubMode()) {
            gatewayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        } else {
            gatewayOrderId = callRazorpayToCreateOrder(order.getId(), plan.getAmountPaise());
        }

        order.setGatewayOrderId(gatewayOrderId);
        orders.save(order);

        log.info("Created order {} (gateway ID: {}) for user {} on plan {}", order.getId(), gatewayOrderId, userId, plan.getCode());
        return new CheckoutResponse(order.getId(), order.getGatewayOrderId(), keyId, order.getAmountPaise(),
                order.getCurrency(), plan.getName(), user.getEmail(), stubMode());
    }

    private String callRazorpayToCreateOrder(Long orderId, long amountPaise) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String authHeader = "Basic " + Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            
            String requestBody = String.format("{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"receipt_%d\"}", amountPaise, orderId);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Razorpay order creation failed. Status: {}, Body: {}", response.statusCode(), response.body());
                throw ApiException.badRequest("Razorpay order creation failed: " + response.body());
            }
            
            String body = response.body();
            int idIndex = body.indexOf("\"id\":\"");
            if (idIndex == -1) {
                throw ApiException.badRequest("No order ID returned from Razorpay");
            }
            int start = idIndex + 6;
            int end = body.indexOf("\"", start);
            return body.substring(start, end);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Exception calling Razorpay: {}", ex.getMessage(), ex);
            throw ApiException.badRequest("Could not reach Razorpay API: " + ex.getMessage());
        }
    }

    /**
     * Confirms a payment and starts the membership.
     *
     * <p>The signature proves the callback really came from Razorpay: it is an HMAC of
     * "orderId|paymentId" keyed with our secret, which only the two parties know.
     */
    @Transactional
    public PaymentResultResponse verify(Long userId, VerifyPaymentRequest request) {
        PaymentOrder order = orders.findByGatewayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Order", request.razorpayOrderId()));

        if (!order.getUserId().equals(userId)) {
            throw ApiException.forbidden("That order belongs to a different account");
        }
        if (order.getStatus() == PaymentOrder.Status.PAID) {
            // A double callback must not extend the membership twice.
            Subscription existing = currentSubscriptionEntity(userId);
            return new PaymentResultResponse(order.getId(), order.getStatus().name(),
                    existing == null ? null : existing.getId(),
                    existing == null ? null : existing.getExpiresAt(), "This payment was already confirmed");
        }
        if (!stubMode() && !signatureMatches(request)) {
            order.setStatus(PaymentOrder.Status.FAILED);
            order.setFailureReason("Signature mismatch");
            throw ApiException.badRequest("We could not verify that payment");
        }

        order.setStatus(PaymentOrder.Status.PAID);
        order.setGatewayPaymentId(request.razorpayPaymentId());
        order.setPaidAt(Instant.now());

        Subscription subscription = activate(order);
        return new PaymentResultResponse(order.getId(), order.getStatus().name(), subscription.getId(),
                subscription.getExpiresAt(), "Premium is active. Enjoy LegacyLoop.");
    }

    /**
     * Starts or extends the membership, and flips the flag on the user in the same transaction.
     * Extending from the later of "now" and the current expiry means paying early is never a loss.
     */
    private Subscription activate(PaymentOrder order) {
        Plan plan = loadPlan(order.getPlanId());
        User user = users.findById(order.getUserId())
                .orElseThrow(() -> ApiException.notFound("User", order.getUserId()));

        Subscription current = currentSubscriptionEntity(user.getId());
        Instant start = current != null && current.getExpiresAt().isAfter(Instant.now())
                ? current.getExpiresAt() : Instant.now();
        Instant expiry = start.plus(Duration.ofDays(plan.getDurationDays()));

        if (current != null) {
            current.setStatus(Subscription.Status.EXPIRED);
        }
        Subscription subscription = subscriptions.save(Subscription.builder()
                .userId(user.getId())
                .planId(plan.getId())
                .planName(plan.getName())
                .orderId(order.getId())
                .startedAt(Instant.now())
                .expiresAt(expiry)
                .build());

        user.setPremium(true);
        user.setPremiumUntil(expiry);

        audit.record(user.getId(), user.getEmail(), AuditLog.Action.PREMIUM_ACTIVATED, plan.getName());
        events.publish(Events.PREMIUM_CHANGED, user.getId(), "Premium activated",
                "Your %s membership is active until %s.".formatted(plan.getName(), expiry), "/premium/billing");
        return subscription;
    }

    private boolean signatureMatches(VerifyPaymentRequest request) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            // Constant-time compare: a byte-by-byte one leaks where the mismatch starts.
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    request.razorpaySignature().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Could not compute the payment signature: {}", ex.getMessage());
            return false;
        }
    }

    /* -------------------------------------------------------------------- subscriptions */

    @Transactional(readOnly = true)
    public SubscriptionResponse currentSubscription(Long userId) {
        Subscription current = currentSubscriptionEntity(userId);
        return current == null ? null : SubscriptionResponse.from(current);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> history(Long userId, Pageable pageable) {
        return PageResponse.of(subscriptions.findByUserIdOrderByIdDesc(userId, pageable),
                SubscriptionResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> orders(Long userId, Pageable pageable) {
        return PageResponse.of(orders.findByUserIdOrderByIdDesc(userId, pageable), OrderResponse::from);
    }

    /** Cancelling stops renewal; the period already paid for is left alone. */
    @Transactional
    public SubscriptionResponse cancel(Long userId, String reason) {
        Subscription current = currentSubscriptionEntity(userId);
        if (current == null) {
            throw ApiException.badRequest("You do not have an active membership");
        }
        current.setStatus(Subscription.Status.CANCELLED);
        current.setCancelledAt(Instant.now());
        current.setCancellationReason(reason);
        audit.record(userId, null, AuditLog.Action.PREMIUM_CANCELLED, reason);
        return SubscriptionResponse.from(current);
    }

    private Subscription currentSubscriptionEntity(Long userId) {
        return subscriptions.findFirstByUserIdAndStatusOrderByExpiresAtDesc(userId, Subscription.Status.ACTIVE)
                .orElseGet(() -> subscriptions
                        .findFirstByUserIdAndStatusOrderByExpiresAtDesc(userId, Subscription.Status.CANCELLED)
                        .filter(Subscription::isCurrent)
                        .orElse(null));
    }

    private Plan loadPlan(Long planId) {
        return plans.findById(planId).orElseThrow(() -> ApiException.notFound("Plan", planId));
    }

    /** Nightly: lapse memberships whose period has run out. */
    @Transactional
    public int expireLapsedMemberships() {
        List<Subscription> lapsed = subscriptions.findByStatusAndExpiresAtBefore(
                Subscription.Status.ACTIVE, Instant.now());
        lapsed.forEach(subscription -> subscription.setStatus(Subscription.Status.EXPIRED));

        List<User> lapsedUsers = users.findLapsedPremium();
        lapsedUsers.forEach(user -> {
            user.setPremium(false);
            user.setPremiumUntil(null);
        });
        return lapsedUsers.size();
    }
}
