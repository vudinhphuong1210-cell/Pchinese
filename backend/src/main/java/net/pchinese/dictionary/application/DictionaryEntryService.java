package net.pchinese.dictionary.application;

import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class DictionaryEntryService {

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public DictionaryEntryService(
            DictionaryEntryRepository dictionaryEntryRepository,
            MediaAssetRepository mediaAssetRepository
    ) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional(readOnly = true)
    public Optional<DictionaryEntryDetailProjection> getPublishedEntryDetail(UUID dictionaryEntryId) {
        Optional<DictionaryEntryEntity> entityOpt = dictionaryEntryRepository
                .findByDictionaryEntryIdAndPublicationState(dictionaryEntryId, PublicationState.PUBLISHED);

        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        DictionaryEntryEntity entry = entityOpt.get();
        UUID audioAssetId = resolveAvailableMediaId(entry.getAudioMediaAssetId());
        UUID imageAssetId = resolveAvailableMediaId(entry.getImageMediaAssetId());

        return Optional.of(new DictionaryEntryDetailProjection(
                entry.getDictionaryEntryId(),
                entry.getSimplifiedHanzi(),
                entry.getTraditionalHanzi(),
                entry.getPrimaryPinyin(),
                entry.getHskLevel(),
                entry.getWordType(),
                entry.getSenses(),
                audioAssetId,
                imageAssetId,
                entry.getPublicationState()
        ));
    }

    private UUID resolveAvailableMediaId(UUID mediaAssetId) {
        if (mediaAssetId == null) {
            return null;
        }
        Optional<MediaAssetEntity> mediaOpt = mediaAssetRepository.findById(mediaAssetId);
        if (mediaOpt.isPresent() && mediaOpt.get().getApprovalStatus() == ApprovalStatus.APPROVED) {
            return mediaAssetId;
        }
        return null; // Omit unavailable asset
    }

    public record DictionaryEntryDetailProjection(
            UUID dictionaryEntryId,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Short hskLevel,
            String wordType,
            String senses,
            UUID audioMediaAssetId,
            UUID imageMediaAssetId,
            PublicationState publicationState
    ) {}
}
