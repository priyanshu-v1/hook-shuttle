package io.github.priyanshu_v1.webhook_gateway.auth;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class AuthTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Runs daily at 3:00 AM.
     * Uses ShedLock to prevent duplicate cleanups in multi-instance production environments.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(
        name = "AuthTokenCleanupScheduler_purgeRevokedAndExpiredTokens",
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT5S"
    )
    @Transactional
    public void purgeRevokedAndExpiredTokens() {
        log.info("Starting scheduled cleanup of revoked and expired auth tokens...");

        try {
            Instant now = Instant.now();
            long deletedCount = refreshTokenRepository.deleteByAbsoluteExpiresAtBeforeOrRevokedTrue(now);
            
            log.info("Token cleanup successfully completed. Purged {} expired or revoked tokens.", deletedCount);
        } catch (Exception ex) {
            log.error("Failed to execute scheduled auth token cleanup: {}", ex.getMessage(), ex);
        }
    }
}