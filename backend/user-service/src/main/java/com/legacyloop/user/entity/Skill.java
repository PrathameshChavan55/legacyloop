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
 * A skill in the shared vocabulary. New names are created on first use and start unapproved, so
 * autocomplete stays useful without an administrator having to seed every technology.
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 60)
    private String category;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private long usageCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;
}
