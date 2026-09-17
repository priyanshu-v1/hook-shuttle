package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;

@Service
public class RedissonRetryQueueService {

    private static final String QUEUE_NAME = "webhook-retry-queue";

    private final RBlockingQueue<WebhookDispatchEvent> blockingQueue;
    @SuppressWarnings("deprecation")
    private final RDelayedQueue<WebhookDispatchEvent> delayedQueue;

    @SuppressWarnings("deprecation")
    public RedissonRetryQueueService(RedissonClient redissonClient) {
        this.blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME);
        this.delayedQueue = redissonClient.getDelayedQueue(this.blockingQueue);
    }

    /**
     * Schedules a WebhookDispatchEvent for retry after the specified delay.
     */
    public void scheduleRetry(WebhookDispatchEvent event, long delay, TimeUnit timeUnit) {
        this.delayedQueue.offer(event, delay, timeUnit);
    }

    /**
     * Passive TCP socket wait call (BLPOP under the hood via Netty).
     * Blocks until a payload is ready for consumption.
     */
    public WebhookDispatchEvent pollNextForExecution() throws InterruptedException {
        return this.blockingQueue.take();
    }
}