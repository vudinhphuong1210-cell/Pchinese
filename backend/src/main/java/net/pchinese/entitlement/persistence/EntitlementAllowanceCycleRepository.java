package net.pchinese.entitlement.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface EntitlementAllowanceCycleRepository extends JpaRepository<EntitlementAllowanceCycleEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cycle from EntitlementAllowanceCycleEntity cycle where cycle.entitlement.userEntitlementId = :entitlementId and cycle.status = 'CURRENT'")
    Optional<EntitlementAllowanceCycleEntity> findCurrentByEntitlementIdLocked(UUID entitlementId);
}
