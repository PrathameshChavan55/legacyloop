package com.legacyloop.social.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.NotificationResponse;
import com.legacyloop.social.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "The in-app inbox")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Your notifications")
    public ApiResponse<PageResponse<NotificationResponse>> inbox(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.inbox(user.id(), unreadOnly,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count, for the bell")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(user.id())));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one as read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal AuthUser user,
                                      @PathVariable String notificationId) {
        notificationService.markRead(notificationId, user.id());
        return ApiResponse.message("Marked as read");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark everything as read")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(Map.of("marked", notificationService.markAllRead(user.id())));
    }

    @DeleteMapping("/read")
    @Operation(summary = "Clear the ones you have read")
    public ApiResponse<Void> clearRead(@AuthenticationPrincipal AuthUser user) {
        notificationService.clearRead(user.id());
        return ApiResponse.message("Cleared");
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete one notification")
    public ApiResponse<Void> deleteNotification(@AuthenticationPrincipal AuthUser user,
                                                @PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId, user.id());
        return ApiResponse.message("Deleted");
    }
}
