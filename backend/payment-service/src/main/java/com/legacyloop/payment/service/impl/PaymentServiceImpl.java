package com.legacyloop.payment.service.impl;

import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.common.enums.PaymentStatus;
import com.legacyloop.common.enums.SubscriptionStatus;
import com.legacyloop.common.exception.BusinessException;
import com.legacyloop.common.exception.ConflictException;
import com.legacyloop.common.exception.ErrorCode;
import com.legacyloop.common.exception.ResourceNotFoundException;
import com.legacyloop.common.security.AuthenticatedUser;
import com.legacyloop.common.security.SecurityContextUtil;
import com.legacyloop.payment.constant.PaymentConstants;
import com.legacyloop.payment.dto.request.CreateOrderRequest;
import com.legacyloop.payment.dto.request.VerifyPaymentRequest;
import com.legacyloop.payment.dto.response.*;
import com.legacyloop.payment.entity.Payment;
import com.legacyloop.payment.entity.Plan;
import com.legacyloop.payment.entity.Subscription;
import com.legacyloop.payment.gateway.PaymentGateway;
import com.legacyloop.payment.gateway.RazorpayGateway;
import com.legacyloop.payment.mapper.PaymentMapper;
import com.legacyloop.payment.publisher.PaymentEventPublisher;
import com.legacyloop.payment.repository.PaymentRepository;
import com.legacyloop.payment.repository.PlanRepository;
import com.legacyloop.payment.repository.SubscriptionRepository;
import com.legacyloop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final RazorpayGateway razorpayGateway;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> activePlans() {
        return planRepository.findByActiveTrueOrderByPriceAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        AuthenticatedUser user = SecurityContextUtil.requireUser();

        Plan plan = planRepository.findById(request.planId())
                .filter(Plan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.planId()));

        subscriptionRepository
                .findFirstByUserIdAndStatusOrderByEndsAtDesc(user.userId(), SubscriptionStatus.ACTIVE)
                .filter(Subscription::isCurrentlyActive)
                .ifPresent(existing -> {
                    throw new ConflictException(ErrorCode.ALREADY_SUBSCRIBED,
                            "Premium is already active until " + existing.getEndsAt());
                });

        PaymentGateway.GatewayOrder order = paymentGateway.createOrder(
                plan.getPrice(), plan.getCurrency(), "user-" + user.userId() + "-plan-" + plan.getId());

        Payment payment = paymentRepository.save(Payment.builder()
                .userId(user.userId())
                .plan(plan)
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .status(PaymentStatus.CREATED)
                .gateway(paymentGateway.name())
                .gatewayOrderId(order.orderId())
                .build());

        log.info("Order {} created for user {} on plan {}", order.orderId(), user.userId(), plan.getCode());

        return new OrderResponse(payment.getId(), order.orderId(), razorpayGateway.getKeyId(),
                plan.getPrice(), order.amountInPaise(), plan.getCurrency(), plan.getName(),
                user.email());
    }

    /**
     * The browser calls this the moment checkout closes so the UI can update immediately.
     * It verifies the signature, but the authoritative grant still comes from the webhook -
     * a client is never trusted to declare its own payment successful.
     */
    @Override
    @Transactional
    public PaymentResponse verifyFromClient(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByGatewayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId",
                        request.razorpayOrderId()));

        boolean valid = paymentGateway.verifyPaymentSignature(request.razorpayOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature());

        if (!valid) {
            log.warn("Invalid client signature for order {}", request.razorpayOrderId());
            throw new BusinessException(ErrorCode.INVALID_WEBHOOK_SIGNATURE,
                    "Payment could not be verified");
        }

        payment.setGatewaySignature(request.razorpaySignature());
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            applyCapture(payment, request.razorpayPaymentId());
        }
        return mapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void capturePayment(String gatewayOrderId, String gatewayPaymentId) {
        Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook for unknown order {} - ignoring", gatewayOrderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.info("Order {} already captured - webhook is a retry", gatewayOrderId);
            return;
        }
        applyCapture(payment, gatewayPaymentId);
    }

    /** Single place where a payment becomes premium, whichever path got us here. */
    private void applyCapture(Payment payment, String gatewayPaymentId) {
        Plan plan = payment.getPlan();

        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setCapturedAt(Instant.now());
        payment.setInvoiceNumber(PaymentConstants.INVOICE_PREFIX + payment.getId());
        paymentRepository.save(payment);

        Instant now = Instant.now();
        Instant endsAt = now.plus(plan.getDurationDays(), ChronoUnit.DAYS);

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .userId(payment.getUserId())
                .plan(plan)
                .payment(payment)
                .status(SubscriptionStatus.ACTIVE)
                .startsAt(now)
                .endsAt(endsAt)
                .expiryNotified(false)
                .build());

        // auth-service consumes this and flips is_premium. We never touch its database.
        eventPublisher.publishPaymentCaptured(payment, subscription);
        log.info("Payment {} captured; premium for user {} until {}",
                payment.getId(), payment.getUserId(), endsAt);
    }

    @Override
    @Transactional
    public void failPayment(String gatewayOrderId, String reason) {
        paymentRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            eventPublisher.publishPaymentFailed(payment, reason);
            log.warn("Payment for order {} failed: {}", gatewayOrderId, reason);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> myPayments(Pageable pageable) {
        Long userId = SecurityContextUtil.requireUserId();
        Page<Payment> page = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse mySubscription() {
        Long userId = SecurityContextUtil.requireUserId();
        return subscriptionRepository
                .findFirstByUserIdAndStatusOrderByEndsAtDesc(userId, SubscriptionStatus.ACTIVE)
                .map(mapper::toResponse)
                .orElse(null);
    }
}

