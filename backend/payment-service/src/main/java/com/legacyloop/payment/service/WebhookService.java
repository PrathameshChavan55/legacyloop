package com.legacyloop.payment.service;

public interface WebhookService {

    /** @return true if the event was accepted (signature valid), false to return 400 */
    boolean handle(String rawBody, String signature, String eventId);
}
