package net.pchinese.aiops.api;

import jakarta.validation.Valid;
import net.pchinese.aiops.application.AiMonitoringAlertService;
import net.pchinese.aiops.domain.AlertState;
import net.pchinese.common.api.ApiEnvelope;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/alerts")
public class AiMonitoringAlertController {
    private final AiMonitoringAlertService alerts;
    public AiMonitoringAlertController(AiMonitoringAlertService alerts) { this.alerts = alerts; }

    @GetMapping
    public ApiEnvelope<java.util.List<AiMonitoringAlertService.MonitoringAlertView>> list(@AuthenticationPrincipal UserPrincipal actor,
                                                                                             @RequestParam(defaultValue = "0") int page,
                                                                                             @RequestParam(defaultValue = "20") int size,
                                                                                             @RequestParam(required = false) AlertState state) {
        if (page < 0 || size < 1 || size > 50) throw net.pchinese.common.error.ApiException.validation("Invalid page request.");
        var result = alerts.list(actor, PageRequest.of(page, size), state);
        return ApiEnvelope.success(result.getContent(), Map.of("page", Map.of("page", page, "size", size,
                "totalItems", result.getTotalElements(), "totalPages", result.getTotalPages())));
    }

    @PostMapping("/{alertId}/acknowledgements")
    public ApiEnvelope<AiMonitoringAlertService.MonitoringAlertView> acknowledge(@AuthenticationPrincipal UserPrincipal actor,
                                                                                   @PathVariable UUID alertId,
                                                                                   @Valid @RequestBody AiAdministrationDtos.AcknowledgeAlertRequest request) {
        return ApiEnvelope.success(alerts.acknowledge(actor, alertId, request.expectedVersion(), request.note()));
    }
}
