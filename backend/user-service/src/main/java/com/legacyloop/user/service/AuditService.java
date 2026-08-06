package com.legacyloop.user.service;

import com.legacyloop.user.entity.AuditLog;
import com.legacyloop.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes audit rows. Kept separate only because four services write to it. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogs;

    /**
     * Runs in its own transaction: a failed sign-in must still be recorded even though the
     * surrounding call ends in an exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String email, AuditLog.Action action, String detail, String ipAddress,
                       Long actorId) {
        auditLogs.save(AuditLog.builder()
                .userId(userId)
                .email(email)
                .action(action)
                .detail(detail)
                .ipAddress(ipAddress)
                .actorId(actorId)
                .build());
    }

    public void record(Long userId, String email, AuditLog.Action action, String detail) {
        record(userId, email, action, detail, null, null);
    }
}
