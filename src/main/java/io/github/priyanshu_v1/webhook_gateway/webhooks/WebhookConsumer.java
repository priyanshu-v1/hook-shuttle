package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.priyanshu_v1.webhook_gateway.config.RabbitMQConfig;
import io.github.priyanshu_v1.webhook_gateway.entity.DeliveryAttempt;
import io.github.priyanshu_v1.webhook_gateway.repository.DeliveryAttemptRepository;
import io.github.priyanshu_v1.webhook_gateway.security.EncryptionService;
import io.github.priyanshu_v1.webhook_gateway.security.SignatureService;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;

@Service
public class WebhookConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookConsumer.class);
    private static final long REDIS_DELAY_CAP_SECONDS = 3600; // 1 Hour

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final RedissonRetryQueueService retryQueueService;
    private final SignatureService signatureService;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public WebhookConsumer(WebhookEventRepository eventRepository,
                           DeliveryAttemptRepository attemptRepository,
                           RedissonRetryQueueService retryQueueService,
                           SignatureService signatureService,
                           EncryptionService encryptionService,
                           ObjectMapper objectMapper,
                           WebClient webClient) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.retryQueueService = retryQueueService;
        this.signatureService = signatureService;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeEvent(WebhookDispatchEvent event) {
        long startTime = System.currentTimeMillis();

        try {
        	
        	// Decrypt payload and secret key right before processing
            String decryptedSecret = encryptionService.decrypt(event.secretKey());
            String decryptedPayload = encryptionService.decrypt(event.payload());
        	
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signatureData = timestamp + "." + decryptedPayload;
            String hmacSignature = signatureService.calculateHmacSha256(signatureData, decryptedSecret);

            webClient.post()
                    .uri(event.targetUrl())
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", event.eventType())
                    .header("X-Webhook-Signature", hmacSignature)
                    .header("X-Webhook-Timestamp", timestamp)
                    .bodyValue(decryptedPayload)
                    .exchangeToMono(response -> {
                        long latency = System.currentTimeMillis() - startTime;
                        int statusCode = response.statusCode().value();

                        Map<String, String> responseHeaders = new HashMap<>();
                        response.headers().asHttpHeaders().forEach((key, values) -> {
                            if (values != null && !values.isEmpty()) {
                                responseHeaders.put(key, String.join(", ", values));
                            }
                        });
                        
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(responseBody -> {
                                    handleDispatchResult(event, statusCode, latency, responseBody, null, responseHeaders);
                                    return reactor.core.publisher.Mono.empty();
                                });
                    })
                    .block();

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            handleDispatchResult(event, 500, latency, "Dispatch Failed: " + ex.getMessage(), ex.getMessage(), Map.of());
        }
    }

    private void handleDispatchResult(WebhookDispatchEvent event, int statusCode, long latency, String responseBody, String errorMessage, Map<String, String> responseHeaders) {
        WebhookEvent webhookEvent = eventRepository.findById(event.eventId())
                .orElseThrow(() -> new IllegalArgumentException("WebhookEvent not found: " + event.eventId()));

        // 1. Log this delivery attempt
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setEvent(webhookEvent);
        attempt.setAttemptNumber(event.attemptNumber());
        attempt.setResponseStatusCode(statusCode);
        attempt.setExecutionTimeMs(latency);
        attempt.setResponseBody(responseBody);
        attempt.setErrorMessage(errorMessage);
        attempt.setScheduledAt(Instant.now());
        
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            attempt.setResponseHeaders(objectMapper.valueToTree(responseHeaders));
        }
        
        attemptRepository.save(attempt);

        // 2. Success Case (2xx)
        if (statusCode >= 200 && statusCode < 300) {
            webhookEvent.setStatus("SUCCESS");
            webhookEvent.setNextRetryAt(null);
            eventRepository.save(webhookEvent);
            log.info("Webhook event {} delivered successfully on attempt {}.", event.eventId(), event.attemptNumber());
            return;
        }

        // 3. Failure Case -> Evaluate Retries
        if (event.hasRetriesRemaining()) {
            WebhookDispatchEvent nextEvent = event.nextAttempt();
            long delaySeconds = nextEvent.calculateBackoffDelaySeconds();
            Instant nextRetryTime = Instant.now().plusSeconds(delaySeconds);

            webhookEvent.setNextRetryAt(nextRetryTime);

            if (delaySeconds <= REDIS_DELAY_CAP_SECONDS) {
                // In-Cap Retry: Route to Redisson RDelayedQueue
                webhookEvent.setStatus("IN_REDIS_RETRY");
                eventRepository.save(webhookEvent);
                retryQueueService.scheduleRetry(nextEvent, delaySeconds, TimeUnit.SECONDS);
                log.info("Event {} scheduled in Redisson for retry in {}s (Attempt {})",
                        event.eventId(), delaySeconds, nextEvent.attemptNumber());
            } else {
                // Off-Cap Retry: Route to PostgreSQL for cold storage
                webhookEvent.setStatus("COLD_RETRY_SCHEDULED");
                eventRepository.save(webhookEvent);
                log.info("Event {} delay ({}s) exceeds cap. Scheduled in DB for {}",
                        event.eventId(), delaySeconds, nextRetryTime);
            }
        } else {
            // Retries Exhausted
            webhookEvent.setStatus("FAILED");
            webhookEvent.setNextRetryAt(null);
            eventRepository.save(webhookEvent);
            log.warn("Webhook event {} exhausted all {} max retries. Marked as FAILED.",
                    event.eventId(), event.maxAttempts());
        }
    }
}