package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookDispatchResponse(
    UUID eventId,
    String status,
    Instant createdAt
) {}