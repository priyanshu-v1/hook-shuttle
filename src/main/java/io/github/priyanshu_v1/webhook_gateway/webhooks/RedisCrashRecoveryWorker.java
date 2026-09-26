package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.priyanshu_v1.webhook_gateway.endpoints.Endpoint;
import io.github.priyanshu_v1.webhook_gateway.repository.DeliveryAttemptRepository;
import io.github.priyanshu_v1.webhook_gateway.security.EncryptionService;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class RedisCrashRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(RedisCrashRecoveryWorker.class);

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final RedissonRetryQueueService retryQueueService;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;
    private final TransactionTemplate transactionTemplate;

    public RedisCrashRecoveryWorker(
            WebhookEventRepository eventRepository,
            DeliveryAttemptRepository attemptRepository,
            RedissonRetryQueueService retryQueueService,
            ObjectMapper objectMapper,
            EncryptionService encryptionService,
            TransactionTemplate transactionTemplate
    ) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.retryQueueService = retryQueueService;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(
        name = "RedisRecoverySweeper_sweepOrphanedRetries", 
        lockAtMostFor = "PT10M", // Generous headroom for massive backlogs
        lockAtLeastFor = "PT5S"
    )
    public void sweepOrphanedRetries() {
        int batchSize = 50;
        Page<WebhookEvent> batch;

        do {
            Instant now = Instant.now();
            Pageable pageable = PageRequest.of(0, batchSize);

            // Process each page in its own short-lived transaction
            batch = transactionTemplate.execute(status -> {
                Page<WebhookEvent> currentBatch = eventRepository
                        .findByStatusAndNextRetryAtLessThanEqualWithDetails("IN_REDIS_RETRY", now, pageable);

                if (currentBatch.isEmpty()) {
                    return currentBatch;
                }

                log.info("Processing orphaned Redis retry batch of size: {}", currentBatch.getNumberOfElements());

                for (WebhookEvent event : currentBatch) {
                    try {
                        Endpoint endpoint = event.getEndpoint();
                        int currentAttempt = attemptRepository.countByEventId(event.getId()) + 1;
                        
                        // Guardrail: Terminal check if max retries were lowered or exhausted
                        if (currentAttempt > endpoint.getMaxRetries()) {
                            event.setStatus("FAILED");
                            event.setNextRetryAt(null);
                            eventRepository.save(event);
                            log.warn("Recovered event {} exceeded max retries ({}). Marked as FAILED.", 
                                    event.getId(), endpoint.getMaxRetries());
                            continue;
                        }
                        
                        // Serialize JsonNode to string
                        String payloadString;
                        try {
                            payloadString = objectMapper.writeValueAsString(event.getRawPayload());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to serialize webhook payload", e);
                        }

                        // Encrypt for in-flight safety
                        String encryptedPayload = encryptionService.encrypt(payloadString);

                        WebhookDispatchEvent dispatchEvent = new WebhookDispatchEvent(
                                event.getId(),
                                endpoint.getId(),
                                event.getUser().getId(),
                                event.getEventType(),
                                endpoint.getTargetUrl(),
                                endpoint.getSecretKey(),
                                encryptedPayload,
                                currentAttempt,
                                endpoint.getMaxRetries(),
                                "REDIS_RETRY"
                        );

                        // Push back to Redisson with 0 delay for immediate recovery
                        retryQueueService.scheduleRetry(dispatchEvent, 0, TimeUnit.SECONDS);

                        // Transition status to prevent infinite re-processing loops
                        event.setStatus("DISPATCHING");
                        eventRepository.save(event);

                    } catch (Exception ex) {
                        log.error("Failed to recover orphaned Redis retry event {}: {}", event.getId(), ex.getMessage(), ex);
                    }
                }

                return currentBatch;
            });

            if (batch == null || batch.isEmpty()) {
                break;
            }

        } while (batch.hasNext());
    }
}