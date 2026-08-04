package com.legacyloop.auth.service.impl;

import com.legacyloop.auth.constant.AuthConstants;
import com.legacyloop.auth.dto.request.ForgotPasswordRequest;
import com.legacyloop.auth.dto.request.LoginRequest;
import com.legacyloop.auth.dto.request.RefreshTokenRequest;
import com.legacyloop.auth.dto.request.RegisterRequest;
import com.legacyloop.auth.dto.request.ResendOtpRequest;
import com.legacyloop.auth.dto.request.ResetPasswordRequest;
import com.legacyloop.auth.dto.request.VerifyOtpRequest;
import com.legacyloop.auth.dto.response.AuthResponse;
import com.legacyloop.auth.dto.response.UserResponse;
import com.legacyloop.auth.entity.RoleEntity;
import com.legacyloop.auth.entity.User;
import com.legacyloop.auth.enums.OtpPurpose;
import com.legacyloop.auth.institution.entity.Batch;
import com.legacyloop.auth.institution.repository.BatchRepository;
import com.legacyloop.auth.institution.service.InstitutionService;
import com.legacyloop.auth.mapper.UserMapper;
import com.legacyloop.auth.publisher.AuthEventPublisher;
import com.legacyloop.auth.repository.RoleRepository;
import com.legacyloop.auth.repository.UserRepository;
import com.legacyloop.auth.security.JwtTokenProvider;
import com.legacyloop.auth.service.AuditService;
import com.legacyloop.auth.service.AuthService;
import com.legacyloop.auth.service.OtpService;
import com.legacyloop.auth.service.TokenService;
import com.legacyloop.common.enums.Role;
import com.legacyloop.common.enums.UserStatus;
import com.legacyloop.common.exception.ConflictException;
import com.legacyloop.common.exception.ErrorCode;
import com.legacyloop.common.exception.ForbiddenException;
import com.legacyloop.common.exception.ResourceNotFoundException;
import com.legacyloop.common.exception.UnauthorizedException;
import com.legacyloop.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Registration -> OTP -> approval -> login -> refresh -> logout.
 *
 * <p>Deliberate choices worth knowing:</p>
 * <ul>
 *   <li>Login failures never reveal WHICH part was wrong - always LL-AUTH-001.</li>
 *   <li>Forgot-password always returns 200, even for an unknown email, so the endpoint
 *       cannot be used to enumerate registered users.</li>
 *   <li>Students are auto-verified after OTP; alumni need staff approval (REQ-1.3).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BatchRepository batchRepository;
    private final InstitutionService institutionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final AuditService auditService;
    private final AuthEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Role role = Role.from(request.role());
        if (role == null || !role.isSelfRegisterable()) {
            throw new ValidationException("Role must be STUDENT or ALUMNI");
        }

        validateRoleSpecificFields(request, role);

        Long institutionId = institutionService.currentInstitutionId();
        String identifier = trimmed(request.studentIdentifier());
        Batch batch = null;

        if (role == Role.ROLE_STUDENT) {
            if (userRepository.existsByStudentIdentifierIgnoreCase(identifier)) {
                throw new ConflictException(ErrorCode.IDENTIFIER_ALREADY_REGISTERED);
            }
            batch = resolveBatch(institutionId, trimmed(request.batchId()));
        }

        RoleEntity roleEntity = roleRepository.findByName(role)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", role));

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .contactNumber(request.contactNumber())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .premium(false)
                .institutionId(institutionId)
                .studentIdentifier(role == Role.ROLE_STUDENT ? identifier : null)
                .batchId(batch == null ? null : batch.getCode())
                .batchRefId(batch == null ? null : batch.getId())
                .graduationYear(role == Role.ROLE_ALUMNI ? request.graduationYear() : null)
                .companyName(role == Role.ROLE_ALUMNI ? trimmed(request.companyName()) : null)
                .designation(role == Role.ROLE_ALUMNI ? trimmed(request.designation()) : null)
                .failedLoginAttempts(0)
                .build();
        user.addRole(roleEntity);

        User saved = userRepository.save(user);
        log.info("Registered user {} with role {} in batch {}", saved.getId(), role, saved.getBatchId());

        otpService.generateAndSend(saved.getEmail(), saved.getFullName(), OtpPurpose.EMAIL_VERIFICATION);
        auditService.record(saved.getId(), AuthConstants.ACTION_REGISTER, "User",
                String.valueOf(saved.getId()), "role=" + role);

        return userMapper.toResponse(saved);
    }

    /**
     * Turns a batch code into the row it names.
     *
     * <p>Previously the code was stored as free text with no lookup at all, which is how
     * {@code batch_ref_id} ended up permanently null and how a typo became a permanent
     * orphan record. Each failure gets its own message: "you didn't pick one", "that one
     * doesn't exist" and "that one is closed" are three different things the person needs
     * to do three different things about.</p>
     */
    private Batch resolveBatch(Long institutionId, String batchCode) {
        Batch batch = batchRepository
                .findByInstitutionIdAndCodeIgnoreCase(institutionId, batchCode)
                .orElseThrow(() -> new ValidationException(
                        "No batch is registered under the code '" + batchCode
                                + "'. Pick one from the list."));

        if (!batch.isActive()) {
            throw new ValidationException("Batch '" + batch.getLabel()
                    + "' is closed and is no longer accepting registrations.");
        }
        return batch;
    }

    private void validateRoleSpecificFields(RegisterRequest request, Role role) {
        // REQ-1.2 - cannot be expressed as field annotations because it depends on the role.
        if (role == Role.ROLE_STUDENT) {
            if (isBlank(request.studentIdentifier())) {
                throw new ValidationException("Your student identifier is required");
            }
            if (isBlank(request.batchId())) {
                throw new ValidationException("Select the batch you belong to");
            }
        } else if (role == Role.ROLE_ALUMNI) {
            if (request.graduationYear() == null || isBlank(request.companyName())
                    || isBlank(request.designation())) {
                throw new ValidationException(
                        "Alumni must provide graduation year, company name and designation");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        otpService.verify(email, request.otp(), OtpPurpose.EMAIL_VERIFICATION);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setEmailVerified(true);

        boolean isStudent = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.ROLE_STUDENT);

        // REQ-1.3: students go straight through, alumni wait for institution staff.
        user.setStatus(isStudent ? UserStatus.ACTIVE : UserStatus.PENDING_APPROVAL);
        userRepository.save(user);

        auditService.record(user.getId(), AuthConstants.ACTION_VERIFY_EMAIL, "User",
                String.valueOf(user.getId()), "status=" + user.getStatus());

        eventPublisher.publishUserRegistered(user);
        if (user.getStatus() == UserStatus.ACTIVE) {
            eventPublisher.publishUserVerified(user);
        }
        log.info("User {} verified email, status now {}", user.getId(), user.getStatus());
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.isEmailVerified()) {
            throw new ConflictException(ErrorCode.VALIDATION_FAILED, "This email is already verified");
        }
        otpService.generateAndSend(user.getEmail(), user.getFullName(), OtpPurpose.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim().toLowerCase();
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email);

        if (maybeUser.isEmpty()) {
            auditService.recordLogin(null, email, false, "UNKNOWN_EMAIL", ipAddress, userAgent);
            // Same error as a wrong password: do not leak which emails exist.
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = maybeUser.get();

        if (user.isLocked()) {
            auditService.recordLogin(user.getId(), email, false, "ACCOUNT_LOCKED", ipAddress, userAgent);
            throw new ForbiddenException(ErrorCode.ACCOUNT_SUSPENDED,
                    "Too many failed attempts. Try again in a few minutes.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            auditService.recordLogin(user.getId(), email, false, "BAD_PASSWORD", ipAddress, userAgent);
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        assertLoginAllowed(user, email, ipAddress, userAgent);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = tokenService.issueRefreshToken(user, ipAddress, userAgent);

        auditService.recordLogin(user.getId(), email, true, null, ipAddress, userAgent);
        auditService.record(user.getId(), AuthConstants.ACTION_LOGIN, "User",
                String.valueOf(user.getId()), null);
        log.info("User {} logged in", user.getId());

        return AuthResponse.of(accessToken, refreshToken,
                jwtTokenProvider.getAccessTtlSeconds(), userMapper.toResponse(user));
    }

    private void assertLoginAllowed(User user, String email, String ipAddress, String userAgent) {
        if (!user.isEmailVerified()) {
            auditService.recordLogin(user.getId(), email, false, "NOT_VERIFIED", ipAddress, userAgent);
            throw new ForbiddenException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        switch (user.getStatus()) {
            case PENDING_APPROVAL -> {
                auditService.recordLogin(user.getId(), email, false, "PENDING_APPROVAL", ipAddress, userAgent);
                throw new ForbiddenException(ErrorCode.ACCOUNT_PENDING_APPROVAL);
            }
            case SUSPENDED, DELETED -> {
                auditService.recordLogin(user.getId(), email, false, "SUSPENDED", ipAddress, userAgent);
                throw new ForbiddenException(ErrorCode.ACCOUNT_SUSPENDED);
            }
            case PENDING_VERIFICATION -> throw new ForbiddenException(ErrorCode.ACCOUNT_NOT_VERIFIED);
            case ACTIVE -> { /* allowed */ }
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= AuthConstants.MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(AuthConstants.ACCOUNT_LOCK_DURATION));
            log.warn("Locking user {} after {} failed attempts", user.getId(), attempts);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        TokenService.RotatedToken rotated =
                tokenService.consumeAndRotate(request.refreshToken(), ipAddress, userAgent);

        // Re-read with roles so the new access token carries current authorities and premium.
        User fresh = userRepository.findWithRolesById(rotated.user().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", rotated.user().getId()));

        if (fresh.getStatus() != UserStatus.ACTIVE) {
            tokenService.revokeAllForUser(fresh.getId());
            throw new ForbiddenException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(fresh);
        auditService.record(fresh.getId(), AuthConstants.ACTION_TOKEN_REFRESH, "User",
                String.valueOf(fresh.getId()), null);

        return AuthResponse.of(accessToken, rotated.rawToken(),
                jwtTokenProvider.getAccessTtlSeconds(), userMapper.toResponse(fresh));
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revokeByRawToken(refreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            tokenService.blocklistAccessToken(accessToken);
        }
        com.legacyloop.common.security.SecurityContextUtil.currentUser().ifPresent(user ->
                auditService.record(user.userId(), AuthConstants.ACTION_LOGOUT, "User",
                        String.valueOf(user.userId()), null));
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        // Always behaves identically, whether or not the account exists.
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                user -> otpService.generateAndSend(user.getEmail(), user.getFullName(),
                        OtpPurpose.PASSWORD_RESET),
                () -> log.info("Password reset requested for unknown email (no mail sent)"));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        otpService.verify(email, request.otp(), OtpPurpose.PASSWORD_RESET);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // A password change invalidates every existing session.
        tokenService.revokeAllForUser(user.getId());
        auditService.record(user.getId(), AuthConstants.ACTION_PASSWORD_RESET, "User",
                String.valueOf(user.getId()), null);
        log.info("Password reset completed for user {}", user.getId());
    }
}
