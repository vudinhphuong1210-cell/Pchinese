import React from 'react';
import { ArrowRight, BookOpenCheck } from 'lucide-react';

export function HomeFooterCta({ isAuthenticated, onStart }) {
  return (
    <section className="rounded-xl border border-primary/40 bg-card p-6 text-center sm:p-10" aria-labelledby="footer-cta-title">
      <BookOpenCheck className="mx-auto h-8 w-8 text-primary" aria-hidden="true" />
      <h2 id="footer-cta-title" className="mt-4 text-2xl font-bold text-foreground sm:text-3xl">Mỗi ngày một bài, tiếng Trung tiến thêm một bước</h2>
      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-muted-foreground">Chọn nội dung đúng trình độ và bắt đầu vòng nghe, nói, phản hồi, ôn tập của riêng bạn.</p>
      <button type="button" onClick={onStart} className="mx-auto mt-6 flex min-h-11 items-center justify-center gap-2 rounded-md bg-primary px-6 text-sm font-bold text-primary-foreground hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
        {isAuthenticated ? 'Chọn bài học tiếp theo' : 'Tạo tài khoản miễn phí'}<ArrowRight className="h-4 w-4" />
      </button>
    </section>
  );
}
