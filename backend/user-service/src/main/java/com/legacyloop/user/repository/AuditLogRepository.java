package com.legacyloop.user.repository;

import com.legacyloop.user.entity.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** Backs the "login history" screen, which is the audit log filtered to the sign-in actions. */
    Page<AuditLog> findByUserIdAndActionInOrderByIdDesc(Long userId, List<AuditLog.Action> actions,
                                                        Pageable pageable);

    Page<AuditLog> findAllByOrderByIdDesc(Pageable pageable);
}
