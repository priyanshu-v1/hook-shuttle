package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<Page<WebhookEventResponse>> listEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WebhookEventResponse> events = webhookEventService.getEventsForUser(principal.userId(), status, pageable);
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
    
    @PostMapping("/{id}/replay")
    public ResponseEntity<Void> replayEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        webhookEventService.replayEvent(principal.userId(), id);
        return ResponseEntity.ok().build();
    }
}