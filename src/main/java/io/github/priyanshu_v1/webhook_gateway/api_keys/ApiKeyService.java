package io.github.priyanshu_v1.webhook_gateway.api_keys;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyCreateRequest;
import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyResponse;
import io.github.priyanshu_v1.webhook_gateway.entity.User;
import io.github.priyanshu_v1.webhook_gateway.repository.UserRepository;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiKeyResponse createApiKey(String userEmail, ApiKeyCreateRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean isLive = request.live() == null || request.live();
        String prefix = isLive ? "hs_live_" : "hs_test_";

        // Generate a secure raw key
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawApiKey = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Extract prefix for safe identification later (e.g., first 16 chars)
        String keyPrefix = rawApiKey.substring(0, Math.min(rawApiKey.length(), 16));

        // Hash the full key for database storage
        String apiKeyHash = passwordEncoder.encode(rawApiKey);
        
        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .keyName(request.keyName())
                .apiKeyHash(apiKeyHash)
                .keyPrefix(keyPrefix)
                .isLive(isLive)
                .status("ACTIVE")
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
                saved.getId(),
                saved.getKeyName(),
                rawApiKey, // Return raw key only this single time
                saved.getKeyPrefix(),
                saved.getStatus(),
                saved.getLastUsedAt(),
                saved.getCreatedAt()
        );
    }
    
    
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeysByUser(UUID userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(apiKey -> new ApiKeyResponse(
                        apiKey.getId(),
                        apiKey.getKeyName(),
                        null, // apiKey (raw key) is never returned in lists!
                        apiKey.getKeyPrefix(),
                        apiKey.getStatus(),
                        apiKey.getLastUsedAt(),
                        apiKey.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void revokeApiKey(UUID id, UUID userId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found or unauthorized"));
        
        apiKey.setStatus("REVOKED");
        apiKeyRepository.save(apiKey);
    }
}