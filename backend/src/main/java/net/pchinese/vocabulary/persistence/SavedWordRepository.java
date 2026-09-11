package net.pchinese.vocabulary.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedWordRepository extends JpaRepository<SavedWordEntity, UUID> {

    Optional<SavedWordEntity> findByUserIdAndDictionaryEntryId(UUID userId, UUID dictionaryEntryId);

    Optional<SavedWordEntity> findBySavedWordIdAndUserId(UUID savedWordId, UUID userId);

    Page<SavedWordEntity> findByUserIdAndStatus(UUID userId, SavedWordStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, SavedWordStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SavedWordEntity s WHERE s.savedWordId = :savedWordId AND s.userId = :userId")
    Optional<SavedWordEntity> findBySavedWordIdAndUserIdForWrite(
            @Param("savedWordId") UUID savedWordId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SavedWordEntity s WHERE s.userId = :userId AND s.status = :status ORDER BY s.savedAt ASC, s.savedWordId ASC")
    List<SavedWordEntity> findActiveForCapacityLock(
            @Param("userId") UUID userId,
            @Param("status") SavedWordStatus status
    );

    @Query("SELECT s FROM SavedWordEntity s WHERE s.userId = :userId AND s.status = :status ORDER BY s.savedAt ASC, s.savedWordId ASC")
    List<SavedWordEntity> findByUserIdAndStatusOrderedByRecencyAsc(
            @Param("userId") UUID userId,
            @Param("status") SavedWordStatus status
    );

    @Query("SELECT s FROM SavedWordEntity s WHERE s.userId = :userId AND s.status = :status ORDER BY s.savedAt DESC, s.savedWordId DESC")
    List<SavedWordEntity> findByUserIdAndStatusOrderedByRecencyDesc(
            @Param("userId") UUID userId,
            @Param("status") SavedWordStatus status
    );
}
