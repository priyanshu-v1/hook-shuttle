package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record WebhookDispatchEvent(
    UUID eventId,
    UUID endpointId,
    UUID userId,
    String eventType,
    String targetUrl,
    String secretKey,
    String payload,
    int attemptNumber,
    int maxAttempts,
    String triggerType
) implements Serializable {

    /**
     * Factory method for initial ingestion (Starts at attempt #1)
     */
    public static WebhookDispatchEvent initial(
            UUID eventId, 
            UUID endpointId, 
            UUID userId,
            String eventType,
            String targetUrl, 
            String secretKey, 
            String payload, 
            int maxAttempts
    ) {
        return new WebhookDispatchEvent(
            eventId, endpointId, userId, eventType, targetUrl, secretKey, payload, 1, maxAttempts, "INITIAL"
        );
    }

    public WebhookDispatchEvent withTriggerType(String newTriggerType) {
        return new WebhookDispatchEvent(
            eventId, endpointId, userId, eventType, targetUrl, secretKey, payload, attemptNumber, maxAttempts, newTriggerType
        );
    }
    
    /**
     * Helper to advance attempt counter for subsequent retries
     */
    public WebhookDispatchEvent nextAttempt() {
        return new WebhookDispatchEvent(
            eventId, endpointId, userId, eventType, targetUrl, secretKey, payload, attemptNumber + 1, maxAttempts, triggerType
        );
    }

    /**
     * Calculates exponential backoff delay in seconds (base^attemptNumber)
     */
    public long calculateBackoffDelaySeconds(double backoffBase, int jitterSeconds) {
    	long exponentialDelay = (long) Math.pow(backoffBase, attemptNumber);
        // Add a random jitter between 0 and jitterSeconds
        int jitter = jitterSeconds > 0 ? ThreadLocalRandom.current().nextInt(jitterSeconds + 1) : 0;
        return exponentialDelay + jitter;
    }

    /**
     * Checks if more retry attempts remain
     */
    public boolean hasRetriesRemaining() {
        return attemptNumber < maxAttempts;
    }
}