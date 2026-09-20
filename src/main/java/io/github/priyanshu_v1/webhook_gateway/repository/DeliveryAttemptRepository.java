package io.github.priyanshu_v1.webhook_gateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.priyanshu_v1.webhook_gateway.entity.DeliveryAttempt;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(UUID eventId);
    int countByEventId(UUID eventId);
    
    Optional<DeliveryAttempt> findTopByEventIdOrderByAttemptNumberDesc(UUID eventId);
}