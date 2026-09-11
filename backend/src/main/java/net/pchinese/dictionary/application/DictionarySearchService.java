package net.pchinese.dictionary.application;

import net.pchinese.content.domain.PublicationState;
import net.pchinese.dictionary.domain.QueryKind;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionarySearchKeyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionarySearchService {

    private final DictionarySearchKeyRepository searchKeyRepository;
    private final DictionarySearchKeyProjector searchKeyProjector;

    public DictionarySearchService(
            DictionarySearchKeyRepository searchKeyRepository,
            DictionarySearchKeyProjector searchKeyProjector
    ) {
        this.searchKeyRepository = searchKeyRepository;
        this.searchKeyProjector = searchKeyProjector;
    }

    @Transactional(readOnly = true)
    public Page<DictionaryEntryEntity> searchDictionary(String rawQuery, int page, int pageSize) {
        validateQuery(rawQuery);
        validatePagination(page, pageSize);

        String trimmed = rawQuery.trim();
        Pageable pageable = PageRequest.of(page, pageSize);

        // Normalize depending on content type
        String normHanzi = searchKeyProjector.normalizeHanzi(trimmed);
        String normPinyin = searchKeyProjector.normalizePinyin(trimmed);
        String normVi = searchKeyProjector.normalizeVietnamese(trimmed);

        // Try exact key search across kinds first
        Page<DictionaryEntryEntity> results = searchKeyRepository.searchPublishedEntriesExactKey(normHanzi, PublicationState.PUBLISHED, pageable);
        if (results.hasContent()) {
            return results;
        }

        if (!normPinyin.isEmpty()) {
            results = searchKeyRepository.searchPublishedEntriesByKindAndKey(QueryKind.PINYIN, normPinyin, PublicationState.PUBLISHED, pageable);
            if (results.hasContent()) {
                return results;
            }
        }

        if (!normVi.isEmpty()) {
            results = searchKeyRepository.searchPublishedEntriesByKindAndKey(QueryKind.VIETNAMESE_KEYWORD, normVi, PublicationState.PUBLISHED, pageable);
            if (results.hasContent()) {
                return results;
            }
        }

        return Page.empty(pageable);
    }

    private void validateQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Search query must not be null.");
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty or whitespace-only.");
        }
        int codePoints = trimmed.codePointCount(0, trimmed.length());
        if (codePoints < 1 || codePoints > 120) {
            throw new IllegalArgumentException("Search query length must be between 1 and 120 code points.");
        }
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be a non-negative integer starting at 0.");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new IllegalArgumentException("Page size must be an integer between 1 and 50.");
        }
    }
}
