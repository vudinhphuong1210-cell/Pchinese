package net.pchinese.aiops.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import net.pchinese.aiops.application.AiMonitoringRuleService;
import net.pchinese.aiops.application.AiPlanPolicyService;
import net.pchinese.aiops.domain.AllowancePeriod;
import net.pchinese.aiops.domain.AvailabilityState;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.EvaluationWindow;
import net.pchinese.aiops.domain.MonitoringMetric;
import net.pchinese.aiops.domain.PriceInterval;
import net.pchinese.entitlement.domain.PlanCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class AiAdministrationDtos {
    private AiAdministrationDtos() { }

    public record PublishPolicyRequest(
            @NotNull @PositiveOrZero Long expectedCurrentVersion,
            @NotBlank @Size(max = 500) String reason,
            @NotBlank @Size(max = 120) String displayName,
            @Size(max = 2000) String description,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 300) String> benefits,
            @NotNull AvailabilityState availabilityState,
            @NotNull @DecimalMin("0.0") BigDecimal priceAmount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @NotNull PriceInterval priceInterval,
            @Size(max = 120) String displayLabel,
            @NotNull @PositiveOrZero Integer allowanceUnits,
            @NotNull AllowancePeriod allowancePeriod
    ) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported policy field."); }
        AiPlanPolicyService.PublishPolicyCommand toCommand() {
            return new AiPlanPolicyService.PublishPolicyCommand(expectedCurrentVersion, reason, displayName, description,
                    benefits, availabilityState, priceAmount, currency, priceInterval, displayLabel, allowanceUnits, allowancePeriod);
        }
    }

    public record RetirePlanRequest(@NotNull @PositiveOrZero Long expectedCurrentVersion,
                                    @NotBlank @Size(max = 500) String reason) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported retirement field."); }
        AiPlanPolicyService.RetirePlanCommand toCommand() { return new AiPlanPolicyService.RetirePlanCommand(expectedCurrentVersion, reason); }
    }

    public record MonitoringRuleWriteRequest(
            @NotNull MonitoringMetric metric,
            @NotNull @DecimalMin("0.0") BigDecimal threshold,
            @NotNull EvaluationWindow evaluationWindow,
            PlanCode planCode,
            UUID policyVersionId,
            AiCapability capability,
            @NotNull Boolean enabled
    ) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported monitoring rule field."); }
        AiMonitoringRuleService.RuleCommand toCommand() {
            return new AiMonitoringRuleService.RuleCommand(metric, threshold, evaluationWindow, planCode, policyVersionId, capability, enabled);
        }
    }

    public record MonitoringRuleUpdateRequest(
            @NotNull @PositiveOrZero Long expectedVersion,
            @NotNull MonitoringMetric metric,
            @NotNull @DecimalMin("0.0") BigDecimal threshold,
            @NotNull EvaluationWindow evaluationWindow,
            PlanCode planCode,
            UUID policyVersionId,
            AiCapability capability,
            @NotNull Boolean enabled
    ) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported monitoring rule field."); }
        AiMonitoringRuleService.UpdateRuleCommand toCommand() {
            return new AiMonitoringRuleService.UpdateRuleCommand(expectedVersion, metric, threshold, evaluationWindow,
                    planCode, policyVersionId, capability, enabled);
        }
    }

    public record AcknowledgeAlertRequest(@NotNull @PositiveOrZero Long expectedVersion,
                                          @Size(max = 500) String note) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported acknowledgement field."); }
    }
}
