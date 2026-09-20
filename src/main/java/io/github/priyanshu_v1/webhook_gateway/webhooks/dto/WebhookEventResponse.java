package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WebhookEventResponse(
    UUID id,
    String eventType,
    UUID endpointId,
    String targetUrl,
    String status,
    Instant receivedAt,
    Long latencyMs,
    Map<String, Object> payload,
    int attemptsCount
) {}