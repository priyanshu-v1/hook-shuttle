package io.github.priyanshu_v1.webhook_gateway.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenValidationException extends RuntimeException {
    public TokenValidationException(String message) {
        super(message);
    }
}