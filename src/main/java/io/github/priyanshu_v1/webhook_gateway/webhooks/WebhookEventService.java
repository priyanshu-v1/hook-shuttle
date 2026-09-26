package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.priyanshu_v1.webhook_gateway.entity.DeliveryAttempt;
import io.github.priyanshu_v1.webhook_gateway.repository.DeliveryAttemptRepository;
import io.github.priyanshu_v1.webhook_gateway.security.EncryptionService;
import io.github.priyanshu_v1.webhook_gateway.security.SignatureService;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.DeliveryAttemptResponse;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventResponse;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventSummaryProjection;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final SignatureService signatureService;
    private final EncryptionService encryptionService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookEventService(
            WebhookEventRepository webhookEventRepository,
            DeliveryAttemptRepository deliveryAttemptRepository,
            DeliveryAttemptRepository attemptRepository,
            SignatureService signatureService,
            EncryptionService encryptionService,
            WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.attemptRepository = attemptRepository;
        this.signatureService = signatureService;
        this.encryptionService = encryptionService;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Page<WebhookEventResponse> getEventsForUser(UUID userId, String status, Pageable pageable) {
    	
    	Page<WebhookEventSummaryProjection> summaries = webhookEventRepository.findEventSummariesByUserIdAndOptionalStatus(userId, status, pageable);
        
        return summaries.map(summary -> new WebhookEventResponse(
                summary.id(),
                summary.eventType(),
                summary.endpointId(),
                summary.targetUrl(),
                summary.status(),
                summary.createdAt(),
                summary.latestLatency(),
                objectMap(summary.rawPayload()),
                summary.attemptsCount() != null ? summary.attemptsCount().intValue() : 0
        ));
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
                attempt.getResponseHeaders() != null ? convertHeaders(attempt.getResponseHeaders()) : Map.of(),
                attempt.getTriggerType()
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
    
    
    @Transactional
    public void replayEvent(UUID userId, UUID eventId) {
        WebhookEvent webhookEvent = webhookEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook event not found"));

        int nextAttemptNumber = deliveryAttemptRepository.countByEventId(eventId) + 1;
        
        String targetUrl = webhookEvent.getEndpoint().getTargetUrl();
        String decryptedSecret = encryptionService.decrypt(webhookEvent.getEndpoint().getSecretKey());
        
        String payloadString;
        try {
            payloadString = objectMapper.writeValueAsString(webhookEvent.getRawPayload());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize webhook payload", e);
        }
        
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signatureData = timestamp + "." + payloadString;
        String hmacSignature = signatureService.calculateHmacSha256(signatureData, decryptedSecret);

        long startTime = System.currentTimeMillis();

        try {
            webClient.post()
                    .uri(targetUrl)
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", webhookEvent.getEventType())
                    .header("X-Webhook-Signature", hmacSignature)
                    .header("X-Webhook-Timestamp", timestamp)
                    .header("X-Manual-Replay", "true")
                    .bodyValue(payloadString)
                    .exchangeToMono(response -> {
                        long latency = System.currentTimeMillis() - startTime;
                        int statusCode = response.statusCode().value();

                        Map<String, String> responseHeaders = new HashMap<>();
                        response.headers().asHttpHeaders().forEach((key, values) -> {
                            if (values != null && !values.isEmpty()) {
                                responseHeaders.put(key, String.join(", ", values));
                            }
                        });

                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(responseBody -> {
                                    recordAttemptAndPersistStatus(webhookEvent, nextAttemptNumber, statusCode, latency, responseBody, null, responseHeaders, "MANUAL_REPLAY");
                                    return reactor.core.publisher.Mono.empty();
                                });
                    })
                    .block();

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            recordAttemptAndPersistStatus(webhookEvent, nextAttemptNumber, 500, latency, "Manual Replay Failed: " + ex.getMessage(), ex.getMessage(), Map.of(), "MANUAL_REPLAY");
        }
    }

    private void recordAttemptAndPersistStatus(WebhookEvent webhookEvent, int attemptNumber, int statusCode, long latency, String responseBody, String errorMessage, Map<String, String> responseHeaders, String triggerType) {
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setEvent(webhookEvent);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setResponseStatusCode(statusCode);
        attempt.setExecutionTimeMs(latency);
        attempt.setResponseBody(responseBody);
        attempt.setErrorMessage(errorMessage);
        attempt.setScheduledAt(Instant.now());
        attempt.setTriggerType(triggerType);

        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            attempt.setResponseHeaders(objectMapper.valueToTree(responseHeaders));
        }
        attemptRepository.save(attempt);

        // Update main event status based on manual execution result
        if (statusCode >= 200 && statusCode < 300) {
            webhookEvent.setStatus("SUCCESS");
            webhookEvent.setNextRetryAt(null);
        } else {
            webhookEvent.setStatus("FAILED");
        }
        webhookEventRepository.save(webhookEvent);
    }
}