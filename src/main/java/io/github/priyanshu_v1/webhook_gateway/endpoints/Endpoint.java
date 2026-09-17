package io.github.priyanshu_v1.webhook_gateway.endpoints;

import io.github.priyanshu_v1.webhook_gateway.entity.BaseEntity;
import io.github.priyanshu_v1.webhook_gateway.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "endpoints")
public class Endpoint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_url", nullable = false, columnDefinition = "TEXT")
    private String targetUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "rate_limit_per_sec", nullable = false)
    private Integer rateLimitPerSec = 100;

    @Builder.Default
    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 5000;

    @Builder.Default
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 5;
}