import React from 'react';
import { CalendarCheck, Check, Flame, LoaderCircle, RotateCcw, Sparkles } from 'lucide-react';
import { useDailyStreak } from '../hooks/useDailyStreak.js';

export function DailyStreakWidget({ isGuest = false, onRequireAuth }) {
  const { streak, isLoading, isCheckingIn, error, celebration, checkIn, retry } = useDailyStreak(isGuest);

  return (
    <section className="rounded-xl border border-border bg-card p-5 sm:p-6" aria-labelledby="streak-title" data-testid="daily-streak-widget">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-secondary text-primary"><Flame className="h-6 w-6" aria-hidden="true" /></span>
          <div>
            <h2 id="streak-title" className="text-lg font-bold text-foreground">Chuỗi học mỗi ngày</h2>
            <p className="text-sm text-muted-foreground">Một lần có mặt, thêm một bước tiến.</p>
          </div>
        </div>
        {!isGuest && !isLoading && !error && (
          <div className="flex gap-3 text-right sm:gap-4">
            <div>
              <strong className="block text-xl text-foreground" data-testid="current-streak-value">{streak.currentStreak}</strong>
              <span className="text-xs text-muted-foreground">ngày liên tiếp</span>
            </div>
            <div className="border-l border-border pl-3 sm:pl-4">
              <strong className="block text-xl text-primary" data-testid="longest-streak-value">{streak.longestStreak ?? 0}</strong>
              <span className="text-xs text-muted-foreground">kỷ lục</span>
            </div>
            <div className="border-l border-border pl-3 sm:pl-4">
              <strong className="block text-xl text-foreground" data-testid="total-xp-value">{streak.totalXp}</strong>
              <span className="text-xs text-muted-foreground">tổng XP</span>
            </div>
          </div>
        )}
        {isGuest && (
          <div className="flex items-center gap-1.5 self-start rounded-md bg-secondary px-3 py-1.5 text-xs font-bold text-muted-foreground sm:self-center">
            <Flame className="h-4 w-4 text-primary" aria-hidden="true" />
            Bản xem trước
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="mt-5 flex min-h-24 items-center justify-center text-sm text-muted-foreground" role="status"><LoaderCircle className="mr-2 h-5 w-5 animate-spin" />Đang tải chuỗi học...</div>
      ) : error ? (
        <div className="mt-5 flex min-h-24 flex-col items-center justify-center rounded-lg bg-secondary/50 p-4 text-center">
          <p className="text-sm text-muted-foreground">{error}</p>
          <button type="button" onClick={retry} className="mt-2 flex min-h-11 items-center gap-2 rounded-md px-4 text-sm font-bold text-primary hover:bg-accent"><RotateCcw className="h-4 w-4" />Thử lại</button>
        </div>
      ) : (
        <>
          <div className="mt-5 grid grid-cols-7 gap-1.5 sm:gap-3" aria-label="Trạng thái điểm danh tuần này">
            {streak.weekdays.map((day) => {
              const completed = day.status === 'COMPLETED';
              const today = day.status === 'TODAY';
              return (
                <div key={day.date} className={`flex min-w-0 flex-col items-center rounded-lg border px-1 py-3 ${completed ? 'border-success/40 bg-success/10' : today ? 'border-primary/50 bg-primary/10' : 'border-border bg-background/40'}`}>
                  <span className="text-[11px] font-bold text-muted-foreground sm:text-xs">{day.label}</span>
                  <span className={`mt-2 flex h-8 w-8 items-center justify-center rounded-full ${completed ? 'bg-success text-primary-foreground' : today ? 'bg-primary text-primary-foreground' : 'bg-secondary text-muted-foreground'}`} aria-label={completed ? 'Đã hoàn thành' : today ? 'Hôm nay' : day.status === 'MISSED' ? 'Chưa hoàn thành' : 'Sắp tới'}>
                    {completed ? <Check className="h-4 w-4" /> : <span className="text-xs font-bold">{Number(day.date.slice(-2))}</span>}
                  </span>
                </div>
              );
            })}
          </div>
          <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm font-medium text-muted-foreground" aria-live="polite">
              {isGuest ? 'Đăng nhập để lưu chuỗi học và XP của bạn.' : celebration ? 'Tuyệt vời! Bạn vừa nhận 10 XP.' : streak.checkedInToday ? 'Đã hoàn thành hôm nay. Hẹn bạn ngày mai!' : 'Điểm danh hôm nay để nhận 10 XP.'}
            </p>
            <button type="button" disabled={!isGuest && (streak.checkedInToday || isCheckingIn)} onClick={isGuest ? onRequireAuth : checkIn} className="min-h-11 shrink-0 rounded-md bg-primary px-5 text-sm font-bold text-primary-foreground transition hover:opacity-90 disabled:cursor-default disabled:bg-secondary disabled:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" data-testid="daily-check-in-button">
              <span className="flex items-center justify-center gap-2">
                {isCheckingIn ? <LoaderCircle className="h-4 w-4 animate-spin" /> : streak.checkedInToday ? <Sparkles className="h-4 w-4" /> : <CalendarCheck className="h-4 w-4" />}
                {isGuest ? 'Đăng nhập để điểm danh' : streak.checkedInToday ? 'Đã hoàn thành hôm nay' : 'Điểm danh hôm nay · +10 XP'}
              </span>
            </button>
          </div>
        </>
      )}
    </section>
  );
}
