package com.legacyloop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Everything the sign-up and sign-in screens send and receive.
 *
 * <p>Records rather than Lombok classes: a request object is a value, and a record gives
 * immutability, equals and a compact constructor for free. The compact constructors below
 * normalise input once, so no service has to remember to trim or lower-case an email.
 *
 * <p>Password rules are a {@code @Pattern} rather than the original's custom
 * {@code @StrongPassword} annotation plus validator class — the same rule, one line, no extra
 * types to explain.
 */
public final class AuthDtos {

    /** At least 8 characters with an upper-case letter, a lower-case letter, a digit and a symbol. */
    public static final String PASSWORD_RULE =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";

    public static final String PASSWORD_MESSAGE =
            "Password needs 8+ characters with an upper-case letter, a lower-case letter, a digit and a symbol";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password,
            @NotBlank @Size(min = 2, max = 60) String firstName,
            @NotBlank @Size(min = 1, max = 60) String lastName,
            @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number") String phone,
            @NotBlank String role,
            @NotNull(message = "Choose your institution") Long institutionId,
            @Size(max = 64) String studentIdentifier) {

        public RegisterRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
            firstName = firstName == null ? null : firstName.trim();
            lastName = lastName == null ? null : lastName.trim();
            studentIdentifier = studentIdentifier == null || studentIdentifier.isBlank()
                    ? null : studentIdentifier.trim().toUpperCase(Locale.ROOT);
            role = role == null ? null : com.legacyloop.common.Roles.prefixed(role);
        }
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

        public LoginRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public record VerifyOtpRequest(@NotBlank @Email String email,
                                   @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code") String otp) {

        public VerifyOtpRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public record EmailOnlyRequest(@NotBlank @Email String email) {

        public EmailOnlyRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record ResetPasswordRequest(
            String email,
            String otp,
            String token,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {

        public ResetPasswordRequest {
            email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
            otp = otp == null ? null : otp.trim();
            token = token == null ? null : token.trim();
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    /** Returned by register: the account exists but cannot sign in until the code is entered. */
    public record RegistrationResponse(Long userId, String email, String status, String nextStep,
                                       long otpValidityMinutes) {
    }

    public record VerificationResponse(Long userId, String status, boolean canSignIn, String message) {
    }

    /** The token pair plus the account, so the client needs one call to sign in and render. */
    public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                               long expiresInSeconds, UserDtos.UserResponse user,
                               boolean mustChangePassword) {

        public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                      UserDtos.UserResponse user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user,
                    user.mustChangePassword());
        }
    }
}
