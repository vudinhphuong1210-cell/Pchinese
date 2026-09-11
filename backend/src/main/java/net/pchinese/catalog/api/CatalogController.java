package net.pchinese.catalog.api;

import net.pchinese.catalog.api.dto.LessonSummaryDto;
import net.pchinese.catalog.api.dto.TopicDetailDto;
import net.pchinese.catalog.api.dto.TopicSummaryDto;
import net.pchinese.catalog.application.CatalogService;
import net.pchinese.common.api.ApiEnvelope;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/topics")
    public ResponseEntity<ApiEnvelope<Page<TopicSummaryDto>>> searchTopics(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short hskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TopicSummaryDto> result = catalogService.searchTopics(keyword, hskLevel, page, size);
        return ResponseEntity.ok()
                .cacheControl(CatalogResponsePolicy.publicCache())
                .body(ApiEnvelope.success(result));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<ApiEnvelope<TopicDetailDto>> getTopicDetail(
            @PathVariable UUID topicId
    ) {
        TopicDetailDto result = catalogService.getTopicDetail(topicId);
        return ResponseEntity.ok()
                .cacheControl(CatalogResponsePolicy.publicCache())
                .body(ApiEnvelope.success(result));
    }

    @GetMapping("/lessons")
    public ResponseEntity<ApiEnvelope<Page<LessonSummaryDto>>> searchLessons(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short hskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<LessonSummaryDto> result = catalogService.searchLessons(topicId, keyword, hskLevel, page, size);
        return ResponseEntity.ok()
                .cacheControl(CatalogResponsePolicy.publicCache())
                .body(ApiEnvelope.success(result));
    }
}
