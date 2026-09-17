package io.github.priyanshu_v1.webhook_gateway.webhooks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record WebhookDispatchRequest(
    @NotNull(message = "endpointId is required")
    UUID endpointId,

    @NotBlank(message = "eventType is required")
    String eventType,

    @NotNull(message = "payload is required")
    Map<String, Object> payload
) {}