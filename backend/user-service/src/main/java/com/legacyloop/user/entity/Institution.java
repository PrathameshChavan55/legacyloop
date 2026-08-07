package com.legacyloop.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant. Branding and the student-identifier rules are data, not code, which is what lets one
 * deployment serve several colleges without a rebuild.
 */
@Entity
@Table(name = "institutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "short_name", length = 60)
    private String shortName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Hex colour used for the accent of the whole UI. */
    @Column(name = "primary_color", length = 9)
    @Builder.Default
    private String primaryColor = "#4f46e5";

    /** What this institution calls a student's number, e.g. "PRN" or "Roll number". */
    @Column(name = "identifier_label", length = 60)
    @Builder.Default
    private String identifierLabel = "Student ID";

    /** Regex a student identifier must match. Null means "any non-blank value". */
    @Column(name = "identifier_pattern", length = 200)
    private String identifierPattern;

    /** Display name for the staff role, e.g. "Placement Head" or "Career Officer". */
    @Column(name = "staff_role_label", length = 60)
    @Builder.Default
    private String staffRoleLabel = "Placement staff";

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(length = 120)
    private String city;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
