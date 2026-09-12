package net.pchinese.aiops.domain;

public enum OperationalMeasurementOutcome {
    PENDING,
    SUCCEEDED,
    QUOTA_DENIED,
    FAILED_REFUNDED,
    FAILED_CONSUMED;

    public boolean isFinal() {
        return this != PENDING;
    }
}
