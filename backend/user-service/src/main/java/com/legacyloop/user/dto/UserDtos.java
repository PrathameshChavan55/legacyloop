package com.legacyloop.user.dto;

import com.legacyloop.user.entity.AuditLog;
import com.legacyloop.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Account views.
 *
 * <p>The {@code from} factories replace the original's MapStruct mappers. Two mappers and their
 * generated implementations existed to copy fields between two shapes; a static factory next to
 * the record it builds is the same code, visible rather than generated.
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String email, String firstName, String lastName, String fullName,
                               String phone, String status, String statusLabel, Set<String> roles,
                               Long institutionId, String studentIdentifier, boolean emailVerified,
                               boolean premium, Instant premiumUntil, boolean mustChangePassword,
                               Instant lastLoginAt, Instant createdAt) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.fullName(), user.getPhone(), user.getStatus().name(), user.getStatus().label(),
                    Set.copyOf(user.getRoles()), user.getInstitutionId(), user.getStudentIdentifier(),
                    user.isEmailVerified(), user.isPremiumNow(), user.getPremiumUntil(),
                    user.isMustChangePassword(), user.getLastLoginAt(), user.getCreatedAt());
        }
    }

    /** The slim view other services fetch to show a name next to an id. */
    public record UserSummary(Long id, String fullName, String email, Set<String> roles,
                              Long institutionId, String studentIdentifier, boolean premium, boolean active) {

        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.fullName(), user.getEmail(), Set.copyOf(user.getRoles()),
                    user.getInstitutionId(), user.getStudentIdentifier(), user.isPremiumNow(),
                    user.getStatus().canSignIn());
        }
    }

    public record AdminCreateUserRequest(
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 2, max = 60) String firstName,
            @NotBlank @Size(min = 1, max = 60) String lastName,
            @NotBlank String role,
            @NotNull Long institutionId,
            @Size(max = 64) String studentIdentifier) {

        public AdminCreateUserRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
            role = role == null ? null : com.legacyloop.common.Roles.prefixed(role);
        }
    }

    /** Carries the reason recorded in the audit log when suspending an account. */
    public record ReasonRequest(@Size(max = 300) String reason) {
    }

    public record AuditLogResponse(Long id, Long userId, String email, String action, String detail,
                                   String ipAddress, Long actorId, Instant createdAt) {

        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.getId(), log.getUserId(), log.getEmail(), log.getAction().name(),
                    log.getDetail(), log.getIpAddress(), log.getActorId(), log.getCreatedAt());
        }
    }

    public record UserStatistics(long total, long pendingVerification, long pendingApproval, long active,
                                 long suspended, long students, long alumni, long staff, long premium) {
    }
}
