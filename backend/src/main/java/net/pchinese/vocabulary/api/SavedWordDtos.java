package net.pchinese.vocabulary.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SavedWordDtos {

    public record SaveWordRequest(
            @NotNull(message = "dictionaryEntryId must not be null")
            UUID dictionaryEntryId,

            @Size(max = 500, message = "Personal note must not exceed 500 characters")
            String personalNotePlaintext
    ) {}

    public record UpdateNoteRequest(
            @Size(max = 500, message = "Personal note must not exceed 500 characters")
            String notePlaintext,

            Long expectedVersion
    ) {}

    public record SavedWordResponse(
            UUID savedWordId,
            UUID userId,
            UUID dictionaryEntryId,
            String personalNotePlaintext,
            String status,
            Instant savedAt,
            Long version,
            boolean unavailable,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Short hskLevel,
            String wordType,
            String senses
    ) {}

    public record SavedWordPageResponse(
            List<SavedWordResponse> items,
            int page,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
