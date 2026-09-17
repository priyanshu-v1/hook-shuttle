package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import java.io.Serializable;
import java.util.UUID;

public record WebhookDispatchEvent(
    UUID eventId,
    UUID endpointId,
    UUID userId,
    String eventType,
    String targetUrl,
    String secretKey,
    String payload,
    int attemptNumber,
    int maxAttempts
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
            eventId, endpointId, userId, eventType, targetUrl, secretKey, payload, 1, maxAttempts
        );
    }

    /**
     * Helper to advance attempt counter for subsequent retries
     */
    public WebhookDispatchEvent nextAttempt() {
        return new WebhookDispatchEvent(
            eventId, endpointId, userId, eventType, targetUrl, secretKey, payload, attemptNumber + 1, maxAttempts
        );
    }

    /**
     * Calculates exponential backoff delay in seconds (2^attemptNumber)
     */
    public long calculateBackoffDelaySeconds() {
        return (long) Math.pow(2, attemptNumber);
    }

    /**
     * Checks if more retry attempts remain
     */
    public boolean hasRetriesRemaining() {
        return attemptNumber < maxAttempts;
    }
}