import React, { useState } from 'react';
import { Mic, Grid, Clock, ChevronRight, CheckCircle2, Play } from 'lucide-react';

const TOPIC_FILTERS = [
  { id: 'all', label: 'Tất cả', count: 24 },
  { id: 'hub', label: 'Chinese Listening Hub', count: 8 },
  { id: 'tech', label: 'Công nghệ – Khoa học máy tính – 科技', count: 6 },
  { id: 'stories', label: 'Everyday Chinese Stories', count: 5 },
  { id: 'family', label: 'Gia đình – 家庭', count: 5 },
];

const HSK_FILTERS = [
  { id: 'all', label: 'Tất cả cấp độ' },
  { id: 'hsk1', label: 'HSK 1', color: 'bg-emerald-600 text-white' },
  { id: 'hsk2', label: 'HSK 2', color: 'bg-emerald-600 text-white' },
  { id: 'hsk3', label: 'HSK 3', color: 'bg-sky-600 text-white' },
  { id: 'hsk4', label: 'HSK 4', color: 'bg-amber-600 text-white' },
  { id: 'hsk5', label: 'HSK 5', color: 'bg-orange-600 text-white' },
  { id: 'hsk6', label: 'HSK 6', color: 'bg-rose-600 text-white' },
];

const LESSONS = [
  {
    id: 'l1',
    title: '人工智能与未来的生活',
    subtitle: 'Trí tuệ nhân tạo & cuộc sống tương lai',
    hsk: 'HSK 4',
    hskBg: 'bg-amber-600',
    duration: '04:15',
    segments: 14,
    imageBg: 'from-purple-900/70 to-slate-900/90',
    progress: 75,
  },
  {
    id: 'l2',
    title: '在中国怎么用微信支付？',
    subtitle: 'Cách dùng WeChat Pay ở Trung Quốc',
    hsk: 'HSK 3',
    hskBg: 'bg-sky-600',
    duration: '03:40',
    segments: 10,
    imageBg: 'from-emerald-900/70 to-slate-900/90',
    progress: 100,
  },
  {
    id: 'l3',
    title: '家庭聚会与传统美食',
    subtitle: 'Bữa cơm gia đình & món ăn truyền thống',
    hsk: 'HSK 2',
    hskBg: 'bg-emerald-600',
    duration: '02:50',
    segments: 8,
    imageBg: 'from-rose-900/70 to-slate-900/90',
    progress: 0,
  },
  {
    id: 'l4',
    title: '乘坐高铁去上海旅行',
    subtitle: 'Đi tàu cao tốc du lịch Thượng Hải',
    hsk: 'HSK 5',
    hskBg: 'bg-orange-600',
    duration: '06:10',
    segments: 18,
    imageBg: 'from-amber-900/70 to-slate-900/90',
    progress: 30,
  },
  {
    id: 'l5',
    title: '北京胡同与传统文化',
    subtitle: 'Ngõ cổ Bắc Kinh & văn hóa truyền thống',
    hsk: 'HSK 3',
    hskBg: 'bg-sky-600',
    duration: '05:15',
    segments: 12,
    imageBg: 'from-blue-900/70 to-slate-900/90',
    progress: 50,
  },
];

