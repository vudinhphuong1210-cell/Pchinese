package net.pchinese.catalog.api.dto;

import java.util.List;
import java.util.UUID;

public record TopicDetailDto(
        UUID id,
        String title,
        String slug,
        String description,
        Short hskLevel,
        List<LessonSummaryDto> lessons
) {}
