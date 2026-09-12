import React from 'react';
import { Check, Clock3, Crown, ShieldCheck } from 'lucide-react';

const freeFeatures = ['Bài học công khai theo HSK', '30 lượt AI trong mỗi chu kỳ', 'Lưu từ và ôn lặp lại ngắt quãng', 'Theo dõi tiến độ học cá nhân'];
const premiumFeatures = ['Mở rộng quyền truy cập bài học', 'Hạn mức AI cao hơn theo cấu hình', 'Trải nghiệm học nâng cao khi phát hành', 'Entitlement được backend xác nhận'];

export function PricingComparisonSection({ onUpgradeClick, isPremiumUser = false }) {
  return (
    <section id="pricing" className="scroll-mt-6" aria-labelledby="pricing-title">
      <div className="mx-auto mb-6 max-w-2xl text-center"><p className="text-xs font-bold uppercase tracking-widest text-primary">Quyền sử dụng minh bạch</p><h2 id="pricing-title" className="mt-2 text-2xl font-bold text-foreground sm:text-3xl">Bắt đầu miễn phí, nâng cấp khi bạn cần</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">Hạn mức hiển thị trong tài khoản là nguồn thông tin chính xác. Premium đang trong giai đoạn chuẩn bị phát hành.</p></div>
      <div className="mx-auto grid max-w-4xl gap-4 md:grid-cols-2">
        <article className="rounded-xl border border-border bg-card p-6">
          <div className="flex items-center justify-between"><div><p className="text-sm font-bold text-muted-foreground">FREE</p><h3 className="mt-1 text-2xl font-bold text-foreground">0đ</h3></div><ShieldCheck className="h-7 w-7 text-success" /></div>
          <p className="mt-4 text-sm text-muted-foreground">Đủ để xây thói quen và trải nghiệm vòng học cốt lõi.</p>
          <ul className="mt-6 space-y-3">{freeFeatures.map((feature) => <li key={feature} className="flex gap-3 text-sm font-medium text-foreground"><Check className="h-5 w-5 shrink-0 text-success" />{feature}</li>)}</ul>
          <div className="mt-7 flex min-h-11 items-center justify-center rounded-md border border-border text-sm font-bold text-muted-foreground">{isPremiumUser ? 'Gói cơ bản' : 'Bạn có thể dùng ngay'}</div>
        </article>
        <article className="relative rounded-xl border border-primary/60 bg-card p-6">
          <span className="absolute right-5 top-5 rounded-md bg-primary/10 px-2.5 py-1 text-[11px] font-bold text-primary">
            {isPremiumUser ? 'ĐANG SỬ DỤNG' : 'SẮP RA MẮT'}
          </span>
          <div>
            <Crown className="h-7 w-7 text-primary" />
            <p className="mt-3 text-sm font-bold text-primary">PREMIUM</p>
            <h3 className="mt-1 text-2xl font-bold text-foreground">
              {isPremiumUser ? 'Gói của bạn' : 'Đang hoàn thiện'}
            </h3>
          </div>
          <p className="mt-4 text-sm text-muted-foreground">Mở rộng cường độ học trong khi vẫn giữ quota và quyền truy cập an toàn ở phía máy chủ.</p>
          <ul className="mt-6 space-y-3">{premiumFeatures.map((feature) => <li key={feature} className="flex gap-3 text-sm font-medium text-foreground"><Check className="h-5 w-5 shrink-0 text-primary" />{feature}</li>)}</ul>
          <button type="button" onClick={onUpgradeClick} className="mt-7 flex min-h-11 w-full items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-bold text-primary-foreground hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <Clock3 className="h-4 w-4" />
            {isPremiumUser ? 'Xem quyền lợi & hạn mức' : 'Xem trạng thái gói'}
          </button>
        </article>
      </div>
    </section>
  );
}
