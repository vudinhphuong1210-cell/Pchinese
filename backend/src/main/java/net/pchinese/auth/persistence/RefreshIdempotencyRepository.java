package net.pchinese.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshIdempotencyRepository extends JpaRepository<RefreshIdempotencyEntity, UUID> {
    @Query("select r from RefreshIdempotencyEntity r where r.sessionId = :sessionId and r.refreshRequestId = :requestId and r.expiresAt > :now")
    Optional<RefreshIdempotencyEntity> findLiveForSessionRequest(@Param("sessionId") UUID sessionId,
                                                                 @Param("requestId") UUID requestId, @Param("now") Instant now);

    @Query("select r from RefreshIdempotencyEntity r where r.sessionId = :sessionId and r.sourceRefreshTokenId = :sourceTokenId and r.refreshRequestId = :requestId and r.expiresAt > :now")
    Optional<RefreshIdempotencyEntity> findLive(@Param("sessionId") UUID sessionId, @Param("sourceTokenId") UUID sourceTokenId,
                                                @Param("requestId") UUID requestId, @Param("now") Instant now);
}
