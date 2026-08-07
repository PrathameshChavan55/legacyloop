package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.ErrorCode;
import com.legacyloop.common.EventPublisher;
import com.legacyloop.common.Events;
import com.legacyloop.common.JwtService;
import com.legacyloop.common.Roles;
import com.legacyloop.user.dto.AuthDtos.AuthResponse;
import com.legacyloop.user.dto.AuthDtos.ChangePasswordRequest;
import com.legacyloop.user.dto.AuthDtos.LoginRequest;
import com.legacyloop.user.dto.AuthDtos.RegisterRequest;
import com.legacyloop.user.dto.AuthDtos.RegistrationResponse;
import com.legacyloop.user.dto.AuthDtos.ResetPasswordRequest;
import com.legacyloop.user.dto.AuthDtos.VerificationResponse;
import com.legacyloop.user.dto.AuthDtos.VerifyOtpRequest;
import com.legacyloop.user.dto.UserDtos.UserResponse;
import com.legacyloop.user.entity.AuditLog;
import com.legacyloop.user.entity.Institution;
import com.legacyloop.user.entity.OtpToken;
import com.legacyloop.user.entity.RefreshToken;
import com.legacyloop.user.entity.User;
import com.legacyloop.user.entity.UserStatus;
import com.legacyloop.user.repository.InstitutionRepository;
import com.legacyloop.user.repository.OtpTokenRepository;
import com.legacyloop.user.repository.RefreshTokenRepository;
import com.legacyloop.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, sign-in, tokens and passwords.
 *
 * <p>The original spread this over four interfaces and four implementation classes (auth, token,
 * OTP, internal lookup), each with one caller. They are one class here because they are one story:
 * a code is issued, a code is checked, a token pair is handed out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final OtpTokenRepository otpTokens;
    private final RefreshTokenRepository refreshTokens;
    private final InstitutionRepository institutions;
    private final ProfileService profiles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService audit;
    private final EventPublisher events;

    @Value("${legacyloop.otp.validity-minutes:10}")
    private long otpValidityMinutes;

    /* ------------------------------------------------------------------ registration */

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (!Roles.SELF_REGISTERABLE.contains(request.role())) {
            throw ApiException.badRequest("You can only register as a student or an alumnus");
        }
        if (users.existsByEmail(request.email())) {
            throw ApiException.conflict("An account already exists for that email address");
        }

        Institution institution = institutions.findById(request.institutionId())
                .filter(Institution::isActive)
                .orElseThrow(() -> ApiException.badRequest("Choose an active institution"));

        String identifier = request.studentIdentifier();
        if (Roles.ROLE_STUDENT.equals(request.role())) {
            validateStudentIdentifier(institution, identifier);
            if (users.existsByInstitutionIdAndStudentIdentifier(institution.getId(), identifier)) {
                throw ApiException.conflict("That %s is already registered"
                        .formatted(institution.getIdentifierLabel()));
            }
        } else {
            identifier = null;
        }

        User user = users.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(emptyToNull(request.phone()))
                .status(UserStatus.PENDING_VERIFICATION)
                .roles(Set.of(request.role()))
                .institutionId(institution.getId())
                .studentIdentifier(identifier)
                .build());

        issueVerificationCode(user.getEmail());
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.REGISTERED, "Self-registered as " + request.role());
        log.info("Registered user {} ({})", user.getId(), user.getEmail());

        return new RegistrationResponse(user.getId(), user.getEmail(), user.getStatus().name(),
                "VERIFY_EMAIL", otpValidityMinutes);
    }

    /**
     * The identifier rule is data on the institution, which is what lets a second college use the
     * platform without a code change. A blank pattern means "any non-empty value".
     */
    private void validateStudentIdentifier(Institution institution, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw ApiException.badRequest("Your %s is required".formatted(institution.getIdentifierLabel()));
        }
        String pattern = institution.getIdentifierPattern();
        if (pattern != null && !pattern.isBlank() && !identifier.matches(pattern)) {
            throw ApiException.badRequest("That does not look like a valid %s"
                    .formatted(institution.getIdentifierLabel()));
        }
    }

    @Transactional
    public VerificationResponse verifyOtp(VerifyOtpRequest request) {
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> ApiException.badRequest("That email address is not registered"));

        consumeCode(request.email(), request.otp(), OtpToken.Purpose.EMAIL_VERIFICATION);

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            // Students and alumni are trusted straight away; staff accounts wait for an admin.
            user.setStatus(user.hasRole(Roles.ROLE_STAFF) ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE);
        }

        profiles.createEmptyProfile(user);
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.EMAIL_VERIFIED, null);
        events.publish(Events.USER_REGISTERED, user.getId(), "Welcome to LegacyLoop",
                "Complete your profile so recruiters and alumni can find you.", "/profile");

        return new VerificationResponse(user.getId(), user.getStatus().name(), user.getStatus().canSignIn(),
                user.getStatus().canSignIn() ? "Your email is verified. You can sign in now."
                        : user.getStatus().rejectionMessage());
    }

    @Transactional
    public void resendOtp(String email) {
        users.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                throw ApiException.badRequest("That email address is already verified");
            }
            issueVerificationCode(email);
        });
        // Silence for an unknown address: it must not be possible to probe for registered emails.
    }

    /* ------------------------------------------------------------------------ sign-in */

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = users.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            audit.record(user == null ? null : user.getId(), request.email(),
                    AuditLog.Action.LOGIN_FAILED, "Bad credentials", ipAddress, null);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.defaultMessage());
        }
        if (!user.getStatus().canSignIn()) {
            audit.record(user.getId(), user.getEmail(), AuditLog.Action.LOGIN_FAILED,
                    "Status " + user.getStatus(), ipAddress, null);
            throw ApiException.forbidden(user.getStatus().rejectionMessage());
        }

        user.setLastLoginAt(Instant.now());
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.LOGIN_SUCCESS, null, ipAddress, null);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String token) {
        RefreshToken stored = refreshTokens.findByToken(token)
                .filter(RefreshToken::isUsable)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED,
                        "Your session has expired. Please sign in again."));

        // Rotate: the presented token is spent whether or not the next call succeeds.
        stored.setRevoked(true);

        User user = users.findById(stored.getUserId())
                .filter(candidate -> candidate.getStatus().canSignIn())
                .orElseThrow(() -> ApiException.unauthorized("This account can no longer sign in"));

        return issueTokens(user);
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.findByToken(token).ifPresent(stored -> {
            stored.setRevoked(true);
            audit.record(stored.getUserId(), null, AuditLog.Action.LOGOUT, null);
        });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.fullName(),
                user.getRoles(), user.getInstitutionId(), user.isPremiumNow());

        RefreshToken refreshToken = refreshTokens.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(jwtService.refreshTokenSeconds()))
                .build());

        return AuthResponse.of(accessToken, refreshToken.getToken(), jwtService.accessTokenSeconds(),
                UserResponse.from(user));
    }

    /* ---------------------------------------------------------------------- passwords */

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(email).ifPresent(user -> {
            String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
            storeCode(email, code, OtpToken.Purpose.PASSWORD_RESET);
            emailService.sendVerificationCode(email, code, otpValidityMinutes);
        });
        // Always report success, so the response cannot be used to enumerate accounts.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user;
        if (request.email() != null && !request.email().isBlank() && request.otp() != null && !request.otp().isBlank()) {
            user = users.findByEmail(request.email())
                    .orElseThrow(() -> ApiException.badRequest("That verification code is invalid or has expired"));
            consumeCode(request.email(), request.otp(), OtpToken.Purpose.PASSWORD_RESET);
        } else if (request.token() != null && !request.token().isBlank()) {
            OtpToken token = otpTokens.findFirstByCodeHashAndPurposeAndUsedFalse(
                            hash(request.token()), OtpToken.Purpose.PASSWORD_RESET)
                    .filter(OtpToken::isUsable)
                    .orElseThrow(() -> ApiException.badRequest("That reset token is invalid or has expired"));
            user = users.findByEmail(token.getEmail())
                    .orElseThrow(() -> ApiException.badRequest("That reset token is invalid or has expired"));
            token.setUsed(true);
        } else {
            throw ApiException.badRequest("Please enter the 6-digit OTP code sent to your email");
        }

        applyNewPassword(user, request.newPassword());
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.PASSWORD_RESET, "Reset by OTP verification");
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Your current password is not correct");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("Choose a password you have not used before");
        }

        applyNewPassword(user, request.newPassword());
        audit.record(user.getId(), user.getEmail(), AuditLog.Action.PASSWORD_CHANGED, null);
    }

    /** Changing a password ends every other session, which is the point of changing it. */
    private void applyNewPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMustChangePassword(false);
        refreshTokens.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("User", userId));
    }

    /* --------------------------------------------------------------------- one-time codes */

    private void issueVerificationCode(String email) {
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        storeCode(email, code, OtpToken.Purpose.EMAIL_VERIFICATION);
        emailService.sendVerificationCode(email, code, otpValidityMinutes);
    }

    /** Any earlier unused code for the same purpose is retired, so only the newest one works. */
    private void storeCode(String email, String rawCode, OtpToken.Purpose purpose) {
        otpTokens.findByEmailAndPurposeAndUsedFalse(email, purpose)
                .forEach(previous -> previous.setUsed(true));

        otpTokens.save(OtpToken.builder()
                .email(email)
                .codeHash(hash(rawCode))
                .purpose(purpose)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(otpValidityMinutes)))
                .build());
    }

    private void consumeCode(String email, String rawCode, OtpToken.Purpose purpose) {
        OtpToken token = otpTokens.findFirstByEmailAndPurposeAndUsedFalseOrderByIdDesc(email, purpose)
                .orElseThrow(() -> ApiException.badRequest("Request a new code and try again"));

        if (!token.isUsable()) {
            throw ApiException.badRequest("That code has expired. Request a new one.");
        }
        if (!token.getCodeHash().equals(hash(rawCode))) {
            token.setAttempts(token.getAttempts() + 1);
            throw ApiException.badRequest("That code is not correct");
        }
        token.setUsed(true);
    }

    /**
     * Codes are stored as a SHA-256 digest rather than in the clear: a leaked row is then not a
     * usable code. The digest is deterministic (unlike BCrypt) so a reset link can be looked up by
     * its hash instead of scanning the table.
     */
    private static String hash(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is always available", ex);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
