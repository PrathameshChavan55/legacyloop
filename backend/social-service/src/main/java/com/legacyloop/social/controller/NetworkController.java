package com.legacyloop.social.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.social.dto.SocialDtos.ConnectionRequestBody;
import com.legacyloop.social.dto.SocialDtos.ConnectionResponse;
import com.legacyloop.social.dto.SocialDtos.NetworkSummary;
import com.legacyloop.social.dto.SocialDtos.PostResponse;
import com.legacyloop.social.service.ConnectionService;
import com.legacyloop.social.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

@Tag(name = "Network", description = "Connection requests and your network")
@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
public class NetworkController {

    private final ConnectionService connectionService;
    private final PostService postService;

    @PostMapping("/requests")
    @Operation(summary = "Ask someone to connect")
    public ApiResponse<ConnectionResponse> request(@AuthenticationPrincipal AuthUser user,
                                                   @Valid @RequestBody ConnectionRequestBody request) {
        return ApiResponse.ok(connectionService.request(request, user), "Request sent");
    }

    @GetMapping("/requests/received")
    @Operation(summary = "Requests waiting on you")
    public ApiResponse<PageResponse<ConnectionResponse>> received(@AuthenticationPrincipal AuthUser user,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(connectionService.received(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/requests/sent")
    @Operation(summary = "Requests you are waiting on")
    public ApiResponse<PageResponse<ConnectionResponse>> sent(@AuthenticationPrincipal AuthUser user,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(connectionService.sent(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @PatchMapping("/requests/{connectionId}/accept")
    @Operation(summary = "Accept a request")
    public ApiResponse<ConnectionResponse> accept(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable String connectionId) {
        return ApiResponse.ok(connectionService.accept(connectionId, user), "Connected");
    }

    @PatchMapping("/requests/{connectionId}/reject")
    @Operation(summary = "Decline a request")
    public ApiResponse<ConnectionResponse> reject(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable String connectionId) {
        return ApiResponse.ok(connectionService.reject(connectionId, user.id()), "Request declined");
    }

    @PatchMapping("/requests/{connectionId}/withdraw")
    @Operation(summary = "Take back a request you sent")
    public ApiResponse<ConnectionResponse> withdraw(@AuthenticationPrincipal AuthUser user,
                                                    @PathVariable String connectionId) {
        return ApiResponse.ok(connectionService.withdraw(connectionId, user.id()), "Request withdrawn");
    }

    @GetMapping("/connections")
    @Operation(summary = "Your network")
    public ApiResponse<PageResponse<ConnectionResponse>> connections(@AuthenticationPrincipal AuthUser user,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(connectionService.accepted(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Remove a connection")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthUser user,
                                    @PathVariable String connectionId) {
        connectionService.remove(connectionId, user.id());
        return ApiResponse.message("Connection removed");
    }

    @GetMapping("/summary")
    @Operation(summary = "Counts for the network page header")
    public ApiResponse<NetworkSummary> summary(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(connectionService.summary(user.id()));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "People your connections know")
    public ApiResponse<List<Long>> suggestions(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(connectionService.suggestions(user.id(), Math.min(limit, 30)));
    }

    @GetMapping("/feed")
    @Operation(summary = "Posts from across the platform, for discovery")
    public ApiResponse<PageResponse<PostResponse>> discoveryFeed(@AuthenticationPrincipal AuthUser user,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(postService.explore(user.id(), PageRequest.of(page, Math.min(size, 30))));
    }
}
