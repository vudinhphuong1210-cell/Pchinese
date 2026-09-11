package net.pchinese.content.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.LessonType;
import net.pchinese.content.domain.SegmentType;

import java.util.UUID;

public class ContentDtos {

    public record TopicCreateRequest(
            @NotBlank String title,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug,
            String description,
            Integer hskLevel,
            @Min(0) Integer sortOrder
    ) {}

    public record TopicUpdateRequest(
            String title,
            @Pattern(regexp = "^[a-z0-9-]+$") String slug,
            String description,
            Integer hskLevel,
            @Min(0) Integer sortOrder,
            @NotNull @Min(0) Long expectedVersion
    ) {}

    public record LessonCreateRequest(
            @NotNull UUID topicId,
            @NotBlank String title,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug,
            String summary,
            Integer hskLevel,
            @Min(0) Integer sortOrder,
            AccessLevel accessLevel
    ) {}

    public record VersionedContentRequest(
            String title,
            String summary,
            @Min(0) Integer sortOrder,
            @NotNull @Min(0) Long expectedVersion
    ) {}

    public record SegmentCreateRequest(
            @NotNull UUID lessonId,
            @NotNull UUID mediaAssetId,
            @NotNull @Min(1) Integer sequenceNo,
            SegmentType segmentType,
            Integer startMilliseconds,
            Integer endMilliseconds,
            @NotBlank String transcriptHanzi,
            String transcriptPinyin,
            String translationVi,
            String dictationHint
    ) {}

    public record SegmentUpdateRequest(
            UUID mediaAssetId,
            @Min(1) Integer sequenceNo,
            String transcriptHanzi,
            String transcriptPinyin,
            String translationVi,
            String dictationHint,
            @NotNull @Min(0) Long expectedVersion
    ) {}

    public record MediaCreateRequest(
            @NotBlank @Size(max = 2048) String youtubeVideoReference,
            @Min(0) Integer durationMilliseconds,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 500) String altText
    ) {}

    public record MediaUpdateRequest(
            @Size(max = 200) String title,
            @Size(max = 500) String altText,
            @Null String youtubeVideoReference,
            @Null Object providerMetadata,
            @Null String providerName,
            @Null String providerAssetIdentifier,
            @NotNull @Min(0) Long expectedVersion
    ) {}

    public record VersionedCommandRequest(
            @NotNull @Min(0) Long expectedVersion
    ) {}
}
