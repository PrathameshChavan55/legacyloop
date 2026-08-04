package com.legacyloop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.legacyloop.common.validation.StrongPassword;
import com.legacyloop.common.validation.ValidStudentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * SRS REQ-1.1 / REQ-1.2. Role-specific fields are validated in the service, because
 * "required only when role = STUDENT" cannot be expressed with a field annotation alone.
 *
 * <p>Two things changed here and both were live bugs:</p>
 * <ul>
 *   <li>The field was called {@code prn} while the SPA posted {@code studentIdentifier}.
 *       Jackson dropped the unknown property, the service saw null, and every student got
 *       "Students must provide a PRN and a batch id" with both boxes filled in. The name is
 *       now the one the client sends, with {@code @JsonAlias} keeping the old spelling
 *       working for saved Postman requests.</li>
 *   <li>The contact number regex was {@code ^[6-9]\\d{9}$} - an Indian mobile. This product
 *       was deliberately made institution-agnostic; a hardcoded country rule undoes that.</li>
 * </ul>
 */
@Schema(description = "Self-registration payload. Only STUDENT and ALUMNI may register.")
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120)
        String fullName,

        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^[+]?[0-9][0-9\\s-]{6,19}$",
                message = "Enter a valid contact number")
        String contactNumber,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(ROLE_)?(STUDENT|ALUMNI)$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Role must be STUDENT or ALUMNI")
        String role,

        // ---- student ----

        /** Whatever the institution calls it. The rule comes from institution settings. */
        @JsonAlias("prn")
        @ValidStudentIdentifier
        @Size(max = 64)
        @Schema(description = "The institution's student identifier. Accepts 'prn' as an alias.")
        String studentIdentifier,

        /** The batch <em>code</em>, not its id or label. Must match an open batch. */
        @JsonAlias({"batchCode", "batch"})
        @Size(max = 60)
        String batchId,

        // ---- alumni ----
        @Min(value = 1950, message = "Graduation year looks wrong")
        @Max(value = 2100, message = "Graduation year looks wrong")
        Integer graduationYear,

        @Size(max = 120)
        String companyName,

        @Size(max = 120)
        String designation) {
}
