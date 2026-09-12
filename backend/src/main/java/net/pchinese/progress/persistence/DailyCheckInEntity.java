package net.pchinese.progress.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_check_ins", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_check_ins_user_date", columnNames = {"user_id", "check_in_date"})
})
public class DailyCheckInEntity {
    @Id
    @Column(name = "daily_check_in_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "check_in_date", nullable = false, updatable = false)
    private LocalDate checkInDate;

    @Column(name = "awarded_xp", nullable = false, updatable = false)
    private int awardedXp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DailyCheckInEntity() {
    }

    public static DailyCheckInEntity create(UUID userId, LocalDate checkInDate, int awardedXp, Instant createdAt) {
        DailyCheckInEntity entity = new DailyCheckInEntity();
        entity.id = UUID.randomUUID();
        entity.userId = userId;
        entity.checkInDate = checkInDate;
        entity.awardedXp = awardedXp;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public int getAwardedXp() { return awardedXp; }
    public Instant getCreatedAt() { return createdAt; }
}
