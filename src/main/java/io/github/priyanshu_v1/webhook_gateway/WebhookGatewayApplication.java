package io.github.priyanshu_v1.webhook_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class WebhookGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookGatewayApplication.class, args);
	}

}
