package io.github.priyanshu_v1.webhook_gateway.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String role,
    Instant createdAt
) {}