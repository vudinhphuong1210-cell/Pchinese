import React, { useEffect, useState } from 'react';
import { Crown, X, Check, ShieldAlert, Sparkles, Zap, Lock, RefreshCw } from 'lucide-react';
import { getProfile } from '../../api/profile.js';
import { EntitlementCard } from './EntitlementCard.jsx';

export function PremiumUpgradeModal({ isOpen, onClose, entitlement: propEntitlement }) {
  const [entitlement, setEntitlement] = useState(propEntitlement || null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (propEntitlement) {
      setEntitlement(propEntitlement);
    }
  }, [propEntitlement]);

  useEffect(() => {
    if (isOpen && !propEntitlement) {
      setLoading(true);
      Promise.resolve(getProfile())
        .then((res) => {
          if (res && res.success && res.data) {
            setEntitlement(res.data.entitlement);
          }
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [isOpen, propEntitlement]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="premium-modal-title"
      data-testid="premium-upgrade-modal"
    >
      <div
        className="w-full max-w-3xl bg-card border border-border rounded-2xl shadow-2xl p-6 sm:p-8 space-y-6 max-h-[90vh] overflow-y-auto relative font-sans"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 w-9 h-9 rounded-xl bg-secondary/80 hover:bg-secondary flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
          title="Đóng cửa sổ"
          aria-label="Đóng cửa sổ"
          data-testid="close-premium-modal-btn"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center space-x-3 border-b border-border/60 pb-4">
          <div className="w-12 h-12 rounded-2xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary shrink-0 shadow-sm">
            <Crown className="w-7 h-7" />
          </div>
          <div>
            <h2 id="premium-modal-title" className="text-xl font-extrabold text-foreground tracking-tight">
              Quản lý Gói & Nâng cấp Premium
            </h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              Theo dõi hạn mức AI Allowance hiện tại và quyền lợi gói Premium
            </p>
          </div>
        </div>

        {/* Current Entitlement & AI Allowance Status Section */}
        {loading ? (
          <div className="p-6 bg-secondary/30 rounded-xl flex items-center justify-center space-x-2 text-xs text-muted-foreground">
            <RefreshCw className="w-4 h-4 animate-spin text-primary" />
            <span>Đang tải thông tin hạn mức AI...</span>
          </div>
        ) : (
          <EntitlementCard entitlement={entitlement} />
        )}

        {/* Comparison Grid */}
        <div className="space-y-3">
          <div className="flex items-center space-x-2 text-foreground font-bold text-sm">
            <Sparkles className="w-4 h-4 text-primary" />
            <span>So sánh quyền lợi Gói học tập</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Free Plan (Current MVP) */}
            <div className="p-5 bg-secondary/30 border-2 border-primary/40 rounded-2xl space-y-4 relative">
              <div className="flex items-center justify-between">
                <span className="text-sm font-extrabold text-primary uppercase tracking-wider">
                  Gói Miễn Phí (Free)
                </span>
                <span className="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-primary/20 text-primary border border-primary/30">
                  Đang sử dụng
                </span>
              </div>
              <div className="text-2xl font-black text-foreground">
                0 VNĐ <span className="text-xs font-normal text-muted-foreground">/ tháng</span>
              </div>
              <ul className="space-y-2.5 text-xs text-foreground/90 font-medium">
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>30 lượt AI / chu kỳ 30 ngày.</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>Chấm phát âm Shadowing tiêu chuẩn (F08).</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>Trợ lý học tập AI Buddy (F11).</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>Tự động hoàn lượt khi dịch vụ AI bị sự cố.</span>
                </li>
              </ul>
            </div>

            {/* Premium Plan (Coming Soon) */}
            <div className="p-5 bg-card border border-border/80 rounded-2xl space-y-4 relative opacity-90">
              <div className="flex items-center justify-between">
                <span className="text-sm font-extrabold text-muted-foreground uppercase tracking-wider flex items-center space-x-1">
                  <span>Gói Premium</span>
                  <Sparkles className="w-4 h-4 text-amber-500" />
                </span>
                <span className="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-amber-500/10 text-amber-500 border border-amber-500/30 flex items-center space-x-1">
                  <Lock className="w-3 h-3" />
                  <span>Sắp ra mắt</span>
                </span>
              </div>
              <div className="text-2xl font-black text-muted-foreground">
                -- <span className="text-xs font-normal text-muted-foreground">/ tháng</span>
              </div>
              <ul className="space-y-2.5 text-xs text-muted-foreground font-medium">
                <li className="flex items-center space-x-2">
                  <Zap className="w-4 h-4 text-amber-500 shrink-0" />
                  <span className="font-semibold text-foreground">Không giới hạn lượt AI.</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-amber-500 shrink-0" />
                  <span>Phân tích thanh điệu & ngữ điệu chi tiết.</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-amber-500 shrink-0" />
                  <span>Gợi ý phản hồi nâng cao theo ngữ cảnh.</span>
                </li>
                <li className="flex items-center space-x-2">
                  <Check className="w-4 h-4 text-amber-500 shrink-0" />
                  <span>Ưu tiên tốc độ xử lý phản hồi AI.</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        {/* MVP Scope Clarification Alert */}
        <div
          className="p-4 bg-primary/10 border border-primary/30 rounded-xl space-y-2 text-xs text-foreground/90 font-medium"
          data-testid="mvp-notice-banner"
        >
          <div className="flex items-center space-x-2 text-primary font-bold text-sm">
            <ShieldAlert className="w-4 h-4 shrink-0" />
            <span>Thông báo về chính sách thử nghiệm MVP</span>
          </div>
          <p className="leading-relaxed">
            Trong giai đoạn MVP hiện tại, hệ thống vận hành <strong>chỉ sử dụng gói Free hoàn toàn miễn phí</strong>. Mọi tài khoản người học hợp lệ đều tự động nhận <strong>30 lượt AI / 30 ngày</strong>. Tính năng nâng cấp gói Premium và cổng thanh toán đang được phát triển và chưa kích hoạt.
          </p>
        </div>

        {/* Modal Actions */}
        <div className="pt-2 border-t border-border/60 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="h-11 px-6 bg-primary text-primary-foreground font-bold text-sm rounded-xl hover:opacity-90 active:scale-95 transition-all shadow-md shadow-primary/20"
            data-testid="confirm-premium-modal-btn"
          >
            Đã hiểu
          </button>
        </div>
      </div>
    </div>
  );
}
