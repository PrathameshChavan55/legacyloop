package com.legacyloop.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Sent by the browser right after Razorpay's checkout closes. It is a UX shortcut only -
 * premium is granted by the verified webhook, never by anything the client posts.
 */
public record VerifyPaymentRequest(

        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {
}
