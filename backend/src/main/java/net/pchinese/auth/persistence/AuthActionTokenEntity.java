package net.pchinese.auth.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.pchinese.auth.domain.ActionTokenPurpose;
import net.pchinese.users.persistence.UserEntity;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_action_tokens")
public class AuthActionTokenEntity {
    @Id private UUID authActionTokenId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @Enumerated(EnumType.STRING) private ActionTokenPurpose purpose;
    @JdbcTypeCode(Types.CHAR) private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant consumedAt;
    private Instant invalidatedAt;
    private Instant createdAt;

    protected AuthActionTokenEntity() { }
    public static AuthActionTokenEntity create(UserEntity user, ActionTokenPurpose purpose, String hash, Instant now) {
        AuthActionTokenEntity token = new AuthActionTokenEntity();
        token.authActionTokenId = UUID.randomUUID(); token.user = user; token.purpose = purpose; token.tokenHash = hash;
        token.issuedAt = now; token.createdAt = now; token.expiresAt = now.plus(java.time.Duration.ofHours(1)); return token;
    }
    public UserEntity getUser() { return user; }
    public ActionTokenPurpose getPurpose() { return purpose; }
    public boolean isUsable(Instant now) { return consumedAt == null && invalidatedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { consumedAt = now; }
    public void invalidate(Instant now) { invalidatedAt = now; }
}
