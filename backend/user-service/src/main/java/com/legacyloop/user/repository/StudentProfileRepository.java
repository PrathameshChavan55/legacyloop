package com.legacyloop.user.repository;

import com.legacyloop.user.entity.StudentProfile;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    @Query("""
            select p from StudentProfile p
            where p.profileVisible = true
              and (:excludeUserId is null or p.userId <> :excludeUserId)
              and (:institutionId is null or p.institutionId = :institutionId)
              and (:batchId is null or p.batchId = :batchId)
              and (:openToWork is null or p.openToWork = :openToWork)
            """)
    Page<StudentProfile> browse(@Param("excludeUserId") Long excludeUserId,
                                @Param("institutionId") Long institutionId,
                                @Param("batchId") Long batchId,
                                @Param("openToWork") Boolean openToWork,
                                Pageable pageable);

    long countByPlacedTrue();
}
