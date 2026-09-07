package net.pchinese.auth.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenEntity r join fetch r.session where r.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findLockedByTokenHash(@Param("tokenHash") String tokenHash);

    @Query("select r from RefreshTokenEntity r where r.familyId = :familyId and r.revokedAt is null")
    List<RefreshTokenEntity> findActiveByFamilyId(@Param("familyId") UUID familyId);
}
