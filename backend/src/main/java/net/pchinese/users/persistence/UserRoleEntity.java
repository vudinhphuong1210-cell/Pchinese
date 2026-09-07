package net.pchinese.users.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.pchinese.users.domain.RoleCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {
    @Id private UUID userRoleGrantId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Enumerated(EnumType.STRING) private RoleCode roleCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "granted_by_user_id") private UserEntity grantedByUser;
    private Instant grantedAt;
    private Instant revokedAt;
    private UUID correlationId;

    protected UserRoleEntity() { }
    public static UserRoleEntity grant(UserEntity user, UserEntity grantedBy, UUID correlationId, Instant now) {
        UserRoleEntity role = new UserRoleEntity();
        role.userRoleGrantId = UUID.randomUUID(); role.user = user; role.grantedByUser = grantedBy;
        role.roleCode = RoleCode.ADMIN; role.correlationId = correlationId; role.grantedAt = now;
        return role;
    }
    public UUID getUserRoleGrantId() { return userRoleGrantId; }
    public UserEntity getUser() { return user; }
    public RoleCode getRoleCode() { return roleCode; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isActive() { return revokedAt == null; }
    public void revoke(Instant now) { revokedAt = now; }
}
