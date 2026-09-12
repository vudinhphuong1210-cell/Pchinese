import React, { useState } from 'react';
import { ArrowLeft, ArrowRight, RotateCw, Sparkles, Volume2 } from 'lucide-react';

const words = [
  { hanzi: '坚持', pinyin: 'jiān chí', meaning: 'kiên trì', level: 'HSK 4', example: '每天坚持学习。', exampleVi: 'Mỗi ngày kiên trì học tập.' },
  { hanzi: '进步', pinyin: 'jìn bù', meaning: 'tiến bộ', level: 'HSK 3', example: '你的中文进步很快。', exampleVi: 'Tiếng Trung của bạn tiến bộ rất nhanh.' },
  { hanzi: '习惯', pinyin: 'xí guàn', meaning: 'thói quen', level: 'HSK 3', example: '养成每天听中文的习惯。', exampleVi: 'Tạo thói quen nghe tiếng Trung mỗi ngày.' },
  { hanzi: '加油', pinyin: 'jiā yóu', meaning: 'cố lên', level: 'HSK 2', example: '相信自己，一起加油！', exampleVi: 'Tin tưởng bản thân, cùng nhau cố lên!' },
  { hanzi: '流利', pinyin: 'liú lì', meaning: 'lưu loát', level: 'HSK 4', example: '他说中文非常流利。', exampleVi: 'Anh ấy nói tiếng Trung rất lưu loát.' },
  { hanzi: '复习', pinyin: 'fù xí', meaning: 'ôn tập', level: 'HSK 2', example: '及时复习才能记得更牢。', exampleVi: 'Ôn tập kịp thời mới nhớ lâu hơn.' },
  { hanzi: '交流', pinyin: 'jiāo liú', meaning: 'giao tiếp', level: 'HSK 4', example: '多跟母语者交流。', exampleVi: 'Giao tiếp nhiều hơn với người bản xứ.' },
  { hanzi: '成功', pinyin: 'chéng gōng', meaning: 'thành công', level: 'HSK 4', example: '坚持到底就是成功。', exampleVi: 'Kiên trì đến cùng chính là thành công.' },
];

export function InteractiveDemoSection() {
  const [index, setIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const word = words[index];
  const move = (step) => {
    setIndex((current) => (current + step + words.length) % words.length);
    setIsFlipped(false);
  };
  const speak = () => {
    if (!globalThis.speechSynthesis || !globalThis.SpeechSynthesisUtterance) return;
    try {
      globalThis.speechSynthesis.cancel();
      const utterance = new globalThis.SpeechSynthesisUtterance(word.hanzi);
      utterance.lang = 'zh-CN';
      utterance.rate = 0.85;
      globalThis.speechSynthesis.speak(utterance);
    } catch (_err) {
      // SpeechSynthesis may not be supported or allowed in test environments
    }
  };

  return (
    <section className="grid gap-5 lg:grid-cols-[.8fr_1.2fr] lg:items-center" aria-labelledby="demo-title" data-testid="interactive-demo-section">
      <div>
        <p className="text-xs font-bold uppercase tracking-widest text-primary">Thử ngay trong 30 giây</p>
        <h2 id="demo-title" className="mt-2 text-2xl font-bold text-foreground sm:text-3xl">Ghi nhớ một từ theo cả âm, nghĩa và ngữ cảnh</h2>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">Bấm lật thẻ để kiểm tra trí nhớ. Khi học thật, từ bạn lưu sẽ đi vào lịch ôn cá nhân do backend tính toán.</p>
        <div className="mt-4 inline-flex items-center gap-2 rounded-md bg-secondary px-3 py-1.5 text-xs font-semibold text-muted-foreground">
          <Sparkles className="h-4 w-4 text-primary" aria-hidden="true" />
          Kho từ mẫu {words.length} từ thực chiến
        </div>
      </div>
      <div className="rounded-xl border border-border bg-card p-5 sm:p-6">
        <div className="mb-3 flex items-center justify-between text-xs font-bold text-muted-foreground">
          <span className="rounded-md bg-secondary px-2.5 py-1 text-primary">{word.level}</span>
          <span data-testid="flashcard-counter">{index + 1} / {words.length}</span>
        </div>
        <button
          type="button"
          onClick={() => setIsFlipped((value) => !value)}
          className="flex min-h-56 w-full flex-col items-center justify-center rounded-lg border border-border bg-background/50 p-6 text-center transition hover:border-primary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring active:scale-[0.99]"
          aria-label={isFlipped ? 'Xem mặt chữ Hán' : 'Lật thẻ xem nghĩa'}
          data-testid="flashcard-demo-card"
        >
          {isFlipped ? (
            <>
              <span className="text-xs font-bold uppercase tracking-widest text-primary">Nghĩa và ngữ cảnh</span>
              <strong className="mt-3 text-3xl font-bold text-foreground capitalize" data-testid="flashcard-meaning">{word.meaning}</strong>
              <div className="mt-3 text-center">
                <p className="font-hanzi text-lg text-foreground">{word.example}</p>
                {word.exampleVi && <p className="mt-1 text-xs text-muted-foreground">{word.exampleVi}</p>}
              </div>
              <span className="mt-4 flex items-center gap-1.5 text-xs text-muted-foreground">
                <RotateCw className="h-3.5 w-3.5" /> Bấm để xem lại chữ Hán
              </span>
            </>
          ) : (
            <>
              <strong className="font-hanzi text-6xl font-bold text-foreground tracking-wider" data-testid="flashcard-hanzi">{word.hanzi}</strong>
              <span className="mt-3 text-lg font-bold text-primary">{word.pinyin}</span>
              <span className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
                <RotateCw className="h-4 w-4" /> Bấm để lật thẻ
              </span>
            </>
          )}
        </button>
        <div className="mt-4 flex items-center justify-between">
          <button
            type="button"
            onClick={() => move(-1)}
            aria-label="Từ trước"
            className="flex h-11 w-11 items-center justify-center rounded-md border border-border hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="flashcard-prev-btn"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <button
            type="button"
            onClick={speak}
            className="flex min-h-11 items-center gap-2 rounded-md px-4 text-sm font-bold text-primary hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="flashcard-speak-btn"
          >
            <Volume2 className="h-4 w-4" />Nghe phát âm
          </button>
          <button
            type="button"
            onClick={() => move(1)}
            aria-label="Từ tiếp theo"
            className="flex h-11 w-11 items-center justify-center rounded-md border border-border hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="flashcard-next-btn"
          >
            <ArrowRight className="h-5 w-5" />
          </button>
        </div>
      </div>
    </section>
  );
}
