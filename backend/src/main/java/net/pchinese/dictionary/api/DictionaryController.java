package net.pchinese.dictionary.api;

import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.dictionary.application.DictionaryEntryService;
import net.pchinese.dictionary.application.DictionarySearchService;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dictionary")
public class DictionaryController {

    private final DictionarySearchService dictionarySearchService;
    private final DictionaryEntryService dictionaryEntryService;

    public DictionaryController(
            DictionarySearchService dictionarySearchService,
            DictionaryEntryService dictionaryEntryService
    ) {
        this.dictionarySearchService = dictionarySearchService;
        this.dictionaryEntryService = dictionaryEntryService;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<DictionaryDtos.DictionarySearchPageResponse>> searchDictionary(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize
    ) {
        Page<DictionaryEntryEntity> pageResult = dictionarySearchService.searchDictionary(q, page, pageSize);

        List<DictionaryDtos.DictionarySummaryResponse> items = pageResult.getContent().stream()
                .map(entry -> new DictionaryDtos.DictionarySummaryResponse(
                        entry.getDictionaryEntryId(),
                        entry.getSimplifiedHanzi(),
                        entry.getTraditionalHanzi(),
                        entry.getPrimaryPinyin(),
                        entry.getHskLevel(),
                        entry.getWordType(),
                        entry.getSenses()
                ))
                .toList();

        DictionaryDtos.DictionarySearchPageResponse response = new DictionaryDtos.DictionarySearchPageResponse(
                items,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return ResponseEntity.ok(ApiEnvelope.success(response));
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<ApiEnvelope<DictionaryDtos.DictionaryDetailResponse>> getDictionaryEntryDetail(
            @PathVariable("entryId") UUID entryId
    ) {
        return dictionaryEntryService.getPublishedEntryDetail(entryId)
                .map(projection -> {
                    DictionaryDtos.DictionaryDetailResponse response = new DictionaryDtos.DictionaryDetailResponse(
                            projection.dictionaryEntryId(),
                            projection.simplifiedHanzi(),
                            projection.traditionalHanzi(),
                            projection.primaryPinyin(),
                            projection.hskLevel(),
                            projection.wordType(),
                            projection.senses(),
                            projection.audioMediaAssetId(),
                            projection.imageMediaAssetId(),
                            projection.publicationState().name()
                    );
                    return ResponseEntity.ok(ApiEnvelope.success(response));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(ApiEnvelope.failure("NOT_FOUND", "Published dictionary entry not found.")));
    }
}
