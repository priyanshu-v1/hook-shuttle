package io.github.priyanshu_v1.webhook_gateway.webhooks;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.priyanshu_v1.webhook_gateway.config.RabbitMQConfig;
import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookDispatchEvent;

@Service
public class AsyncRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AsyncRabbitPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void publishEventAsync(WebhookDispatchEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}