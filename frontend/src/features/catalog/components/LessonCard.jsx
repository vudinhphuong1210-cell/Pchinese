import React from 'react';
import { Play, Clock } from 'lucide-react';

const HSK_COLORS = {
  1: 'bg-emerald-600',
  2: 'bg-emerald-600',
  3: 'bg-sky-600',
  4: 'bg-amber-600',
  5: 'bg-orange-600',
  6: 'bg-rose-600',
};

const BGS = [
  'from-purple-900/70 to-slate-900/90',
  'from-emerald-900/70 to-slate-900/90',
  'from-rose-900/70 to-slate-900/90',
  'from-amber-900/70 to-slate-900/90',
  'from-blue-900/70 to-slate-900/90',
  'from-sky-900/70 to-slate-900/90',
  'from-indigo-900/70 to-slate-900/90',
];

const formatDuration = (seconds) => {
  if (!seconds) return '00:00';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};

export function LessonCard({ lesson, onClick }) {
  const hskBg = lesson.hskLevel ? HSK_COLORS[lesson.hskLevel] || 'bg-slate-600' : 'bg-slate-600';
  const hskText = lesson.hskLevel ? `HSK ${lesson.hskLevel}` : 'All';
  
  // deterministic background based on id length or char codes
  const bgIndex = lesson.id ? lesson.id.charCodeAt(0) % BGS.length : 0;
  const imageBg = BGS[bgIndex];

  return (
    <div
      className="bg-card border border-border rounded-lg overflow-hidden group hover:border-primary/50 transition-all cursor-pointer shadow-sm flex flex-col justify-between"
      data-testid={`lesson-card-${lesson.id}`}
      onClick={() => onClick(lesson.id)}
    >
      {/* 16:9 Thumbnail Cover */}
      <div className={`relative aspect-video bg-gradient-to-br ${imageBg} p-2.5 flex flex-col justify-between overflow-hidden shrink-0`}>
        <div className="flex items-center justify-between z-10">
          <span className={`px-2 py-0.5 rounded-sm text-[10px] font-bold text-white ${hskBg}`}>
            {hskText}
          </span>
          <div className="w-7 h-7 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition-opacity">
            <Play className="w-3.5 h-3.5 fill-white ml-0.5" />
          </div>
        </div>

        <div className="flex items-center justify-between text-[10px] text-white/90 z-10">
          <span className="font-semibold bg-black/70 px-1.5 py-0.5 rounded-sm">
            {lesson.publishedSegmentCount || 0} tập
          </span>
          <div className="flex items-center space-x-1 bg-black/80 px-1.5 py-0.5 rounded-sm">
            <Clock className="w-3 h-3" />
            <span>{formatDuration(lesson.estimatedDurationSeconds)}</span>
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
          <p className="text-[11px] text-muted-foreground line-clamp-1 mt-0.5" title={lesson.description}>
            {lesson.description}
          </p>
        </div>
      </div>
    </div>
  );
}
