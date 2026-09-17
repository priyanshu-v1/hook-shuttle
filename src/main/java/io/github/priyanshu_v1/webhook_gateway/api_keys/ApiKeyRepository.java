package io.github.priyanshu_v1.webhook_gateway.api_keys;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyAuthProjection;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByApiKeyHashAndStatus(String apiKeyHash, String status);
    List<ApiKey> findByUserId(UUID userId);
    List<ApiKey> findByKeyPrefixAndStatus(String keyPrefix, String status);
    
    @Query("SELECT new io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyAuthProjection(k.apiKeyHash, u.email, u.id) " +
            "FROM ApiKey k JOIN k.user u " +
            "WHERE k.keyPrefix = :prefix AND k.status = :status")
     List<ApiKeyAuthProjection> findAuthDetailsByPrefixAndStatus(
             @Param("prefix") String prefix, 
             @Param("status") String status
     );
}