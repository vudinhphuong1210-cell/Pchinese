package net.pchinese.users.application;

import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.auth.application.AuditEventTaxonomy;
import net.pchinese.auth.application.SessionLifecycleService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.domain.AccessReason;
import net.pchinese.users.domain.RoleCode;
import net.pchinese.users.domain.UserStatus;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleEntity;
import net.pchinese.users.persistence.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountRoleService {
    private static final String SYSTEM_ACCOUNT_NAME = "Hệ thống";
    private static final String UNAVAILABLE_ACCOUNT_NAME = "Không khả dụng";
    private static final String MISSING_ACCOUNT_NAME = "Chưa đặt tên";
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final SessionLifecycleService sessions;
    private final AuthAuditService audit;
    private final SensitiveValueService sensitiveValues;

    public AccountRoleService(UserRepository users, UserRoleRepository roles, SessionLifecycleService sessions,
                              AuthAuditService audit, SensitiveValueService sensitiveValues) {
        this.users = users; this.roles = roles; this.sessions = sessions; this.audit = audit; this.sensitiveValues = sensitiveValues;
    }

    @Transactional
    public AccountRoleProjection getProjection(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        UserEntity target = findManageable(targetId, actor.userId());
        return projection(target);
    }

    @Transactional(readOnly = true)
    public UserDirectoryPage listUsers(UserPrincipal actor, int page, int size) {
        ensureAdmin(actor);
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("userId")));
        Page<UserRepository.AdminUserSummary> result = users.findAllBy(request);
        List<UserDirectorySummary> items = result.getContent().stream()
                .map(user -> new UserDirectorySummary(
                        user.getUserId(),
                        accountName(user.getDisplayNameCiphertext()),
                        user.getStatus().name(),
                        roles.findActiveRoleCodesByUserId(user.getUserId())))
                .toList();
        return new UserDirectoryPage(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SystemActivityPage listSystemActivity(UserPrincipal actor, int page, int size) {
        ensureAdmin(actor);
        AuthAuditService.SystemActivityPage auditPage = audit.systemActivity(page, size);
        Set<UUID> accountIds = new HashSet<>();
        auditPage.items().forEach(item -> {
            if (item.actorUserId() != null) accountIds.add(item.actorUserId());
            if (item.targetUserId() != null) accountIds.add(item.targetUserId());
        });
        Map<UUID, UserEntity> accountsById = new HashMap<>();
        users.findAllById(accountIds).forEach(account -> accountsById.put(account.getUserId(), account));
        List<SystemActivityItem> items = auditPage.items().stream()
                .map(item -> new SystemActivityItem(item.eventType(),
                        activityAccountName(item.actorUserId(), accountsById),
                        activityAccountName(item.targetUserId(), accountsById), item.occurredAt()))
                .toList();
        return new SystemActivityPage(items, auditPage.page(), auditPage.size(), auditPage.totalItems(), auditPage.totalPages());
    }

    @Transactional
    public AccountRoleProjection grantAdmin(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (target.getStatus() != UserStatus.ACTIVE) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ROLE_GRANTED, now);
        }
        Set<String> before = roles.findActiveRoleCodesByUserId(targetId);
        if (before.contains(RoleCode.ADMIN.name())) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ROLE_GRANTED, now);
        }
        UserEntity actorUser = users.findLockedById(actor.userId()).orElseThrow(ApiException::unauthenticated);
        roles.save(UserRoleEntity.grant(target, actorUser, UUID.fromString(net.pchinese.common.api.CorrelationId.current()), now));
        target.incrementAuthzVersion(now);
        sessions.revokeAllForUser(targetId, "ROLE_CHANGED", now);
        audit.record(AuditEventTaxonomy.EventType.ROLE_GRANTED, actor.userId(), targetId, null,
                audit.roles(before), audit.roles(Set.of("ADMIN")), audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection revokeAdmin(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId())) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ROLE_REVOKED, now);
        }
        var activeGrants = roles.findActiveLocked(targetId, RoleCode.ADMIN);
        Set<String> before = roles.findActiveRoleCodesByUserId(targetId);
        if (activeGrants.isEmpty() || (target.getStatus() == UserStatus.ACTIVE
                && roles.countActiveRolesForStatus(RoleCode.ADMIN, UserStatus.ACTIVE) <= 1)) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ROLE_REVOKED, now);
        }
        activeGrants.forEach(grant -> grant.revoke(now));
        target.incrementAuthzVersion(now);
        sessions.revokeAllForUser(targetId, "ROLE_CHANGED", now);
        audit.record(AuditEventTaxonomy.EventType.ROLE_REVOKED, actor.userId(), targetId, null,
                audit.roles(before), audit.roles(Set.of()), audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection lock(UserPrincipal actor, UUID targetId, AccessReason reason, String note) {
        ensureAdmin(actor);
        validateReason(reason, note);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId()) || target.getStatus() != UserStatus.ACTIVE
                || (roles.findActiveRoleCodesByUserId(targetId).contains("ADMIN")
                && roles.countActiveRolesForStatus(RoleCode.ADMIN, UserStatus.ACTIVE) <= 1)) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ACCOUNT_LOCKED, now, reason);
        }
        target.lock(now);
        sessions.revokeAllForUser(targetId, "ACCOUNT_LOCKED", now);
        audit.record(AuditEventTaxonomy.EventType.ACCOUNT_LOCKED, actor.userId(), targetId, null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS, reasonCode(reason)), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection unlock(UserPrincipal actor, UUID targetId, AccessReason reason, String note) {
        ensureAdmin(actor);
        validateReason(reason, note);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId()) || target.getStatus() != UserStatus.LOCKED) {
            return rejectConflict(actor, target, AuditEventTaxonomy.EventType.ACCOUNT_UNLOCKED, now, reason);
        }
        target.unlock(now);
        audit.record(AuditEventTaxonomy.EventType.ACCOUNT_UNLOCKED, actor.userId(), targetId, null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS, reasonCode(reason)), now);
        return projection(target);
    }

    public static String validateReason(AccessReason reason, String note) {
        if (reason == null) throw ApiException.validation("A valid access reason is required.");
        String normalized = note == null ? null : note.trim();
        if (reason == AccessReason.OTHER && (normalized == null || normalized.isEmpty())) {
            throw ApiException.validation("A short note is required when reason is OTHER.");
        }
        if (normalized != null && (normalized.length() > 280 || normalized.contains("\n") || normalized.contains("\r")
                || normalized.matches("(?i).*\\b(password|token|bearer)\\b.*"))) {
            throw ApiException.validation("The access note is not safe.");
        }
        return normalized;
    }

    private UserEntity findManageable(UUID targetId, UUID actorId) {
        UserEntity target = users.findLockedById(targetId).orElse(null);
        if (target == null || (target.getStatus() != UserStatus.ACTIVE && target.getStatus() != UserStatus.LOCKED)) {
            audit.record(AuditEventTaxonomy.EventType.ACCOUNT_COMMAND_REJECTED, actorId, null, null, null, null,
                    audit.details(AuditEventTaxonomy.OutcomeCode.DENIED), Instant.now());
            throw ApiException.notFound();
        }
        return target;
    }
    private AccountRoleProjection projection(UserEntity target) {
        return new AccountRoleProjection(target.getUserId(), accountName(target.getDisplayNameCiphertext()),
                roles.findActiveRoleCodesByUserId(target.getUserId()), target.getStatus().name());
    }
    private String accountName(byte[] encryptedDisplayName) {
        if (encryptedDisplayName == null || encryptedDisplayName.length == 0) {
            return MISSING_ACCOUNT_NAME;
        }
        String decryptedName = sensitiveValues.decryptToString(encryptedDisplayName);
        return decryptedName == null || decryptedName.isBlank() ? MISSING_ACCOUNT_NAME : decryptedName;
    }
    private String activityAccountName(UUID userId, Map<UUID, UserEntity> accountsById) {
        if (userId == null) return SYSTEM_ACCOUNT_NAME;
        UserEntity account = accountsById.get(userId);
        return account == null ? UNAVAILABLE_ACCOUNT_NAME : accountName(account.getDisplayNameCiphertext());
    }
    private void ensureAdmin(UserPrincipal actor) { if (actor == null || !actor.isAdmin()) throw ApiException.forbidden(); }
    private AccountRoleProjection rejectConflict(UserPrincipal actor, UserEntity target, AuditEventTaxonomy.EventType event, Instant now) {
        audit.record(event, actor.userId(), target.getUserId(), null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.DENIED), now);
        throw ApiException.conflict("The requested account transition is not allowed.");
    }
    private AccountRoleProjection rejectConflict(UserPrincipal actor, UserEntity target, AuditEventTaxonomy.EventType event, Instant now,
                                                  AccessReason reason) {
        audit.record(event, actor.userId(), target.getUserId(), null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.DENIED, reasonCode(reason)), now);
        throw ApiException.conflict("The requested account transition is not allowed.");
    }
    private AuditEventTaxonomy.ReasonCode reasonCode(AccessReason reason) {
        return AuditEventTaxonomy.ReasonCode.valueOf(reason.name());
    }

    public record AccountRoleProjection(UUID userId, String accountName, Set<String> roles, String accessState) { }
    public record UserDirectorySummary(UUID userId, String accountName, String accountState, Set<String> roles) { }
    public record UserDirectoryPage(List<UserDirectorySummary> items, int page, int size, long totalItems, int totalPages) { }
    public record SystemActivityItem(String eventType, String actorAccountName, String targetAccountName, Instant occurredAt) { }
    public record SystemActivityPage(List<SystemActivityItem> items, int page, int size, long totalItems, int totalPages) { }
}
