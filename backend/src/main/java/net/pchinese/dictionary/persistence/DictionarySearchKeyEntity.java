package net.pchinese.dictionary.persistence;

import jakarta.persistence.*;
import net.pchinese.dictionary.domain.QueryKind;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dictionary_search_keys")
public class DictionarySearchKeyEntity {

    @Id
    @Column(name = "dictionary_search_key_id", nullable = false)
    private UUID dictionarySearchKeyId;

    @Column(name = "dictionary_entry_id", nullable = false)
    private UUID dictionaryEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_kind", nullable = false, length = 30)
    private QueryKind queryKind;

    @Column(name = "normalized_key", nullable = false, length = 200)
    private String normalizedKey;

    @Column(name = "match_rank", nullable = false, columnDefinition = "smallint")
    private Short matchRank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DictionarySearchKeyEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (dictionarySearchKeyId == null) {
            dictionarySearchKeyId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getDictionarySearchKeyId() {
        return dictionarySearchKeyId;
    }

    public void setDictionarySearchKeyId(UUID dictionarySearchKeyId) {
        this.dictionarySearchKeyId = dictionarySearchKeyId;
    }

    public UUID getDictionaryEntryId() {
        return dictionaryEntryId;
    }

    public void setDictionaryEntryId(UUID dictionaryEntryId) {
        this.dictionaryEntryId = dictionaryEntryId;
    }

    public QueryKind getQueryKind() {
        return queryKind;
    }

    public void setQueryKind(QueryKind queryKind) {
        this.queryKind = queryKind;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public void setNormalizedKey(String normalizedKey) {
        this.normalizedKey = normalizedKey;
    }

    public Short getMatchRank() {
        return matchRank;
    }

    public void setMatchRank(Short matchRank) {
        this.matchRank = matchRank;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
