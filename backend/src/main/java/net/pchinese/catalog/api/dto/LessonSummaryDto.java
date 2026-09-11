package net.pchinese.catalog.api.dto;

import net.pchinese.catalog.domain.AccessLevel;

import java.util.UUID;

public record LessonSummaryDto(
        UUID id,
        UUID topicId,
        String title,
        String slug,
        String description,
        Short hskLevel,
        AccessLevel accessLevel,
        int estimatedDurationSeconds,
        long publishedSegmentCount
) {}
