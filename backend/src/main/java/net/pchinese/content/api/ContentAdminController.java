package net.pchinese.content.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.content.application.ContentDraftService;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
public class ContentAdminController {

    private final ContentDraftService contentDraftService;

    public ContentAdminController(ContentDraftService contentDraftService) {
        this.contentDraftService = contentDraftService;
    }

    @GetMapping("/admin/content/topics")
    public ApiEnvelope<List<AdminContentProjection>> listTopics(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                  @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return page(contentDraftService.listTopics(PageRequest.of(page, size, Sort.by("sortOrder").ascending())), AdminContentProjection::from);
    }

    @GetMapping("/admin/content/topics/{topicId}")
    public ApiEnvelope<AdminContentProjection> getTopic(@PathVariable UUID topicId) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.getTopic(topicId)));
    }

    @PostMapping("/topics")
    public ResponseEntity<ApiEnvelope<AdminContentProjection>> createTopic(@AuthenticationPrincipal UserPrincipal actor,
                                                                             @Valid @RequestBody ContentDtos.TopicCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(AdminContentProjection.from(
                contentDraftService.createTopic(req.title(), req.slug(), req.description(), req.hskLevel(), req.sortOrder(), actor.userId()))));
    }

    @PatchMapping("/topics/{topicId}")
    public ApiEnvelope<AdminContentProjection> updateTopic(@AuthenticationPrincipal UserPrincipal actor,
                                                             @PathVariable UUID topicId,
                                                             @Valid @RequestBody ContentDtos.TopicUpdateRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.updateTopic(topicId, req.title(), req.slug(), req.description(),
                req.hskLevel(), req.sortOrder(), req.expectedVersion(), actor.userId())));
    }

    @GetMapping("/admin/content/lessons")
    public ApiEnvelope<List<AdminContentProjection>> listLessons(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                   @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return page(contentDraftService.listLessons(PageRequest.of(page, size, Sort.by("sortOrder").ascending())), AdminContentProjection::from);
    }

    @GetMapping("/admin/content/lessons/{lessonId}")
    public ApiEnvelope<AdminContentProjection> getLesson(@PathVariable UUID lessonId) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.getLesson(lessonId)));
    }

    @PostMapping("/lessons")
    public ResponseEntity<ApiEnvelope<AdminContentProjection>> createLesson(@AuthenticationPrincipal UserPrincipal actor,
                                                                              @Valid @RequestBody ContentDtos.LessonCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(AdminContentProjection.from(
                contentDraftService.createLesson(req.topicId(), req.title(), req.slug(), req.summary(), req.hskLevel(),
                        req.sortOrder(), req.accessLevel(), actor.userId()))));
    }

    @PatchMapping("/lessons/{lessonId}")
    public ApiEnvelope<AdminContentProjection> updateLesson(@AuthenticationPrincipal UserPrincipal actor,
                                                              @PathVariable UUID lessonId,
                                                              @Valid @RequestBody ContentDtos.VersionedContentRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.updateLesson(lessonId, req.title(), req.summary(),
                req.sortOrder(), req.expectedVersion(), actor.userId())));
    }

    @GetMapping("/admin/content/segments")
    public ApiEnvelope<List<AdminContentProjection>> listSegments(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                    @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return page(contentDraftService.listSegments(PageRequest.of(page, size, Sort.by("sequenceNo").ascending())), AdminContentProjection::from);
    }

    @GetMapping("/admin/content/segments/{segmentId}")
    public ApiEnvelope<AdminContentProjection> getSegment(@PathVariable UUID segmentId) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.getSegment(segmentId)));
    }

    @PostMapping("/segments")
    public ResponseEntity<ApiEnvelope<AdminContentProjection>> createSegment(@AuthenticationPrincipal UserPrincipal actor,
                                                                               @Valid @RequestBody ContentDtos.SegmentCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(AdminContentProjection.from(
                contentDraftService.createSegment(req.lessonId(), req.mediaAssetId(), req.sequenceNo(), req.segmentType(),
                        req.startMilliseconds(), req.endMilliseconds(), req.transcriptHanzi(), req.transcriptPinyin(),
                        req.translationVi(), req.dictationHint(), actor.userId()))));
    }

    @PatchMapping("/segments/{segmentId}")
    public ApiEnvelope<AdminContentProjection> updateSegment(@AuthenticationPrincipal UserPrincipal actor,
                                                               @PathVariable UUID segmentId,
                                                               @Valid @RequestBody ContentDtos.SegmentUpdateRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(contentDraftService.updateSegment(segmentId, req.mediaAssetId(),
                req.sequenceNo(), req.transcriptHanzi(), req.transcriptPinyin(), req.translationVi(), req.dictationHint(),
                req.expectedVersion(), actor.userId())));
    }

    private static <T> ApiEnvelope<List<AdminContentProjection>> page(Page<T> result,
                                                                        Function<T, AdminContentProjection> mapper) {
        return ApiEnvelope.success(result.getContent().stream().map(mapper).toList(), Map.of(
                "page", result.getNumber(), "size", result.getSize(),
                "totalElements", result.getTotalElements(), "totalPages", result.getTotalPages()));
    }
}
