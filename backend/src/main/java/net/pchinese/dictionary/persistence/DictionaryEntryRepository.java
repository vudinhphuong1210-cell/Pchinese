package net.pchinese.dictionary.persistence;

import net.pchinese.content.domain.PublicationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntryEntity, UUID> {

    Optional<DictionaryEntryEntity> findByDictionaryEntryIdAndPublicationState(
            UUID dictionaryEntryId,
            PublicationState publicationState
    );
}
