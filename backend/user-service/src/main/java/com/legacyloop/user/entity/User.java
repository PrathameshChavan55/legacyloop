package com.legacyloop.user.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An account.
 *
 * <p>Roles are a string collection rather than the original's role and permission entities joined
 * through two more tables. Four fixed roles do not need a database-driven permission model, and
 * {@code @PreAuthorize("hasRole('STUDENT')")} reads better than a permission lookup.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_institution", columnList = "institution_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /** Eagerly fetched: every request needs the roles to build the token or the security context. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 40)
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Column(name = "institution_id")
    private Long institutionId;

    /** Roll number, PRN, employee code — whatever the institution calls it. */
    @Column(name = "student_identifier", length = 64)
    private String studentIdentifier;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean premium = false;

    @Column(name = "premium_until")
    private Instant premiumUntil;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Premium expires by time, so the flag on its own is not the answer. */
    public boolean isPremiumNow() {
        return premium && premiumUntil != null && premiumUntil.isAfter(Instant.now());
    }
}
