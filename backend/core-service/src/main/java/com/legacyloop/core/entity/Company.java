package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies", indexes = @Index(name = "idx_company_name", columnList = "name"))
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 100)
    private String industry;

    @Column(length = 300)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String location;

    @Column(nullable = false)
    private boolean verified;
}
