package com.legacyloop.user.dto;

import com.legacyloop.user.entity.Institution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public final class InstitutionDtos {

    private InstitutionDtos() {
    }

    public record InstitutionRequest(
            @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,31}$",
                    message = "Code may contain upper-case letters, digits, hyphens and underscores") String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 60) String shortName,
            @Size(max = 500) String logoUrl,
            @Pattern(regexp = "^#([0-9a-fA-F]{6})$", message = "Use a hex colour such as #4f46e5") String primaryColor,
            @Size(max = 60) String identifierLabel,
            @Size(max = 200) String identifierPattern,
            @Size(max = 60) String staffRoleLabel,
            @Size(max = 180) String contactEmail,
            @Size(max = 120) String city) {

        public InstitutionRequest {
            code = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
            name = name == null ? null : name.trim();
        }
    }

    /** Everything an administrator sees. */
    public record InstitutionResponse(Long id, String code, String name, String shortName, String logoUrl,
                                      String primaryColor, String identifierLabel, String identifierPattern,
                                      String staffRoleLabel, String contactEmail, String city, boolean active) {

        public static InstitutionResponse from(Institution institution) {
            return new InstitutionResponse(institution.getId(), institution.getCode(), institution.getName(),
                    institution.getShortName(), institution.getLogoUrl(), institution.getPrimaryColor(),
                    institution.getIdentifierLabel(), institution.getIdentifierPattern(),
                    institution.getStaffRoleLabel(), institution.getContactEmail(), institution.getCity(),
                    institution.isActive());
        }
    }

    /**
     * The public subset: the sign-up page needs the name, the colour and the label for the student
     * identifier before anyone has authenticated, but not the contact details.
     */
    public record BrandingResponse(Long id, String code, String name, String shortName, String logoUrl,
                                   String primaryColor, String identifierLabel, String identifierPattern,
                                   String staffRoleLabel) {

        public static BrandingResponse from(Institution institution) {
            return new BrandingResponse(institution.getId(), institution.getCode(), institution.getName(),
                    institution.getShortName(), institution.getLogoUrl(), institution.getPrimaryColor(),
                    institution.getIdentifierLabel(), institution.getIdentifierPattern(),
                    institution.getStaffRoleLabel());
        }
    }
}
