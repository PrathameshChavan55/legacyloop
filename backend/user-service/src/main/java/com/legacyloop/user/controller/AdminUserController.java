package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.UserDtos.AdminCreateUserRequest;
import com.legacyloop.user.dto.UserDtos.AuditLogResponse;
import com.legacyloop.user.dto.UserDtos.ReasonRequest;
import com.legacyloop.user.dto.UserDtos.UserResponse;
import com.legacyloop.user.dto.UserDtos.UserStatistics;
import com.legacyloop.user.entity.UserStatus;
import com.legacyloop.user.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** The admin console. One class-level rule guards every method. */
@Tag(name = "Admin — users", description = "Account administration and the audit trail")
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @Operation(summary = "Search accounts by name, email, identifier, status or role")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"));
        return ApiResponse.ok(userAdminService.search(query, status, role, institutionId, pageable));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Counts for the admin dashboard")
    public ApiResponse<UserStatistics> statistics() {
        return ApiResponse.ok(userAdminService.statistics());
    }

    @GetMapping("/statuses")
    @Operation(summary = "The account lifecycle values, so the filter is not hard-coded in the UI")
    public ApiResponse<UserStatus[]> statuses() {
        return ApiResponse.ok(UserStatus.values());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "The platform-wide audit trail")
    public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userAdminService.auditLogs(null, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "One account")
    public ApiResponse<UserResponse> findById(@PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.findById(userId));
    }

    @GetMapping("/{userId}/audit-logs")
    @Operation(summary = "The audit trail for one account")
    public ApiResponse<PageResponse<AuditLogResponse>> userAuditLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userAdminService.auditLogs(userId, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/{userId}/login-history")
    @Operation(summary = "Sign-in attempts for one account")
    public ApiResponse<PageResponse<AuditLogResponse>> loginHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userAdminService.loginHistory(userId, PageRequest.of(page, Math.min(size, 100))));
    }

    @PostMapping
    @Operation(summary = "Create a staff or admin account with a temporary password")
    public ApiResponse<UserResponse> create(@AuthenticationPrincipal AuthUser admin,
                                            @Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(userAdminService.create(request, admin.id()), "Account created");
    }

    @PatchMapping("/{userId}/approve")
    @Operation(summary = "Approve an account that is waiting for a decision")
    public ApiResponse<UserResponse> approve(@AuthenticationPrincipal AuthUser admin, @PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.approve(userId, admin.id()), "Account approved");
    }

    @PatchMapping("/{userId}/verify")
    @Operation(summary = "Directly verify and activate any pending user account")
    public ApiResponse<UserResponse> verify(@AuthenticationPrincipal AuthUser admin, @PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.verify(userId, admin.id()), "Account verified and activated");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user account and profile")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser admin, @PathVariable Long userId) {
        userAdminService.deleteUser(userId, admin.id());
        return ApiResponse.message("User account and profile deleted successfully");
    }

    @PatchMapping("/{userId}/suspend")
    @Operation(summary = "Suspend an account and end its sessions")
    public ApiResponse<UserResponse> suspend(@AuthenticationPrincipal AuthUser admin, @PathVariable Long userId,
                                             @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(userAdminService.suspend(userId, request.reason(), admin.id()), "Account suspended");
    }

    @PatchMapping("/{userId}/reactivate")
    @Operation(summary = "Lift a suspension")
    public ApiResponse<UserResponse> reactivate(@AuthenticationPrincipal AuthUser admin,
                                                @PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.reactivate(userId, admin.id()), "Account reactivated");
    }

    @PostMapping("/{userId}/force-password-reset")
    @Operation(summary = "Issue a temporary password the holder must change")
    public ApiResponse<Void> forcePasswordReset(@AuthenticationPrincipal AuthUser admin,
                                                @PathVariable Long userId) {
        userAdminService.forcePasswordReset(userId, admin.id());
        return ApiResponse.message("A temporary password has been emailed");
    }
}
