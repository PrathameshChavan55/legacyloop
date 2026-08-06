package com.legacyloop.social.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.Author;
import com.legacyloop.social.dto.SocialDtos.ConversationResponse;
import com.legacyloop.social.dto.SocialDtos.MessageResponse;
import com.legacyloop.social.dto.SocialDtos.SendMessageRequest;
import com.legacyloop.social.entity.Conversation;
import com.legacyloop.social.entity.Message;
import com.legacyloop.social.repository.ConversationRepository;
import com.legacyloop.social.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * One-to-one chat.
 *
 * <p>A message is saved first and pushed over the WebSocket second. That order matters: a
 * delivered-but-unsaved message would disappear on refresh, whereas a saved-but-undelivered one
 * simply arrives when the recipient next opens the thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final NotificationService notifications;
    private final PeopleClient people;
    private final SimpMessagingTemplate websocket;

    public PageResponse<ConversationResponse> inbox(Long userId, Pageable pageable) {
        Page<Conversation> page = conversations
                .findByParticipantIdsContainingOrderByLastMessageAtDesc(userId, pageable);
        Map<Long, Author> authors = people.authors(
                page.getContent().stream().map(conversation -> conversation.otherParticipant(userId)).toList());

        return PageResponse.of(page, conversation -> {
            Long otherId = conversation.otherParticipant(userId);
            Author other = people.authorOrUnknown(authors, otherId);
            return new ConversationResponse(conversation.getId(), otherId, other.name(), other.photoUrl(),
                    conversation.getLastMessagePreview(), conversation.getLastMessageSenderId(),
                    conversation.getLastMessageAt(), conversation.unreadFor(userId));
        });
    }

    /** Opens the thread with someone, creating it on first use. */
    public ConversationResponse with(Long otherUserId, Long userId) {
        Conversation conversation = findOrCreate(userId, otherUserId);
        Author other = people.author(otherUserId);
        return new ConversationResponse(conversation.getId(), otherUserId, other.name(), other.photoUrl(),
                conversation.getLastMessagePreview(), conversation.getLastMessageSenderId(),
                conversation.getLastMessageAt(), conversation.unreadFor(userId));
    }

    /** Gets a conversation by ID for the requesting user. */
    public ConversationResponse get(String conversationId, Long userId) {
        Conversation conversation = loadFor(conversationId, userId);
        Long otherId = conversation.otherParticipant(userId);
        Author other = people.author(otherId);
        return new ConversationResponse(conversation.getId(), otherId, other.name(), other.photoUrl(),
                conversation.getLastMessagePreview(), conversation.getLastMessageSenderId(),
                conversation.getLastMessageAt(), conversation.unreadFor(userId));
    }

    public PageResponse<MessageResponse> messages(String conversationId, Long userId, Pageable pageable) {
        Conversation conversation = loadFor(conversationId, userId);
        Page<Message> page = messages.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                conversation.getId(), pageable);
        Map<Long, Author> authors = people.authors(page.getContent().stream()
                .map(Message::getSenderId).toList());

        return PageResponse.of(page, message -> toResponse(message,
                people.authorOrUnknown(authors, message.getSenderId()).name(), userId));
    }

    public MessageResponse send(SendMessageRequest request, AuthUser sender) {
        Conversation conversation = request.conversationId() != null
                ? loadFor(request.conversationId(), sender.id())
                : findOrCreate(sender.id(), require(request.recipientId()));

        Long recipientId = conversation.otherParticipant(sender.id());

        Message message = messages.save(Message.builder()
                .conversationId(conversation.getId())
                .senderId(sender.id())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .createdAt(Instant.now())
                .build());

        conversation.setLastMessagePreview(preview(request.content()));
        conversation.setLastMessageSenderId(sender.id());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.getUnread().merge(String.valueOf(recipientId), 1, Integer::sum);
        conversations.save(conversation);

        MessageResponse response = toResponse(message, sender.fullName(), sender.id());
        push(conversation.getId(), response);
        notifications.create(recipientId, "MESSAGE", "New message",
                "%s: %s".formatted(sender.fullName(), preview(request.content())),
                "/messages/" + conversation.getId());

        return response;
    }

    /** Marks everything the other person sent as read, and clears the badge. */
    public int markRead(String conversationId, Long userId) {
        Conversation conversation = loadFor(conversationId, userId);
        List<Message> unread = messages.findByConversationIdAndSenderIdNotAndReadAtIsNull(
                conversation.getId(), userId);
        unread.forEach(message -> message.setReadAt(Instant.now()));
        messages.saveAll(unread);

        conversation.getUnread().put(String.valueOf(userId), 0);
        conversations.save(conversation);
        return unread.size();
    }

    public void deleteMessage(String messageId, Long userId) {
        Message message = messages.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message", messageId));
        if (!message.getSenderId().equals(userId)) {
            throw ApiException.forbidden("You can only delete your own messages");
        }
        message.setDeleted(true);
        messages.save(message);
    }

    public long unreadCount(Long userId) {
        return conversations.findByParticipantIdsContaining(userId).stream()
                .mapToInt(conversation -> conversation.unreadFor(userId))
                .sum();
    }

    private void push(String conversationId, MessageResponse message) {
        try {
            websocket.convertAndSend("/topic/conversations/" + conversationId, message);
        } catch (Exception ex) {
            log.warn("Could not push a message to conversation {}: {}", conversationId, ex.getMessage());
        }
    }

    private Conversation findOrCreate(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw ApiException.badRequest("You cannot message yourself");
        }
        return conversations.findBetween(userId, otherUserId)
                .orElseGet(() -> conversations.save(Conversation.builder()
                        .participantIds(List.of(userId, otherUserId))
                        .lastMessageAt(Instant.now())
                        .build()));
    }

    private Conversation loadFor(String conversationId, Long userId) {
        Conversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation", conversationId));
        if (!conversation.getParticipantIds().contains(userId)) {
            throw ApiException.forbidden("That conversation is not yours");
        }
        return conversation;
    }

    private MessageResponse toResponse(Message message, String senderName, Long viewerId) {
        return new MessageResponse(message.getId(), message.getConversationId(), message.getSenderId(),
                senderName, message.getContent(), message.getAttachmentUrl(),
                message.getSenderId().equals(viewerId), message.getReadAt(), message.getCreatedAt());
    }

    private static Long require(Long recipientId) {
        if (recipientId == null) {
            throw ApiException.badRequest("Say who the message is for");
        }
        return recipientId;
    }

    private static String preview(String content) {
        String single = content.replaceAll("\\s+", " ").trim();
        return single.length() <= 80 ? single : single.substring(0, 77) + "...";
    }
}
