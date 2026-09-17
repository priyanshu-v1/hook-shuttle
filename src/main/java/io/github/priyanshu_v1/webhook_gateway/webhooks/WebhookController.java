package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

	private final WebhookService webhookService;

	public WebhookController(WebhookService webhookService) {
		this.webhookService = webhookService;
	}

	@PostMapping("/dispatch")
	public ResponseEntity<WebhookDispatchResponse> dispatchWebhook(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader("X-Endpoint-ID") UUID endpointId,
			@RequestHeader("X-Event-Type") String eventType,
			@Valid @RequestBody String rawPayload
		) {
		WebhookDispatchResponse response = webhookService.dispatchWebhook(principal.userId(), endpointId, eventType,
				rawPayload);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}
}