package io.github.priyanshu_v1.webhook_gateway.endpoints.dto;

import java.time.Instant;
import java.util.UUID;

public record EndpointResponse(
        UUID id,
        String targetUrl,
        String description,
        String secretKey,
        String status,
        Integer rateLimitPerSec,
        Integer timeoutMs,
        Integer maxRetries,
        Instant createdAt,
        Instant updatedAt
) {}