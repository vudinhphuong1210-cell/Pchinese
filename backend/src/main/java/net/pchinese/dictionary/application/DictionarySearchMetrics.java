package net.pchinese.dictionary.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DictionarySearchMetrics {

    private final AtomicLong totalSearches = new AtomicLong();
    private final AtomicLong successfulSearches = new AtomicLong();
    private final AtomicLong emptySearches = new AtomicLong();
    private final AtomicLong validationFailures = new AtomicLong();

    public void recordSearchOutcome(boolean hasResults) {
        totalSearches.incrementAndGet();
        if (hasResults) {
            successfulSearches.incrementAndGet();
        } else {
            emptySearches.incrementAndGet();
        }
    }

    public void recordValidationFailure() {
        validationFailures.incrementAndGet();
    }

    public long getTotalSearches() {
        return totalSearches.get();
    }

    public long getSuccessfulSearches() {
        return successfulSearches.get();
    }

    public long getEmptySearches() {
        return emptySearches.get();
    }

    public long getValidationFailures() {
        return validationFailures.get();
    }
}
