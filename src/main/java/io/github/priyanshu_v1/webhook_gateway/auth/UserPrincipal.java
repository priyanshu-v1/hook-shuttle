package io.github.priyanshu_v1.webhook_gateway.auth;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        String email
) {}