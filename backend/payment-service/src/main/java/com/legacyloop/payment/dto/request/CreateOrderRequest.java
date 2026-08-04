package com.legacyloop.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

        @NotNull(message = "Plan is required")
        Long planId) {
}

