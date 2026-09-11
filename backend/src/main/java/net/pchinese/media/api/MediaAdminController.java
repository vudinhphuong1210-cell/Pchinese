package net.pchinese.media.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.content.api.AdminContentProjection;
import net.pchinese.content.api.ContentDtos;
import net.pchinese.media.application.MediaAssetService;
import net.pchinese.media.application.MediaLifecycleService;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
public class MediaAdminController {

    private final MediaAssetService mediaAssetService;
    private final MediaLifecycleService mediaLifecycleService;

    public MediaAdminController(MediaAssetService mediaAssetService, MediaLifecycleService mediaLifecycleService) {
        this.mediaAssetService = mediaAssetService;
        this.mediaLifecycleService = mediaLifecycleService;
    }

    @GetMapping("/admin/content/media")
    public ApiEnvelope<List<AdminContentProjection>> listMedia(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                 @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        Page<AdminContentProjection> result = mediaAssetService.listMediaAssets(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(AdminContentProjection::from);
        return ApiEnvelope.success(result.getContent(), Map.of("page", result.getNumber(), "size", result.getSize(),
                "totalElements", result.getTotalElements(), "totalPages", result.getTotalPages()));
    }

    @GetMapping("/admin/content/media/{mediaAssetId}")
    public ApiEnvelope<AdminContentProjection> getMedia(@PathVariable UUID mediaAssetId) {
        return ApiEnvelope.success(AdminContentProjection.from(mediaAssetService.getMediaAsset(mediaAssetId)));
    }

    @PostMapping(value = "/media", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiEnvelope<AdminContentProjection>> createMedia(@AuthenticationPrincipal UserPrincipal actor,
                                                                             @Valid @RequestBody ContentDtos.MediaCreateRequest req) {
        var created = mediaAssetService.createYoutubeMediaAsset(req.youtubeVideoReference(), req.durationMilliseconds(),
                req.title(), req.altText(), actor.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(AdminContentProjection.from(created)));
    }

    @PatchMapping("/media/{mediaAssetId}")
    public ApiEnvelope<AdminContentProjection> updateMedia(@AuthenticationPrincipal UserPrincipal actor,
                                                            @PathVariable UUID mediaAssetId,
                                                            @Valid @RequestBody ContentDtos.MediaUpdateRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(mediaAssetService.updateMediaAsset(mediaAssetId, req.title(), req.altText(),
                req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/media/{mediaAssetId}/approve")
    public ApiEnvelope<AdminContentProjection> approveMedia(@AuthenticationPrincipal UserPrincipal actor,
                                                             @PathVariable UUID mediaAssetId,
                                                             @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(mediaLifecycleService.approveMedia(mediaAssetId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/media/{mediaAssetId}/reject")
    public ApiEnvelope<AdminContentProjection> rejectMedia(@AuthenticationPrincipal UserPrincipal actor,
                                                            @PathVariable UUID mediaAssetId,
                                                            @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(mediaLifecycleService.rejectMedia(mediaAssetId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/media/{mediaAssetId}/quarantine")
    public ApiEnvelope<AdminContentProjection> quarantineMedia(@AuthenticationPrincipal UserPrincipal actor,
                                                                @PathVariable UUID mediaAssetId,
                                                                @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(mediaLifecycleService.quarantineMedia(mediaAssetId, req.expectedVersion(), actor.userId())));
    }
}
