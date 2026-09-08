package net.pchinese.users;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.auth.application.AuditEventTaxonomy;
import net.pchinese.auth.application.SessionLifecycleService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AccountRoleServiceTest {
    @Mock UserRepository users;
    @Mock UserRoleRepository roles;
    @Mock SessionLifecycleService sessions;
    @Mock AuthAuditService audit;
    @Mock SensitiveValueService sensitiveValues;
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
        when(audit.details(any(AuditEventTaxonomy.OutcomeCode.class))).thenReturn(JsonNodeFactory.instance.objectNode());
        when(audit.roles(any())).thenReturn(JsonNodeFactory.instance.arrayNode());
        long beforeVersion = target.getAuthzVersion();

        service.grantAdmin(principal, target.getUserId());

        verify(roles).save(any());
        verify(sessions).revokeAllForUser(eq(target.getUserId()), eq("ROLE_CHANGED"), any());
        verify(audit).record(eq(AuditEventTaxonomy.EventType.ROLE_GRANTED), eq(actor.getUserId()), eq(target.getUserId()), eq(null), any(), any(), any(), any());
        assertTrue(target.getAuthzVersion() > beforeVersion);
    }

    @Test
    void listingUsersReturnsOnlySafeSummariesAndUsesServerPagination() {
        UserEntity actor = activeUser("directory-actor");
        UUID targetId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(actor.getUserId(), UUID.randomUUID(), actor.getAuthzVersion(), Set.of("ADMIN"));
        byte[] encryptedAccountName = new byte[] {7, 8, 9};
        UserRepository.AdminUserSummary summary = new UserRepository.AdminUserSummary() {
            @Override public UUID getUserId() { return targetId; }
            @Override public net.pchinese.users.domain.UserStatus getStatus() { return net.pchinese.users.domain.UserStatus.ACTIVE; }
            @Override public byte[] getDisplayNameCiphertext() { return encryptedAccountName; }
        };
        when(users.findAllBy(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
        when(roles.findActiveRoleCodesByUserId(targetId)).thenReturn(Set.of("ADMIN"));
        when(sensitiveValues.decryptToString(encryptedAccountName)).thenReturn("Học viên Demo");

        AccountRoleService.UserDirectoryPage page = service.listUsers(principal, 0, 20);

        assertEquals(0, page.page());
        assertEquals(20, page.size());
        assertEquals(1, page.totalItems());
        assertEquals(1, page.totalPages());
        assertEquals(targetId, page.items().getFirst().userId());
        assertEquals("Học viên Demo", page.items().getFirst().accountName());
        assertEquals("ACTIVE", page.items().getFirst().accountState());
        assertEquals(Set.of("ADMIN"), page.items().getFirst().roles());
        verify(users).findAllBy(any(Pageable.class));
    }

    @Test
    void listingUsersUsesNeutralNameWhenTheOwnerHasNotSetOne() {
        UserEntity actor = activeUser("directory-actor");
        UUID targetId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(actor.getUserId(), UUID.randomUUID(), actor.getAuthzVersion(), Set.of("ADMIN"));
        UserRepository.AdminUserSummary summary = new UserRepository.AdminUserSummary() {
            @Override public UUID getUserId() { return targetId; }
            @Override public net.pchinese.users.domain.UserStatus getStatus() { return net.pchinese.users.domain.UserStatus.ACTIVE; }
            @Override public byte[] getDisplayNameCiphertext() { return null; }
        };
        when(users.findAllBy(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
        when(roles.findActiveRoleCodesByUserId(targetId)).thenReturn(Set.of());

        AccountRoleService.UserDirectoryPage page = service.listUsers(principal, 0, 20);

        assertEquals("Chưa đặt tên", page.items().getFirst().accountName());
    }

    @Test
    void listingSystemActivityIsAdminOnlyAndExposesOnlySafeAccountNames() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserPrincipal admin = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), 0, Set.of("ADMIN"));
        byte[] actorName = new byte[] {1};
        byte[] targetName = new byte[] {2};
        UserEntity actor = mock(UserEntity.class);
        UserEntity target = mock(UserEntity.class);
        when(actor.getUserId()).thenReturn(actorId);
        when(actor.getDisplayNameCiphertext()).thenReturn(actorName);
        when(target.getUserId()).thenReturn(targetId);
        when(target.getDisplayNameCiphertext()).thenReturn(targetName);
        when(users.findAllById(any())).thenReturn(List.of(actor, target));
        when(sensitiveValues.decryptToString(actorName)).thenReturn("Admin actor");
        when(sensitiveValues.decryptToString(targetName)).thenReturn("Learner target");
        when(audit.systemActivity(0, 20)).thenReturn(new AuthAuditService.SystemActivityPage(
                List.of(new AuthAuditService.SystemActivityItem("PROFILE_PREFERENCES_UPDATED", actorId, targetId,
                        Instant.parse("2026-09-09T08:00:00Z"))), 0, 20, 1, 1));

        AccountRoleService.SystemActivityPage page = service.listSystemActivity(admin, 0, 20);

        assertEquals("PROFILE_PREFERENCES_UPDATED", page.items().getFirst().eventType());
        assertEquals("Admin actor", page.items().getFirst().actorAccountName());
        assertEquals("Learner target", page.items().getFirst().targetAccountName());
        assertEquals(1, page.totalItems());
        assertEquals(List.of("eventType", "actorAccountName", "targetAccountName", "occurredAt"),
                Arrays.stream(AccountRoleService.SystemActivityItem.class.getRecordComponents())
                        .map(component -> component.getName()).toList());
        verify(audit).systemActivity(0, 20);
    }

    @Test
    void learnersCannotListSystemActivity() {
        UserPrincipal learner = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), 0, Set.of());

        assertThrows(ApiException.class, () -> service.listSystemActivity(learner, 0, 20));

        verifyNoInteractions(audit);
    }

    private UserEntity activeUser(String prefix) {
        UserEntity user = UserEntity.pending(new byte[] {1}, (prefix + "0".repeat(64)).substring(0, 64), "hash", Instant.now());
        user.activate(Instant.now());
        return user;
    }
}
