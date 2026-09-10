import React, { useState, useEffect, useCallback } from 'react';
import {
  Shield,
  RefreshCw,
  AlertTriangle,
  HelpCircle,
  Sparkles,
  CheckCircle2,
  Crown,
  Check,
  Zap,
} from 'lucide-react';
import { getProfile } from '../../api/profile.js';
import { EntitlementCard } from './EntitlementCard.jsx';
import { PremiumUpgradeModal } from './PremiumUpgradeModal.jsx';

export function EntitlementPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [entitlement, setEntitlement] = useState(null);
  const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);

  const loadEntitlementData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getProfile();
      if (res && res.success && res.data) {
        setEntitlement(res.data.entitlement || {
          planCode: 'FREE',
          allowanceLimit: 30,
          usedUnits: 0,
          remainingUnits: 30,
          cycleStartAt: new Date().toISOString(),
          cycleEndAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        });
      } else {
        setError('Không thể lấy thông tin gói đăng ký.');
      }
    } catch (err) {
      setError(err?.error?.message || 'Có lỗi xảy ra khi kết nối máy chủ.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadEntitlementData();
  }, [loadEntitlementData]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-12 bg-card border border-border rounded-2xl flex flex-col items-center justify-center space-y-4 font-sans" data-testid="entitlement-loading">
        <RefreshCw className="w-8 h-8 text-primary animate-spin" />
        <p className="text-sm font-medium text-muted-foreground">Đang tải hạn mức AI & gói học tập...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto p-6 bg-destructive/10 border border-destructive/30 rounded-2xl space-y-4 font-sans" data-testid="entitlement-error">
        <div className="flex items-center space-x-3 text-destructive font-bold">
          <AlertTriangle className="w-6 h-6 shrink-0" />
          <span>Lỗi tải dữ liệu gói đăng ký</span>
        </div>
        <p className="text-sm text-foreground/90">{error}</p>
        <button
          type="button"
          onClick={loadEntitlementData}
          className="h-11 px-4 bg-primary text-primary-foreground font-bold text-sm rounded-xl flex items-center space-x-2 transition-all hover:opacity-90 active:scale-95 shadow-md shadow-primary/20"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Tải lại ngay</span>
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12 font-sans" data-testid="entitlement-page">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/80 pb-6">
        <div className="space-y-1">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
              <Shield className="w-5 h-5" />
            </div>
            <h1 className="text-2xl font-extrabold text-foreground tracking-tight">
              Quản lý Gói & Hạn mức AI (Entitlement)
            </h1>
          </div>
          <p className="text-sm text-muted-foreground pl-13">
            Theo dõi lượt AI Allowance khả dụng, chu kỳ làm mới và quy định sử dụng dịch vụ.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setIsUpgradeModalOpen(true)}
          className="h-11 px-5 bg-primary text-primary-foreground font-bold text-sm rounded-xl flex items-center space-x-2 shadow-md shadow-primary/20 hover:opacity-90 active:scale-95 transition-all self-start sm:self-auto shrink-0"
        >
          <Crown className="w-4 h-4" />
          <span>Nâng cấp Premium</span>
        </button>
      </div>

      {/* Main Entitlement Summary Card */}
      <EntitlementCard
        entitlement={entitlement}
        onOpenUpgradeModal={() => setIsUpgradeModalOpen(true)}
      />

      {/* Feature Comparison Table */}
      <div className="p-6 bg-card border border-border rounded-2xl shadow-sm space-y-4">
        <div className="flex items-center space-x-2 text-foreground font-bold">
          <Sparkles className="w-5 h-5 text-primary" />
          <h3 className="text-lg">So sánh tính năng các Gói</h3>
        </div>

        <div className="overflow-x-auto border border-border/60 rounded-xl">
          <table className="w-full text-left border-collapse text-xs sm:text-sm">
            <thead>
              <tr className="bg-secondary/60 text-foreground border-b border-border/60 font-bold">
                <th className="p-3.5 sm:p-4">Tính năng học tập</th>
                <th className="p-3.5 sm:p-4 text-center text-primary">Gói Free (MVP hiện tại)</th>
                <th className="p-3.5 sm:p-4 text-center text-muted-foreground">Gói Premium (Sắp có)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40 text-foreground/90 font-medium">
              <tr>
                <td className="p-3.5 sm:p-4 font-semibold">Lượt AI Allowance / chu kỳ 30 ngày</td>
                <td className="p-3.5 sm:p-4 text-center font-bold text-primary">30 lượt</td>
                <td className="p-3.5 sm:p-4 text-center text-amber-500 font-bold">Không giới hạn</td>
              </tr>
              <tr>
                <td className="p-3.5 sm:p-4 font-semibold">Luyện phát âm Shadowing (F08)</td>
                <td className="p-3.5 sm:p-4 text-center">Tiêu chuẩn</td>
                <td className="p-3.5 sm:p-4 text-center">Phân tích thanh điệu nâng cao</td>
              </tr>
              <tr>
                <td className="p-3.5 sm:p-4 font-semibold">Trợ lý học tập AI Buddy (F11)</td>
                <td className="p-3.5 sm:p-4 text-center">Phản hồi cơ bản</td>
                <td className="p-3.5 sm:p-4 text-center">Tư vấn ngữ pháp chuyên sâu</td>
              </tr>
              <tr>
                <td className="p-3.5 sm:p-4 font-semibold">Tải bài học & Nghe audio không giới hạn</td>
                <td className="p-3.5 sm:p-4 text-center"><Check className="w-4 h-4 text-emerald-500 mx-auto" /></td>
                <td className="p-3.5 sm:p-4 text-center"><Check className="w-4 h-4 text-emerald-500 mx-auto" /></td>
              </tr>
              <tr>
                <td className="p-3.5 sm:p-4 font-semibold">Tự động hoàn lượt khi dịch vụ AI bị lỗi</td>
                <td className="p-3.5 sm:p-4 text-center"><Check className="w-4 h-4 text-emerald-500 mx-auto" /></td>
                <td className="p-3.5 sm:p-4 text-center"><Check className="w-4 h-4 text-emerald-500 mx-auto" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* FAQ / Clarification Section */}
      <div className="p-6 bg-card border border-border rounded-2xl shadow-sm space-y-4">
        <div className="flex items-center space-x-2 text-foreground font-bold">
          <HelpCircle className="w-5 h-5 text-primary" />
          <h3 className="text-lg">Câu hỏi thường gặp về AI Allowance</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs sm:text-sm">
          <div className="p-4 bg-secondary/30 border border-border/60 rounded-xl space-y-2">
            <h4 className="font-bold text-foreground flex items-center space-x-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span>Nếu dịch vụ AI gặp sự cố timeout thì sao?</span>
            </h4>
            <p className="text-muted-foreground leading-relaxed">
              Hệ thống áp dụng cơ chế tự động hoàn lại đúng 1 lượt AI (<code className="bg-secondary px-1 py-0.5 rounded text-foreground">FAILED_REFUNDED</code>) cho bất kỳ sự cố kết nối, timeout hay từ chối an toàn nào mà không tạo ra kết quả học tập hợp lệ.
            </p>
          </div>

          <div className="p-4 bg-secondary/30 border border-border/60 rounded-xl space-y-2">
            <h4 className="font-bold text-foreground flex items-center space-x-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span>Những thao tác nào KHÔNG tốn lượt AI?</span>
            </h4>
            <p className="text-muted-foreground leading-relaxed">
              Việc mở xem danh sách bài học Shadowing, nghe audio bài học, hoặc các thao tác quản lý cuộc hội thoại (tạo mới, đổi tên, xóa cuộc hội thoại AI Buddy) hoàn toàn miễn phí và không tiêu tốn lượt AI nào.
            </p>
          </div>

          <div className="p-4 bg-secondary/30 border border-border/60 rounded-xl space-y-2">
            <h4 className="font-bold text-foreground flex items-center space-x-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span>Nếu tôi khóa tài khoản rồi mở lại thì sao?</span>
            </h4>
            <p className="text-muted-foreground leading-relaxed">
              Hệ thống bảo toàn chu kỳ và số lượt AI hiện tại. Việc khóa hoặc mở khóa tài khoản không làm mất hoặc thay đổi chu kỳ làm mới 30 ngày của bạn.
            </p>
          </div>

          <div className="p-4 bg-secondary/30 border border-border/60 rounded-xl space-y-2">
            <h4 className="font-bold text-foreground flex items-center space-x-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
              <span>Lượt AI có được cộng dồn sang tháng sau không?</span>
            </h4>
            <p className="text-muted-foreground leading-relaxed">
              Lượt AI không sử dụng hết sẽ tự động làm mới về mốc 30 lượt khi bắt đầu chu kỳ 30 ngày tiếp theo chứ không cộng dồn.
            </p>
          </div>
        </div>
      </div>

      {/* Upgrade Modal */}
      <PremiumUpgradeModal
        isOpen={isUpgradeModalOpen}
        entitlement={entitlement}
        onClose={() => setIsUpgradeModalOpen(false)}
      />
    </div>
  );
}
