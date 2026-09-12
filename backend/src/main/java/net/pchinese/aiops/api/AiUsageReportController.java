package net.pchinese.aiops.api;

import net.pchinese.aiops.application.AiUsageReportService;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/usage-report")
public class AiUsageReportController {
    private final AiUsageReportService reports;

    public AiUsageReportController(AiUsageReportService reports) { this.reports = reports; }

    @GetMapping
    public ApiEnvelope<AiUsageReportService.UsageReportView> report(@AuthenticationPrincipal UserPrincipal actor,
                                                                      @RequestParam Instant fromInclusive,
                                                                      @RequestParam Instant toExclusive,
                                                                      @RequestParam(required = false) PlanCode planCode,
                                                                      @RequestParam(required = false) UUID policyVersionId,
                                                                      @RequestParam(required = false) AiCapability capability) {
        return ApiEnvelope.success(reports.report(actor, new AiUsageReportService.ReportQuery(fromInclusive, toExclusive,
                planCode, policyVersionId, capability)));
    }
}
