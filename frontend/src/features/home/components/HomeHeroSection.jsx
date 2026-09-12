import React from 'react';
import { ArrowRight, BookOpenCheck, Headphones, MessageCircle, Mic2, Sparkles } from 'lucide-react';

const benefits = [
  { label: 'Pinyin tức thì', icon: BookOpenCheck },
  { label: 'Video tuyển chọn', icon: Headphones },
  { label: 'AI hỗ trợ', icon: MessageCircle },
  { label: 'Ôn đúng thời điểm', icon: Sparkles },
];

export function HomeHeroSection({ isAuthenticated, onStart, onShadowing, onExplore }) {
  return (
    <section className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(300px,.65fr)]" aria-labelledby="home-hero-title">
      <div className="rounded-xl border border-border bg-card p-6 sm:p-8 lg:p-10">
        <div className="mb-5 inline-flex items-center gap-2 rounded-md bg-secondary px-3 py-2 text-xs font-bold text-primary">
          <Sparkles className="h-4 w-4" aria-hidden="true" />
          Lộ trình HSK dành cho người Việt
        </div>
        <h1 id="home-hero-title" className="max-w-3xl text-3xl font-bold leading-tight text-foreground sm:text-4xl lg:text-5xl">
          Học tiếng Trung thú vị và hiệu quả hơn mỗi ngày
        </h1>
        <p className="mt-5 max-w-2xl text-base font-medium leading-7 text-muted-foreground sm:text-lg">
          Nghe kỹ với Dictation, nói tự nhiên qua Shadowing, ghi nhớ từ bằng lặp lại ngắt quãng và luyện phản xạ cùng AI Buddy.
        </p>
        <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <button type="button" onClick={onStart} className="min-h-11 rounded-md bg-primary px-5 py-3 text-sm font-bold text-primary-foreground transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background">
            <span className="flex items-center justify-center gap-2">
              {isAuthenticated ? 'Tiếp tục học' : 'Bắt đầu học miễn phí'}
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </span>
          </button>
          <button type="button" onClick={onShadowing} className="min-h-11 rounded-md border border-border bg-transparent px-5 py-3 text-sm font-bold text-foreground transition hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <span className="flex items-center justify-center gap-2"><Mic2 className="h-4 w-4 text-primary" aria-hidden="true" />Luyện Shadowing</span>
          </button>
          <button type="button" onClick={onExplore} className="min-h-11 rounded-md px-4 py-3 text-sm font-bold text-primary hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            Khám phá phương pháp
          </button>
        </div>
        <ul className="mt-8 grid gap-2 border-t border-border pt-5 sm:grid-cols-2 lg:grid-cols-4" aria-label="Điểm nổi bật">
          {benefits.map(({ label, icon: Icon }) => (
            <li key={label} className="flex items-center gap-2 text-xs font-semibold text-muted-foreground">
              <Icon className="h-4 w-4 text-primary" aria-hidden="true" />{label}
            </li>
          ))}
        </ul>
      </div>

      <div className="relative overflow-hidden rounded-xl border border-border bg-card p-6 sm:p-8">
        <p className="text-xs font-bold uppercase tracking-widest text-primary">Một vòng học chủ động</p>
        <div className="mt-7 space-y-3">
          {[
            ['01', 'Nghe', 'Bắt đúng âm và ngữ cảnh'],
            ['02', 'Lặp lại', 'Nói và viết bằng phản xạ'],
            ['03', 'Nhận phản hồi', 'Biết chính xác điểm cần sửa'],
            ['04', 'Ôn lại', 'Gặp lại kiến thức đúng lúc'],
          ].map(([number, title, text]) => (
            <div key={number} className="flex items-center gap-4 rounded-lg border border-border bg-background/50 p-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-secondary text-sm font-bold text-primary">{number}</span>
              <div><p className="font-bold text-foreground">{title}</p><p className="text-xs text-muted-foreground">{text}</p></div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
