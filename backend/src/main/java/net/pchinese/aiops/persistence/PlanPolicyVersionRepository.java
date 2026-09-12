package net.pchinese.aiops.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.aiops.domain.PolicyStatus;
import net.pchinese.entitlement.domain.PlanCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPolicyVersionRepository extends JpaRepository<PlanPolicyVersionEntity, UUID> {
    Optional<PlanPolicyVersionEntity> findBySubscriptionPlan_PlanCodeAndStatus(PlanCode planCode, PolicyStatus status);
    List<PlanPolicyVersionEntity> findBySubscriptionPlan_PlanCodeOrderByRevisionNumberDesc(PlanCode planCode);
    Page<PlanPolicyVersionEntity> findBySubscriptionPlan_PlanCodeOrderByRevisionNumberDesc(PlanCode planCode, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select policy from PlanPolicyVersionEntity policy where policy.subscriptionPlan.planCode = :planCode and policy.status = 'PUBLISHED'")
    Optional<PlanPolicyVersionEntity> findCurrentByPlanCodeLocked(PlanCode planCode);
}
