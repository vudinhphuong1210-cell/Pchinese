package net.pchinese.aiops.api;

import net.pchinese.aiops.application.AiAdminAuditQueryService;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai/audit-events")
public class AiAdminAuditController {
    private final AiAdminAuditQueryService audits;
    public AiAdminAuditController(AiAdminAuditQueryService audits) { this.audits = audits; }

    @GetMapping
    public ApiEnvelope<java.util.List<AiAdminAuditQueryService.AuditEventView>> list(@AuthenticationPrincipal UserPrincipal actor,
                                                                                       @RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) throw net.pchinese.common.error.ApiException.validation("Invalid page request.");
        var result = audits.list(actor, PageRequest.of(page, size));
        return ApiEnvelope.success(result.getContent(), Map.of("page", Map.of("page", page, "size", size,
                "totalItems", result.getTotalElements(), "totalPages", result.getTotalPages())));
    }
}
