package com.legacyloop.social.service;

import com.legacyloop.common.Events;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.NotificationResponse;
import com.legacyloop.social.entity.Notification;
import com.legacyloop.social.repository.NotificationRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * The inbox, and the single consumer of the RabbitMQ queue.
 *
 * <p>Both routes into a notification end here: {@link #create} is called directly when something
 * happens inside this service, and {@link #onPlatformEvent} is called when another service
 * publishes. Each new notification is also pushed over the user's WebSocket topic, so the bell
 * updates without polling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final SimpMessagingTemplate websocket;

    /** Every event type produces the same document, so one listener handles all of them. */
    @RabbitListener(queues = Events.NOTIFICATION_QUEUE)
    public void onPlatformEvent(Events.PlatformEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        log.debug("Received {} for user {}", event.type(), event.userId());
        create(event.userId(), event.type(), event.title(), event.body(), event.link());
    }

    public NotificationResponse create(Long userId, String type, String title, String body, String link) {
        Notification notification = notifications.save(Notification.builder()
                .userId(userId).type(type).title(title).body(body).link(link).build());

        NotificationResponse response = NotificationResponse.from(notification);
        push(userId, response);
        return response;
    }

    /** Never let a WebSocket problem lose the notification that is already saved. */
    private void push(Long userId, NotificationResponse response) {
        try {
            websocket.convertAndSend("/topic/notifications/" + userId, response);
        } catch (Exception ex) {
            log.warn("Could not push a notification to user {}: {}", userId, ex.getMessage());
        }
    }

    public PageResponse<NotificationResponse> inbox(Long userId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, NotificationResponse::from);
    }

    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    public void markRead(String notificationId, Long userId) {
        notifications.findById(notificationId)
                .filter(notification -> notification.getUserId().equals(userId))
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notification.setReadAt(Instant.now());
                    notifications.save(notification);
                });
    }

    public int markAllRead(Long userId) {
        var unread = notifications.findByUserIdAndReadFalse(userId);
        unread.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        });
        notifications.saveAll(unread);
        return unread.size();
    }

    public void clearRead(Long userId) {
        notifications.deleteByUserIdAndReadTrue(userId);
    }

    public void deleteNotification(String notificationId, Long userId) {
        notifications.findById(notificationId)
                .filter(notification -> notification.getUserId().equals(userId))
                .ifPresent(notifications::delete);
    }
}
