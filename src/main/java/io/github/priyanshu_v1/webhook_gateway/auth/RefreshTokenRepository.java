package io.github.priyanshu_v1.webhook_gateway.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Fast O(1) lookup using the unencrypted prefix
    Optional<RefreshToken> findByTokenPrefix(String tokenPrefix);

    // Revoke all tokens belonging to a family (triggered on replay attack detection)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeFamily(@Param("familyId") UUID familyId);
    
    // Efficient bulk deletion for expired absolute limits or explicitly revoked tokens
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.absoluteExpiresAt < :now OR r.revoked = true")
    long deleteByAbsoluteExpiresAtBeforeOrRevokedTrue(@Param("now") Instant now);
}