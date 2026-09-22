package io.github.priyanshu_v1.webhook_gateway.auth.dto;

import java.time.Duration;

public record AuthResult(
        AuthResponse authResponse,
        String rawRefreshToken,
        Duration cookieMaxAge
) {}