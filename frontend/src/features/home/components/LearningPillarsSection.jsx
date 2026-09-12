import React from 'react';
import { ArrowRight, BookMarked, Headphones, MessageCircle, Mic2 } from 'lucide-react';

const pillars = [
  {
    id: 'catalog', icon: Mic2, eyebrow: 'Video tuyển chọn', title: 'Shadowing',
    description: 'Nói đuổi theo từng câu, so nhịp điệu và sửa phát âm qua phản hồi có hướng dẫn.',
    highlights: ['Chia nhỏ theo phân đoạn', 'Nghe và ghi âm trong cùng ngữ cảnh', 'Theo dõi lần luyện tốt nhất'],
    action: 'Chọn bài luyện nói',
  },
  {
    id: 'catalog', icon: Headphones, eyebrow: 'Active listening', title: 'Dictation',
    description: 'Nghe kỹ từng âm, chép lại điều đã nghe và nhìn rõ vị trí còn nhầm lẫn.',
    highlights: ['Chấm theo nội dung gốc', 'Sửa lỗi theo từng ký tự', 'Luyện lại đúng đoạn yếu'],
    action: 'Chọn bài luyện nghe',
  },
  {
    id: 'ai-buddy', icon: MessageCircle, eyebrow: 'AI có hạn mức', title: 'AI Learning Buddy',
    description: 'Luyện hội thoại theo tình huống với gợi ý vừa đủ để bạn tự tạo câu trả lời.',
    highlights: ['Kịch bản giao tiếp thực tế', 'Giải thích bằng tiếng Việt', 'Quota do máy chủ kiểm soát'],
    action: 'Mở AI Buddy',
  },
  {
    id: 'vocabulary', icon: BookMarked, eyebrow: 'Spaced repetition', title: 'Từ vựng & ôn tập',
    description: 'Lưu từ gặp trong bài học và ôn theo lịch để chuyển trí nhớ ngắn hạn thành dài hạn.',
    highlights: ['Kho từ cá nhân', 'Flashcard có ngữ cảnh', 'Lịch ôn do hệ thống tính'],
    action: 'Mở kho từ',
  },
];

export function LearningPillarsSection({ onNavigate }) {
  return (
    <section id="learning-pillars" className="scroll-mt-6" aria-labelledby="pillars-title">
      <div className="mb-5 max-w-2xl">
        <p className="text-xs font-bold uppercase tracking-widest text-primary">Phương pháp học tập</p>
        <h2 id="pillars-title" className="mt-2 text-2xl font-bold text-foreground sm:text-3xl">Bốn kỹ năng, cùng một vòng tiến bộ</h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">Mỗi hoạt động tạo dữ liệu học tập để lần luyện tiếp theo tập trung đúng điểm bạn cần cải thiện.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {pillars.map(({ id, icon: Icon, eyebrow, title, description, highlights, action }) => (
          <article key={title} className="group rounded-xl border border-border bg-card p-5 transition hover:border-primary/50 sm:p-6">
            <div className="flex items-start justify-between gap-4">
              <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-secondary text-primary"><Icon className="h-5 w-5" aria-hidden="true" /></span>
              <span className="rounded-md bg-secondary px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-muted-foreground">{eyebrow}</span>
            </div>
            <h3 className="mt-5 text-xl font-bold text-foreground">{title}</h3>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p>
            <ul className="mt-4 space-y-2">
              {highlights.map((item) => <li key={item} className="flex gap-2 text-sm font-medium text-foreground"><span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />{item}</li>)}
            </ul>
            <button type="button" onClick={() => onNavigate(id)} className="mt-5 flex min-h-11 items-center gap-2 rounded-md text-sm font-bold text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              {action}<ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" aria-hidden="true" />
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}
