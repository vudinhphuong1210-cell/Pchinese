package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import net.pchinese.aiops.domain.AlertState;
import net.pchinese.aiops.persistence.AiMonitoringAlertEntity;
import net.pchinese.aiops.persistence.AiMonitoringAlertRepository;
import net.pchinese.aiops.persistence.AiMonitoringRuleEntity;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiMonitoringAlertService {
    private final AiMonitoringAlertRepository alerts;
    private final AiAdminAuditService auditService;

    public AiMonitoringAlertService(AiMonitoringAlertRepository alerts, AiAdminAuditService auditService) {
        this.alerts = alerts;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<MonitoringAlertView> list(UserPrincipal actor, Pageable pageable, AlertState state) {
        requireAdmin(actor);
        return (state == null ? alerts.findAllByOrderByLastEvaluatedAtDesc(pageable)
                : alerts.findByStateOrderByLastEvaluatedAtDesc(state, pageable)).map(this::toView);
    }

    @Transactional
    public MonitoringAlertView acknowledge(UserPrincipal actor, UUID alertId, long expectedVersion, String note) {
        requireAdmin(actor);
        AiMonitoringAlertEntity alert = alerts.findById(alertId).orElseThrow(ApiException::notFound);
        if (alert.getVersion() != expectedVersion) {
            throw ApiException.conflict("This alert has changed. Reload it before acknowledging.");
        }
        if (alert.getState() == AlertState.RESOLVED) {
            throw ApiException.conflict("A resolved alert cannot be acknowledged.");
        }
        Instant now = Instant.now();
        alert.acknowledge(actor.userId(), note == null || note.isBlank() ? null : note.trim(), now);
        alerts.save(alert);
        auditService.record(actor.userId(), AiAdminAuditEventType.ALERT_ACKNOWLEDGED,
                AiAdminAuditTargetType.MONITORING_ALERT, alert.getId(), note, null, safeSummary(alert), now);
        return toView(alert);
    }

    /** Called by the scheduled evaluator; it never sends an external notification. */
    @Transactional
    public void applyEvaluation(AiMonitoringRuleEntity rule, BigDecimal value, boolean partialData, boolean breached, Instant now) {
        var active = alerts.findByRule_IdAndStateIn(rule.getId(), List.of(AlertState.OPEN, AlertState.ACKNOWLEDGED));
        if (partialData) {
            active.ifPresent(alert -> { alert.updateBreach(value, true, now); alerts.save(alert); });
            return;
        }
        if (breached) {
            AiMonitoringAlertEntity alert = active.orElseGet(() -> AiMonitoringAlertEntity.open(rule, value, false, now));
            alert.updateBreach(value, false, now);
            alerts.save(alert);
            return;
        }
        active.ifPresent(alert -> { alert.resolve(now); alerts.save(alert); });
    }

    private MonitoringAlertView toView(AiMonitoringAlertEntity alert) {
        return new MonitoringAlertView(alert.getId(), alert.getRule().getId(), alert.getState(), alert.getMetric(),
                alert.getThreshold(), alert.getLatestValue(), alert.isPartialData(), alert.getFirstSeenAt(),
                alert.getLastEvaluatedAt(), alert.getAcknowledgedAt(), alert.getResolvedAt(), alert.getVersion());
    }

    private Map<String, Object> safeSummary(AiMonitoringAlertEntity alert) {
        return Map.of("state", alert.getState().name(), "metric", alert.getMetric().name(),
                "latestValue", alert.getLatestValue(), "partialData", alert.isPartialData());
    }

    private void requireAdmin(UserPrincipal actor) {
        if (actor == null) throw ApiException.unauthenticated();
        if (!actor.isAdmin()) throw ApiException.forbidden();
    }

    public record MonitoringAlertView(UUID id, UUID ruleId, AlertState state,
                                      net.pchinese.aiops.domain.MonitoringMetric metric,
                                      BigDecimal threshold, BigDecimal latestValue, boolean partialData,
                                      Instant firstSeenAt, Instant lastEvaluatedAt, Instant acknowledgedAt,
                                      Instant resolvedAt, long version) { }
}