export function DashboardPage() {
  const [selectedTopic, setSelectedTopic] = useState('all');
  const [selectedHsk, setSelectedHsk] = useState('all');

  return (
    <div className="space-y-6 pb-10" data-testid="dashboard-page">
      {/* Workspace Header */}
      <header className="min-h-[76px] flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border pb-5">
        <div className="flex items-start space-x-3.5">
          <div className="w-11 h-11 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary shrink-0">
            <Mic className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-foreground">Luyện Shadowing</h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              Chọn bài học video để luyện phản xạ nói & ngữ điệu chuẩn Trung Quốc.
            </p>
          </div>
        </div>

        {/* Metrics & Utility Actions */}
        <div className="flex items-center space-x-5 border-t md:border-t-0 md:border-l border-border pt-3 md:pt-0 md:pl-5">
          <div className="flex items-center space-x-4">
            <div>
              <span className="block text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">Đang học</span>
              <span className="text-base font-bold text-foreground">5 bài</span>
            </div>
            <div className="w-px h-7 bg-border" />
            <div>
              <span className="block text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">Đã hoàn thành</span>
              <div className="flex items-center space-x-1 text-success font-bold text-base">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>12 bài</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            className="h-9 px-3.5 bg-secondary border border-border text-foreground font-semibold rounded-md hover:bg-accent text-xs flex items-center space-x-1.5 transition-colors"
          >
            <Grid className="w-3.5 h-3.5 text-primary" />
            <span>Xem chủ đề</span>
          </button>
        </div>
      </header>

      {/* Filter Dock */}
      <section className="space-y-3 border-b border-border pb-5" data-testid="filter-dock">
        {/* Topic Row */}
        <div className="flex flex-wrap gap-2">
          {TOPIC_FILTERS.map((topic) => (
            <button
              key={topic.id}
              type="button"
              onClick={() => setSelectedTopic(topic.id)}
              className={`h-9 px-4 rounded-pill text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                selectedTopic === topic.id
                  ? 'bg-primary text-primary-foreground shadow-sm'
                  : 'bg-secondary/40 hover:bg-secondary text-foreground border border-border'
              }`}
            >
              <span>{topic.label}</span>
              <span className={`text-[11px] font-medium ${selectedTopic === topic.id ? 'opacity-80' : 'text-muted-foreground'}`}>
                ({topic.count})
              </span>
            </button>
          ))}
        </div>

        {/* HSK Level Row */}
        <div className="flex flex-wrap gap-1.5 pt-0.5">
          {HSK_FILTERS.map((hsk) => (
            <button
              key={hsk.id}
              type="button"
              onClick={() => setSelectedHsk(hsk.id)}
              className={`h-8 px-3 rounded-md text-[11px] font-bold transition-all ${
                selectedHsk === hsk.id
                  ? 'bg-primary text-primary-foreground ring-2 ring-primary ring-offset-2 ring-offset-background'
                  : 'bg-secondary/30 hover:bg-secondary text-muted-foreground'
              }`}
            >
              {hsk.label}
            </button>
          ))}
        </div>
      </section>

      {/* Lesson Grid (5 columns, no horizontal scroll) */}
      <div className="space-y-8">
        <section className="space-y-3.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <h2 className="text-lg font-bold text-foreground">Bài học mới nhất</h2>
              <span className="px-2 py-0.5 rounded-md bg-secondary text-[11px] font-bold text-muted-foreground">
                5 bài học
              </span>
            </div>
            <button type="button" className="text-xs font-semibold text-primary hover:underline flex items-center">
              <span>Xem thêm</span>
              <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
            </button>
          </div>

          {/* Grid containing 5 compact cards seamlessly */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3" data-testid="lesson-rail">
            {LESSONS.map((lesson) => (
              <div
                key={lesson.id}
                className="bg-card border border-border rounded-lg overflow-hidden group hover:border-primary/50 transition-all cursor-pointer shadow-sm flex flex-col justify-between"
                data-testid={`lesson-card-${lesson.id}`}
              >
                {/* 16:9 Thumbnail Cover */}
                <div className={`relative aspect-video bg-gradient-to-br ${lesson.imageBg} p-2.5 flex flex-col justify-between overflow-hidden shrink-0`}>
                  <div className="flex items-center justify-between z-10">
                    <span className={`px-2 py-0.5 rounded-sm text-[10px] font-bold text-white ${lesson.hskBg}`}>
                      {lesson.hsk}
                    </span>
                    <div className="w-7 h-7 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition-opacity">
                      <Play className="w-3.5 h-3.5 fill-white ml-0.5" />
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-[10px] text-white/90 z-10">
                    <span className="font-semibold bg-black/70 px-1.5 py-0.5 rounded-sm">
                      {lesson.segments} tập
                    </span>
                    <div className="flex items-center space-x-1 bg-black/80 px-1.5 py-0.5 rounded-sm">
                      <Clock className="w-3 h-3" />
                      <span>{lesson.duration}</span>
                    </div>
                  </div>

                  <div className="absolute inset-0 bg-black/20 group-hover:bg-black/0 transition-colors" />
                </div>

                {/* Body Compact */}
                <div className="p-3 space-y-1.5 flex-1 flex flex-col justify-between">
                  <div>
                    <h3 className="text-xs font-bold text-foreground line-clamp-1 font-hanzi leading-snug" title={lesson.title}>
                      {lesson.title}
                    </h3>
                    <p className="text-[11px] text-muted-foreground line-clamp-1 mt-0.5" title={lesson.subtitle}>
                      {lesson.subtitle}
                    </p>
                  </div>

                  {/* Progress bar if started */}
                  {lesson.progress > 0 && (
                    <div className="pt-1 space-y-1">
                      <div className="flex justify-between text-[10px] font-semibold">
                        <span className="text-primary">Đang học</span>
                        <span className="text-muted-foreground">{lesson.progress}%</span>
                      </div>
                      <div className="w-full h-1 bg-secondary rounded-pill overflow-hidden">
                        <div
                          className="h-full bg-primary rounded-pill transition-all"
                          style={{ width: `${lesson.progress}%` }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
