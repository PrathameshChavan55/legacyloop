package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.user.dto.AcademicDtos.AcademicRequest;
import com.legacyloop.user.dto.AcademicDtos.AcademicResponse;
import com.legacyloop.user.entity.AcademicUnit;
import com.legacyloop.user.entity.AcademicUnit.Type;
import com.legacyloop.user.repository.AcademicUnitRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Departments, programs, branches and batches — one service for all four.
 *
 * <p>The type arrives as a path segment, so adding a fifth kind of unit would be an enum constant
 * and nothing else.
 */
@Service
@RequiredArgsConstructor
public class AcademicService {

    private final AcademicUnitRepository units;

    @Transactional(readOnly = true)
    public List<AcademicResponse> list(Type type, Long institutionId, Long parentId, Boolean active) {
        List<AcademicUnit> found = units.findAll(type, institutionId, parentId, active);
        return withParentNames(found);
    }

    /** Batches whose students may currently be shortlisted for a drive. */
    @Transactional(readOnly = true)
    public List<AcademicResponse> placementOpenBatches() {
        return withParentNames(units.findByTypeAndPlacementOpenTrueAndActiveTrue(Type.BATCH));
    }

    @Transactional(readOnly = true)
    public AcademicResponse findById(Long id) {
        AcademicUnit unit = load(id);
        return AcademicResponse.from(unit, parentName(unit.getParentId()));
    }

    @Transactional
    public AcademicResponse create(Type type, AcademicRequest request) {
        if (units.existsByTypeAndInstitutionIdAndCode(type, request.institutionId(), request.code())) {
            throw ApiException.conflict("A %s with code %s already exists"
                    .formatted(type.name().toLowerCase(), request.code()));
        }
        validateParent(type, request.parentId());

        AcademicUnit unit = AcademicUnit.builder()
                .type(type)
                .institutionId(request.institutionId())
                .code(request.code())
                .name(request.name())
                .parentId(request.parentId())
                .startYear(request.startYear())
                .endYear(request.endYear())
                .placementOpen(type == Type.BATCH && Boolean.TRUE.equals(request.placementOpen()))
                .build();
        return AcademicResponse.from(units.save(unit), parentName(unit.getParentId()));
    }

    @Transactional
    public AcademicResponse update(Long id, AcademicRequest request) {
        AcademicUnit unit = load(id);
        validateParent(unit.getType(), request.parentId());

        unit.setName(request.name());
        unit.setParentId(request.parentId());
        unit.setStartYear(request.startYear());
        unit.setEndYear(request.endYear());
        if (unit.getType() == Type.BATCH && request.placementOpen() != null) {
            unit.setPlacementOpen(request.placementOpen());
        }
        return AcademicResponse.from(unit, parentName(unit.getParentId()));
    }

    /**
     * Master data is deactivated, never deleted: a batch referenced by five hundred student
     * profiles cannot disappear without taking their history with it.
     */
    @Transactional
    public AcademicResponse setActive(Long id, boolean active) {
        AcademicUnit unit = load(id);
        unit.setActive(active);
        if (!active) {
            deactivateChildrenRecursively(id);
        } else if (unit.getParentId() != null) {
            activateParentRecursively(unit.getParentId());
        }
        AcademicUnit saved = units.save(unit);
        return AcademicResponse.from(saved, parentName(saved.getParentId()));
    }

    private void deactivateChildrenRecursively(Long parentId) {
        List<AcademicUnit> children = units.findByParentId(parentId);
        for (AcademicUnit child : children) {
            if (child.isActive()) {
                child.setActive(false);
                units.save(child);
                deactivateChildrenRecursively(child.getId());
            }
        }
    }

    private void activateParentRecursively(Long parentId) {
        if (parentId == null) return;
        units.findById(parentId).ifPresent(parent -> {
            if (!parent.isActive()) {
                parent.setActive(true);
                units.save(parent);
                activateParentRecursively(parent.getParentId());
            }
        });
    }

    /** Resolves names for a list of ids in one query — used by the profile views. */
    @Transactional(readOnly = true)
    public Map<Long, String> namesOf(List<Long> ids) {
        List<Long> present = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (present.isEmpty()) {
            return Map.of();
        }
        return units.findByIdIn(present).stream()
                .collect(Collectors.toMap(AcademicUnit::getId, AcademicUnit::getName));
    }

    private void validateParent(Type type, Long parentId) {
        if (type == Type.DEPARTMENT) {
            return;
        }
        if (parentId == null) {
            throw ApiException.badRequest("A %s needs a parent".formatted(type.name().toLowerCase()));
        }
        Type expected = type == Type.PROGRAM ? Type.DEPARTMENT : Type.PROGRAM;
        AcademicUnit parent = load(parentId);
        if (parent.getType() != expected) {
            throw ApiException.badRequest("The parent of a %s must be a %s"
                    .formatted(type.name().toLowerCase(), expected.name().toLowerCase()));
        }
    }

    /** One extra query for the whole list rather than one per row. */
    private List<AcademicResponse> withParentNames(List<AcademicUnit> found) {
        Map<Long, String> parents = namesOf(found.stream().map(AcademicUnit::getParentId).toList());
        return found.stream()
                .map(unit -> AcademicResponse.from(unit, parents.get(unit.getParentId())))
                .toList();
    }

    private String parentName(Long parentId) {
        return Optional.ofNullable(parentId).flatMap(units::findById).map(AcademicUnit::getName).orElse(null);
    }

    private AcademicUnit load(Long id) {
        return units.findById(id).orElseThrow(() -> ApiException.notFound("Academic unit", id));
    }
}
