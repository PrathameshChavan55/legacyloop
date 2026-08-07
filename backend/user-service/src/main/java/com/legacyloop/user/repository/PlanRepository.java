package com.legacyloop.user.repository;

import com.legacyloop.user.entity.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActiveTrueOrderByDisplayOrderAscAmountPaiseAsc();

    boolean existsByCode(String code);
}
