package net.pchinese.auth.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.pchinese.auth.domain.Platform;
import net.pchinese.users.persistence.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {
    @Id private UUID sessionId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    private UUID familyId;
    private long authzVersion;
    private String deviceId;
    private String deviceLabel;
    @Enumerated(EnumType.STRING) private Platform platform;
    private Instant createdAt;
    private Instant lastSeenAt;
    private Instant idleExpiresAt;
    private Instant absoluteExpiresAt;
    private Instant revokedAt;
    private String revokedReason;
    @Version private long version;

    protected AuthSessionEntity() { }
    public static AuthSessionEntity create(UserEntity user, String deviceId, String deviceLabel, Platform platform, Instant now) {
        AuthSessionEntity session = new AuthSessionEntity();
        session.sessionId = UUID.randomUUID(); session.familyId = UUID.randomUUID(); session.user = user;
        session.authzVersion = user.getAuthzVersion(); session.deviceId = deviceId; session.deviceLabel = deviceLabel;
        session.platform = platform; session.createdAt = now; session.lastSeenAt = now;
        session.idleExpiresAt = now.plus(java.time.Duration.ofDays(7)); session.absoluteExpiresAt = now.plus(java.time.Duration.ofDays(30));
        return session;
    }
    public UUID getSessionId() { return sessionId; }
    public UserEntity getUser() { return user; }
    public UUID getFamilyId() { return familyId; }
    public long getAuthzVersion() { return authzVersion; }
    public String getDeviceLabel() { return deviceLabel; }
    public Platform getPlatform() { return platform; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getAbsoluteExpiresAt() { return absoluteExpiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isActive(Instant now) { return revokedAt == null && absoluteExpiresAt.isAfter(now) && idleExpiresAt.isAfter(now); }
    public void seen(Instant now) { lastSeenAt = now; }
    public void revoke(String reason, Instant now) { if (revokedAt == null) { revokedAt = now; revokedReason = reason; } }
}
