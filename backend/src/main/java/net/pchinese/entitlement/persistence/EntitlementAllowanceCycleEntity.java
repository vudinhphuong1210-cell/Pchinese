package net.pchinese.entitlement.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.pchinese.aiops.domain.AllowancePeriod;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entitlement_allowance_cycles")
public class EntitlementAllowanceCycleEntity {
    public enum CycleStatus { CURRENT, CLOSED }

    @Id
    @Column(name = "entitlement_allowance_cycle_id", nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_entitlement_id", nullable = false)
    private UserEntitlementEntity entitlement;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_policy_version_id", nullable = false)
    private PlanPolicyVersionEntity policyVersion;
    @Column(name = "cycle_started_at", nullable = false)
    private Instant cycleStartedAt;
    @Column(name = "cycle_ends_at", nullable = false)
    private Instant cycleEndsAt;
    @Column(name = "allowance_limit", nullable = false)
    private int allowanceLimit;
    @Enumerated(EnumType.STRING)
    @Column(name = "allowance_period", nullable = false, length = 16)
    private AllowancePeriod allowancePeriod;
    @Column(name = "used_units", nullable = false)
    private int usedUnits;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CycleStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected EntitlementAllowanceCycleEntity() { }

    public static EntitlementAllowanceCycleEntity createCurrent(UserEntitlementEntity entitlement,
                                                                  PlanPolicyVersionEntity policy,
                                                                  Instant startsAt, Instant endsAt) {
        EntitlementAllowanceCycleEntity cycle = new EntitlementAllowanceCycleEntity();
        cycle.id = UUID.randomUUID();
        cycle.entitlement = entitlement;
        cycle.policyVersion = policy;
        cycle.cycleStartedAt = startsAt;
        cycle.cycleEndsAt = endsAt;
        cycle.allowanceLimit = policy.getAllowanceUnits();
        cycle.allowancePeriod = policy.getAllowancePeriod();
        cycle.usedUnits = 0;
        cycle.status = CycleStatus.CURRENT;
        cycle.createdAt = startsAt;
        return cycle;
    }

    public void close(Instant now) { this.status = CycleStatus.CLOSED; this.closedAt = now; }
    public void incrementUsedUnits(int units) { this.usedUnits += units; }
    public void refundUsedUnits(int units) { this.usedUnits = Math.max(0, this.usedUnits - units); }

    public UUID getId() { return id; }
    public UserEntitlementEntity getEntitlement() { return entitlement; }
    public PlanPolicyVersionEntity getPolicyVersion() { return policyVersion; }
    public Instant getCycleStartedAt() { return cycleStartedAt; }
    public Instant getCycleEndsAt() { return cycleEndsAt; }
    public int getAllowanceLimit() { return allowanceLimit; }
    public AllowancePeriod getAllowancePeriod() { return allowancePeriod; }
    public int getUsedUnits() { return usedUnits; }
    public CycleStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public long getVersion() { return version; }
}
