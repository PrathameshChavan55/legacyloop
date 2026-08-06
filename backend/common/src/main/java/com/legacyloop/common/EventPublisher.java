package com.legacyloop.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@link Events.PlatformEvent}. A broker that is down must never fail the business
 * transaction that triggered the event, so the failure is logged and swallowed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String type, Long userId, String title, String body, String link) {
        try {
            rabbitTemplate.convertAndSend(Events.EXCHANGE, type,
                    Events.PlatformEvent.of(type, userId, title, body, link));
            log.debug("Published {} for user {}", type, userId);
        } catch (Exception ex) {
            log.error("Could not publish {} for user {}: {}", type, userId, ex.getMessage());
        }
    }
}
