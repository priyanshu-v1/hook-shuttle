package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.DeliveryAttemptResponse;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventResponse;

@RestController
@RequestMapping("/api/v1/events")
public class WebhookEventController {

    private final WebhookEventService webhookEventService;

    public WebhookEventController(WebhookEventService webhookEventService) {
        this.webhookEventService = webhookEventService;
    }

    @GetMapping
    public ResponseEntity<List<WebhookEventResponse>> listEvents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<WebhookEventResponse> events = webhookEventService.getEventsForUser(principal.userId());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}/attempts")
    public ResponseEntity<List<DeliveryAttemptResponse>> listEventAttempts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID eventId
    ) {
        List<DeliveryAttemptResponse> attempts = webhookEventService.getAttemptsForEvent(principal.userId(), eventId);
        return ResponseEntity.ok(attempts);
    }
}