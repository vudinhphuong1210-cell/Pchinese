package net.pchinese.users.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.auth.application.SessionLifecycleService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.domain.AccessReason;
import net.pchinese.users.domain.RoleCode;
import net.pchinese.users.domain.UserStatus;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleEntity;
import net.pchinese.users.persistence.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountRoleService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final SessionLifecycleService sessions;
    private final AuthAuditService audit;
    public AccountRoleService(UserRepository users, UserRoleRepository roles, SessionLifecycleService sessions, AuthAuditService audit) {
        this.users = users; this.roles = roles; this.sessions = sessions; this.audit = audit;
    }

    @Transactional
    public AccountRoleProjection getProjection(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        UserEntity target = findManageable(targetId, actor.userId());
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection grantAdmin(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (target.getStatus() != UserStatus.ACTIVE) return rejectConflict(actor, target, "ROLE_GRANTED", now);
        Set<String> before = roles.findActiveRoleCodesByUserId(targetId);
        if (before.contains(RoleCode.ADMIN.name())) return rejectConflict(actor, target, "ROLE_GRANTED", now);
        UserEntity actorUser = users.findLockedById(actor.userId()).orElseThrow(ApiException::unauthenticated);
        roles.save(UserRoleEntity.grant(target, actorUser, UUID.fromString(net.pchinese.common.api.CorrelationId.current()), now));
        target.incrementAuthzVersion(now);
        sessions.revokeAllForUser(targetId, "ROLE_CHANGED", now);
        audit.record("ROLE_GRANTED", actor.userId(), targetId, null, audit.roles(before), audit.roles(Set.of("ADMIN")), audit.details("ACCEPTED"), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection revokeAdmin(UserPrincipal actor, UUID targetId) {
        ensureAdmin(actor);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId())) return rejectConflict(actor, target, "ROLE_REVOKED", now);
        var activeGrants = roles.findActiveLocked(targetId, RoleCode.ADMIN);
        Set<String> before = roles.findActiveRoleCodesByUserId(targetId);
        if (activeGrants.isEmpty() || (target.getStatus() == UserStatus.ACTIVE
                && roles.countActiveRolesForStatus(RoleCode.ADMIN, UserStatus.ACTIVE) <= 1)) {
            return rejectConflict(actor, target, "ROLE_REVOKED", now);
        }
        activeGrants.forEach(grant -> grant.revoke(now));
        target.incrementAuthzVersion(now);
        sessions.revokeAllForUser(targetId, "ROLE_CHANGED", now);
        audit.record("ROLE_REVOKED", actor.userId(), targetId, null, audit.roles(before), audit.roles(Set.of()), audit.details("ACCEPTED"), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection lock(UserPrincipal actor, UUID targetId, AccessReason reason, String note) {
        ensureAdmin(actor);
        String safeNote = validateReason(reason, note);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId()) || target.getStatus() != UserStatus.ACTIVE
                || (roles.findActiveRoleCodesByUserId(targetId).contains("ADMIN")
                && roles.countActiveRolesForStatus(RoleCode.ADMIN, UserStatus.ACTIVE) <= 1)) {
            return rejectConflict(actor, target, "ACCOUNT_LOCKED", now, reason, safeNote);
        }
        target.lock(now);
        sessions.revokeAllForUser(targetId, "ACCOUNT_LOCKED", now);
        audit.record("ACCOUNT_LOCKED", actor.userId(), targetId, null, null, null, details("ACCEPTED", reason, safeNote), now);
        return projection(target);
    }

    @Transactional
    public AccountRoleProjection unlock(UserPrincipal actor, UUID targetId, AccessReason reason, String note) {
        ensureAdmin(actor);
        String safeNote = validateReason(reason, note);
        Instant now = Instant.now();
        UserEntity target = findManageable(targetId, actor.userId());
        if (targetId.equals(actor.userId()) || target.getStatus() != UserStatus.LOCKED) {
            return rejectConflict(actor, target, "ACCOUNT_UNLOCKED", now, reason, safeNote);
        }
        target.unlock(now);
        audit.record("ACCOUNT_UNLOCKED", actor.userId(), targetId, null, null, null, details("ACCEPTED", reason, safeNote), now);
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
            audit.record("ACCOUNT_COMMAND_REJECTED", actorId, null, null, null, null, audit.details("REJECTED"), Instant.now());
            throw ApiException.notFound();
        }
        return target;
    }
    private AccountRoleProjection projection(UserEntity target) {
        return new AccountRoleProjection(target.getUserId(), roles.findActiveRoleCodesByUserId(target.getUserId()), target.getStatus().name());
    }
    private void ensureAdmin(UserPrincipal actor) { if (actor == null || !actor.isAdmin()) throw ApiException.forbidden(); }
    private AccountRoleProjection rejectConflict(UserPrincipal actor, UserEntity target, String event, Instant now) {
        audit.record(event, actor.userId(), target.getUserId(), null, null, null, audit.details("REJECTED"), now);
        throw ApiException.conflict("The requested account transition is not allowed.");
    }
    private AccountRoleProjection rejectConflict(UserPrincipal actor, UserEntity target, String event, Instant now, AccessReason reason, String note) {
        audit.record(event, actor.userId(), target.getUserId(), null, null, null, details("REJECTED", reason, note), now);
        throw ApiException.conflict("The requested account transition is not allowed.");
    }
    private ObjectNode details(String outcome, AccessReason reason, String note) {
        ObjectNode details = audit.details(outcome); details.put("reason", reason.name());
        if (note != null) details.put("note", note); return details;
    }

    public record AccountRoleProjection(UUID userId, Set<String> roles, String accessState) { }
}
