package com.legacyloop.auth.controller;

import com.legacyloop.auth.dto.request.AdminCreateUserRequest;
import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.auth.service.AdminAccountService;
import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.exception.UnauthorizedException;
import com.legacyloop.common.exception.ErrorCode;
import com.legacyloop.common.security.SecurityContextUtil;
import com.legacyloop.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account provisioning. Separate from {@code AdminUserController}, which is about the
 * lifecycle of accounts that already exist (approve, suspend, reactivate).
 *
 * <p>Platform admin only. Institution staff can approve a registration but cannot mint an
 * account outright - that is the difference between vetting and provisioning, and giving
 * staff both would mean any staff member could create themselves an admin.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Account provisioning", description = "Administrator-created accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @PostMapping
    @Operation(summary = "Create an account of any role",
            description = "Active and verified immediately. No OTP is sent.")
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                adminAccountService.create(request, currentUserId()), "Account created"));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Set a new password",
            description = "Signs the account out of every device it is currently signed in on.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long userId, @Valid @RequestBody ResetPasswordByAdminRequest request) {
        adminAccountService.resetPassword(userId, request.newPassword(), currentUserId());
        return ResponseEntity.ok(ApiResponse.message("Password updated"));
    }

    private Long currentUserId() {
        return SecurityContextUtil.currentUser()
                .map(user -> user.userId())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHENTICATED));
    }

    /** Small enough to live with its endpoint rather than in the dto package. */
    public record ResetPasswordByAdminRequest(
            @NotBlank(message = "A new password is required")
            @StrongPassword
            String newPassword) {
    }
}
