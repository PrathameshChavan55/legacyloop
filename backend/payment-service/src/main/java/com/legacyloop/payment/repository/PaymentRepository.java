package com.legacyloop.payment.repository;

import com.legacyloop.common.enums.PaymentStatus;
import com.legacyloop.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "plan")
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    @EntityGraph(attributePaths = "plan")
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndStatus(Long userId, PaymentStatus status);

    long countByStatus(PaymentStatus status);
}

