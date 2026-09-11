package net.pchinese.shadowing.persistence;

import jakarta.persistence.*;
import net.pchinese.shadowing.domain.RecordingStatus;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recordings")
public class RecordingEntity {

    @Id
    @Column(name = "recording_id", nullable = false)
    private UUID recordingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "classification", nullable = false, length = 32)
    private String classification = "ASSESSMENT_ONLY";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RecordingStatus status = RecordingStatus.AVAILABLE;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider = "LOCAL";

    @Column(name = "object_key_ciphertext")
    private byte[] objectKeyCiphertext;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "duration_milliseconds")
    private Integer durationMilliseconds;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "malware_scan_status", nullable = false, length = 16)
    private String malwareScanStatus = "CLEAN";

    @Column(name = "assessment_succeeded_at")
    private Instant assessmentSucceededAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public RecordingEntity() {
    }

    public static RecordingEntity create(UUID userId, String mimeType, long byteSize, String checksumSha256, Integer durationMs, byte[] objectKeyCiphertext) {
        RecordingEntity entity = new RecordingEntity();
        entity.recordingId = UUID.randomUUID();
        entity.userId = userId;
        entity.classification = "ASSESSMENT_ONLY";
        entity.status = RecordingStatus.AVAILABLE;
        entity.storageProvider = "LOCAL";
        entity.mimeType = mimeType;
        entity.byteSize = byteSize;
        entity.checksumSha256 = checksumSha256;
        entity.durationMilliseconds = durationMs;
        entity.objectKeyCiphertext = objectKeyCiphertext;
        entity.malwareScanStatus = "CLEAN";
        Instant now = Instant.now();
        entity.expiresAt = now.plus(Duration.ofDays(7));
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void markSucceeded(Instant now) {
        this.assessmentSucceededAt = now;
        this.updatedAt = now;
    }

    public UUID getRecordingId() { return recordingId; }
    public UUID getUserId() { return userId; }
    public String getClassification() { return classification; }
    public RecordingStatus getStatus() { return status; }
    public String getStorageProvider() { return storageProvider; }
    public byte[] getObjectKeyCiphertext() { return objectKeyCiphertext; }
    public String getMimeType() { return mimeType; }
    public long getByteSize() { return byteSize; }
    public Integer getDurationMilliseconds() { return durationMilliseconds; }
    public String getChecksumSha256() { return checksumSha256; }
    public String getMalwareScanStatus() { return malwareScanStatus; }
    public Instant getAssessmentSucceededAt() { return assessmentSucceededAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
