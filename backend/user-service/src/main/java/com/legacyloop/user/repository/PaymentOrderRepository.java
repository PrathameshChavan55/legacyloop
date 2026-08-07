package com.legacyloop.user.repository;

import com.legacyloop.user.entity.PaymentOrder;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);

    Page<PaymentOrder> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
