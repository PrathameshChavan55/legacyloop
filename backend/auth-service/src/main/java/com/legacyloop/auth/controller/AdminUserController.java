package com.legacyloop.auth.controller;

import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.auth.service.UserService;
import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SRS REQ-5.1: placement heads approve or revoke accounts. Admins can do the same plus
 * everything else.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
@Tag(name = "User administration", description = "Approval, suspension and directory")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/pending")
    @Operation(summary = "Accounts awaiting approval")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> pending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.listPending(pageable)));
    }

    @GetMapping
    @Operation(summary = "Directory", description = "Optionally filter by status and role.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.listAll(status, role, pageable)));
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve an account")
    public ResponseEntity<ApiResponse<UserResponse>> approve(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.approve(userId), "User approved"));
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend an account", description = "Immediately revokes all their sessions.")
    public ResponseEntity<ApiResponse<UserResponse>> suspend(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Suspended by administrator") String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspend(userId, reason), "User suspended"));
    }

    @PostMapping("/{userId}/reactivate")
    @Operation(summary = "Reactivate a suspended account")
    public ResponseEntity<ApiResponse<UserResponse>> reactivate(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.reactivate(userId), "User reactivated"));
    }
}
