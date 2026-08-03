package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alumni", indexes = @Index(name = "idx_alumni_user", columnList = "user_id", unique = true))
public class AlumniProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "company_name", length = 120)
    private String companyName;

    @Column(length = 120)
    private String designation;

    @Column(length = 1000)
    private String expertise;

    @Column(name = "mentorship_available", nullable = false)
    private boolean mentorshipAvailable;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;
}
