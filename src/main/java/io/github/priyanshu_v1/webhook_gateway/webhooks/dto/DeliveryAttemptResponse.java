package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import java.time.Instant;
import java.util.Map;

public record DeliveryAttemptResponse(
    int attempt,
    Integer statusCode,
    long executionTimeMs,
    String errorMessage,
    Instant attemptedAt,
    Map<String, String> headers
) {}