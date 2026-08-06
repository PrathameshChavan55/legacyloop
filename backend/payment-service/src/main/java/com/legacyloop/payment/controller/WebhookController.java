
package com.legacyloop.payment.controller;

import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.payment.constant.PaymentConstants;
import com.legacyloop.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public by design - Razorpay has no JWT. The signature IS the authentication, which is why
 * the raw body is taken as a String: re-serialising a parsed object would change the bytes
 * and break the HMAC.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Razorpay callbacks (SRS REQ-7.2)")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Razorpay webhook receiver")
    public ResponseEntity<ApiResponse<Void>> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = PaymentConstants.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = PaymentConstants.EVENT_ID_HEADER, required = false) String eventId) {

        boolean accepted = webhookService.handle(rawBody, signature, eventId);
        if (!accepted) {
            return ResponseEntity.badRequest().body(ApiResponse.message("Invalid signature"));
        }
        return ResponseEntity.ok(ApiResponse.message("ok"));
    }
}

