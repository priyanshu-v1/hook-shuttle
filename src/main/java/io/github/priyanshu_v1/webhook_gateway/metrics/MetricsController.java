package io.github.priyanshu_v1.webhook_gateway.metrics;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import io.github.priyanshu_v1.webhook_gateway.metrics.dto.MetricsSummaryResponse;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<MetricsSummaryResponse> getMetricsSummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MetricsSummaryResponse response = metricsService.getMetricsSummary(principal.userId());
        return ResponseEntity.ok(response);
    }
}