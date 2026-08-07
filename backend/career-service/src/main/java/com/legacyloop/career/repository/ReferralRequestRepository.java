package com.legacyloop.career.repository;

import com.legacyloop.career.entity.Enums.ReferralStatus;
import com.legacyloop.career.entity.ReferralRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, Long> {

    boolean existsByApplicationIdAndReferrerUserId(Long applicationId, Long referrerUserId);

    Page<ReferralRequest> findByReferrerUserIdOrderByIdDesc(Long referrerUserId, Pageable pageable);

    Page<ReferralRequest> findByRequesterUserIdOrderByIdDesc(Long requesterUserId, Pageable pageable);

    long countByStatus(ReferralStatus status);
}

