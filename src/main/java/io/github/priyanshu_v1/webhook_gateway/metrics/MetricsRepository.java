package io.github.priyanshu_v1.webhook_gateway.metrics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.priyanshu_v1.webhook_gateway.webhooks.WebhookEvent; // Ensure this matches your exact entity package

public interface MetricsRepository extends JpaRepository<WebhookEvent, UUID> {

    // --- 1. EVENTS (30 days vs previous 30 days) ---
    @Query(value = "SELECT COUNT(*) FROM webhook_events WHERE user_id = :userId AND created_at >= NOW() - INTERVAL '30 days'", nativeQuery = true)
    long countEventsLast30Days(@Param("userId") UUID userId);

    @Query(value = "SELECT COUNT(*) FROM webhook_events WHERE user_id = :userId AND created_at >= NOW() - INTERVAL '60 days' AND created_at < NOW() - INTERVAL '30 days'", nativeQuery = true)
    long countEventsPrevious30Days(@Param("userId") UUID userId);

    // --- 2. SUCCESS RATE & LATENCY (Last 24h) ---
    @Query(value = "SELECT " +
            "COUNT(da.id) AS total_attempts, " +
            "COUNT(CASE WHEN da.response_status_code BETWEEN 200 AND 299 THEN 1 END) AS success_attempts, " +
            "COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY da.execution_time_ms) FILTER (WHERE da.response_status_code BETWEEN 200 AND 299), 0) AS p50_latency " +
            "FROM delivery_attempts da " + 
            "JOIN webhook_events we ON da.event_id = we.id " +
            "WHERE we.user_id = :userId AND da.attempted_at >= NOW() - INTERVAL '24 hours'", nativeQuery = true)
    List<Object[]> getDeliveryStats24h(@Param("userId") UUID userId);

    // --- 3. SUCCESS RATE & LATENCY (Previous 24h: 48h to 24h ago) ---
    @Query(value = "SELECT " +
            "COUNT(da.id) AS total_attempts, " +
            "COUNT(CASE WHEN da.response_status_code BETWEEN 200 AND 299 THEN 1 END) AS success_attempts, " +
            "COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY da.execution_time_ms) FILTER (WHERE da.response_status_code BETWEEN 200 AND 299), 0) AS p50_latency " +
            "FROM delivery_attempts da " +
            "JOIN webhook_events we ON da.event_id = we.id " +
            "WHERE we.user_id = :userId AND da.attempted_at >= NOW() - INTERVAL '48 hours' AND da.attempted_at < NOW() - INTERVAL '24 hours'", nativeQuery = true)
    List<Object[]> getDeliveryStatsPrevious24h(@Param("userId") UUID userId);

    // --- 4. ACTIVE ENDPOINTS (Last 24h vs Previous 24h) ---
    @Query(value = "SELECT COUNT(DISTINCT endpoint_id) FROM webhook_events WHERE user_id = :userId AND created_at >= NOW() - INTERVAL '24 hours'", nativeQuery = true)
    long countActiveEndpoints24h(@Param("userId") UUID userId);

    @Query(value = "SELECT COUNT(DISTINCT endpoint_id) FROM webhook_events WHERE user_id = :userId AND created_at >= NOW() - INTERVAL '48 hours' AND created_at < NOW() - INTERVAL '24 hours'", nativeQuery = true)
    long countActiveEndpointsPrevious24h(@Param("userId") UUID userId);

    // --- 5. STATUS BREAKDOWN & THROUGHPUT (Last 24h) ---
    @Query(value = "SELECT status, COUNT(*) FROM webhook_events WHERE user_id = :userId AND created_at >= NOW() - INTERVAL '24 hours' GROUP BY status", nativeQuery = true)
    List<Object[]> getStatusBreakdown24h(@Param("userId") UUID userId);

    @Query(value = "SELECT " +
            "TO_CHAR(DATE_BIN('3 hours', da.attempted_at, TIMESTAMP '2000-01-01'), 'HH24:00') AS time_bucket, " +
            "COUNT(CASE WHEN da.response_status_code BETWEEN 200 AND 299 THEN 1 END) AS delivered, " +
            "COUNT(CASE WHEN da.response_status_code NOT BETWEEN 200 AND 299 OR da.response_status_code IS NULL THEN 1 END) AS failed " +
            "FROM delivery_attempts da " +
            "JOIN webhook_events we ON da.event_id = we.id " +
            "WHERE we.user_id = :userId AND da.attempted_at >= NOW() - INTERVAL '24 hours' " +
            "GROUP BY time_bucket ORDER BY time_bucket ASC", nativeQuery = true)
    List<Object[]> getThroughput24h(@Param("userId") UUID userId);
}