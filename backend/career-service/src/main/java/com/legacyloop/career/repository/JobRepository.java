package com.legacyloop.career.repository;

import com.legacyloop.career.entity.Enums.JobStatus;
import com.legacyloop.career.entity.Enums.JobType;
import com.legacyloop.career.entity.Enums.WorkMode;
import com.legacyloop.career.entity.Job;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {

    /**
     * The job board and the staff console are the same query with different parameters.
     *
     * <p>The original built this with a {@code JobSpecification} class plus a criteria DTO plus a
     * builder; every branch in it was "add this predicate unless the field is null", which is what
     * the null checks below do in one place.
     */
    @Query("""
            select j from Job j
            where (:query is null or lower(j.title) like :query or lower(j.company.name) like :query
                   or lower(j.location) like :query)
              and (:status is null or j.status = :status)
              and (:jobType is null or j.jobType = :jobType)
              and (:workMode is null or j.workMode = :workMode)
              and (:companyId is null or j.company.id = :companyId)
              and (:institutionId is null or j.institutionId = :institutionId)
              and (:postedBy is null or j.postedByUserId = :postedBy)
            """)
    Page<Job> search(@Param("query") String query,
                     @Param("status") JobStatus status,
                     @Param("jobType") JobType jobType,
                     @Param("workMode") WorkMode workMode,
                     @Param("companyId") Long companyId,
                     @Param("institutionId") Long institutionId,
                     @Param("postedBy") Long postedBy,
                     Pageable pageable);

    long countByStatus(JobStatus status);

    long countByPublishedAtAfter(Instant since);

    /** Top employers by number of postings — one row per company, for the dashboard. */
    @Query("select j.company.name, count(j) from Job j group by j.company.name order by count(j) desc")
    List<Object[]> countByCompany(Pageable pageable);
}

