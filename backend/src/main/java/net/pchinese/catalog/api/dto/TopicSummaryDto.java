package net.pchinese.catalog.api.dto;

import java.util.UUID;

public record TopicSummaryDto(
        UUID id,
        String title,
        String slug,
        String description,
        Short hskLevel,
        long publishedLessonCount
) {}
