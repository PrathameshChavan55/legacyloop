package com.legacyloop.user.repository;

import com.legacyloop.user.entity.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserIdAndStatusOrderByExpiresAtDesc(Long userId, Subscription.Status status);

    Page<Subscription> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    List<Subscription> findByStatusAndExpiresAtBefore(Subscription.Status status, Instant cutoff);
}
