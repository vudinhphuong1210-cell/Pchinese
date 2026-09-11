package net.pchinese.review.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.dictionary.persistence.DictionaryEntryEntity;
import net.pchinese.dictionary.persistence.DictionaryEntryRepository;
import net.pchinese.review.api.SrsReviewDtos.DueQueueResponse;
import net.pchinese.review.api.SrsReviewDtos.DueReviewCardResponse;
import net.pchinese.review.api.SrsReviewDtos.SubmitReviewRequest;
import net.pchinese.review.api.SrsReviewDtos.SubmitReviewResponse;
import net.pchinese.review.domain.SrsPolicyEngine;
import net.pchinese.review.domain.SrsStatus;
import net.pchinese.review.persistence.SrsReviewEventEntity;
import net.pchinese.review.persistence.SrsReviewEventRepository;
import net.pchinese.review.persistence.SrsScheduleEntity;
import net.pchinese.review.persistence.SrsScheduleRepository;
import net.pchinese.vocabulary.domain.SavedWordStatus;
import net.pchinese.vocabulary.persistence.SavedWordEntity;
import net.pchinese.vocabulary.persistence.SavedWordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SrsReviewService {

    private final SrsScheduleRepository scheduleRepository;
    private final SrsReviewEventRepository reviewEventRepository;
    private final SavedWordRepository savedWordRepository;
    private final DictionaryEntryRepository dictionaryEntryRepository;

    public SrsReviewService(
            SrsScheduleRepository scheduleRepository,
            SrsReviewEventRepository reviewEventRepository,
            SavedWordRepository savedWordRepository,
            DictionaryEntryRepository dictionaryEntryRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.reviewEventRepository = reviewEventRepository;
        this.savedWordRepository = savedWordRepository;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Transactional(readOnly = true)
    public DueQueueResponse getDueQueue(UUID userId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        Instant now = Instant.now();

        List<SrsScheduleEntity> dueSchedules = scheduleRepository.findDueSchedules(
                userId, now, PageRequest.of(0, boundedLimit)
        );

        List<DueReviewCardResponse> items = new ArrayList<>();
        for (SrsScheduleEntity schedule : dueSchedules) {
            Optional<SavedWordEntity> savedWordOpt = savedWordRepository.findById(schedule.getSavedWordId());
            if (savedWordOpt.isEmpty() || savedWordOpt.get().getStatus() != SavedWordStatus.ACTIVE) {
                continue;
            }
            SavedWordEntity savedWord = savedWordOpt.get();

            Optional<DictionaryEntryEntity> entryOpt = dictionaryEntryRepository.findById(savedWord.getDictionaryEntryId());
            if (entryOpt.isEmpty()) {
                continue;
            }
            DictionaryEntryEntity entry = entryOpt.get();

            String notePlaintext = null;
            if (savedWord.getPersonalNoteCiphertext() != null && savedWord.getPersonalNoteCiphertext().length > 0) {
                notePlaintext = new String(savedWord.getPersonalNoteCiphertext(), StandardCharsets.UTF_8);
            }

            DueReviewCardResponse card = new DueReviewCardResponse(
                    schedule.getSrsScheduleId(),
                    savedWord.getSavedWordId(),
                    entry.getDictionaryEntryId(),
                    entry.getSimplifiedHanzi(),
                    entry.getTraditionalHanzi(),
                    entry.getPrimaryPinyin(),
                    entry.getHskLevel() != null ? entry.getHskLevel().intValue() : null,
                    entry.getWordType(),
                    entry.getSenses(),
                    entry.getAudioMediaAssetId(),
                    notePlaintext,
                    schedule.getStatus(),
                    schedule.getDueAt(),
                    schedule.getIntervalDays(),
                    schedule.getEaseFactor(),
                    schedule.getVersion()
            );
            items.add(card);
        }

        int totalDueCount = items.size();
        return new DueQueueResponse(items, totalDueCount);
    }

    @Transactional
    public SubmitReviewResponse submitReview(UUID userId, SubmitReviewRequest request) {
        // 1. Idempotency replay check by (userId, clientReviewId)
        Optional<SrsReviewEventEntity> existingEvent = reviewEventRepository.findByUserIdAndClientReviewId(
                userId, request.clientReviewId()
        );
        if (existingEvent.isPresent()) {
            SrsReviewEventEntity event = existingEvent.get();
            SrsScheduleEntity schedule = scheduleRepository.findById(event.getSrsScheduleId())
                    .orElseThrow(ApiException::notFound);

            return new SubmitReviewResponse(
                    schedule.getSrsScheduleId(),
                    event.getSrsReviewEventId(),
                    event.getClientReviewId(),
                    event.getRating(),
                    schedule.getStatus(),
                    schedule.getStatus(),
                    event.getPreviousDueAt(),
                    event.getNextDueAt(),
                    event.getPreviousIntervalDays(),
                    event.getNextIntervalDays(),
                    schedule.getEaseFactor(),
                    schedule.getVersion(),
                    true
            );
        }

        // 2. Lock & retrieve schedule for update
        SrsScheduleEntity schedule = scheduleRepository.findBySrsScheduleIdAndUserIdForUpdate(
                request.srsScheduleId(), userId
        ).orElseThrow(ApiException::notFound);

        // 3. Stale version check
        if (!schedule.getVersion().equals(request.expectedScheduleVersion())) {
            throw ApiException.conflict("Stale schedule state. Expected version "
                    + request.expectedScheduleVersion() + " but observed version " + schedule.getVersion());
        }

        Instant now = Instant.now();
        SrsStatus previousStatus = schedule.getStatus();
        Instant previousDueAt = schedule.getDueAt();
        var previousIntervalDays = schedule.getIntervalDays();

        // 4. Calculate next state using SrsPolicyEngine
        SrsPolicyEngine.CalculationResult result = SrsPolicyEngine.calculate(
                previousStatus,
                request.rating(),
                previousIntervalDays,
                schedule.getEaseFactor(),
                schedule.getRepetitions(),
                schedule.getLapses(),
                now
        );

        // 5. Update schedule
        schedule.setStatus(result.newStatus());
        schedule.setDueAt(result.newDueAt());
        schedule.setIntervalDays(result.newIntervalDays());
        schedule.setEaseFactor(result.newEaseFactor());
        schedule.setRepetitions(result.newRepetitions());
        schedule.setLapses(result.newLapses());
        schedule.setLastReviewedAt(now);
        schedule.setUpdatedAt(now);
        SrsScheduleEntity updatedSchedule = scheduleRepository.save(schedule);

        // 6. Record review event
        UUID reviewEventId = UUID.randomUUID();
        SrsReviewEventEntity reviewEvent = new SrsReviewEventEntity(
                reviewEventId,
                updatedSchedule.getSrsScheduleId(),
                userId,
                request.clientReviewId(),
                request.rating(),
                previousDueAt,
                result.newDueAt(),
                previousIntervalDays,
                result.newIntervalDays(),
                now,
                now
        );
        reviewEventRepository.save(reviewEvent);

        return new SubmitReviewResponse(
                updatedSchedule.getSrsScheduleId(),
                reviewEventId,
                request.clientReviewId(),
                request.rating(),
                previousStatus,
                updatedSchedule.getStatus(),
                previousDueAt,
                result.newDueAt(),
                previousIntervalDays,
                result.newIntervalDays(),
                updatedSchedule.getEaseFactor(),
                updatedSchedule.getVersion(),
                false
        );
    }
}
