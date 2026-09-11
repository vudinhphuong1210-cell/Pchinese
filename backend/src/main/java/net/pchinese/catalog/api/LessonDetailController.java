package net.pchinese.catalog.api;

import net.pchinese.catalog.api.dto.LessonSummaryDto;
import net.pchinese.catalog.application.LessonAccessService;
import net.pchinese.common.api.ApiEnvelope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LessonDetailController {

    private final LessonAccessService lessonAccessService;

    public LessonDetailController(LessonAccessService lessonAccessService) {
        this.lessonAccessService = lessonAccessService;
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiEnvelope<LessonSummaryDto>> getLessonDetail(@PathVariable UUID lessonId) {
        LessonSummaryDto result = lessonAccessService.getLessonDetail(lessonId);
        return ResponseEntity.ok()
                .cacheControl(CatalogResponsePolicy.publicCache())
                .body(ApiEnvelope.success(result));
    }
}
