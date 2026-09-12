package net.pchinese.aiops.api;

import jakarta.validation.Valid;
import net.pchinese.aiops.application.AiPlanPolicyService;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai/plans")
public class AiPlanPolicyController {
    private final AiPlanPolicyService policies;

    public AiPlanPolicyController(AiPlanPolicyService policies) { this.policies = policies; }

    @GetMapping
    public ApiEnvelope<java.util.List<AiPlanPolicyService.PolicyView>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiEnvelope.success(policies.listCurrent(actor));
    }

    @GetMapping("/{planCode}/revisions")
    public ApiEnvelope<java.util.List<AiPlanPolicyService.PolicyView>> history(@AuthenticationPrincipal UserPrincipal actor,
                                                                                 @PathVariable PlanCode planCode,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        var result = policies.listHistory(actor, planCode, PageRequest.of(page, size));
        return ApiEnvelope.success(result.getContent(), pageMeta(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/{planCode}/revisions")
    public ApiEnvelope<AiPlanPolicyService.PolicyView> publish(@AuthenticationPrincipal UserPrincipal actor,
                                                                 @PathVariable PlanCode planCode,
                                                                 @Valid @RequestBody AiAdministrationDtos.PublishPolicyRequest request) {
        return ApiEnvelope.success(policies.publish(actor, planCode, request.toCommand()));
    }

    @PostMapping("/{planCode}/retire")
    public ApiEnvelope<AiPlanPolicyService.PolicyView> retire(@AuthenticationPrincipal UserPrincipal actor,
                                                                @PathVariable PlanCode planCode,
                                                                @Valid @RequestBody AiAdministrationDtos.RetirePlanRequest request) {
        return ApiEnvelope.success(policies.retire(actor, planCode, request.toCommand()));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw net.pchinese.common.error.ApiException.validation("Invalid page request.");
    }
    private Map<String, Object> pageMeta(int page, int size, long totalItems, int totalPages) {
        return Map.of("page", Map.of("page", page, "size", size, "totalItems", totalItems, "totalPages", totalPages));
    }
}
