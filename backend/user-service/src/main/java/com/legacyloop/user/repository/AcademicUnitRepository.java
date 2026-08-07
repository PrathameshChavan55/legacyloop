package com.legacyloop.user.repository;

import com.legacyloop.user.entity.AcademicUnit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicUnitRepository extends JpaRepository<AcademicUnit, Long> {

    /** One finder serves departments, programs, branches and batches. */
    @Query("""
            select a from AcademicUnit a
            where a.type = :type
              and (:institutionId is null or a.institutionId = :institutionId)
              and (:parentId is null or a.parentId = :parentId)
              and (:active is null or a.active = :active)
            order by a.name asc
            """)
    List<AcademicUnit> findAll(@Param("type") AcademicUnit.Type type,
                               @Param("institutionId") Long institutionId,
                               @Param("parentId") Long parentId,
                               @Param("active") Boolean active);

    List<AcademicUnit> findByTypeAndPlacementOpenTrueAndActiveTrue(AcademicUnit.Type type);

    boolean existsByTypeAndInstitutionIdAndCode(AcademicUnit.Type type, Long institutionId, String code);

    /** Loads several units in one query, so a list of profiles does not fan out into N lookups. */
    List<AcademicUnit> findByIdIn(List<Long> ids);

    List<AcademicUnit> findByParentId(Long parentId);

    boolean existsByParentIdAndActiveTrue(Long parentId);
}
