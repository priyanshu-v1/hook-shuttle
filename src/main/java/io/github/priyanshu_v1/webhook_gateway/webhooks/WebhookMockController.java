package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-webhook")
public class WebhookMockController {

	private final AtomicInteger callCount = new AtomicInteger(0);

	@PostMapping("/simulate-500")
	public ResponseEntity<String> simulateFailure(@RequestHeader("X-Webhook-Signature") String signature,
			@RequestBody String body) {

		int count = callCount.incrementAndGet();
		System.out.println("Received attempt #" + count);

		// Fail first 2 attempts with 500, then succeed on 3rd attempt
		if (count < 4) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("""

					{"error": "Simulated server failure"})

					""");
		}
		

		return ResponseEntity.ok("""
				{"status": "success"}
							""");
	}
}