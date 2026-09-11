package net.pchinese.vocabulary;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import net.pchinese.review.application.SrsScheduleCommands;
import net.pchinese.vocabulary.application.PersonalNoteCipherService;
import net.pchinese.vocabulary.application.SavedWordService;
import net.pchinese.vocabulary.application.VocabularyCapacityCommands;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import net.pchinese.vocabulary.persistence.SavedWordEntity;
import net.pchinese.vocabulary.persistence.SavedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedWordServiceTest {

    @Mock
    private SavedWordRepository savedWordRepository;

    @Mock
    private DictionaryEntryRepository dictionaryEntryRepository;

    @Mock
    private PersonalNoteCipherService noteCipherService;

    @Mock
    private VocabularyCapacityCommands capacityCommands;

    @Mock
    private SrsScheduleCommands srsScheduleCommands;

    private SavedWordService savedWordService;

    private final UUID userId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();
    private final UUID savedWordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        savedWordService = new SavedWordService(
                savedWordRepository,
                dictionaryEntryRepository,
                noteCipherService,
                capacityCommands,
                srsScheduleCommands
        );
    }

    @Test
    void saveWordReturnsExistingActiveWordIfAlreadyActive() {
        DictionaryEntryEntity entry = new DictionaryEntryEntity();
        entry.setDictionaryEntryId(entryId);
        entry.setPublicationState(PublicationState.PUBLISHED);

        SavedWordEntity existing = new SavedWordEntity();
        existing.setSavedWordId(savedWordId);
        existing.setUserId(userId);
        existing.setDictionaryEntryId(entryId);
        existing.setStatus(SavedWordStatus.ACTIVE);
        existing.setSavedAt(Instant.now());

        when(dictionaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(savedWordRepository.findByUserIdAndDictionaryEntryId(userId, entryId)).thenReturn(Optional.of(existing));

        SavedWordService.SavedWordDetailProjection projection = savedWordService.saveWord(userId, entryId, null);

        assertNotNull(projection);
        assertEquals(savedWordId, projection.savedWordId());
        verify(savedWordRepository, never()).save(any());
        verify(capacityCommands, never()).enforceFreeCapacityOnSaveOrRestore(any(), any());
    }

    @Test
    void updateNoteThrowsConflictErrorWhenVersionMismatch() {
        SavedWordEntity existing = new SavedWordEntity();
        existing.setSavedWordId(savedWordId);
        existing.setUserId(userId);
        existing.setVersion(2L);

        when(savedWordRepository.findBySavedWordIdAndUserIdForWrite(savedWordId, userId)).thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(ApiException.class, () ->
                savedWordService.updateNote(userId, savedWordId, "New Note", 1L)
        );

        assertEquals("STATE_CONFLICT", exception.code());
    }
}
