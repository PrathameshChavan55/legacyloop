package com.legacyloop.payment.constant;

public final class PaymentConstants {

    private PaymentConstants() {
    }

    public static final String GATEWAY_RAZORPAY = "RAZORPAY";

    public static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    public static final String EVENT_PAYMENT_FAILED = "payment.failed";
    public static final String EVENT_REFUND_PROCESSED = "refund.processed";

    public static final String SIGNATURE_HEADER = "X-Razorpay-Signature";
    public static final String EVENT_ID_HEADER = "X-Razorpay-Event-Id";

    /** Razorpay works in the smallest currency unit. */
    public static final int PAISE_PER_RUPEE = 100;

    public static final String INVOICE_PREFIX = "LL-INV-";
}

