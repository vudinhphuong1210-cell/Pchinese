import React, { useState, useEffect } from 'react';
import { ArrowLeft, Clock, Play } from 'lucide-react';
import { fetchLessonDetail } from '../../api/catalog';
import { authSessionStore } from '../auth/authSessionStore';

const formatDuration = (seconds) => {
  if (!seconds) return '00:00';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};

export function LessonDetailPage({ lessonId, onBack, onLogin, onStartLearning }) {
  const [lesson, setLesson] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const isAuth = authSessionStore.getState().isAuthenticated;

  useEffect(() => {
    setLoading(true);
    fetchLessonDetail(lessonId)
      .then(data => {
        setLesson(data);
        setError(null);
      })
      .catch(err => {
        setError('Bài học không tồn tại hoặc đã bị ẩn.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [lessonId]);

  if (loading) {
    return <div className="p-10 text-center text-muted-foreground">Đang tải dữ liệu...</div>;
  }

  if (error || !lesson) {
    return (
      <div className="p-10 text-center space-y-4">
        <p className="text-destructive font-semibold">{error}</p>
        <button
          onClick={onBack}
          className="inline-flex items-center text-primary hover:underline font-semibold"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Quay lại catalog
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-10" data-testid="lesson-detail-page">
      <button
        onClick={onBack}
        className="inline-flex items-center text-muted-foreground hover:text-foreground font-semibold text-sm transition-colors"
      >
        <ArrowLeft className="w-4 h-4 mr-1.5" />
        Trở về
      </button>

      <div className="bg-card border border-border rounded-xl p-6 md:p-8 space-y-6 shadow-sm">
        <div className="space-y-2">
          <div className="flex items-center space-x-3 mb-2">
            {lesson.hskLevel && (
              <span className="px-2.5 py-0.5 rounded-sm text-xs font-bold bg-primary text-primary-foreground">
                HSK {lesson.hskLevel}
              </span>
            )}
            <div className="flex items-center space-x-1 text-xs font-medium text-muted-foreground bg-secondary px-2 py-0.5 rounded-sm">
              <Clock className="w-3.5 h-3.5" />
              <span>{formatDuration(lesson.estimatedDurationSeconds)}</span>
            </div>
            <span className="text-xs font-medium text-muted-foreground bg-secondary px-2 py-0.5 rounded-sm">
              {lesson.publishedSegmentCount} tập
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-bold text-foreground font-hanzi leading-tight">
            {lesson.title}
          </h1>
          <p className="text-base text-muted-foreground">
            {lesson.description}
          </p>
        </div>

        <div className="pt-6 border-t border-border flex justify-center">
          {isAuth ? (
            <button
              type="button"
              onClick={() => onStartLearning && onStartLearning(lessonId)}
              className="h-12 px-8 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-pill shadow-md flex items-center space-x-2 transition-transform active:scale-95"
            >
              <Play className="w-5 h-5 fill-current" />
              <span>Bắt đầu học ngay</span>
            </button>
          ) : (

            <div className="text-center space-y-3">
              <p className="text-sm text-muted-foreground">Bạn cần đăng nhập để bắt đầu bài học này.</p>
              <button
                onClick={onLogin}
                className="h-10 px-6 bg-secondary hover:bg-secondary/80 border border-border text-foreground font-bold rounded-pill transition-colors"
              >
                Đăng nhập ngay
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
