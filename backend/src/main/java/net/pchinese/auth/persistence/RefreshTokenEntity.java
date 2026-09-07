package net.pchinese.auth.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id private UUID refreshTokenId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id", nullable = false) private AuthSessionEntity session;
    private UUID familyId;
    @JdbcTypeCode(Types.CHAR) private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant rotatedAt;
    private UUID replacedByTokenId;
    private Instant revokedAt;
    private String revokedReason;
    private Instant createdAt;
    @Version private long version;

    protected RefreshTokenEntity() { }
    public static RefreshTokenEntity create(AuthSessionEntity session, String tokenHash, Instant now) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.refreshTokenId = UUID.randomUUID(); token.session = session; token.familyId = session.getFamilyId(); token.tokenHash = tokenHash;
        token.issuedAt = now; token.createdAt = now; token.expiresAt = now.plus(java.time.Duration.ofDays(30));
        return token;
    }
    public UUID getRefreshTokenId() { return refreshTokenId; }
    public AuthSessionEntity getSession() { return session; }
    public UUID getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRotatedAt() { return rotatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isUsable(Instant now) { return revokedAt == null && rotatedAt == null && expiresAt.isAfter(now); }
    public void rotateTo(RefreshTokenEntity replacement, Instant now) { rotatedAt = now; replacedByTokenId = replacement.refreshTokenId; }
    public void revoke(String reason, Instant now) { if (revokedAt == null) { revokedAt = now; revokedReason = reason; } }
}
