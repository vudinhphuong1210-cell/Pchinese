import React, { useState, useEffect } from 'react';
import { Mic, Grid, ChevronRight, CheckCircle2 } from 'lucide-react';
import { TopicFilter } from '../features/catalog/components/TopicFilter';
import { HskFilter } from '../features/catalog/components/HskFilter';
import { LessonCard } from '../features/catalog/components/LessonCard';
import { fetchTopics, fetchLessons } from '../api/catalog';

export function DashboardPage({ onOpenLesson }) {
  const [selectedTopic, setSelectedTopic] = useState('all');
  const [selectedHsk, setSelectedHsk] = useState('all');

  const [topics, setTopics] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [totalLessons, setTotalLessons] = useState(0);

  useEffect(() => {
    fetchTopics({ page: 0, size: 50 })
      .then(data => {
        setTopics(data.content);
      })
      .catch(err => console.error('Failed to load topics', err));
  }, []);

  useEffect(() => {
    fetchLessons({ topicId: selectedTopic, hskLevel: selectedHsk, page: 0, size: 20 })
      .then(data => {
        setLessons(data.content);
        setTotalLessons(data.totalElements);
      })
      .catch(err => console.error('Failed to load lessons', err));
  }, [selectedTopic, selectedHsk]);

  const handleLessonClick = (lessonId) => {
    if (onOpenLesson) {
      onOpenLesson(lessonId);
    }
  };

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

        {/* Metrics & Utility Actions (F05 doesn't provide real progress data yet) */}
        <div className="flex items-center space-x-5 border-t md:border-t-0 md:border-l border-border pt-3 md:pt-0 md:pl-5">
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
        <TopicFilter topics={topics} selectedTopic={selectedTopic} onSelect={setSelectedTopic} />

        {/* HSK Level Row */}
        <HskFilter selectedHsk={selectedHsk} onSelect={setSelectedHsk} />
      </section>

      {/* Lesson Grid (5 columns, no horizontal scroll) */}
      <div className="space-y-8">
        <section className="space-y-3.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <h2 className="text-lg font-bold text-foreground">Bài học</h2>
              <span className="px-2 py-0.5 rounded-md bg-secondary text-[11px] font-bold text-muted-foreground">
                {totalLessons} bài học
              </span>
            </div>
            {totalLessons > 0 && (
              <button type="button" className="text-xs font-semibold text-primary hover:underline flex items-center">
                <span>Xem thêm</span>
                <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
              </button>
            )}
          </div>

          {/* Grid containing 5 compact cards seamlessly */}
          {lessons.length > 0 ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3" data-testid="lesson-rail">
              {lessons.map((lesson) => (
                <LessonCard key={lesson.id} lesson={lesson} onClick={handleLessonClick} />
              ))}
            </div>
          ) : (
            <div className="text-center py-10">
              <p className="text-sm text-muted-foreground">Không tìm thấy bài học nào phù hợp.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
