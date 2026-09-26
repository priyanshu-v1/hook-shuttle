package io.github.priyanshu_v1.webhook_gateway.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.priyanshu_v1.webhook_gateway.metrics.dto.MetricsSummaryResponse;
import io.github.priyanshu_v1.webhook_gateway.metrics.dto.MetricsSummaryResponse.MetricsCardsDto;
import io.github.priyanshu_v1.webhook_gateway.metrics.dto.MetricsSummaryResponse.StatusBreakdownDto;
import io.github.priyanshu_v1.webhook_gateway.metrics.dto.MetricsSummaryResponse.ThroughputPointDto;

@Service
public class MetricsService {

    private final MetricsRepository metricsRepository;

    public MetricsService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Transactional(readOnly = true)
    public MetricsSummaryResponse getMetricsSummary(UUID userId) {
        // 1. Events processed & delta (30d vs prev 30d)
        long totalEvents = metricsRepository.countEventsLast30Days(userId);
        long prevEvents = metricsRepository.countEventsPrevious30Days(userId);
        String eventsDelta = calculatePercentageDelta(totalEvents, prevEvents);

        // 2. Active endpoints & delta (24h vs prev 24h)
        long activeEndpoints = metricsRepository.countActiveEndpoints24h(userId);
        long prevEndpoints = metricsRepository.countActiveEndpointsPrevious24h(userId);
        String endpointsDelta = calculateAbsoluteDelta(activeEndpoints, prevEndpoints, "");

        // 3. Success rate & latency (Rolling 24h)
        List<Object[]> stats24h = metricsRepository.getDeliveryStats24h(userId);
        long totalAttempts = 0, successAttempts = 0, p50Latency = 0;
        if (!stats24h.isEmpty() && stats24h.get(0) != null) {
            Object[] row = stats24h.get(0);
            totalAttempts = row[0] != null ? ((Number) row[0]).longValue() : 0;
            successAttempts = row[1] != null ? ((Number) row[1]).longValue() : 0;
            p50Latency = row[2] != null ? ((Number) row[2]).longValue() : 0;
        }

        double successRate = totalAttempts > 0 
                ? BigDecimal.valueOf((double) successAttempts * 100 / totalAttempts).setScale(2, RoundingMode.HALF_UP).doubleValue() 
                : 100.00;

        // 4. Success rate & latency (Previous 24h for deltas)
        List<Object[]> prevStats24h = metricsRepository.getDeliveryStatsPrevious24h(userId);
        long prevTotalAttempts = 0, prevSuccessAttempts = 0, prevP50Latency = 0;
        if (!prevStats24h.isEmpty() && prevStats24h.get(0) != null) {
            Object[] row = prevStats24h.get(0);
            prevTotalAttempts = row[0] != null ? ((Number) row[0]).longValue() : 0;
            prevSuccessAttempts = row[1] != null ? ((Number) row[1]).longValue() : 0;
            prevP50Latency = row[2] != null ? ((Number) row[2]).longValue() : 0;
        }

        double prevSuccessRate = prevTotalAttempts > 0 
                ? BigDecimal.valueOf((double) prevSuccessAttempts * 100 / prevTotalAttempts).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 100.00;

        double successRateChange = successRate - prevSuccessRate;
        String successDelta = String.format("%s%.1f%%", successRateChange >= 0 ? "+" : "", successRateChange);

        long latencyChange = p50Latency - prevP50Latency;
        String latencyDelta = String.format("%s%dms", latencyChange >= 0 ? "+" : "", latencyChange);

        MetricsCardsDto cardsDto = new MetricsCardsDto(
                totalEvents,
                successRate,
                p50Latency,
                activeEndpoints,
                eventsDelta,
                successDelta,
                latencyDelta,
                endpointsDelta
        );

        // 5. Throughput Chart Data
        List<Object[]> rawThroughput = metricsRepository.getThroughput24h(userId);
        List<ThroughputPointDto> throughputList = new ArrayList<>();
        for (Object[] row : rawThroughput) {
            String timeBucket = (String) row[0];
            long delivered = row[1] != null ? ((Number) row[1]).longValue() : 0;
            long failed = row[2] != null ? ((Number) row[2]).longValue() : 0;
            throughputList.add(new ThroughputPointDto(timeBucket, delivered, failed));
        }

        // 6. Status Breakdown Data
        List<Object[]> rawStatus = metricsRepository.getStatusBreakdown24h(userId);
        List<StatusBreakdownDto> statusList = new ArrayList<>();
        for (Object[] row : rawStatus) {
            String statusKey = (String) row[0];
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
            String name = formatStatusName(statusKey);
            statusList.add(new StatusBreakdownDto(name, count, statusKey));
        }

        return new MetricsSummaryResponse(cardsDto, throughputList, statusList);
    }

    private String calculatePercentageDelta(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double change = ((double) (current - previous) / previous) * 100;
        String sign = change >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, change);
    }

    private String calculateAbsoluteDelta(long current, long previous, String suffix) {
        long diff = current - previous;
        String sign = diff >= 0 ? "+" : "";
        return String.format("%s%d%s", sign, diff, suffix);
    }

    private String formatStatusName(String status) {
    	if (status == null) return "Pending";
        
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> "Success";
            case "FAILED" -> "Failed";
            case "PENDING", "DISPATCHING", "IN_REDIS_RETRY", "COLD_RETRY_SCHEDULED" -> "Pending";
            default -> "Pending";
        };
    }
}