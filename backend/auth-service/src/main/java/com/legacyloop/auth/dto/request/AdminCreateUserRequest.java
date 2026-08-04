package com.legacyloop.auth.dto.request;

import com.legacyloop.common.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * An account created by an administrator rather than by self-registration.
 *
 * <p>This is the gap that made the platform unusable on day one: self-registration is
 * limited to STUDENT and ALUMNI, so there was no way to create a second staff member or a
 * second administrator except by editing the database. Accounts created here skip OTP and
 * approval - an administrator vouching for someone is the verification.</p>
 */
@Schema(description = "Administrator-created account. Any role. Active immediately.")
public record AdminCreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150)
        String email,

        @NotBlank(message = "A starting password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120)
        String fullName,

        @Pattern(regexp = "^$|^[+]?[0-9][0-9\\s-]{6,19}$", message = "Enter a valid contact number")
        @Size(max = 20)
        String contactNumber,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(ROLE_)?(STUDENT|ALUMNI|INSTITUTION_STAFF|PLATFORM_ADMIN)$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Role must be STUDENT, ALUMNI, INSTITUTION_STAFF or PLATFORM_ADMIN")
        String role,

        @Size(max = 200)
        String headline,

        // ---- only read when role = STUDENT ----
        @Size(max = 64) String studentIdentifier,
        @Size(max = 60) String batchId,

        // ---- only read when role = ALUMNI ----
        @Min(1950) @Max(2100) Integer graduationYear,
        @Size(max = 120) String companyName,
        @Size(max = 120) String designation) {
}
