package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventSummaryProjection;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
	List<WebhookEvent> findByUserIdAndStatus(UUID userId, String status);

	List<WebhookEvent> findByEndpointId(UUID endpointId);

	@Query("SELECT e FROM WebhookEvent e JOIN FETCH e.endpoint JOIN FETCH e.user WHERE e.status = :status AND e.nextRetryAt <= :dateTime")
	Page<WebhookEvent> findByStatusAndNextRetryAtLessThanEqualWithDetails(
	    @Param("status") String status, 
	    @Param("dateTime") Instant dateTime,
	    Pageable pageable
	);

	// Added for frontend queries
	Page<WebhookEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	Optional<WebhookEvent> findByIdAndUserId(UUID id, UUID userId);

	@Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId AND (:status IS NULL OR e.status = :status)")
	Page<WebhookEvent> findByUserIdAndOptionalStatus(@Param("userId") UUID userId, @Param("status") String status,
			Pageable pageable);

	@Query("""
			    SELECT new io.github.priyanshu_v1.webhook_gateway.webhooks.dto.WebhookEventSummaryProjection(
			        e.id,
			        e.eventType,
			        ep.id,
			        ep.targetUrl,
			        e.status,
			        e.createdAt,
			        (SELECT da.executionTimeMs FROM DeliveryAttempt da WHERE da.event.id = e.id ORDER BY da.attemptNumber DESC LIMIT 1),
			        e.rawPayload,
			        (SELECT COUNT(da2) FROM DeliveryAttempt da2 WHERE da2.event.id = e.id)
			    )
			    FROM WebhookEvent e
			    JOIN e.endpoint ep
			    WHERE e.user.id = :userId AND (:status IS NULL OR e.status = :status)
			""")
	Page<WebhookEventSummaryProjection> findEventSummariesByUserIdAndOptionalStatus(@Param("userId") UUID userId,
			@Param("status") String status, Pageable pageable);
}