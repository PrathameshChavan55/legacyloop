package com.legacyloop.user.repository;

import com.legacyloop.user.entity.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findTop10ByNameContainingIgnoreCaseOrderByUsageCountDesc(String query);

    List<Skill> findByOrderByUsageCountDesc(Pageable pageable);
}
