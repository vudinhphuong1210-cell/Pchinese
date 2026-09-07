package net.pchinese.auth.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_idempotency")
public class RefreshIdempotencyEntity {
    @Id private UUID refreshIdempotencyId;
    private UUID sessionId;
    private UUID sourceRefreshTokenId;
    private UUID refreshRequestId;
    private byte[] responseCiphertext;
    private Instant expiresAt;
    private Instant createdAt;
    protected RefreshIdempotencyEntity() { }
    public static RefreshIdempotencyEntity create(UUID sessionId, UUID sourceId, UUID requestId, byte[] ciphertext, Instant now) {
        RefreshIdempotencyEntity entry = new RefreshIdempotencyEntity(); entry.refreshIdempotencyId = UUID.randomUUID();
        entry.sessionId = sessionId; entry.sourceRefreshTokenId = sourceId; entry.refreshRequestId = requestId;
        entry.responseCiphertext = ciphertext; entry.createdAt = now; entry.expiresAt = now.plusSeconds(30); return entry;
    }
    public byte[] getResponseCiphertext() { return responseCiphertext; }
    public boolean isLive(Instant now) { return expiresAt.isAfter(now); }
}
