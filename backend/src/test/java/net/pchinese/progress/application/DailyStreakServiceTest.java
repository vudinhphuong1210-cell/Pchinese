package net.pchinese.progress.application;

import net.pchinese.progress.persistence.DailyCheckInEntity;
import net.pchinese.progress.persistence.DailyCheckInRepository;
import net.pchinese.users.domain.UserStatus;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyStreakServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T02:00:00Z"), ZoneOffset.UTC);
    private DailyCheckInRepository checkIns;
    private UserRepository users;
    private UserEntity user;
    private List<DailyCheckInEntity> entries;
    private DailyStreakService service;

    @BeforeEach
    void setUp() {
        checkIns = mock(DailyCheckInRepository.class);
        users = mock(UserRepository.class);
        user = mock(UserEntity.class);
        entries = new ArrayList<>();
        service = new DailyStreakService(checkIns, users, clock);

        when(user.getUserId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getEmailVerifiedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(user.getTimeZone()).thenReturn("Asia/Ho_Chi_Minh");
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.findLockedById(userId)).thenReturn(Optional.of(user));
        when(checkIns.findAllByUserIdOrderByCheckInDateAsc(userId)).thenAnswer(ignored -> List.copyOf(entries));
        when(checkIns.existsByUserIdAndCheckInDate(any(), any()))
                .thenAnswer(invocation -> entries.stream().anyMatch(entry ->
                        entry.getUserId().equals(invocation.getArgument(0))
                                && entry.getCheckInDate().equals(invocation.getArgument(1))));
        when(checkIns.saveAndFlush(any())).thenAnswer(invocation -> {
            DailyCheckInEntity entry = invocation.getArgument(0);
            entries.add(entry);
            return entry;
        });
    }

    @Test
    void checkInAwardsXpOnceAndExtendsTheCurrentStreak() {
        entries.add(entry("2026-09-10"));
        entries.add(entry("2026-09-11"));

        DailyStreakService.DailyStreakView first = service.checkIn(userId);
        DailyStreakService.DailyStreakView retry = service.checkIn(userId);

        assertEquals(3, first.currentStreak());
        assertEquals(30, first.totalXp());
        assertTrue(first.checkedInToday());
        assertFalse(first.alreadyCheckedIn());
        assertEquals(10, first.awardedXp());
        assertTrue(retry.alreadyCheckedIn());
        assertEquals(0, retry.awardedXp());
        assertEquals(30, retry.totalXp());
        verify(checkIns).saveAndFlush(any(DailyCheckInEntity.class));
    }

    @Test
    void summaryKeepsYesterdayStreakAliveAndBuildsASevenDayWeek() {
        entries.add(entry("2026-09-08"));
        entries.add(entry("2026-09-09"));
        entries.add(entry("2026-09-11"));

        DailyStreakService.DailyStreakView view = service.get(userId);

        assertEquals(1, view.currentStreak());
        assertEquals(2, view.longestStreak());
        assertEquals(30, view.totalXp());
        assertFalse(view.checkedInToday());
        assertEquals(7, view.weekdays().size());
        assertEquals(LocalDate.parse("2026-09-07"), view.weekdays().get(0).date());
        assertEquals("TODAY", view.weekdays().get(5).status());
    }

    private DailyCheckInEntity entry(String date) {
        return DailyCheckInEntity.create(userId, LocalDate.parse(date), 10, clock.instant());
    }
}
