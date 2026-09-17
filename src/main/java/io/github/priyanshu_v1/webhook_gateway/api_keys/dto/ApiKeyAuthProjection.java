package io.github.priyanshu_v1.webhook_gateway.api_keys.dto;

import java.util.UUID;

public record ApiKeyAuthProjection(
        String apiKeyHash,
        String userEmail,
        UUID userId
) {}