package io.github.priyanshu_v1.webhook_gateway.api_keys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyCreateRequest(
    @NotBlank(message = "Key name is required")
    @Size(max = 100, message = "Key name must be at most 100 characters")
    String keyName
) {}