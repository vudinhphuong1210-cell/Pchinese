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
import net.pchinese.aiops.domain.AlertState;
import net.pchinese.aiops.domain.MonitoringMetric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_monitoring_alerts")
public class AiMonitoringAlertEntity {
    @Id
    @Column(name = "ai_monitoring_alert_id", nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_monitoring_rule_id", nullable = false)
    private AiMonitoringRuleEntity rule;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertState state;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MonitoringMetric metric;
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal threshold;
    @Column(name = "latest_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal latestValue;
    @Column(name = "partial_data", nullable = false)
    private boolean partialData;
    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;
    @Column(name = "last_evaluated_at", nullable = false)
    private Instant lastEvaluatedAt;
    @Column(name = "acknowledged_by_user_id")
    private UUID acknowledgedByUserId;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    @Column(name = "acknowledgement_note", length = 500)
    private String acknowledgementNote;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected AiMonitoringAlertEntity() { }

    public static AiMonitoringAlertEntity open(AiMonitoringRuleEntity rule, BigDecimal value,
                                               boolean partialData, Instant now) {
        AiMonitoringAlertEntity alert = new AiMonitoringAlertEntity();
        alert.id = UUID.randomUUID();
        alert.rule = rule;
        alert.state = AlertState.OPEN;
        alert.metric = rule.getMetric();
        alert.threshold = rule.getThreshold();
        alert.latestValue = value;
        alert.partialData = partialData;
        alert.firstSeenAt = now;
        alert.lastEvaluatedAt = now;
        return alert;
    }

    public void updateBreach(BigDecimal value, boolean partialData, Instant now) {
        this.latestValue = value;
        this.partialData = partialData;
        this.lastEvaluatedAt = now;
    }
    public void acknowledge(UUID actorId, String note, Instant now) {
        if (state == AlertState.RESOLVED) return;
        this.state = AlertState.ACKNOWLEDGED;
        this.acknowledgedByUserId = actorId;
        this.acknowledgedAt = now;
        this.acknowledgementNote = note;
        this.lastEvaluatedAt = now;
    }
    public void resolve(Instant now) {
        if (state == AlertState.RESOLVED) return;
        this.state = AlertState.RESOLVED;
        this.resolvedAt = now;
        this.lastEvaluatedAt = now;
    }

    public UUID getId() { return id; }
    public AiMonitoringRuleEntity getRule() { return rule; }
    public AlertState getState() { return state; }
    public MonitoringMetric getMetric() { return metric; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getLatestValue() { return latestValue; }
    public boolean isPartialData() { return partialData; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public long getVersion() { return version; }
}
