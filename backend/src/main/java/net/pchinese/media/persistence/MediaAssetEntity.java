package net.pchinese.media.persistence;

import jakarta.persistence.*;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.domain.MediaKind;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAssetEntity {

    @Id
    @Column(name = "media_asset_id", nullable = false)
    private UUID mediaAssetId;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "provider_asset_identifier", nullable = false, length = 255)
    private String providerAssetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false, length = 50)
    private MediaKind mediaKind;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "duration_milliseconds")
    private Integer durationMilliseconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_SCAN;

    @Column(name = "malware_scan_status", nullable = false, length = 50)
    private String malwareScanStatus = "CLEAN";

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public MediaAssetEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.PENDING_SCAN;
        }
        if (malwareScanStatus == null) {
            malwareScanStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getMediaAssetId() {
        return mediaAssetId;
    }

    public void setMediaAssetId(UUID mediaAssetId) {
        this.mediaAssetId = mediaAssetId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderAssetIdentifier() {
        return providerAssetIdentifier;
    }

    public void setProviderAssetIdentifier(String providerAssetIdentifier) {
        this.providerAssetIdentifier = providerAssetIdentifier;
    }

    public MediaKind getMediaKind() {
        return mediaKind;
    }

    public void setMediaKind(MediaKind mediaKind) {
        this.mediaKind = mediaKind;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public void setDurationMilliseconds(Integer durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getMalwareScanStatus() {
        return malwareScanStatus;
    }

    public void setMalwareScanStatus(String malwareScanStatus) {
        this.malwareScanStatus = malwareScanStatus;
    }

    public UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(UUID approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
