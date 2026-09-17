package io.github.priyanshu_v1.webhook_gateway.api_keys;

import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyCreateRequest;
import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ApiKeyCreateRequest request) {
        ApiKeyResponse response = apiKeyService.createApiKey(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}