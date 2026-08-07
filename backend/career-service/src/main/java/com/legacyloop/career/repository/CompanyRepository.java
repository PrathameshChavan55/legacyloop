package com.legacyloop.career.repository;

import com.legacyloop.career.entity.Company;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Company> findByActiveTrueOrderByNameAsc();

    @Query("""
            select c from Company c
            where (:query is null or lower(c.name) like :query or lower(c.industry) like :query)
              and (:verified is null or c.verified = :verified)
              and (:active is null or c.active = :active)
            """)
    Page<Company> search(@Param("query") String query,
                         @Param("verified") Boolean verified,
                         @Param("active") Boolean active,
                         Pageable pageable);
}

