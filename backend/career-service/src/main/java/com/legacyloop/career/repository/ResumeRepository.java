package com.legacyloop.career.repository;

import com.legacyloop.career.entity.Resume;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByOwnerUserIdOrderByIdDesc(Long ownerUserId);

    Optional<Resume> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Optional<Resume> findFirstByOwnerUserIdAndPrimaryTrue(Long ownerUserId);

    long countByOwnerUserId(Long ownerUserId);
}
