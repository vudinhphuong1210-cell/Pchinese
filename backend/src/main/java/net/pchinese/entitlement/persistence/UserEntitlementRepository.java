package net.pchinese.entitlement.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.entitlement.domain.EntitlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlementEntity, UUID> {
    Optional<UserEntitlementEntity> findByUserIdAndStatus(UUID userId, EntitlementStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM UserEntitlementEntity e WHERE e.userId = :userId AND e.status = 'ACTIVE'")
    Optional<UserEntitlementEntity> findActiveByUserIdLocked(UUID userId);
}
