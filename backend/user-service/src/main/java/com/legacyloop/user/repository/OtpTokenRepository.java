package com.legacyloop.user.repository;

import com.legacyloop.user.entity.OtpToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findFirstByEmailAndPurposeAndUsedFalseOrderByIdDesc(String email, OtpToken.Purpose purpose);

    List<OtpToken> findByEmailAndPurposeAndUsedFalse(String email, OtpToken.Purpose purpose);

    /** Password-reset links carry the token itself, so they are found by hash rather than by email. */
    Optional<OtpToken> findFirstByCodeHashAndPurposeAndUsedFalse(String codeHash, OtpToken.Purpose purpose);

    @Modifying
    @Query("delete from OtpToken o where o.expiresAt < :cutoff")
    int deleteExpired(Instant cutoff);
}
