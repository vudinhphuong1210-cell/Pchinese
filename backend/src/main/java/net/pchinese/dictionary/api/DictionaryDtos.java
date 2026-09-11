package net.pchinese.dictionary.api;

import java.util.List;
import java.util.UUID;

public class DictionaryDtos {

    public record DictionarySummaryResponse(
            UUID dictionaryEntryId,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Short hskLevel,
            String wordType,
            String senses
    ) {}

    public record DictionaryDetailResponse(
            UUID dictionaryEntryId,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Short hskLevel,
            String wordType,
            String senses,
            UUID audioMediaAssetId,
            UUID imageMediaAssetId,
            String publicationState
    ) {}

    public record DictionarySearchPageResponse(
            List<DictionarySummaryResponse> items,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
