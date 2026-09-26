package io.github.priyanshu_v1.webhook_gateway.endpoints;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.priyanshu_v1.webhook_gateway.endpoints.dto.EndpointCreateRequest;
import io.github.priyanshu_v1.webhook_gateway.endpoints.dto.EndpointResponse;
import io.github.priyanshu_v1.webhook_gateway.entity.User;
import io.github.priyanshu_v1.webhook_gateway.repository.UserRepository;
import io.github.priyanshu_v1.webhook_gateway.security.EncryptionService;

@Service
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private static final SecureRandom secureRandom = new SecureRandom();

    public EndpointService(EndpointRepository endpointRepository, UserRepository userRepository, EncryptionService encryptionService) {
        this.endpointRepository = endpointRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public EndpointResponse createEndpoint(UUID userId, EndpointCreateRequest request) {
        // Fetch proxy reference for the user relationship
        User user = userRepository.getReferenceById(userId);

        // Generate secure secret key
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String rawSecretKey = "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        
        // 2. Encrypt secret key for DB storage
        String encryptedSecretKey = encryptionService.encrypt(rawSecretKey);

        // Apply defaults if optional fields are null
        Integer rateLimit = request.rateLimitPerSec() != null ? request.rateLimitPerSec() : 100;
        Integer timeout = request.timeoutMs() != null ? request.timeoutMs() : 5000;
        Integer retries = request.maxRetries() != null ? request.maxRetries() : 5;
        
        boolean isActive = request.active() == null || request.active();
        String initialStatus = isActive ? "ACTIVE" : "DISABLED";

        Endpoint endpoint = Endpoint.builder()
                .user(user)
                .targetUrl(request.targetUrl())
                .description(request.description())
                .secretKey(encryptedSecretKey)
                .status(initialStatus)
                .rateLimitPerSec(rateLimit)
                .timeoutMs(timeout)
                .maxRetries(retries)
                .build();

        Endpoint savedEndpoint = endpointRepository.save(endpoint);
        return mapToResponse(savedEndpoint, rawSecretKey);
    }

    @Transactional(readOnly = true)
    public Page<EndpointResponse> getEndpointsByUser(UUID userId, Pageable pageable) {
        return endpointRepository.findByUser_Id(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void deleteEndpoint(UUID id, UUID userId) {
        Endpoint endpoint = endpointRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found or unauthorized"));
        endpointRepository.delete(endpoint);
    }

    // 1. Single-parameter method for streams (lists)
    private EndpointResponse mapToResponse(Endpoint endpoint) {
        return mapToResponse(endpoint, endpoint.getSecretKey());
    }
    
    // 2. Two-parameter method for explicit overrides (creation)
    private EndpointResponse mapToResponse(Endpoint endpoint, String secretKeyToReturn) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getTargetUrl(),
                endpoint.getDescription(),
                secretKeyToReturn,
                endpoint.getStatus(),
                endpoint.getRateLimitPerSec(),
                endpoint.getTimeoutMs(),
                endpoint.getMaxRetries(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }
    
    @Transactional
    public void toggleEndpointStatus(UUID id, UUID userId) {
        Endpoint endpoint = endpointRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found or unauthorized"));
        
        String newStatus = "ACTIVE".equals(endpoint.getStatus()) ? "DISABLED" : "ACTIVE";
        endpoint.setStatus(newStatus);
        
        endpointRepository.save(endpoint);
    }
}