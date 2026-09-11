package net.pchinese.dictation.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.dictation.application.DictationService;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/dictation-attempts")
public class DictationController {

    private final DictationService dictationService;

    public DictationController(DictationService dictationService) {
        this.dictationService = dictationService;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<DictationDtos.DictationAttemptView>> startAttempt(
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody DictationDtos.StartAttemptRequest request
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        DictationDtos.DictationAttemptView view = dictationService.startAttempt(actor.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(view));
    }

    @GetMapping
    public ApiEnvelope<List<DictationDtos.DictationAttemptView>> listAttempts(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) UUID segmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        if (page < 0 || size < 1 || size > 50) {
            throw ApiException.validation("Invalid pagination parameters.");
        }
        Page<DictationDtos.DictationAttemptView> result = dictationService.listAttempts(
                actor.userId(), lessonId, segmentId, PageRequest.of(page, size)
        );
        return ApiEnvelope.success(result.getContent(), Map.of(
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        ));
    }

    @GetMapping("/{attemptId}")
    public ApiEnvelope<DictationDtos.DictationAttemptView> getAttempt(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID attemptId
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(dictationService.getAttempt(actor.userId(), attemptId));
    }

    @PostMapping("/{attemptId}/submit")
    public ApiEnvelope<DictationDtos.DictationAttemptView> submitAttempt(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID attemptId,
            @Valid @RequestBody DictationDtos.SubmitAttemptRequest request
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(dictationService.submitAttempt(actor.userId(), attemptId, request));
    }
}
