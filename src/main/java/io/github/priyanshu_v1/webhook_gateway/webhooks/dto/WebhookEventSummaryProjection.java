package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record WebhookEventSummaryProjection(
    UUID id,
    String eventType,
    UUID endpointId,
    String targetUrl,
    String status,
    Instant createdAt,
    Long latestLatency,
    JsonNode rawPayload,
    Long attemptsCount
) {}