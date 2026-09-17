package io.github.priyanshu_v1.webhook_gateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "webhook.exchange";
    public static final String QUEUE = "webhook.dispatch.queue";
    public static final String ROUTING_KEY = "webhook.dispatch";

    @Bean
    public DirectExchange webhookExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue webhookDispatchQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding webhookBinding(Queue webhookDispatchQueue, DirectExchange webhookExchange) {
        return BindingBuilder.bind(webhookDispatchQueue)
                .to(webhookExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}