package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.priyanshu_v1.webhook_gateway.entity.DeliveryAttempt;
import io.github.priyanshu_v1.webhook_gateway.repository.DeliveryAttemptRepository;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.DeliveryAttemptResponse;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventResponse;

@Service
@Transactional(readOnly = true)
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    public WebhookEventService(
            WebhookEventRepository webhookEventRepository,
            DeliveryAttemptRepository deliveryAttemptRepository,
            ObjectMapper objectMapper
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.objectMapper = objectMapper;
    }

    public List<WebhookEventResponse> getEventsForUser(UUID userId) {
        List<WebhookEvent> events = webhookEventRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return events.stream().map(event -> {
            int attemptsCount = deliveryAttemptRepository.countByEventId(event.getId());
            Long latestLatency = deliveryAttemptRepository.findTopByEventIdOrderByAttemptNumberDesc(event.getId())
                    .map(DeliveryAttempt::getExecutionTimeMs)
                    .orElse(null);

            return new WebhookEventResponse(
                    event.getId(),
                    event.getEventType(),
                    event.getEndpoint().getId(),
                    event.getEndpoint().getTargetUrl(),
                    event.getStatus(),
                    event.getCreatedAt(),
                    latestLatency,
                    objectMap(event.getRawPayload()),
                    attemptsCount
            );
        }).collect(Collectors.toList());
    }

    public List<DeliveryAttemptResponse> getAttemptsForEvent(UUID userId, UUID eventId) {
        WebhookEvent event = webhookEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook event not found or unauthorized"));

        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByEventIdOrderByAttemptNumberAsc(event.getId());

        return attempts.stream().map(attempt -> new DeliveryAttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getResponseStatusCode(),
                attempt.getExecutionTimeMs() != null ? attempt.getExecutionTimeMs() : 0L,
                attempt.getErrorMessage(),
                attempt.getAttemptedAt(),
                attempt.getResponseHeaders() != null ? convertHeaders(attempt.getResponseHeaders()) : Map.of()
        )).collect(Collectors.toList());
    }

 
    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(JsonNode node) {
        if (node == null) return Map.of();
        return objectMapper.convertValue(node, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> convertHeaders(JsonNode node) {
        if (node == null) return Map.of();
        return objectMapper.convertValue(node, Map.class);
    }
}