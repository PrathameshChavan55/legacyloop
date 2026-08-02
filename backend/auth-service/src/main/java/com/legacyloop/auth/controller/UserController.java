package com.legacyloop.auth.controller;

import com.legacyloop.auth.dto.request.ChangePasswordRequest;
import com.legacyloop.auth.dto.request.UpdateProfileRequest;
import com.legacyloop.auth.dto.response.LoginHistoryResponse;
import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.auth.service.UserService;
import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.common.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User profile", description = "Self-service profile and account operations")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Current user")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrent()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Fetch a user by id", description = "Own record, or any record for an admin.")
    @PreAuthorize("@sec.isSelf(#userId) or hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    public ResponseEntity<ApiResponse<UserResponse>> byId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(userId)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update your profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityContextUtil.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(userId, request), "Profile updated"));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change your password", description = "Signs out every other session.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityContextUtil.requireUserId(), request);
        return ResponseEntity.ok(ApiResponse.message("Password changed. Other sessions were signed out."));
    }

    @GetMapping("/me/login-history")
    @Operation(summary = "Your recent sign-in attempts")
    public ResponseEntity<ApiResponse<PageResponse<LoginHistoryResponse>>> loginHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.loginHistory(SecurityContextUtil.requireUserId(), pageable)));
    }
}
