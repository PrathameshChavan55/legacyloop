package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.PageResponse;
import com.legacyloop.common.Roles;
import com.legacyloop.user.dto.UserDtos.AdminCreateUserRequest;
import com.legacyloop.user.dto.UserDtos.AuditLogResponse;
import com.legacyloop.user.dto.UserDtos.UserResponse;
import com.legacyloop.user.dto.UserDtos.UserStatistics;
import com.legacyloop.user.dto.UserDtos.UserSummary;
import com.legacyloop.user.entity.AuditLog;
import com.legacyloop.user.entity.User;
import com.legacyloop.user.entity.UserStatus;
import com.legacyloop.user.repository.AuditLogRepository;
import com.legacyloop.user.repository.RefreshTokenRepository;
import com.legacyloop.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything the admin console does to accounts, plus the lookups other services call. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<AuditLog.Action> LOGIN_ACTIONS =
            List.of(AuditLog.Action.LOGIN_SUCCESS, AuditLog.Action.LOGIN_FAILED);

    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final RefreshTokenRepository refreshTokens;
    private final ProfileService profiles;
    private final EmailService emailService;
    private final AuditService audit;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String query, UserStatus status, String role,
                                             Long institutionId, Pageable pageable) {
        String like = query == null || query.isBlank() ? null
                : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        Page<User> page = users.search(like, status, Roles.prefixed(role), institutionId, pageable);
        return PageResponse.of(page, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("User", userId));
    }

    /**
     * Creates a staff or admin account with a generated password the holder must change on first
     * sign-in. Self-registration cannot produce these roles, which is why this endpoint exists.
     */
    @Transactional
    public UserResponse create(AdminCreateUserRequest request, Long actorId) {
        if (users.existsByEmail(request.email())) {
            throw ApiException.conflict("An account already exists for that email address");
        }
        if (!Roles.ALL.contains(request.role())) {
            throw ApiException.badRequest("Unknown role: " + request.role());
        }

        String temporaryPassword = generatePassword();
        User user = users.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(temporaryPassword))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(request.role()))
                .institutionId(request.institutionId())
                .studentIdentifier(request.studentIdentifier())
                .emailVerified(true)
                .mustChangePassword(true)
                .build());

        profiles.createEmptyProfile(user);
        emailService.sendTemporaryPassword(user.getEmail(), temporaryPassword);
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.USER_CREATED,
                "Created with role " + request.role(), null, actorId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse approve(Long userId, Long actorId) {
        User user = load(userId);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        profiles.createEmptyProfile(user);
        audit.record(userId, user.getEmail(), AuditLog.Action.USER_APPROVED, null, null, actorId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse verify(Long userId, Long actorId) {
        User user = load(userId);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        profiles.createEmptyProfile(user);
        audit.record(userId, user.getEmail(), AuditLog.Action.EMAIL_VERIFIED, "Directly verified and activated by Admin", null, actorId);
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId, Long actorId) {
        if (userId.equals(actorId)) {
            throw ApiException.badRequest("You cannot delete your own account");
        }
        User user = load(userId);
        refreshTokens.revokeAllForUser(userId);
        profiles.deleteProfileByUserId(userId);
        users.delete(user);
        audit.record(userId, user.getEmail(), AuditLog.Action.USER_SUSPENDED, "User profile and account deleted by Admin", null, actorId);
    }

    @Transactional
    public UserResponse suspend(Long userId, String reason, Long actorId) {
        User user = load(userId);
        user.setStatus(UserStatus.SUSPENDED);
        refreshTokens.revokeAllForUser(userId);
        audit.record(userId, user.getEmail(), AuditLog.Action.USER_SUSPENDED, reason, null, actorId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse reactivate(Long userId, Long actorId) {
        User user = load(userId);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw ApiException.badRequest("Only a suspended account can be reactivated");
        }
        user.setStatus(UserStatus.ACTIVE);
        audit.record(userId, user.getEmail(), AuditLog.Action.USER_REACTIVATED, null, null, actorId);
        return UserResponse.from(user);
    }

    @Transactional
    public void forcePasswordReset(Long userId, Long actorId) {
        User user = load(userId);
        String temporaryPassword = generatePassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        refreshTokens.revokeAllForUser(userId);
        emailService.sendTemporaryPassword(user.getEmail(), temporaryPassword);
        audit.record(userId, user.getEmail(), AuditLog.Action.PASSWORD_RESET_FORCED, null, null, actorId);
    }

    @Transactional(readOnly = true)
    public UserStatistics statistics() {
        return new UserStatistics(
                users.count(),
                users.countByStatus(UserStatus.PENDING_VERIFICATION),
                users.countByStatus(UserStatus.PENDING_APPROVAL),
                users.countByStatus(UserStatus.ACTIVE),
                users.countByStatus(UserStatus.SUSPENDED),
                users.countByRole(Roles.ROLE_STUDENT),
                users.countByRole(Roles.ROLE_ALUMNI),
                users.countByRole(Roles.ROLE_STAFF),
                users.countPremium());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> auditLogs(Long userId, Pageable pageable) {
        Page<AuditLog> page = userId == null
                ? auditLogs.findAllByOrderByIdDesc(pageable)
                : auditLogs.findByUserIdOrderByIdDesc(userId, pageable);
        return PageResponse.of(page, AuditLogResponse::from);
    }

    /** "Login history" is this table filtered to the two sign-in actions. */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> loginHistory(Long userId, Pageable pageable) {
        return PageResponse.of(auditLogs.findByUserIdAndActionInOrderByIdDesc(userId, LOGIN_ACTIONS, pageable),
                AuditLogResponse::from);
    }

    /* ------------------------------------------------- lookups used by the other services */

    @Transactional(readOnly = true)
    public UserSummary summary(Long userId) {
        return UserSummary.from(load(userId));
    }

    /** One query for many ids: this is what keeps a page of posts or applications off N+1. */
    @Transactional(readOnly = true)
    public Map<Long, UserSummary> summaries(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return users.findByIdIn(userIds).stream()
                .map(UserSummary::from)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
    }

    private static String generatePassword() {
        // Guaranteed to satisfy the password rule: letters, a digit and a symbol.
        String alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return password + "%d!".formatted(RANDOM.nextInt(10));
    }
}
