package net.pchinese.entitlement.persistence;

import net.pchinese.entitlement.domain.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, UUID> {
    Optional<SubscriptionPlanEntity> findByPlanCode(PlanCode planCode);
}
