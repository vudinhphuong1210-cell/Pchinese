package net.pchinese.vocabulary.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VocabularyMetrics {

    private final AtomicLong capacityRemovals = new AtomicLong();
    private final AtomicLong noteConflicts = new AtomicLong();
    private final AtomicLong expiryReconciliations = new AtomicLong();

    public void recordCapacityRemoval() {
        capacityRemovals.incrementAndGet();
    }

    public void recordNoteConflict() {
        noteConflicts.incrementAndGet();
    }

    public void recordExpiryReconciliation() {
        expiryReconciliations.incrementAndGet();
    }

    public long getCapacityRemovals() {
        return capacityRemovals.get();
    }

    public long getNoteConflicts() {
        return noteConflicts.get();
    }

    public long getExpiryReconciliations() {
        return expiryReconciliations.get();
    }
}
