package com.legacyloop.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Departments, programs, branches and batches.
 *
 * <p>The original modelled these as four entities with four repositories, four services and four
 * near-identical controller blocks — roughly nine hundred lines to say "named thing, optional
 * parent, active flag" four times. They are one entity here, distinguished by {@link Type} and
 * chained through {@code parentId}: batch → program → department, branch → program.
 *
 * <p>The three batch-only columns are null for the other types. That is the whole cost of the
 * merge, and it buys one repository, one service and one controller.
 */
@Entity
@Table(name = "academic_units", indexes = {
        @Index(name = "idx_academic_type", columnList = "institution_id,type"),
        @Index(name = "idx_academic_parent", columnList = "parent_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicUnit {

    public enum Type {
        DEPARTMENT, PROGRAM, BRANCH, BATCH;

        /** The plural slug used in the URL, e.g. {@code /api/v1/academics/departments}. */
        public String slug() {
            return name().toLowerCase() + "s";
        }

        public static Type fromSlug(String slug) {
            for (Type type : values()) {
                if (type.slug().equalsIgnoreCase(slug)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown academic type: " + slug);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    /** Department for a program, program for a branch or batch, null for a department. */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    /** Batches only: whether this cohort may be shortlisted for drives right now. */
    @Column(name = "placement_open", nullable = false)
    @Builder.Default
    private boolean placementOpen = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
