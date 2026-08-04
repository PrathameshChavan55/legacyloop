package com.legacyloop.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Issued on successful login or refresh")
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String email,
        String fullName,
        Set<String> roles,
        boolean premium,
        String avatarUrl) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                  UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds,
                user.id(), user.email(), user.fullName(), user.roles(), user.premium(), user.avatarUrl());
    }
}
