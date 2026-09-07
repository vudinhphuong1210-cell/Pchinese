package net.pchinese.auth.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSessionEntity s where s.sessionId = :sessionId")
    Optional<AuthSessionEntity> findLockedById(@Param("sessionId") UUID sessionId);

    @Query("select case when count(s) > 0 then true else false end from AuthSessionEntity s where s.sessionId = :sessionId and s.user.userId = :userId and s.revokedAt is null and s.idleExpiresAt > CURRENT_TIMESTAMP and s.absoluteExpiresAt > CURRENT_TIMESTAMP")
    boolean isActiveForUser(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);

    @Query("select s from AuthSessionEntity s where s.user.userId = :userId and s.revokedAt is null and s.idleExpiresAt > :now and s.absoluteExpiresAt > :now order by s.createdAt")
    List<AuthSessionEntity> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("select s from AuthSessionEntity s where s.familyId = :familyId and s.revokedAt is null")
    List<AuthSessionEntity> findActiveByFamilyId(@Param("familyId") UUID familyId);
}
