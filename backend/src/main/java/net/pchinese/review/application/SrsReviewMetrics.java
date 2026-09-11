package net.pchinese.review.application;

import net.pchinese.review.domain.ReviewRating;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SrsReviewMetrics {

    private final AtomicLong dueQueueCounter = new AtomicLong(0);
    private final AtomicLong idempotentReplayCounter = new AtomicLong(0);
    private final AtomicLong staleConflictCounter = new AtomicLong(0);
    private final Map<ReviewRating, AtomicLong> ratingCounters = new EnumMap<>(ReviewRating.class);

    public SrsReviewMetrics() {
        for (ReviewRating rating : ReviewRating.values()) {
            ratingCounters.put(rating, new AtomicLong(0));
        }
    }

    public void recordDueQueueRequest() {
        dueQueueCounter.incrementAndGet();
    }

    public void recordRating(ReviewRating rating) {
        AtomicLong counter = ratingCounters.get(rating);
        if (counter != null) {
            counter.incrementAndGet();
        }
    }

    public void recordIdempotentReplay() {
        idempotentReplayCounter.incrementAndGet();
    }

    public void recordStaleConflict() {
        staleConflictCounter.incrementAndGet();
    }

    public long getDueQueueRequests() {
        return dueQueueCounter.get();
    }

    public long getIdempotentReplays() {
        return idempotentReplayCounter.get();
    }

    public long getStaleConflicts() {
        return staleConflictCounter.get();
    }

    public long getRatingCount(ReviewRating rating) {
        AtomicLong counter = ratingCounters.get(rating);
        return counter != null ? counter.get() : 0L;
    }
}
