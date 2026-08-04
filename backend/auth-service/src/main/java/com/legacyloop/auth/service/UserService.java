package com.legacyloop.auth.service;

import com.legacyloop.auth.dto.request.ChangePasswordRequest;
import com.legacyloop.auth.dto.request.UpdateProfileRequest;
import com.legacyloop.auth.dto.response.LoginHistoryResponse;
import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.common.dto.UserSummaryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse getById(Long userId);

    UserResponse getCurrent();

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    PageResponse<LoginHistoryResponse> loginHistory(Long userId, Pageable pageable);

    // ---- admin ----
    PageResponse<UserResponse> listPending(Pageable pageable);

    PageResponse<UserResponse> listAll(String status, String role, Pageable pageable);

    UserResponse approve(Long userId);

    UserResponse suspend(Long userId, String reason);

    UserResponse reactivate(Long userId);

    // ---- internal, service-to-service ----
    UserSummaryDto getSummary(Long userId);

    List<UserSummaryDto> getSummaries(List<Long> userIds);

    void applyPremium(Long userId, boolean premium, java.time.Instant premiumUntil);
}
