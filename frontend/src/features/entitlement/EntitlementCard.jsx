import React from 'react';
import {
  ShieldCheck,
  Zap,
  Calendar,
  AlertTriangle,
  Crown,
  CheckCircle,
  Info,
  Sparkles,
  RefreshCw,
} from 'lucide-react';
import { calculateQuotaStatus, formatCycleDate } from '../../api/entitlement.js';

export function EntitlementCard({ entitlement, onOpenUpgradeModal, className = '' }) {
  const quotaStatus = calculateQuotaStatus(entitlement);
  const { state, percentage, remainingUnits, allowanceLimit, usedUnits } = quotaStatus;

  const cycleStart = formatCycleDate(entitlement?.cycleStartAt);
  const cycleEnd = formatCycleDate(entitlement?.cycleEndAt);

  return (
    <div
      className={`p-6 bg-card border border-border rounded-2xl shadow-sm space-y-6 font-sans ${className}`}
      data-testid="entitlement-card"
    >
      {/* Header Row: Plan Identity & Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/60 pb-4">
        <div className="flex items-center space-x-3">
          <div className="w-11 h-11 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary shrink-0 shadow-sm">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-lg font-bold text-foreground">
                Gói hiện tại: {entitlement?.planCode || 'FREE'}
              </span>
              <span
                className="px-2.5 py-0.5 text-xs font-bold rounded-full bg-emerald-500/10 text-emerald-500 border border-emerald-500/30 flex items-center space-x-1"
                data-testid="entitlement-status-pill"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                <span>Đang hoạt động</span>
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              30 lượt AI / chu kỳ 30 ngày tự động gia hạn
            </p>
          </div>
        </div>

        {onOpenUpgradeModal && (
          <button
            type="button"
            onClick={onOpenUpgradeModal}
            className="h-10 px-4 bg-primary text-primary-foreground font-bold text-xs rounded-xl flex items-center justify-center space-x-2 shadow-md shadow-primary/20 hover:opacity-90 active:scale-95 transition-all shrink-0"
            data-testid="upgrade-premium-btn"
          >
            <Crown className="w-4 h-4" />
            <span>Nâng cấp Premium</span>
          </button>
        )}
      </div>

      {/* AI Allowance Quota Progress Bar */}
      <div className="space-y-2.5 p-4 bg-secondary/40 border border-border/80 rounded-xl">
        <div className="flex items-center justify-between text-sm font-semibold">
          <div className="flex items-center space-x-2 text-foreground">
            <Zap className="w-4 h-4 text-primary" />
            <span>Hạn mức AI lượt học</span>
          </div>
          <div className="text-xs font-bold" data-testid="quota-counter-text">
            <span
              className={
                state === 'EXHAUSTED'
                  ? 'text-destructive font-extrabold'
                  : state === 'LOW'
                  ? 'text-amber-500 font-extrabold'
                  : 'text-foreground'
              }
            >
              {remainingUnits}
            </span>
            <span className="text-muted-foreground"> / {allowanceLimit} lượt còn lại</span>
          </div>
        </div>

        {/* Progress Track */}
        <div className="w-full h-3 bg-secondary rounded-full overflow-hidden border border-border/50">
          <div
            className={`h-full transition-all duration-500 rounded-full ${
              state === 'EXHAUSTED'
                ? 'bg-destructive'
                : state === 'LOW'
                ? 'bg-amber-500'
                : 'bg-primary'
            }`}
            style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }}
            data-testid="quota-progress-bar"
          />
        </div>

        <div className="flex items-center justify-between text-[11px] text-muted-foreground font-medium pt-0.5">
          <span>Đã sử dụng: {usedUnits} lượt</span>
          <span>Tỷ lệ: {percentage}%</span>
        </div>

        {/* Warning messages based on Quota state */}
        {state === 'EXHAUSTED' && (
          <div
            className="p-3 bg-destructive/10 border border-destructive/30 rounded-lg flex items-start space-x-2 text-xs text-destructive font-semibold"
            data-testid="quota-exhausted-alert"
          >
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>
              Bạn đã sử dụng hết 30 lượt AI cho chu kỳ này. Lượt AI sẽ tự động khôi phục vào chu kỳ kế tiếp ({cycleEnd}).
            </span>
          </div>
        )}

        {state === 'LOW' && (
          <div
            className="p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg flex items-start space-x-2 text-xs text-amber-600 dark:text-amber-400 font-medium"
            data-testid="quota-low-alert"
          >
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-500" />
            <span>
              Lượt AI của bạn sắp hết (còn {remainingUnits} lượt). Hãy sử dụng tiết kiệm trước ngày làm mới ({cycleEnd}).
            </span>
          </div>
        )}
      </div>

      {/* Cycle Dates Details */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="p-3.5 bg-secondary/30 border border-border/60 rounded-xl flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-background border border-border flex items-center justify-center text-muted-foreground shrink-0">
            <Calendar className="w-4 h-4" />
          </div>
          <div>
            <span className="text-xs text-muted-foreground block font-medium">Bắt đầu chu kỳ</span>
            <span className="text-sm font-bold text-foreground">{cycleStart}</span>
          </div>
        </div>

        <div className="p-3.5 bg-secondary/30 border border-border/60 rounded-xl flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-background border border-border flex items-center justify-center text-muted-foreground shrink-0">
            <RefreshCw className="w-4 h-4" />
          </div>
          <div>
            <span className="text-xs text-muted-foreground block font-medium">Ngày làm mới tiếp theo</span>
            <span className="text-sm font-bold text-primary">{cycleEnd}</span>
          </div>
        </div>
      </div>

      {/* Usage Policy Rules */}
      <div className="p-4 bg-background border border-border/80 rounded-xl space-y-3">
        <div className="flex items-center space-x-2 text-xs font-bold text-foreground uppercase tracking-wider">
          <Info className="w-4 h-4 text-primary" />
          <span>Quy tắc tính lượt AI Allowance</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
          {/* Consumes Quota */}
          <div className="p-3 bg-card border border-border/60 rounded-lg space-y-1.5">
            <div className="flex items-center space-x-1.5 font-bold text-amber-500">
              <Zap className="w-3.5 h-3.5" />
              <span>Tiêu tốn 1 lượt AI / lần</span>
            </div>
            <ul className="space-y-1 text-muted-foreground pl-1">
              <li className="flex items-start space-x-1.5">
                <span className="text-primary font-bold">•</span>
                <span>Mỗi bài chấm điểm phát âm thực tế (F08 Shadowing).</span>
              </li>
              <li className="flex items-start space-x-1.5">
                <span className="text-primary font-bold">•</span>
                <span>Mỗi tin nhắn yêu cầu Trợ lý AI phản hồi (F11 AI Buddy).</span>
              </li>
            </ul>
          </div>

          {/* Free / No Quota Consumed */}
          <div className="p-3 bg-card border border-border/60 rounded-lg space-y-1.5">
            <div className="flex items-center space-x-1.5 font-bold text-emerald-500">
              <CheckCircle className="w-3.5 h-3.5" />
              <span>Miễn phí 100% (Không tốn lượt)</span>
            </div>
            <ul className="space-y-1 text-muted-foreground pl-1">
              <li className="flex items-start space-x-1.5">
                <span className="text-emerald-500 font-bold">•</span>
                <span>Mở xem bài học, nghe audio & xem transcript.</span>
              </li>
              <li className="flex items-start space-x-1.5">
                <span className="text-emerald-500 font-bold">•</span>
                <span>Tạo mới, đổi tên hoặc xóa hội thoại AI Buddy.</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
