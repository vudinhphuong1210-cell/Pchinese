package net.pchinese.shadowing.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import net.pchinese.shadowing.application.ShadowingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1")
public class ShadowingController {

    private final ShadowingService shadowingService;

    public ShadowingController(ShadowingService shadowingService) {
        this.shadowingService = shadowingService;
    }

    @PostMapping(value = "/recordings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<ShadowingDtos.RecordingView>> uploadRecording(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam("file") MultipartFile file,
            @RequestParam("segmentId") UUID segmentId
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        ShadowingDtos.RecordingView view = shadowingService.uploadRecording(actor.userId(), segmentId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(view));
    }

    @PostMapping("/shadowing-attempts")
    public ResponseEntity<ApiEnvelope<ShadowingDtos.ShadowingAttemptView>> createAttempt(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam("segmentId") UUID segmentId,
            @Valid @RequestBody ShadowingDtos.CreateAttemptRequest request
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        ShadowingDtos.ShadowingAttemptView view = shadowingService.createAttempt(actor.userId(), request, segmentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiEnvelope.success(view));
    }

    @GetMapping("/shadowing-attempts")
    public ApiEnvelope<List<ShadowingDtos.ShadowingAttemptView>> listAttempts(
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
        Page<ShadowingDtos.ShadowingAttemptView> result = shadowingService.listAttempts(
                actor.userId(), lessonId, segmentId, PageRequest.of(page, size)
        );
        return ApiEnvelope.success(result.getContent(), Map.of(
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        ));
    }

    @GetMapping("/shadowing-attempts/{attemptId}")
    public ApiEnvelope<ShadowingDtos.ShadowingAttemptView> getAttempt(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID attemptId
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(shadowingService.getAttempt(actor.userId(), attemptId));
    }
}
