package io.github.priyanshu_v1.webhook_gateway.api_keys;

import java.security.SecureRandom;
import java.util.Base64;

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

        // Generate a secure raw key
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawKey = "wh_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Extract prefix for safe identification later (e.g., first 16 chars)
        String prefix = rawKey.substring(0, Math.min(rawKey.length(), 16));

        // Hash the full key for database storage
        String hashedKey = passwordEncoder.encode(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyName(request.keyName());
        apiKey.setApiKeyHash(hashedKey);
        apiKey.setKeyPrefix(prefix);
        apiKey.setStatus("ACTIVE");

        ApiKey saved = apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
                saved.getId(),
                saved.getKeyName(),
                rawKey, // Return raw key only this single time
                saved.getKeyPrefix(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }
}