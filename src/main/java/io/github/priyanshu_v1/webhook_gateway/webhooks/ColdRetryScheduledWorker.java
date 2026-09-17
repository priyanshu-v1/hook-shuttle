package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.priyanshu_v1.webhook_gateway.endpoints.Endpoint;
import io.github.priyanshu_v1.webhook_gateway.repository.DeliveryAttemptRepository;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class ColdRetryScheduledWorker {

    private static final Logger log = LoggerFactory.getLogger(ColdRetryScheduledWorker.class);

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final AsyncRabbitPublisher asyncRabbitPublisher;

    public ColdRetryScheduledWorker(
            WebhookEventRepository eventRepository,
            DeliveryAttemptRepository attemptRepository,
            AsyncRabbitPublisher asyncRabbitPublisher
    ) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.asyncRabbitPublisher = asyncRabbitPublisher;
    }

    @Scheduled(cron = "0/30 * * * * *")
    @SchedulerLock(
        name = "ColdRetryScheduledWorker_pollColdRetries", 
        lockAtMostFor = "PT2M", 
        lockAtLeastFor = "PT5S"
    )
    public void pollColdRetries() {
        Instant now = Instant.now();

        List<WebhookEvent> pendingColdRetries = eventRepository
                .findByStatusAndNextRetryAtLessThanEqualWithDetails("COLD_RETRY_SCHEDULED", now);

        if (pendingColdRetries.isEmpty()) {
            return;
        }

        log.info("Found {} cold retries ready for re-dispatch.", pendingColdRetries.size());

        for (WebhookEvent event : pendingColdRetries) {
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

                WebhookDispatchEvent dispatchEvent = new WebhookDispatchEvent(
                        event.getId(),
                        endpoint.getId(),
                        event.getUser().getId(),
                        event.getEventType(),
                        endpoint.getTargetUrl(),
                        endpoint.getSecretKey(),
                        event.getRawPayload().toString(),
                        currentAttempt,
                        endpoint.getMaxRetries()
                );

                // Asynchronous fire-and-forget RabbitMQ publish
                asyncRabbitPublisher.publishEventAsync(dispatchEvent);

                event.setStatus("DISPATCHING");
                eventRepository.save(event);

                log.info("Cold retry event {} successfully pushed to RabbitMQ queue.", event.getId());

            } catch (Exception ex) {
                log.error("Failed to re-queue cold retry event {}: {}", event.getId(), ex.getMessage(), ex);
            }
        }
    }
}