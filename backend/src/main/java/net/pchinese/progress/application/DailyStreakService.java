package net.pchinese.progress.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.progress.persistence.DailyCheckInEntity;
import net.pchinese.progress.persistence.DailyCheckInRepository;
import net.pchinese.users.domain.UserStatus;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DailyStreakService {
    public static final int DAILY_XP = 10;

    private final DailyCheckInRepository checkIns;
    private final UserRepository users;
    private final Clock clock;

    public DailyStreakService(DailyCheckInRepository checkIns, UserRepository users) {
        this(checkIns, users, Clock.systemUTC());
    }

    DailyStreakService(DailyCheckInRepository checkIns, UserRepository users, Clock clock) {
        this.checkIns = checkIns;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DailyStreakView get(UUID userId) {
        UserEntity user = requireActiveUser(userId, false);
        return buildView(user, false, 0);
    }

    @Transactional
    public DailyStreakView checkIn(UUID userId) {
        UserEntity user = requireActiveUser(userId, true);
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, safeZone(user.getTimeZone()));
        boolean alreadyCheckedIn = checkIns.existsByUserIdAndCheckInDate(userId, today);
        if (!alreadyCheckedIn) {
            checkIns.saveAndFlush(DailyCheckInEntity.create(userId, today, DAILY_XP, now));
        }
        return buildView(user, alreadyCheckedIn, alreadyCheckedIn ? 0 : DAILY_XP);
    }

    private UserEntity requireActiveUser(UUID userId, boolean lock) {
        UserEntity user = (lock ? users.findLockedById(userId) : users.findById(userId))
                .orElseThrow(ApiException::unauthenticated);
        if (user.getStatus() != UserStatus.ACTIVE || user.getEmailVerifiedAt() == null) {
            throw ApiException.unauthenticated();
        }
        return user;
    }

    private DailyStreakView buildView(UserEntity user, boolean alreadyCheckedIn, int awardedXp) {
        LocalDate today = LocalDate.now(clock.withZone(safeZone(user.getTimeZone())));
        List<DailyCheckInEntity> entries = checkIns.findAllByUserIdOrderByCheckInDateAsc(user.getUserId());
        Set<LocalDate> dates = new HashSet<>();
        int totalXp = 0;
        for (DailyCheckInEntity entry : entries) {
            dates.add(entry.getCheckInDate());
            totalXp += entry.getAwardedXp();
        }

        int currentStreak = consecutiveBackwards(dates, dates.contains(today) ? today : today.minusDays(1));
        int longestStreak = longestRun(dates);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<WeekdayView> weekdays = new ArrayList<>(7);
        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (int index = 0; index < 7; index++) {
            LocalDate date = monday.plusDays(index);
            String status;
            if (dates.contains(date)) status = "COMPLETED";
            else if (date.equals(today)) status = "TODAY";
            else if (date.isBefore(today)) status = "MISSED";
            else status = "UPCOMING";
            weekdays.add(new WeekdayView(labels[index], date, status));
        }

        return new DailyStreakView(currentStreak, longestStreak, totalXp, dates.contains(today),
                alreadyCheckedIn, DAILY_XP, awardedXp, today, weekdays);
    }

    private static int consecutiveBackwards(Set<LocalDate> dates, LocalDate cursor) {
        int count = 0;
        while (dates.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    private static int longestRun(Set<LocalDate> dates) {
        int longest = 0;
        for (LocalDate date : dates) {
            if (!dates.contains(date.minusDays(1))) {
                int run = 1;
                while (dates.contains(date.plusDays(run))) run++;
                longest = Math.max(longest, run);
            }
        }
        return longest;
    }

    private static ZoneId safeZone(String value) {
        try {
            return ZoneId.of(value);
        } catch (RuntimeException ignored) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }

    public record DailyStreakView(int currentStreak, int longestStreak, int totalXp,
                                  boolean checkedInToday, boolean alreadyCheckedIn,
                                  int dailyXpReward, int awardedXp, LocalDate today, List<WeekdayView> weekdays) {
    }

    public record WeekdayView(String label, LocalDate date, String status) {
    }
}
