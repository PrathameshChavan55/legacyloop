package com.legacyloop.common;

import java.time.Instant;

/**
 * The RabbitMQ contract, in one file.
 *
 * <p>One topic exchange, one queue, one routing key per event type. The original had four
 * exchanges, a dead-letter exchange, retry policies and a database table per service to
 * de-duplicate deliveries; here a notification is idempotent enough that redelivery is harmless,
 * so all of that is gone.
 */
public final class Events {

    public static final String EXCHANGE = "legacyloop.events";
    public static final String NOTIFICATION_QUEUE = "legacyloop.notifications";
    public static final String ROUTING_PATTERN = "event.#";

    public static final String USER_REGISTERED = "event.user.registered";
    public static final String PREMIUM_CHANGED = "event.user.premium";
    public static final String APPLICATION_SUBMITTED = "event.application.submitted";
    public static final String APPLICATION_STATUS_CHANGED = "event.application.status";
    public static final String REFERRAL_REQUESTED = "event.referral.requested";
    public static final String RESUME_ANALYSED = "event.resume.analysed";

    private Events() {
    }

    /**
     * Every event has the same shape, because every consumer does the same thing with it: turn it
     * into a notification for one user.
     *
     * @param type      one of the routing keys above
     * @param userId    who should be told
     * @param title     notification heading
     * @param body      notification text
     * @param link      in-app path the notification opens, may be null
     * @param occurredAt when the source action happened
     */
    public record PlatformEvent(String type, Long userId, String title, String body, String link,
                                Instant occurredAt) {

        public static PlatformEvent of(String type, Long userId, String title, String body, String link) {
            return new PlatformEvent(type, userId, title, body, link, Instant.now());
        }
    }
}
