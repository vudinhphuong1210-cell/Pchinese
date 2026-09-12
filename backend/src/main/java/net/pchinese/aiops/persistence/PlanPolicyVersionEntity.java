package net.pchinese.aiops.persistence;

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
import net.pchinese.aiops.domain.AvailabilityState;
import net.pchinese.aiops.domain.PolicyStatus;
import net.pchinese.aiops.domain.PriceInterval;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan_policy_versions")
public class PlanPolicyVersionEntity {
    @Id
    @Column(name = "plan_policy_version_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlanEntity subscriptionPlan;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PolicyStatus status;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> benefits;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_state", nullable = false, length = 16)
    private AvailabilityState availabilityState;
    @Column(name = "price_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceAmount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "price_interval", nullable = false, length = 16)
    private PriceInterval priceInterval;
    @Column(name = "display_label", length = 120)
    private String displayLabel;
    @Column(name = "allowance_units", nullable = false)
    private int allowanceUnits;
    @Enumerated(EnumType.STRING)
    @Column(name = "allowance_period", nullable = false, length = 16)
    private AllowancePeriod allowancePeriod;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "retired_at")
    private Instant retiredAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PlanPolicyVersionEntity() { }

    public static PlanPolicyVersionEntity publish(SubscriptionPlanEntity plan, int revisionNumber, UUID actorId,
                                                   String reason, String displayName, String description,
                                                   List<String> benefits, AvailabilityState availabilityState,
                                                   BigDecimal priceAmount, String currency, PriceInterval priceInterval,
                                                   String displayLabel, int allowanceUnits,
                                                   AllowancePeriod allowancePeriod, Instant now) {
        PlanPolicyVersionEntity policy = new PlanPolicyVersionEntity();
        policy.id = UUID.randomUUID();
        policy.subscriptionPlan = plan;
        policy.revisionNumber = revisionNumber;
        policy.status = PolicyStatus.PUBLISHED;
        policy.createdByUserId = actorId;
        policy.reason = reason;
        policy.displayName = displayName;
        policy.description = description;
        policy.benefits = List.copyOf(benefits);
        policy.availabilityState = availabilityState;
        policy.priceAmount = priceAmount;
        policy.currency = currency;
        policy.priceInterval = priceInterval;
        policy.displayLabel = displayLabel;
        policy.allowanceUnits = allowanceUnits;
        policy.allowancePeriod = allowancePeriod;
        policy.createdAt = now;
        return policy;
    }

    public void supersede() { this.status = PolicyStatus.SUPERSEDED; }
    public void retire(Instant now) { this.status = PolicyStatus.RETIRED; this.retiredAt = now; }

    public UUID getId() { return id; }
    public SubscriptionPlanEntity getSubscriptionPlan() { return subscriptionPlan; }
    public int getRevisionNumber() { return revisionNumber; }
    public PolicyStatus getStatus() { return status; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getBenefits() { return benefits == null ? List.of() : List.copyOf(benefits); }
    public AvailabilityState getAvailabilityState() { return availabilityState; }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public String getCurrency() { return currency; }
    public PriceInterval getPriceInterval() { return priceInterval; }
    public String getDisplayLabel() { return displayLabel; }
    public int getAllowanceUnits() { return allowanceUnits; }
    public AllowancePeriod getAllowancePeriod() { return allowancePeriod; }
    public String getReason() { return reason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRetiredAt() { return retiredAt; }
    public long getVersion() { return version; }
}
