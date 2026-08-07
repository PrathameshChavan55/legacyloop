package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.user.dto.AuthDtos.AuthResponse;
import com.legacyloop.user.dto.AuthDtos.ChangePasswordRequest;
import com.legacyloop.user.dto.AuthDtos.EmailOnlyRequest;
import com.legacyloop.user.dto.AuthDtos.LoginRequest;
import com.legacyloop.user.dto.AuthDtos.RefreshTokenRequest;
import com.legacyloop.user.dto.AuthDtos.RegisterRequest;
import com.legacyloop.user.dto.AuthDtos.RegistrationResponse;
import com.legacyloop.user.dto.AuthDtos.ResetPasswordRequest;
import com.legacyloop.user.dto.AuthDtos.VerificationResponse;
import com.legacyloop.user.dto.AuthDtos.VerifyOtpRequest;
import com.legacyloop.user.dto.UserDtos.UserResponse;
import com.legacyloop.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Sign-up, sign-in, tokens and passwords")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Create a student or alumni account and email a verification code")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Check your email for the verification code");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Confirm the emailed code")
    public ApiResponse<VerificationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(authService.verifyOtp(request), "Email verified");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Send a fresh verification code")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody EmailOnlyRequest request) {
        authService.resendOtp(request.email());
        return ApiResponse.message("If that address needs verifying, a new code is on its way");
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access and refresh token")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, clientIp(servletRequest)), "Signed in");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Trade a refresh token for a new pair")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.message("Signed out");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a password reset link")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailOnlyRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.message("If that address has an account, a reset link is on its way");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using the emailed link")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Your password has been changed. Please sign in.");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change your own password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.id(), request);
        return ApiResponse.message("Your password has been changed");
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in account")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(authService.currentUser(user.id()));
    }

    /** Behind Nginx the socket address is the proxy, so the forwarded header wins when present. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
