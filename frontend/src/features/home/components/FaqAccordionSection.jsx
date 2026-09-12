import React, { useState } from 'react';
import { ChevronDown, HelpCircle } from 'lucide-react';

const faqs = [
  { id: 'method', question: 'Nên bắt đầu với Dictation hay Shadowing?', answer: 'Nếu bạn thường nghe không kịp, hãy bắt đầu bằng Dictation để nhận diện âm. Khi đã nghe rõ hơn, dùng Shadowing để luyện nhịp điệu và phản xạ nói.' },
  { id: 'ai', question: 'AI Buddy có tự chấm mọi kết quả học không?', answer: 'AI Buddy hỗ trợ giải thích và hội thoại. Điểm số, tiến độ, quota và lịch ôn vẫn do backend P Chinese xác nhận theo quy tắc ổn định.' },
  { id: 'free', question: 'Gói Free hiện có những gì?', answer: 'Bạn có thể xem bài học công khai, dùng các công cụ học đã phát hành và nhận 30 lượt AI trong mỗi chu kỳ. Hạn mức thực tế luôn hiển thị trong hồ sơ.' },
  { id: 'premium', question: 'Có thể thanh toán Premium ngay chưa?', answer: 'Chưa. Premium và cổng thanh toán đang được hoàn thiện. Nút xem trạng thái gói hiện cung cấp thông tin quyền sử dụng, không thực hiện giao dịch.' },
  { id: 'device', question: 'Trang chủ có dùng tốt trên điện thoại không?', answer: 'Có. Bố cục hỗ trợ từ màn hình rộng 360px, bộ lọc có thể cuộn ngang và mọi nút tương tác chính đều có vùng chạm tối thiểu 44px.' },
];

export function FaqAccordionSection() {
  const [openId, setOpenId] = useState(faqs[0].id);
  return (
    <section aria-labelledby="faq-title" className="grid gap-6 lg:grid-cols-[.65fr_1.35fr]">
      <div><span className="flex h-11 w-11 items-center justify-center rounded-lg bg-secondary text-primary"><HelpCircle className="h-5 w-5" /></span><h2 id="faq-title" className="mt-4 text-2xl font-bold text-foreground sm:text-3xl">Câu hỏi thường gặp</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">Thông tin ngắn gọn về phương pháp học, AI và quyền sử dụng.</p></div>
      <div className="divide-y divide-border rounded-xl border border-border bg-card">
        {faqs.map((faq) => {
          const isOpen = openId === faq.id;
          return (
            <div key={faq.id}>
              <h3>
                <button
                  type="button"
                  id={`faq-btn-${faq.id}`}
                  aria-expanded={isOpen}
                  aria-controls={`faq-panel-${faq.id}`}
                  onClick={() => setOpenId(isOpen ? null : faq.id)}
                  className="flex min-h-14 w-full items-center justify-between gap-4 px-5 py-4 text-left font-bold text-foreground hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"
                >
                  <span>{faq.question}</span>
                  <ChevronDown className={`h-5 w-5 shrink-0 text-primary transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`} />
                </button>
              </h3>
              {isOpen && (
                <div
                  id={`faq-panel-${faq.id}`}
                  role="region"
                  aria-labelledby={`faq-btn-${faq.id}`}
                  className="animate-accordion-down px-5 pb-5 text-sm leading-6 text-muted-foreground"
                >
                  {faq.answer}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
