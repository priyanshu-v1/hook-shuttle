package io.github.priyanshu_v1.webhook_gateway.webhooks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    List<WebhookEvent> findByUserIdAndStatus(UUID userId, String status);
    List<WebhookEvent> findByEndpointId(UUID endpointId);
    
    
    @Query("SELECT e FROM WebhookEvent e JOIN FETCH e.endpoint JOIN FETCH e.user WHERE e.status = :status AND e.nextRetryAt <= :dateTime")
    List<WebhookEvent> findByStatusAndNextRetryAtLessThanEqualWithDetails(
        @Param("status") String status, 
        @Param("dateTime") Instant dateTime
    );
    
    
 // Added for frontend queries
    List<WebhookEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<WebhookEvent> findByIdAndUserId(UUID id, UUID userId);
}