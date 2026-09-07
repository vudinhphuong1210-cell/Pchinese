package net.pchinese.users;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.auth.application.SessionLifecycleService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.application.AccountRoleService;
import net.pchinese.users.domain.AccessReason;
import net.pchinese.users.domain.RoleCode;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRoleServiceTest {
    @Mock UserRepository users;
    @Mock UserRoleRepository roles;
    @Mock SessionLifecycleService sessions;
    @Mock AuthAuditService audit;
    @InjectMocks AccountRoleService service;

    @Test
    void otherReasonRequiresABoundedSafeNote() {
        assertThrows(ApiException.class, () -> AccountRoleService.validateReason(AccessReason.OTHER, " "));
        assertThrows(ApiException.class, () -> AccountRoleService.validateReason(AccessReason.OTHER, "token=secret"));
        assertThrows(ApiException.class, () -> AccountRoleService.validateReason(AccessReason.OTHER, "x".repeat(281)));
        assertEquals("Security review", AccountRoleService.validateReason(AccessReason.OTHER, " Security review "));
        assertEquals(null, AccountRoleService.validateReason(AccessReason.SECURITY, null));
    }

    @Test
    void grantingAdminWritesRoleHistoryIncrementsAuthzAndRevokesTargetSessions() {
        UserEntity actor = activeUser("actor");
        UserEntity target = activeUser("target");
        UserPrincipal principal = new UserPrincipal(actor.getUserId(), UUID.randomUUID(), actor.getAuthzVersion(), Set.of("ADMIN"));
        when(users.findLockedById(target.getUserId())).thenReturn(Optional.of(target));
        when(users.findLockedById(actor.getUserId())).thenReturn(Optional.of(actor));
        when(roles.findActiveRoleCodesByUserId(target.getUserId())).thenReturn(Set.of());
        when(audit.details(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(audit.roles(any())).thenReturn(JsonNodeFactory.instance.arrayNode());
        long beforeVersion = target.getAuthzVersion();

        service.grantAdmin(principal, target.getUserId());

        verify(roles).save(any());
        verify(sessions).revokeAllForUser(eq(target.getUserId()), eq("ROLE_CHANGED"), any());
        verify(audit).record(eq("ROLE_GRANTED"), eq(actor.getUserId()), eq(target.getUserId()), eq(null), any(), any(), any(), any());
        assertTrue(target.getAuthzVersion() > beforeVersion);
    }

    private UserEntity activeUser(String prefix) {
        UserEntity user = UserEntity.pending(new byte[] {1}, (prefix + "0".repeat(64)).substring(0, 64), "hash", Instant.now());
        user.activate(Instant.now());
        return user;
    }
}
