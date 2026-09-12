import { useCallback, useEffect, useMemo, useState } from 'react';
import { checkInToday, getDailyStreak } from '../../../api/dailyStreak.js';

const LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

function localDateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function guestPreview() {
  const now = new Date();
  const day = now.getDay() || 7;
  const monday = new Date(now);
  monday.setHours(12, 0, 0, 0);
  monday.setDate(now.getDate() - day + 1);
  return {
    currentStreak: 0,
    longestStreak: 0,
    totalXp: 0,
    checkedInToday: false,
    weekdays: LABELS.map((label, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);
      return {
        label,
        date: localDateKey(date),
        status: index < day - 1 ? 'MISSED' : index === day - 1 ? 'TODAY' : 'UPCOMING',
      };
    }),
  };
}

export function useDailyStreak(isGuest) {
  const preview = useMemo(guestPreview, []);
  const [streak, setStreak] = useState(preview);
  const [isLoading, setIsLoading] = useState(!isGuest);
  const [isCheckingIn, setIsCheckingIn] = useState(false);
  const [error, setError] = useState(null);
  const [celebration, setCelebration] = useState(false);

  const load = useCallback(async () => {
    if (isGuest) {
      setStreak(preview);
      setIsLoading(false);
      setError(null);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      setStreak(await getDailyStreak());
    } catch (requestError) {
      setError(requestError?.error?.message || 'Chưa thể tải chuỗi học tập.');
    } finally {
      setIsLoading(false);
    }
  }, [isGuest, preview]);

  useEffect(() => {
    void load();
  }, [load]);

  const checkIn = useCallback(async () => {
    if (isGuest || streak.checkedInToday || isCheckingIn) return null;
    setIsCheckingIn(true);
    setError(null);
    try {
      const next = await checkInToday();
      setStreak(next);
      setCelebration(!next.alreadyCheckedIn);
      return next;
    } catch (requestError) {
      setError(requestError?.error?.message || 'Điểm danh chưa thành công. Vui lòng thử lại.');
      return null;
    } finally {
      setIsCheckingIn(false);
    }
  }, [isCheckingIn, isGuest, streak.checkedInToday]);

  return { streak, isLoading, isCheckingIn, error, celebration, checkIn, retry: load };
}
