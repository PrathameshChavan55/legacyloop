package com.legacyloop.user.service;

import com.legacyloop.user.repository.OtpTokenRepository;
import com.legacyloop.user.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One nightly job for all the housekeeping the original spread over two schedulers. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {

    private final OtpTokenRepository otpTokens;
    private final RefreshTokenRepository refreshTokens;
    private final BillingService billing;

    @Scheduled(cron = "${legacyloop.maintenance-cron:0 30 2 * * *}")
    @Transactional
    public void nightly() {
        int codes = otpTokens.deleteExpired(Instant.now());
        int tokens = refreshTokens.deleteExpired(Instant.now());
        int lapsed = billing.expireLapsedMemberships();
        log.info("Nightly clean-up: {} codes, {} refresh tokens, {} lapsed memberships", codes, tokens, lapsed);
    }
}
