package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.priyanshu_v1.webhook_gateway.endpoints.Endpoint;
import io.github.priyanshu_v1.webhook_gateway.endpoints.EndpointRepository;
import io.github.priyanshu_v1.webhook_gateway.security.EncryptionService;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchResponse;

@Service
public class WebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final EndpointRepository endpointRepository;
    private final AsyncRabbitPublisher asyncRabbitPublisher;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;

    public WebhookService(
            WebhookEventRepository webhookEventRepository,
            EndpointRepository endpointRepository,
            AsyncRabbitPublisher asyncRabbitPublisher,
            ObjectMapper objectMapper,
            EncryptionService encryptionService
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.endpointRepository = endpointRepository;
        this.asyncRabbitPublisher = asyncRabbitPublisher;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
    }

    public WebhookDispatchResponse dispatchWebhook(UUID userId, UUID endpointId, String eventType, String rawPayload) {
        // 1. Fetch endpoint
        Endpoint endpoint = endpointRepository.findByIdAndUser_Id(endpointId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found or unauthorized"));

        // 2. Parse payload
        JsonNode jsonPayload;
        try {
            jsonPayload = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload format", e);
        }

        // 3. Save event to DB (plain JSONB for GIN indexing & dashboard)
        WebhookEvent savedEvent = saveWebhookEvent(endpoint, eventType, jsonPayload);

        // 4. Encrypt rawPayload for in-flight safety (secretKey is already encrypted from DB)
        String encryptedPayload = encryptionService.encrypt(rawPayload);

        // 5. Asynchronous fire-and-forget RabbitMQ publish carrying execution metadata
        asyncRabbitPublisher.publishEventAsync(WebhookDispatchEvent.initial(
                savedEvent.getId(),
                endpoint.getId(),
                userId,
                savedEvent.getEventType(),
                endpoint.getTargetUrl(),
                endpoint.getSecretKey(),
                encryptedPayload,
                endpoint.getMaxRetries()
        ));

        return new WebhookDispatchResponse(
                savedEvent.getId(),
                savedEvent.getStatus(),
                savedEvent.getCreatedAt()
        );
    }

    @Transactional
    protected WebhookEvent saveWebhookEvent(Endpoint endpoint, String eventType, JsonNode payload) {
        WebhookEvent event = WebhookEvent.builder()
                .user(endpoint.getUser())
                .endpoint(endpoint)
                .eventType(eventType)
                .rawPayload(payload)
                .status("PENDING")
                .build();

        return webhookEventRepository.save(event);
    }
}