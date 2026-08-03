package com.legacyloop.auth.controller;

import com.legacyloop.auth.dto.request.ForgotPasswordRequest;
import com.legacyloop.auth.dto.request.LoginRequest;
import com.legacyloop.auth.dto.request.RefreshTokenRequest;
import com.legacyloop.auth.dto.request.RegisterRequest;
import com.legacyloop.auth.dto.request.ResendOtpRequest;
import com.legacyloop.auth.dto.request.ResetPasswordRequest;
import com.legacyloop.auth.dto.request.VerifyOtpRequest;
import com.legacyloop.auth.dto.response.AuthResponse;
import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.auth.service.AuthService;
import com.legacyloop.auth.util.RequestUtil;
import com.legacyloop.common.constant.SecurityConstants;
import com.legacyloop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, verification, login and token lifecycle")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a student or alumni account",
            description = "Creates the account in PENDING_VERIFICATION and emails a 6-digit OTP.")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Registered, OTP sent"))
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created,
                        "Registration successful. Check your email for the verification code."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the email OTP",
            description = "Students become ACTIVE; alumni move to PENDING_APPROVAL for a placement head.")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.message("Email verified successfully."));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend the verification OTP")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.message("A new code has been sent."));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns an access token (15 min) and a rotating refresh token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request,
                RequestUtil.clientIp(httpRequest), RequestUtil.userAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token",
            description = "Rotates the token. Presenting a consumed token revokes the whole family.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                             HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(request,
                RequestUtil.clientIp(httpRequest), RequestUtil.userAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Blocklists the access token and revokes the refresh token.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
                                                    @RequestBody(required = false) RefreshTokenRequest request) {
        String header = httpRequest.getHeader(SecurityConstants.AUTH_HEADER);
        String accessToken = (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX))
                ? header.substring(SecurityConstants.BEARER_PREFIX.length())
                : null;
        authService.logout(accessToken, request == null ? null : request.refreshToken());
        return ResponseEntity.ok(ApiResponse.message("Logged out."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Start a password reset",
            description = "Always returns 200 - it must not reveal whether an email is registered.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.message(
                "If that email is registered, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete a password reset", description = "Revokes every existing session.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.message("Password updated. Please log in again."));
    }
}
