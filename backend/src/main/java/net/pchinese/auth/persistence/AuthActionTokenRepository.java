package net.pchinese.auth.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.auth.domain.ActionTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthActionTokenEntity t join fetch t.user where t.tokenHash = :hash")
    Optional<AuthActionTokenEntity> findLockedByTokenHash(@Param("hash") String hash);

    @Query("select t from AuthActionTokenEntity t where t.user.userId = :userId and t.purpose = :purpose and t.consumedAt is null and t.invalidatedAt is null")
    List<AuthActionTokenEntity> findOpenByUserAndPurpose(@Param("userId") UUID userId, @Param("purpose") ActionTokenPurpose purpose);
}
