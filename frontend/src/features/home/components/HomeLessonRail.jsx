import React, { useCallback, useEffect, useState } from 'react';
import { ArrowRight, BookOpen, Clock3, LockKeyhole, Play, RotateCcw } from 'lucide-react';
import { fetchLessons, fetchTopics } from '../../../api/catalog.js';

const HSK_LEVELS = ['all', 'hsk1', 'hsk2', 'hsk3', 'hsk4', 'hsk5', 'hsk6'];

function duration(seconds) {
  const total = Number(seconds) || 0;
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}

function LessonPreviewCard({ lesson, onOpenLesson }) {
  const hsk = lesson.hskLevel ? `HSK ${lesson.hskLevel}` : 'Tổng hợp';
  const thumbnail = lesson.thumbnailUrl || (lesson.youtubeVideoId ? `https://i.ytimg.com/vi/${lesson.youtubeVideoId}/hqdefault.jpg` : null);
  const [imgError, setImgError] = useState(false);

  return (
    <button type="button" onClick={() => onOpenLesson(lesson.id)} className="group min-w-0 overflow-hidden rounded-lg border border-border bg-card text-left transition hover:border-primary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" data-testid={`home-lesson-${lesson.id}`}>
      <span className="relative flex aspect-video items-center justify-center overflow-hidden bg-secondary">
        {thumbnail && !imgError ? (
          <img
            src={thumbnail}
            alt={lesson.title}
            loading="lazy"
            onError={() => setImgError(true)}
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <span className="font-hanzi text-4xl font-bold text-primary/70 transition-transform group-hover:scale-[1.02]" aria-hidden="true">学</span>
        )}
        <span className={`absolute left-3 top-3 z-10 rounded-sm px-2 py-1 text-[11px] font-bold text-primary-foreground ${lesson.hskLevel <= 2 ? 'bg-success' : lesson.hskLevel === 3 ? 'bg-info' : lesson.hskLevel === 4 ? 'bg-warning' : 'bg-destructive'}`}>{hsk}</span>
        <span className="absolute bottom-3 right-3 z-10 flex items-center gap-1 rounded-sm bg-background/90 px-2 py-1 text-[11px] font-bold text-foreground"><Clock3 className="h-3 w-3" />{duration(lesson.estimatedDurationSeconds)}</span>
        <span className="absolute inset-0 z-10 flex items-center justify-center bg-background/20 opacity-0 transition-opacity group-hover:opacity-100"><span className="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-md"><Play className="h-5 w-5 fill-current" /></span></span>
      </span>
      <span className="block p-4">
        <span className="line-clamp-2 min-h-12 font-hanzi text-base font-bold leading-6 text-foreground">{lesson.title}</span>
        <span className="mt-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span>{lesson.publishedSegmentCount || 0} phân đoạn</span>
          <span className="flex items-center gap-1 font-semibold">{lesson.accessLevel === 'PREMIUM' && <LockKeyhole className="h-3 w-3 text-warning" />}{lesson.accessLevel === 'PREMIUM' ? 'Premium' : 'Miễn phí'}</span>
        </span>
      </span>
    </button>
  );
}

