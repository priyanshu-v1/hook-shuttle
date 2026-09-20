package io.github.priyanshu_v1.webhook_gateway.api_keys.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String keyName,
    String apiKey,      // Raw key - SHOWN ONLY ONCE!
    String keyPrefix,
    String status,
    Instant lastUsedAt,
    Instant createdAt
) {}