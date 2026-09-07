package net.pchinese.users.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.pchinese.users.domain.UserStatus;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "email_ciphertext", nullable = false)
    private byte[] emailCiphertext;
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "email_lookup_hash", nullable = false, unique = true, length = 64)
    private String emailLookupHash;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    private Instant emailVerifiedAt;
    @Column(nullable = false)
    private long authzVersion;
    @Column(nullable = false)
    private Instant lastActivityAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected UserEntity() { }

    public static UserEntity pending(byte[] encryptedEmail, String lookupHash, String passwordHash, Instant now) {
        UserEntity user = new UserEntity();
        user.userId = UUID.randomUUID();
        user.emailCiphertext = encryptedEmail;
        user.emailLookupHash = lookupHash;
        user.passwordHash = passwordHash;
        user.status = UserStatus.PENDING_VERIFICATION;
        user.authzVersion = 1;
        user.lastActivityAt = now;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public UUID getUserId() { return userId; }
    public byte[] getEmailCiphertext() { return emailCiphertext; }
    public String getEmailLookupHash() { return emailLookupHash; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public long getAuthzVersion() { return authzVersion; }
    public boolean isActiveVerified() { return status == UserStatus.ACTIVE && emailVerifiedAt != null; }
    public void activate(Instant now) { status = UserStatus.ACTIVE; emailVerifiedAt = now; touch(now); }
    public void lock(Instant now) { status = UserStatus.LOCKED; incrementAuthzVersion(now); }
    public void unlock(Instant now) { status = UserStatus.ACTIVE; touch(now); }
    public void replacePassword(String hash, Instant now) { passwordHash = hash; incrementAuthzVersion(now); }
    public void incrementAuthzVersion(Instant now) { authzVersion++; touch(now); }
    public void touch(Instant now) { lastActivityAt = now; updatedAt = now; }
}
