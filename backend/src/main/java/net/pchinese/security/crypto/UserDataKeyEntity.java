package net.pchinese.security.crypto;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_data_keys")
public class UserDataKeyEntity {
    @Id @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "wrapped_user_dek", nullable = false) private byte[] wrappedUserDek;
    @Column(name = "kms_key_reference", nullable = false, length = 255) private String kmsKeyReference;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "destroyed_at") private Instant destroyedAt;
    protected UserDataKeyEntity() { }
    public static UserDataKeyEntity active(UUID userId, byte[] wrappedUserDek, String kmsReference, Instant now) {
        UserDataKeyEntity value = new UserDataKeyEntity(); value.userId = userId; value.wrappedUserDek = wrappedUserDek;
        value.kmsKeyReference = kmsReference; value.status = "ACTIVE"; value.createdAt = now; return value;
    }
    public byte[] getWrappedUserDek() { return wrappedUserDek; } public boolean isActive() { return "ACTIVE".equals(status); }
}
