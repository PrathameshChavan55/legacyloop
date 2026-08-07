package com.legacyloop.social.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.ConversationResponse;
import com.legacyloop.social.dto.SocialDtos.MessageResponse;
import com.legacyloop.social.dto.SocialDtos.SendMessageRequest;
import com.legacyloop.social.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat over HTTP, plus one STOMP mapping for the live path.
 *
 * <p>Sending works both ways on purpose: the WebSocket is the fast path, and the plain POST is
 * what the client falls back to when the socket is down. Both call the same service method, so
 * there is one code path for what a sent message means.
 */
@Tag(name = "Chat", description = "Conversations and messages")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "Your conversations, most recent first")
    public ApiResponse<PageResponse<ConversationResponse>> inbox(@AuthenticationPrincipal AuthUser user,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatService.inbox(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Total unread messages, for the badge")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(Map.of("count", chatService.unreadCount(user.id())));
    }

    @GetMapping("/with/{userId}")
    @Operation(summary = "Open the thread with someone, creating it if this is the first message")
    public ApiResponse<ConversationResponse> with(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long userId) {
        return ApiResponse.ok(chatService.with(userId, user.id()));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message")
    public ApiResponse<MessageResponse> send(@AuthenticationPrincipal AuthUser user,
                                             @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.send(request, user));
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message you sent")
    public ApiResponse<Void> deleteMessage(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable String messageId) {
        chatService.deleteMessage(messageId, user.id());
        return ApiResponse.message("Message deleted");
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details by id")
    public ApiResponse<ConversationResponse> get(@AuthenticationPrincipal AuthUser user,
                                                 @PathVariable String conversationId) {
        return ApiResponse.ok(chatService.get(conversationId, user.id()));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Messages in a conversation, newest first")
    public ApiResponse<PageResponse<MessageResponse>> messages(@AuthenticationPrincipal AuthUser user,
                                                               @PathVariable String conversationId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(chatService.messages(conversationId, user.id(),
                PageRequest.of(page, Math.min(size, 100))));
    }

    @PatchMapping("/{conversationId}/read")
    @Operation(summary = "Mark the thread as read")
    public ApiResponse<Map<String, Integer>> markRead(@AuthenticationPrincipal AuthUser user,
                                                      @PathVariable String conversationId) {
        return ApiResponse.ok(Map.of("marked", chatService.markRead(conversationId, user.id())));
    }

    /**
     * The live path: a client publishes to {@code /app/chat.send} and everyone subscribed to the
     * conversation topic receives the message. The service does the broadcasting.
     *
     * <p>The caller arrives as a {@link Principal} — the one the STOMP interceptor attached when
     * it verified the token on CONNECT — rather than through {@code @AuthenticationPrincipal},
     * which needs an extra Spring Security messaging module to work on this side.
     */
    @MessageMapping("/chat.send")
    public void sendOverWebSocket(@Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthUser user) {
            chatService.send(request, user);
        }
    }
}
