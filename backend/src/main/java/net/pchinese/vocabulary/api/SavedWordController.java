package net.pchinese.vocabulary.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import net.pchinese.vocabulary.application.SavedWordService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-words")
public class SavedWordController {

    private final SavedWordService savedWordService;

    public SavedWordController(SavedWordService savedWordService) {
        this.savedWordService = savedWordService;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<SavedWordDtos.SavedWordResponse>> saveWord(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SavedWordDtos.SaveWordRequest request
    ) {
        if (principal == null) {
            throw ApiException.unauthenticated();
        }

        SavedWordService.SavedWordDetailProjection projection = savedWordService.saveWord(
                principal.userId(),
                request.dictionaryEntryId(),
                request.personalNotePlaintext()
        );

        return ResponseEntity.ok(ApiEnvelope.success(mapToResponse(projection)));
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<SavedWordDtos.SavedWordPageResponse>> listSavedWords(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize
    ) {
        if (principal == null) {
            throw ApiException.unauthenticated();
        }

        Page<SavedWordService.SavedWordDetailProjection> pageResult = savedWordService.listSavedWords(principal.userId(), page, pageSize);

        List<SavedWordDtos.SavedWordResponse> items = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        SavedWordDtos.SavedWordPageResponse response = new SavedWordDtos.SavedWordPageResponse(
                items,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return ResponseEntity.ok(ApiEnvelope.success(response));
    }

    @GetMapping("/{savedWordId}")
    public ResponseEntity<ApiEnvelope<SavedWordDtos.SavedWordResponse>> getSavedWord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("savedWordId") UUID savedWordId
    ) {
        if (principal == null) {
            throw ApiException.unauthenticated();
        }

        SavedWordService.SavedWordDetailProjection projection = savedWordService.getSavedWord(principal.userId(), savedWordId);
        return ResponseEntity.ok(ApiEnvelope.success(mapToResponse(projection)));
    }

    @PatchMapping("/{savedWordId}")
    public ResponseEntity<ApiEnvelope<SavedWordDtos.SavedWordResponse>> updateNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("savedWordId") UUID savedWordId,
            @Valid @RequestBody SavedWordDtos.UpdateNoteRequest request
    ) {
        if (principal == null) {
            throw ApiException.unauthenticated();
        }

        SavedWordService.SavedWordDetailProjection projection = savedWordService.updateNote(
                principal.userId(),
                savedWordId,
                request.notePlaintext(),
                request.expectedVersion()
        );

        return ResponseEntity.ok(ApiEnvelope.success(mapToResponse(projection)));
    }

    @DeleteMapping("/{savedWordId}")
    public ResponseEntity<ApiEnvelope<Void>> deleteSavedWord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("savedWordId") UUID savedWordId
    ) {
        if (principal == null) {
            throw ApiException.unauthenticated();
        }

        savedWordService.deleteSavedWord(principal.userId(), savedWordId);
        return ResponseEntity.ok(ApiEnvelope.success(null));
    }

    private SavedWordDtos.SavedWordResponse mapToResponse(SavedWordService.SavedWordDetailProjection p) {
        return new SavedWordDtos.SavedWordResponse(
                p.savedWordId(),
                p.userId(),
                p.dictionaryEntryId(),
                p.personalNotePlaintext(),
                p.status(),
                p.savedAt(),
                p.version(),
                p.unavailable(),
                p.simplifiedHanzi(),
                p.traditionalHanzi(),
                p.primaryPinyin(),
                p.hskLevel(),
                p.wordType(),
                p.senses()
        );
    }
}
