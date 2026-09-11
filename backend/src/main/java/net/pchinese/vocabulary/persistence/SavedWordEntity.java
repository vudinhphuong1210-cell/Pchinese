package net.pchinese.vocabulary.persistence;

import jakarta.persistence.*;
import net.pchinese.vocabulary.domain.SavedWordStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_words", uniqueConstraints = {
        @UniqueConstraint(name = "uq_saved_words_user_entry", columnNames = {"user_id", "dictionary_entry_id"})
})
public class SavedWordEntity {

    @Id
    @Column(name = "saved_word_id", nullable = false)
    private UUID savedWordId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "dictionary_entry_id", nullable = false)
    private UUID dictionaryEntryId;

    @Column(name = "personal_note_ciphertext", columnDefinition = "bytea")
    private byte[] personalNoteCiphertext;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SavedWordStatus status = SavedWordStatus.ACTIVE;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public SavedWordEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (savedWordId == null) {
            savedWordId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (savedAt == null) {
            savedAt = Instant.now();
        }
        if (status == null) {
            status = SavedWordStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getSavedWordId() {
        return savedWordId;
    }

    public void setSavedWordId(UUID savedWordId) {
        this.savedWordId = savedWordId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getDictionaryEntryId() {
        return dictionaryEntryId;
    }

    public void setDictionaryEntryId(UUID dictionaryEntryId) {
        this.dictionaryEntryId = dictionaryEntryId;
    }

    public byte[] getPersonalNoteCiphertext() {
        return personalNoteCiphertext;
    }

    public void setPersonalNoteCiphertext(byte[] personalNoteCiphertext) {
        this.personalNoteCiphertext = personalNoteCiphertext;
    }

    public SavedWordStatus getStatus() {
        return status;
    }

    public void setStatus(SavedWordStatus status) {
        this.status = status;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
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
