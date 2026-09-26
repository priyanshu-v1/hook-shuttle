package io.github.priyanshu_v1.webhook_gateway.metrics.dto;

import java.util.List;

public record MetricsSummaryResponse(
        MetricsCardsDto metrics,
        List<ThroughputPointDto> throughput,
        List<StatusBreakdownDto> statusBreakdown
) {
    public record MetricsCardsDto(
            long totalEvents,
            double successRate,
            long p50Latency,
            long activeEndpoints,
            String eventsDelta,
            String successDelta,
            String latencyDelta,
            String endpointsDelta
    ) {}

    public record ThroughputPointDto(
            String time,
            long delivered,
            long failed
    ) {}

    public record StatusBreakdownDto(
            String name,
            long value,
            String key
    ) {}
}