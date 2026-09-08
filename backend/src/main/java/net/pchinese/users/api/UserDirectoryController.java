package net.pchinese.users.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.application.AccountRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/users")
public class UserDirectoryController {
    private final AccountRoleService service;

    public UserDirectoryController(AccountRoleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiEnvelope<AccountRoleService.UserDirectoryPage> list(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiEnvelope.success(service.listUsers(actor, page, size));
    }
}
