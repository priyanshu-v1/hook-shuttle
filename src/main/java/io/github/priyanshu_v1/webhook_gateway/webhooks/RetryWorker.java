package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.redisson.RedissonShutdownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;

@Component
public class RetryWorker implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(RetryWorker.class);

	private final RedissonRetryQueueService retryQueueService;
	private final AsyncRabbitPublisher rabbitPublisher;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public RetryWorker(RedissonRetryQueueService retryQueueService, AsyncRabbitPublisher rabbitPublisher) {
		this.retryQueueService = retryQueueService;
		this.rabbitPublisher = rabbitPublisher;
	}

	@Override
	public void run(String... args) {
		executor.submit(this::consumeRetryEvents);
	}

	private void consumeRetryEvents() {
		log.info("Starting Webhook Retry Worker daemon listener...");
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Passive blocking call — waits until an event's delay expires
				WebhookDispatchEvent event = retryQueueService.pollNextForExecution();

				log.info("Retry delay expired for event {}. Re-enqueuing to RabbitMQ (Attempt {})", event.eventId(),
						event.attemptNumber());

				// Re-publish back to RabbitMQ for execution
//				System.out.println(event);
				rabbitPublisher.publishEventAsync(event);

			} catch (RedissonShutdownException e) {
				log.warn("Redisson has shut down, stopping RetryWorker loop.");
				break;
			} catch (InterruptedException e) {
				log.warn("RetryWorker thread interrupted, shutting down listener loop.");
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("Unexpected error during retry consumption loop", e);
			}
		}
	}
}