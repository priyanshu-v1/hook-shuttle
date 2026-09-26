package io.github.priyanshu_v1.webhook_gateway.endpoints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
	Optional<Endpoint> findByIdAndUser_Id(UUID id, UUID userId);
	Page<Endpoint> findByUser_Id(UUID userId, Pageable pageable);
    List<Endpoint> findByUser_IdAndStatus(UUID userId, String status);
}