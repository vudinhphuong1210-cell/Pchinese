import React from 'react';
import { AlertTriangle, Crown, Calendar } from 'lucide-react';
import { formatCycleDate } from '../../api/entitlement.js';

export function QuotaExhaustedBanner({ entitlement, onOpenUpgradeModal, className = '' }) {
  const cycleEnd = formatCycleDate(entitlement?.cycleEndAt);

  return (
    <div
      className={`p-4 bg-destructive/10 border border-destructive/30 rounded-2xl space-y-3 font-sans ${className}`}
      data-testid="quota-exhausted-banner"
      role="alert"
    >
      <div className="flex items-start space-x-3">
        <div className="w-9 h-9 rounded-xl bg-destructive/20 text-destructive flex items-center justify-center shrink-0 mt-0.5">
          <AlertTriangle className="w-5 h-5" />
        </div>
        <div className="space-y-1 flex-1">
          <h4 className="font-bold text-destructive text-sm">
            Hạn mức AI học tập cho chu kỳ này đã hết (0/30 lượt)
          </h4>
          <p className="text-xs text-foreground/90 leading-relaxed">
            Bạn đã sử dụng hết 30 lượt AI trong chu kỳ hiện tại. Tính năng chấm điểm phát âm AI (Shadowing) và Trợ lý trò chuyện (AI Buddy) sẽ khả dụng lại khi chu kỳ mới bắt đầu.
          </p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1 border-t border-destructive/20 text-xs">
        <div className="flex items-center space-x-1.5 text-muted-foreground">
          <Calendar className="w-4 h-4 text-primary" />
          <span>Thời gian làm mới: <strong className="text-foreground">{cycleEnd}</strong></span>
        </div>

        {onOpenUpgradeModal && (
          <button
            type="button"
            onClick={onOpenUpgradeModal}
            className="h-8 px-3 bg-primary text-primary-foreground font-bold text-xs rounded-lg flex items-center justify-center space-x-1.5 hover:opacity-90 active:scale-95 transition-all self-end sm:self-auto shadow-sm"
            data-testid="quota-banner-upgrade-btn"
          >
            <Crown className="w-3.5 h-3.5" />
            <span>Xem chi tiết gói</span>
          </button>
        )}
      </div>
    </div>
  );
}
