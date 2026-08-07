package com.legacyloop.career.repository;

import com.legacyloop.career.entity.ResumeAnalysis;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    Optional<ResumeAnalysis> findByIdAndUserId(Long id, Long userId);

    Optional<ResumeAnalysis> findFirstByResumeIdAndUserIdOrderByIdDesc(Long resumeId, Long userId);

    Page<ResumeAnalysis> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}

