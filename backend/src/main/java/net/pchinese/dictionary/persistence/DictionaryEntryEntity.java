package net.pchinese.dictionary.persistence;

import jakarta.persistence.*;
import net.pchinese.content.domain.PublicationState;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dictionary_entries")
public class DictionaryEntryEntity {

    @Id
    @Column(name = "dictionary_entry_id", nullable = false)
    private UUID dictionaryEntryId;

    @Column(name = "simplified_hanzi", nullable = false, length = 100)
    private String simplifiedHanzi;

    @Column(name = "traditional_hanzi", length = 100)
    private String traditionalHanzi;

    @Column(name = "normalized_hanzi", nullable = false, length = 100)
    private String normalizedHanzi;

    @Column(name = "primary_pinyin", nullable = false, length = 150)
    private String primaryPinyin;

    @Column(name = "normalized_pinyin", nullable = false, length = 150)
    private String normalizedPinyin;

    @Column(name = "hsk_level", columnDefinition = "smallint")
    private Short hskLevel;

    @Column(name = "word_type", length = 50)
    private String wordType;

    @Column(name = "senses", nullable = false, columnDefinition = "jsonb")
    private String senses;

    @Column(name = "audio_media_asset_id")
    private UUID audioMediaAssetId;

    @Column(name = "image_media_asset_id")
    private UUID imageMediaAssetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", nullable = false, length = 50)
    private PublicationState publicationState = PublicationState.PUBLISHED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public DictionaryEntryEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (dictionaryEntryId == null) {
            dictionaryEntryId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (publicationState == null) {
            publicationState = PublicationState.PUBLISHED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getDictionaryEntryId() {
        return dictionaryEntryId;
    }

    public void setDictionaryEntryId(UUID dictionaryEntryId) {
        this.dictionaryEntryId = dictionaryEntryId;
    }

    public String getSimplifiedHanzi() {
        return simplifiedHanzi;
    }

    public void setSimplifiedHanzi(String simplifiedHanzi) {
        this.simplifiedHanzi = simplifiedHanzi;
    }

    public String getTraditionalHanzi() {
        return traditionalHanzi;
    }

    public void setTraditionalHanzi(String traditionalHanzi) {
        this.traditionalHanzi = traditionalHanzi;
    }

    public String getNormalizedHanzi() {
        return normalizedHanzi;
    }

    public void setNormalizedHanzi(String normalizedHanzi) {
        this.normalizedHanzi = normalizedHanzi;
    }

    public String getPrimaryPinyin() {
        return primaryPinyin;
    }

    public void setPrimaryPinyin(String primaryPinyin) {
        this.primaryPinyin = primaryPinyin;
    }

    public String getNormalizedPinyin() {
        return normalizedPinyin;
    }

    public void setNormalizedPinyin(String normalizedPinyin) {
        this.normalizedPinyin = normalizedPinyin;
    }

    public Short getHskLevel() {
        return hskLevel;
    }

    public void setHskLevel(Short hskLevel) {
        this.hskLevel = hskLevel;
    }

    public String getWordType() {
        return wordType;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public String getSenses() {
        return senses;
    }

    public void setSenses(String senses) {
        this.senses = senses;
    }

    public UUID getAudioMediaAssetId() {
        return audioMediaAssetId;
    }

    public void setAudioMediaAssetId(UUID audioMediaAssetId) {
        this.audioMediaAssetId = audioMediaAssetId;
    }

    public UUID getImageMediaAssetId() {
        return imageMediaAssetId;
    }

    public void setImageMediaAssetId(UUID imageMediaAssetId) {
        this.imageMediaAssetId = imageMediaAssetId;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public void setPublicationState(PublicationState publicationState) {
        this.publicationState = publicationState;
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
