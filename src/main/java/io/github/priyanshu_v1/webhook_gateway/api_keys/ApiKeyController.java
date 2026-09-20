package io.github.priyanshu_v1.webhook_gateway.api_keys;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyCreateRequest;
import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApiKeyCreateRequest request) {
        ApiKeyResponse response = apiKeyService.createApiKey(principal.email(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ApiKeyResponse> keys = apiKeyService.getApiKeysByUser(principal.userId());
        return ResponseEntity.ok(keys);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        apiKeyService.revokeApiKey(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}