package com.legacyloop.career.repository;

import com.legacyloop.career.entity.Enums.ApplicationStatus;
import com.legacyloop.career.entity.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndApplicantUserId(Long jobId, Long applicantUserId);

    Optional<JobApplication> findByIdAndApplicantUserId(Long id, Long applicantUserId);

    Optional<JobApplication> findByJobIdAndApplicantUserId(Long jobId, Long applicantUserId);

    @Query("""
            select a from JobApplication a
            where a.applicantUserId = :userId
              and (:status is null or a.status = :status)
            order by a.id desc
            """)
    Page<JobApplication> findMine(@Param("userId") Long userId,
                                  @Param("status") ApplicationStatus status,
                                  Pageable pageable);

    @Query("""
            select a from JobApplication a
            where a.job.id = :jobId
              and (:status is null or a.status = :status)
            order by a.id desc
            """)
    Page<JobApplication> findForJob(@Param("jobId") Long jobId,
                                    @Param("status") ApplicationStatus status,
                                    Pageable pageable);

    /** Counts per status, for the dashboard funnel: one query instead of eight. */
    @Query("select a.status, count(a) from JobApplication a group by a.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
            select a.status, count(a) from JobApplication a
            where a.applicantUserId = :userId group by a.status
            """)
    List<Object[]> countGroupedByStatusForUser(@Param("userId") Long userId);

    /** Every application, oldest first — the CSV export streams this. */
    List<JobApplication> findAllByOrderByIdAsc();

    List<JobApplication> findByStatus(ApplicationStatus status);
}

