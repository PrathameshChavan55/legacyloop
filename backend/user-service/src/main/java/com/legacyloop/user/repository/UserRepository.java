package com.legacyloop.user.repository;

import com.legacyloop.user.entity.User;
import com.legacyloop.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByInstitutionIdAndStudentIdentifier(Long institutionId, String studentIdentifier);

    List<User> findByIdIn(List<Long> ids);

    /**
     * The admin user search.
     *
     * <p>The original built this with a JPA Specification class and a criteria DTO. One query with
     * null-tolerant parameters does the same job: each condition is skipped when its parameter is
     * null, which is exactly what the Specification's if-chain was doing.
     */
    @Query("""
            select distinct u from User u left join u.roles r
            where (:query is null or lower(u.email) like :query or lower(u.firstName) like :query
                   or lower(u.lastName) like :query or lower(u.studentIdentifier) like :query)
              and (:status is null or u.status = :status)
              and (:role is null or r = :role)
              and (:institutionId is null or u.institutionId = :institutionId)
            """)
    Page<User> search(@Param("query") String query,
                      @Param("status") UserStatus status,
                      @Param("role") String role,
                      @Param("institutionId") Long institutionId,
                      Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("select count(u) from User u join u.roles r where r = :role")
    long countByRole(@Param("role") String role);

    @Query("select count(u) from User u where u.premium = true and u.premiumUntil > current_timestamp")
    long countPremium();

    /** Nightly clean-up: memberships that quietly lapsed. */
    @Query("select u from User u where u.premium = true and u.premiumUntil < current_timestamp")
    List<User> findLapsedPremium();
}
