package com.legacyloop.auth.service;

import com.legacyloop.auth.dto.request.ForgotPasswordRequest;
import com.legacyloop.auth.dto.request.LoginRequest;
import com.legacyloop.auth.dto.request.RefreshTokenRequest;
import com.legacyloop.auth.dto.request.RegisterRequest;
import com.legacyloop.auth.dto.request.ResendOtpRequest;
import com.legacyloop.auth.dto.request.ResetPasswordRequest;
import com.legacyloop.auth.dto.request.VerifyOtpRequest;
import com.legacyloop.auth.dto.response.AuthResponse;
import com.legacyloop.auth.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent);

    void logout(String accessToken, String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
