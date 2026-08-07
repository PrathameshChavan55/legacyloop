package com.legacyloop.user.repository;

import com.legacyloop.user.entity.AlumniProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

    Optional<AlumniProfile> findByUserId(Long userId);

    @Query("""
            select p from AlumniProfile p
            where p.profileVisible = true
              and (:excludeUserId is null or p.userId <> :excludeUserId)
              and (:institutionId is null or p.institutionId = :institutionId)
              and (:company is null or lower(p.currentCompany) = :company)
              and (:mentorsOnly = false or p.availableForMentorship = true)
            """)
    Page<AlumniProfile> browse(@Param("excludeUserId") Long excludeUserId,
                               @Param("institutionId") Long institutionId,
                               @Param("company") String company,
                               @Param("mentorsOnly") boolean mentorsOnly,
                               Pageable pageable);

    /** Alumni at a company who will refer — the candidate referrers for a job at that company. */
    @Query("""
            select p from AlumniProfile p
            where p.willingToRefer = true and p.profileVisible = true
              and lower(p.currentCompany) = lower(:company)
            """)
    List<AlumniProfile> findReferrersAtCompany(@Param("company") String company);

    @Query("select distinct p.currentCompany from AlumniProfile p "
            + "where p.currentCompany is not null order by p.currentCompany asc")
    List<String> findDistinctCompanies();

    long countByAvailableForMentorshipTrue();
}
