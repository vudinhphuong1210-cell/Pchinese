package net.pchinese.aiops.api;

import jakarta.validation.Valid;
import net.pchinese.aiops.application.AiMonitoringRuleService;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/monitoring-rules")
public class AiMonitoringRuleController {
    private final AiMonitoringRuleService rules;
    public AiMonitoringRuleController(AiMonitoringRuleService rules) { this.rules = rules; }

    @GetMapping
    public ApiEnvelope<java.util.List<AiMonitoringRuleService.MonitoringRuleView>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiEnvelope.success(rules.list(actor));
    }
    @PostMapping
    public ApiEnvelope<AiMonitoringRuleService.MonitoringRuleView> create(@AuthenticationPrincipal UserPrincipal actor,
                                                                            @Valid @RequestBody AiAdministrationDtos.MonitoringRuleWriteRequest request) {
        return ApiEnvelope.success(rules.create(actor, request.toCommand()));
    }
    @PatchMapping("/{ruleId}")
    public ApiEnvelope<AiMonitoringRuleService.MonitoringRuleView> update(@AuthenticationPrincipal UserPrincipal actor,
                                                                            @PathVariable UUID ruleId,
                                                                            @Valid @RequestBody AiAdministrationDtos.MonitoringRuleUpdateRequest request) {
        return ApiEnvelope.success(rules.update(actor, ruleId, request.toCommand()));
    }
}
