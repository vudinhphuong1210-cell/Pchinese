package net.pchinese.content.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.content.application.ContentPublicationService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
public class ContentLifecycleController {

    private final ContentPublicationService publicationService;

    public ContentLifecycleController(ContentPublicationService publicationService) {
        this.publicationService = publicationService;
    }

    // --- Topic Lifecycle ---

    @PostMapping("/topics/{topicId}/publish")
    public ApiEnvelope<AdminContentProjection> publishTopic(@AuthenticationPrincipal UserPrincipal actor,
                                                  @PathVariable UUID topicId,
                                                  @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.publishTopic(topicId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/topics/{topicId}/unpublish")
    public ApiEnvelope<AdminContentProjection> unpublishTopic(@AuthenticationPrincipal UserPrincipal actor,
                                                    @PathVariable UUID topicId,
                                                    @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.unpublishTopic(topicId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/topics/{topicId}/archive")
    public ApiEnvelope<AdminContentProjection> archiveTopic(@AuthenticationPrincipal UserPrincipal actor,
                                                  @PathVariable UUID topicId,
                                                  @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.archiveTopic(topicId, req.expectedVersion(), actor.userId())));
    }

    // --- Lesson Lifecycle ---

    @PostMapping("/lessons/{lessonId}/publish")
    public ApiEnvelope<AdminContentProjection> publishLesson(@AuthenticationPrincipal UserPrincipal actor,
                                                    @PathVariable UUID lessonId,
                                                    @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.publishLesson(lessonId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/lessons/{lessonId}/unpublish")
    public ApiEnvelope<AdminContentProjection> unpublishLesson(@AuthenticationPrincipal UserPrincipal actor,
                                                      @PathVariable UUID lessonId,
                                                      @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.unpublishLesson(lessonId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/lessons/{lessonId}/archive")
    public ApiEnvelope<AdminContentProjection> archiveLesson(@AuthenticationPrincipal UserPrincipal actor,
                                                    @PathVariable UUID lessonId,
                                                    @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.archiveLesson(lessonId, req.expectedVersion(), actor.userId())));
    }

    // --- Segment Lifecycle ---

    @PostMapping("/segments/{segmentId}/publish")
    public ApiEnvelope<AdminContentProjection> publishSegment(@AuthenticationPrincipal UserPrincipal actor,
                                                      @PathVariable UUID segmentId,
                                                      @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.publishSegment(segmentId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/segments/{segmentId}/unpublish")
    public ApiEnvelope<AdminContentProjection> unpublishSegment(@AuthenticationPrincipal UserPrincipal actor,
                                                        @PathVariable UUID segmentId,
                                                        @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.unpublishSegment(segmentId, req.expectedVersion(), actor.userId())));
    }

    @PostMapping("/segments/{segmentId}/archive")
    public ApiEnvelope<AdminContentProjection> archiveSegment(@AuthenticationPrincipal UserPrincipal actor,
                                                      @PathVariable UUID segmentId,
                                                      @Valid @RequestBody ContentDtos.VersionedCommandRequest req) {
        return ApiEnvelope.success(AdminContentProjection.from(publicationService.archiveSegment(segmentId, req.expectedVersion(), actor.userId())));
    }
}
