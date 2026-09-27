package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Duration;
import java.time.Instant;

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
public class ColdRetryScheduledWorker {

    private static final Logger log = LoggerFactory.getLogger(ColdRetryScheduledWorker.class);

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final AsyncRabbitPublisher asyncRabbitPublisher;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;
    private final TransactionTemplate transactionTemplate;

    public ColdRetryScheduledWorker(
            WebhookEventRepository eventRepository,
            DeliveryAttemptRepository attemptRepository,
            ObjectMapper objectMapper,
            AsyncRabbitPublisher asyncRabbitPublisher,
            EncryptionService encryptionService,
            TransactionTemplate transactionTemplate
    ) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.asyncRabbitPublisher = asyncRabbitPublisher;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "0 0/15 * * * *")
    @SchedulerLock(
        name = "ColdRetryScheduledWorker_pollColdRetries", 
        lockAtMostFor = "PT14M", 
        lockAtLeastFor = "PT5S"
    )
    public void pollColdRetries() {
        int batchSize = 100;
        
        Instant startTime = Instant.now();
        Duration maxRuntime = Duration.ofMinutes(10);
        
        Page<WebhookEvent> batch;
        
        do {
        	// Safety check: Have we run out of our allocated time window?
            if (Duration.between(startTime, Instant.now()).compareTo(maxRuntime) > 0) {
                log.warn("Cold retry worker reached max runtime window (10 min). Yielding lock gracefully for the next tick.");
                break;
            }
            
            Instant now = Instant.now();
            Pageable pageable = PageRequest.of(0, batchSize);
            
            // Execute each batch in its own short-lived transaction
            batch = transactionTemplate.execute(_ -> {
                Page<WebhookEvent> currentBatch = eventRepository.findByStatusAndNextRetryAtLessThanEqualWithDetails(
                    "COLD_RETRY_SCHEDULED", 
                    now, 
                    pageable
                );

                if (currentBatch.isEmpty()) {
                    return currentBatch;
                }

                log.info("Processing cold retry batch of size: {}", currentBatch.getNumberOfElements());

                for (WebhookEvent event : currentBatch) {
                    try {
                        Endpoint endpoint = event.getEndpoint();
                        int currentAttempt = attemptRepository.countByEventId(event.getId()) + 1;
                        
                        // Guardrail: Terminal check if max retries were lowered or exhausted
                        if (currentAttempt > endpoint.getMaxRetries()) {
                            event.setStatus("FAILED");
                            event.setNextRetryAt(null);
                            eventRepository.save(event);
                            log.warn("Cold retry event {} exceeded max retries ({}). Marked as FAILED.", 
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
                                "COLD_RETRY"
                        );

                        // Asynchronous fire-and-forget RabbitMQ publish
                        asyncRabbitPublisher.publishEventAsync(dispatchEvent);

                        // Transition status so it drops out of the COLD_RETRY_SCHEDULED queue
                        event.setStatus("DISPATCHING");
                        eventRepository.save(event);

                    } catch (Exception ex) {
                        log.error("Failed to re-queue cold retry event {}: {}", event.getId(), ex.getMessage(), ex);
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