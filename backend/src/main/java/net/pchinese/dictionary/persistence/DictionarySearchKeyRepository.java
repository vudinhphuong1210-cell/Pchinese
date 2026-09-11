package net.pchinese.dictionary.persistence;

import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.domain.QueryKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DictionarySearchKeyRepository extends JpaRepository<DictionarySearchKeyEntity, UUID> {

    @Query("""
        SELECT e FROM DictionarySearchKeyEntity k
        JOIN DictionaryEntryEntity e ON k.dictionaryEntryId = e.dictionaryEntryId
        WHERE k.normalizedKey = :normalizedKey
          AND e.publicationState = :publicationState
        ORDER BY k.matchRank ASC, e.dictionaryEntryId ASC
    """)
    Page<DictionaryEntryEntity> searchPublishedEntriesExactKey(
            @Param("normalizedKey") String normalizedKey,
            @Param("publicationState") PublicationState publicationState,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM DictionarySearchKeyEntity k
        JOIN DictionaryEntryEntity e ON k.dictionaryEntryId = e.dictionaryEntryId
        WHERE k.queryKind = :queryKind
          AND k.normalizedKey = :normalizedKey
          AND e.publicationState = :publicationState
        ORDER BY k.matchRank ASC, e.dictionaryEntryId ASC
    """)
    Page<DictionaryEntryEntity> searchPublishedEntriesByKindAndKey(
            @Param("queryKind") QueryKind queryKind,
            @Param("normalizedKey") String normalizedKey,
            @Param("publicationState") PublicationState publicationState,
            Pageable pageable
    );

    void deleteByDictionaryEntryId(UUID dictionaryEntryId);

    List<DictionarySearchKeyEntity> findByDictionaryEntryId(UUID dictionaryEntryId);
}
