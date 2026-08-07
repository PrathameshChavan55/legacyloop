package com.legacyloop.user.repository;

import com.legacyloop.user.entity.Institution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByCode(String code);

    boolean existsByCode(String code);

    List<Institution> findByActiveTrueOrderByNameAsc();

    @Query("""
            select i from Institution i
            where (:query is null or lower(i.name) like :query or lower(i.code) like :query)
              and (:active is null or i.active = :active)
            """)
    Page<Institution> search(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);
}
