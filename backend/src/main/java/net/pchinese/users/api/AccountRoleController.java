package net.pchinese.users.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.application.AccountRoleService;
import net.pchinese.users.domain.AccessReason;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/users/{userId}")
public class AccountRoleController {
    private final AccountRoleService service;
    public AccountRoleController(AccountRoleService service) { this.service = service; }

    @GetMapping("/roles")
    public ApiEnvelope<AccountRoleService.AccountRoleProjection> getProjection(@AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID userId) { return ApiEnvelope.success(service.getProjection(actor, userId)); }
    @PostMapping("/roles")
    public ApiEnvelope<AccountRoleService.AccountRoleProjection> grant(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID userId,
            @Valid @RequestBody GrantRoleRequest request) {
        if (!"ADMIN".equals(request.role())) throw ApiException.validation("Only ADMIN can be managed by this endpoint.");
        return ApiEnvelope.success(service.grantAdmin(actor, userId));
    }
    @DeleteMapping("/roles/ADMIN")
    public ApiEnvelope<AccountRoleService.AccountRoleProjection> revoke(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID userId) {
        return ApiEnvelope.success(service.revokeAdmin(actor, userId));
    }
    @PostMapping("/lock")
    public ApiEnvelope<AccountRoleService.AccountRoleProjection> lock(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID userId,
            @Valid @RequestBody AccountAccessCommandRequest request) {
        return ApiEnvelope.success(service.lock(actor, userId, request.reason(), request.note()));
    }
    @PostMapping("/unlock")
    public ApiEnvelope<AccountRoleService.AccountRoleProjection> unlock(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID userId,
            @Valid @RequestBody AccountAccessCommandRequest request) {
        return ApiEnvelope.success(service.unlock(actor, userId, request.reason(), request.note()));
    }

    public record GrantRoleRequest(@NotBlank String role) { }
    public record AccountAccessCommandRequest(@NotNull AccessReason reason, @Size(max = 280) String note) {
        @AssertTrue(message = "OTHER requires a short note")
        public boolean isOtherNoteValid() { return reason != AccessReason.OTHER || (note != null && !note.trim().isEmpty()); }
    }
}
