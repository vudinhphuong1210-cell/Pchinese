package net.pchinese.vocabulary.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import net.pchinese.review.application.SrsScheduleCommands;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import net.pchinese.vocabulary.persistence.SavedWordEntity;
import net.pchinese.vocabulary.persistence.SavedWordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SavedWordService {

    private final SavedWordRepository savedWordRepository;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final PersonalNoteCipherService noteCipherService;
    private final VocabularyCapacityCommands capacityCommands;
    private final SrsScheduleCommands srsScheduleCommands;

    public SavedWordService(
            SavedWordRepository savedWordRepository,
            DictionaryEntryRepository dictionaryEntryRepository,
            PersonalNoteCipherService noteCipherService,
            VocabularyCapacityCommands capacityCommands,
            SrsScheduleCommands srsScheduleCommands
    ) {
        this.savedWordRepository = savedWordRepository;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.noteCipherService = noteCipherService;
        this.capacityCommands = capacityCommands;
        this.srsScheduleCommands = srsScheduleCommands;
    }

    @Transactional
    public SavedWordDetailProjection saveWord(UUID userId, UUID dictionaryEntryId, String notePlaintext) {
        DictionaryEntryEntity entry = dictionaryEntryRepository.findById(dictionaryEntryId)
                .orElseThrow(() -> ApiException.notFound());

        Instant now = Instant.now();
        Optional<SavedWordEntity> existingOpt = savedWordRepository.findByUserIdAndDictionaryEntryId(userId, dictionaryEntryId);

        SavedWordEntity savedWord;
        if (existingOpt.isPresent()) {
            savedWord = existingOpt.get();
            if (savedWord.getStatus() == SavedWordStatus.ACTIVE) {
                // Idempotent: return existing active word without modifying recency or capacity
                return mapToProjection(userId, savedWord, entry);
            }
            // Restore deleted/archived word
            savedWord.setStatus(SavedWordStatus.ACTIVE);
            savedWord.setSavedAt(now);
            savedWord.setDeletedAt(null);
            if (notePlaintext != null) {
                savedWord.setPersonalNoteCiphertext(noteCipherService.encryptNote(userId, notePlaintext));
            }
            savedWordRepository.save(savedWord);
            srsScheduleCommands.onSavedWordRestored(userId, savedWord.getSavedWordId());
        } else {
            savedWord = new SavedWordEntity();
            savedWord.setSavedWordId(UUID.randomUUID());
            savedWord.setUserId(userId);
            savedWord.setDictionaryEntryId(dictionaryEntryId);
            savedWord.setStatus(SavedWordStatus.ACTIVE);
            savedWord.setSavedAt(now);
            if (notePlaintext != null) {
                savedWord.setPersonalNoteCiphertext(noteCipherService.encryptNote(userId, notePlaintext));
            }
            savedWordRepository.save(savedWord);
            srsScheduleCommands.onSavedWordCreated(userId, savedWord.getSavedWordId());
        }

        capacityCommands.enforceFreeCapacityOnSaveOrRestore(userId, savedWord.getSavedWordId());
        return mapToProjection(userId, savedWord, entry);
    }

    @Transactional(readOnly = true)
    public Page<SavedWordDetailProjection> listSavedWords(UUID userId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "savedAt", "savedWordId"));
        Page<SavedWordEntity> pageResult = savedWordRepository.findByUserIdAndStatus(userId, SavedWordStatus.ACTIVE, pageRequest);

        return pageResult.map(savedWord -> {
            Optional<DictionaryEntryEntity> entryOpt = dictionaryEntryRepository.findById(savedWord.getDictionaryEntryId());
            return mapToProjection(userId, savedWord, entryOpt.orElse(null));
        });
    }

    @Transactional(readOnly = true)
    public SavedWordDetailProjection getSavedWord(UUID userId, UUID savedWordId) {
        SavedWordEntity savedWord = savedWordRepository.findBySavedWordIdAndUserId(savedWordId, userId)
                .orElseThrow(ApiException::notFound);

        Optional<DictionaryEntryEntity> entryOpt = dictionaryEntryRepository.findById(savedWord.getDictionaryEntryId());
        return mapToProjection(userId, savedWord, entryOpt.orElse(null));
    }

    @Transactional
    public SavedWordDetailProjection updateNote(UUID userId, UUID savedWordId, String newNotePlaintext, Long expectedVersion) {
        SavedWordEntity savedWord = savedWordRepository.findBySavedWordIdAndUserIdForWrite(savedWordId, userId)
                .orElseThrow(ApiException::notFound);

        if (expectedVersion != null && !expectedVersion.equals(savedWord.getVersion())) {
            throw ApiException.conflict("Ghi chú đã được thay đổi từ một thiết bị khác. Vui lòng tải lại bản mới nhất.");
        }

        savedWord.setPersonalNoteCiphertext(noteCipherService.encryptNote(userId, newNotePlaintext));
        savedWordRepository.save(savedWord);

        Optional<DictionaryEntryEntity> entryOpt = dictionaryEntryRepository.findById(savedWord.getDictionaryEntryId());
        return mapToProjection(userId, savedWord, entryOpt.orElse(null));
    }

    @Transactional
    public void deleteSavedWord(UUID userId, UUID savedWordId) {
        SavedWordEntity savedWord = savedWordRepository.findBySavedWordIdAndUserIdForWrite(savedWordId, userId)
                .orElseThrow(ApiException::notFound);

        savedWord.setStatus(SavedWordStatus.DELETED);
        savedWord.setDeletedAt(Instant.now());
        savedWordRepository.save(savedWord);
        srsScheduleCommands.onSavedWordDeleted(userId, savedWordId);
    }

    private SavedWordDetailProjection mapToProjection(UUID userId, SavedWordEntity savedWord, DictionaryEntryEntity entry) {
        String decryptedNote = noteCipherService.decryptNote(userId, savedWord.getPersonalNoteCiphertext());
        boolean unavailable = entry == null || entry.getPublicationState() != PublicationState.PUBLISHED;

        return new SavedWordDetailProjection(
                savedWord.getSavedWordId(),
                savedWord.getUserId(),
                savedWord.getDictionaryEntryId(),
                decryptedNote,
                savedWord.getStatus().name(),
                savedWord.getSavedAt(),
                savedWord.getVersion(),
                unavailable,
                unavailable ? null : entry.getSimplifiedHanzi(),
                unavailable ? null : entry.getTraditionalHanzi(),
                unavailable ? null : entry.getPrimaryPinyin(),
                unavailable ? null : entry.getHskLevel(),
                unavailable ? null : entry.getWordType(),
                unavailable ? null : entry.getSenses()
        );
    }

    public record SavedWordDetailProjection(
            UUID savedWordId,
            UUID userId,
            UUID dictionaryEntryId,
            String personalNotePlaintext,
            String status,
            Instant savedAt,
            Long version,
            boolean unavailable,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Short hskLevel,
            String wordType,
            String senses
    ) {}
}
