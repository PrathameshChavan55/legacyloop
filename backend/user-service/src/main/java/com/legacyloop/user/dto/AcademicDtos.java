package com.legacyloop.user.dto;

import com.legacyloop.user.entity.AcademicUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** One request and one response shape for departments, programs, branches and batches. */
public final class AcademicDtos {

    private AcademicDtos() {
    }

    public record AcademicRequest(
            @NotNull(message = "An institution is required") Long institutionId,
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 160) String name,
            /** Department for a program, program for a branch or a batch. */
            Long parentId,
            Integer startYear,
            Integer endYear,
            Boolean placementOpen) {

        public AcademicRequest {
            code = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
            name = name == null ? null : name.trim();
        }
    }

    public record AcademicResponse(Long id, String type, Long institutionId, String code, String name,
                                   Long parentId, String parentName, Integer startYear, Integer endYear,
                                   boolean placementOpen, boolean active) {

        public static AcademicResponse from(AcademicUnit unit, String parentName) {
            return new AcademicResponse(unit.getId(), unit.getType().name(), unit.getInstitutionId(),
                    unit.getCode(), unit.getName(), unit.getParentId(), parentName, unit.getStartYear(),
                    unit.getEndYear(), unit.isPlacementOpen(), unit.isActive());
        }
    }
}
