package io.github.priyanshu_v1.webhook_gateway.endpoints.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record EndpointCreateRequest(
        @NotBlank(message = "Target URL is required")
        @URL(message = "Invalid URL format")
        String targetUrl,

        String description,
        Integer rateLimitPerSec,
        Integer timeoutMs,
        Integer maxRetries,
        Boolean active
) {}