export function HomeLessonRail({ onOpenLesson, onViewAllCatalog }) {
  const [topics, setTopics] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [selectedTopic, setSelectedTopic] = useState('all');
  const [selectedHsk, setSelectedHsk] = useState('all');
  const [total, setTotal] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadTopics = useCallback(async () => {
    const data = await fetchTopics({ page: 0, size: 50 });
    setTopics(data?.content || []);
  }, []);

  const loadLessons = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchLessons({ topicId: selectedTopic, hskLevel: selectedHsk, page: 0, size: 10 });
      setLessons(data?.content || []);
      setTotal(data?.totalElements || 0);
    } catch (requestError) {
      setError(requestError?.error?.message || 'Chưa thể tải bài học nổi bật.');
      setLessons([]);
    } finally {
      setIsLoading(false);
    }
  }, [selectedHsk, selectedTopic]);

  useEffect(() => {
    void loadTopics().catch(() => setTopics([]));
  }, [loadTopics]);

  useEffect(() => {
    void loadLessons();
  }, [loadLessons]);

  return (
    <section id="featured-lessons" className="scroll-mt-6" aria-labelledby="lessons-title">
      <div className="flex flex-col gap-4 border-b border-border pb-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div><p className="text-xs font-bold uppercase tracking-widest text-primary">Bắt đầu từ nội dung thật</p><h2 id="lessons-title" className="mt-2 text-2xl font-bold text-foreground sm:text-3xl">Bài học dành cho bạn</h2></div>
          <button type="button" onClick={onViewAllCatalog} className="flex min-h-11 items-center gap-2 self-start rounded-md px-3 text-sm font-bold text-primary hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">Xem toàn bộ thư viện<ArrowRight className="h-4 w-4" /></button>
        </div>
        <div className="space-y-3">
          <div className="flex gap-2 overflow-x-auto pb-1" aria-label="Lọc theo chủ đề">
            {[{ id: 'all', title: 'Tất cả chủ đề' }, ...topics].map((topic) => <button key={topic.id} type="button" aria-pressed={selectedTopic === topic.id} onClick={() => setSelectedTopic(topic.id)} className={`min-h-11 shrink-0 rounded-pill px-4 text-sm font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${selectedTopic === topic.id ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent hover:text-foreground'}`}>{topic.title}</button>)}
          </div>
          <div className="flex gap-1 overflow-x-auto" aria-label="Lọc theo HSK">
            {HSK_LEVELS.map((level) => <button key={level} type="button" aria-pressed={selectedHsk === level} onClick={() => setSelectedHsk(level)} className={`min-h-11 shrink-0 rounded-pill px-4 text-sm font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${selectedHsk === level ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent hover:text-foreground'}`}>{level === 'all' ? 'Tất cả cấp độ' : level.toUpperCase().replace('HSK', 'HSK ')}</button>)}
          </div>
        </div>
      </div>

      <div className="mt-5" aria-live="polite">
        {isLoading ? (
          <div className="grid grid-cols-1 gap-4 min-[440px]:grid-cols-2 md:grid-cols-3 2xl:grid-cols-5" role="status" aria-label="Đang tải bài học">{Array.from({ length: 5 }).map((_, index) => <div key={index} className="overflow-hidden rounded-lg border border-border bg-card"><div className="aspect-video animate-pulse bg-muted" /><div className="space-y-3 p-4"><div className="h-4 animate-pulse rounded bg-muted" /><div className="h-4 w-2/3 animate-pulse rounded bg-muted" /></div></div>)}</div>
        ) : error ? (
          <div className="flex min-h-48 flex-col items-center justify-center rounded-xl border border-border bg-card p-6 text-center"><RotateCcw className="h-8 w-8 text-primary" /><p className="mt-3 font-bold text-foreground">Không tải được bài học</p><p className="mt-1 text-sm text-muted-foreground">{error}</p><button type="button" onClick={loadLessons} className="mt-4 min-h-11 rounded-md border border-border px-4 text-sm font-bold text-foreground hover:bg-accent">Thử lại</button></div>
        ) : lessons.length === 0 ? (
          <div className="flex min-h-48 flex-col items-center justify-center rounded-xl border border-border bg-card p-6 text-center"><BookOpen className="h-9 w-9 text-primary" /><p className="mt-3 font-bold text-foreground">Không tìm thấy bài học</p><p className="mt-1 text-sm text-muted-foreground">Hãy đổi chủ đề hoặc cấp độ HSK để xem lựa chọn khác.</p></div>
        ) : (
          <><p className="mb-3 text-xs font-semibold text-muted-foreground">Hiển thị {lessons.length} trong {total} bài phù hợp</p><div className="grid grid-cols-1 gap-4 min-[440px]:grid-cols-2 md:grid-cols-3 2xl:grid-cols-5">{lessons.map((lesson) => <LessonPreviewCard key={lesson.id} lesson={lesson} onOpenLesson={onOpenLesson} />)}</div></>
        )}
      </div>
    </section>
  );
}
