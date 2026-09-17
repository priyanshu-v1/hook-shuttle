package io.github.priyanshu_v1.webhook_gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditConfig {
	// Enables Spring Data JPA entity listeners for @CreatedDate and @LastModifiedDate
}
