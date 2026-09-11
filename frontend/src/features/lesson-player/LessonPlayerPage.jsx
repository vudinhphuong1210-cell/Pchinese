import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  ArrowLeft,
  Play,
  Pause,
  RotateCcw,
  SkipBack,
  SkipForward,
  Repeat,
  Sparkles,
  BookOpen,
  Edit3,
  Mic,
  CheckCircle2,
  Lock,
  Layers,
  Volume2,
  AlertCircle,
} from 'lucide-react';
import { fetchLessonPlayback, submitPlaybackEvent } from '../../api/lessonPlayback.js';
import { DictationPanel } from '../dictation/DictationPanel.jsx';
import { ShadowingPage } from '../shadowing/ShadowingPage.jsx';

export function LessonPlayerPage({ lessonId, onBack, onOpenUpgradeModal }) {
  const [playbackData, setPlaybackData] = useState(null);
  const [activeSegmentIndex, setActiveSegmentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [playbackRate, setPlaybackRate] = useState(1.0);
  const [isLoopingSegment, setIsLoopingSegment] = useState(false);
  const [practiceMode, setPracticeMode] = useState('listen'); // 'listen' | 'dictation' | 'shadowing'
  const [showPinyin, setShowPinyin] = useState(true);
  const [showVietnamese, setShowVietnamese] = useState(true);
  const [completedSegments, setCompletedSegments] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const mediaRef = useRef(null);
  const heartbeatTimerRef = useRef(null);

  // Fetch playback details
  useEffect(() => {
    setLoading(true);
    setError(null);
    fetchLessonPlayback(lessonId)
      .then((data) => {
        setPlaybackData(data);
        // Pre-fill completed segments
        const completed = new Set();
        (data.segments || []).forEach((seg) => {
          if (seg.playbackState === 'COMPLETED') {
            completed.add(seg.segmentId);
          }
        });
        setCompletedSegments(completed);
      })
      .catch((err) => {
        if (err.status === 403 || err.code === 'FORBIDDEN') {
          setError('PREMIUM_REQUIRED');
        } else {
          setError(err.message || 'Không thể tải nội dung bài học.');
        }
      })
      .finally(() => {
        setLoading(false);
      });

    return () => {
      if (heartbeatTimerRef.current) clearInterval(heartbeatTimerRef.current);
    };
  }, [lessonId]);

  // Periodic heartbeat sync
  useEffect(() => {
    if (!playbackData?.sessionToken) return;

    heartbeatTimerRef.current = setInterval(() => {
      if (isPlaying) {
        submitPlaybackEvent(lessonId, {
          sessionToken: playbackData.sessionToken,
          segmentId: playbackData.segments?.[activeSegmentIndex]?.segmentId,
          eventType: 'PLAYBACK_HEARTBEAT',
          currentPositionSeconds: Math.floor(currentTime),
        }).catch(() => {
          // Heartbeat failures are non-fatal
        });
      }
    }, 12000);

    return () => {
      if (heartbeatTimerRef.current) clearInterval(heartbeatTimerRef.current);
    };
  }, [lessonId, playbackData?.sessionToken, isPlaying, currentTime, activeSegmentIndex]);

  const activeSegment = playbackData?.segments?.[activeSegmentIndex] || null;

  // Jump to specific segment
  const handleSelectSegment = useCallback(
    (index) => {
      if (!playbackData?.segments?.[index]) return;
      setActiveSegmentIndex(index);
      const seg = playbackData.segments[index];
      if (mediaRef.current) {
        mediaRef.current.currentTime = seg.startTimeSeconds;
        mediaRef.current.play().catch(() => {});
        setIsPlaying(true);
      }
    },
    [playbackData],
  );

  // Handle media time update
  const handleTimeUpdate = () => {
    if (!mediaRef.current || !playbackData?.segments?.length) return;
    const current = mediaRef.current.currentTime;
    setCurrentTime(current);

    if (activeSegment) {
      // Loop or stop if reaching segment end
      if (current >= activeSegment.endTimeSeconds) {
        if (isLoopingSegment) {
          mediaRef.current.currentTime = activeSegment.startTimeSeconds;
          mediaRef.current.play().catch(() => {});
        } else {
          // Auto complete segment progress
          handleSegmentComplete(activeSegment.segmentId);
        }
      }
    }
  };

  const handleSegmentComplete = (segmentId) => {
    if (!completedSegments.has(segmentId)) {
      setCompletedSegments((prev) => new Set([...prev, segmentId]));
      if (playbackData?.sessionToken) {
        submitPlaybackEvent(lessonId, {
          sessionToken: playbackData.sessionToken,
          segmentId,
          eventType: 'SEGMENT_COMPLETE',
          currentPositionSeconds: Math.floor(currentTime),
        }).catch(() => {});
      }
    }
  };

  const togglePlayPause = () => {
    if (!mediaRef.current) return;
    if (isPlaying) {
      mediaRef.current.pause();
      setIsPlaying(false);
      if (playbackData?.sessionToken) {
        submitPlaybackEvent(lessonId, {
          sessionToken: playbackData.sessionToken,
          segmentId: activeSegment?.segmentId,
          eventType: 'LESSON_PAUSE',
          currentPositionSeconds: Math.floor(currentTime),
        }).catch(() => {});
      }
    } else {
      mediaRef.current.play().catch(() => {});
      setIsPlaying(true);
      if (playbackData?.sessionToken) {
        submitPlaybackEvent(lessonId, {
          sessionToken: playbackData.sessionToken,
          segmentId: activeSegment?.segmentId,
          eventType: 'LESSON_RESUME',
          currentPositionSeconds: Math.floor(currentTime),
        }).catch(() => {});
      }
    }
  };

  const handleReplaySegment = () => {
    if (!mediaRef.current || !activeSegment) return;
    mediaRef.current.currentTime = activeSegment.startTimeSeconds;
    mediaRef.current.play().catch(() => {});
    setIsPlaying(true);
  };

  const handleNextSegment = () => {
    if (playbackData?.segments && activeSegmentIndex < playbackData.segments.length - 1) {
      handleSelectSegment(activeSegmentIndex + 1);
    }
  };

  const handlePrevSegment = () => {
    if (activeSegmentIndex > 0) {
      handleSelectSegment(activeSegmentIndex - 1);
    }
  };

  const changePlaybackRate = (rate) => {
    setPlaybackRate(rate);
    if (mediaRef.current) {
      mediaRef.current.playbackRate = rate;
    }
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto p-12 text-center text-muted-foreground animate-pulse">
        Đang chuẩn bị không gian học tập tương tác...
      </div>
    );
  }

  if (error === 'PREMIUM_REQUIRED') {
    return (
      <div className="max-w-xl mx-auto p-8 bg-card border border-border rounded-2xl text-center space-y-6 shadow-md mt-10">
        <div className="w-16 h-16 rounded-full bg-primary/20 text-primary flex items-center justify-center mx-auto">
          <Lock className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <h2 className="text-2xl font-bold text-foreground">Nội dung dành cho Hội viên Premium</h2>
          <p className="text-sm text-muted-foreground leading-relaxed">
            Bài học này thuộc chương trình nâng cao. Hãy nâng cấp tài khoản Premium để mở khóa toàn bộ bài giảng, phòng nghe chép chính tả và chấm phát âm AI không giới hạn.
          </p>
        </div>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
          <button
            type="button"
            onClick={onBack}
            className="w-full sm:w-auto px-6 py-2.5 bg-secondary hover:bg-secondary/80 text-foreground font-semibold rounded-pill transition-colors text-sm"
          >
            Quay lại
          </button>
          <button
            type="button"
            onClick={onOpenUpgradeModal}
            className="w-full sm:w-auto px-8 py-2.5 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-pill shadow-md transition-transform active:scale-95 text-sm"
          >
            Nâng cấp Premium ngay
          </button>
        </div>
      </div>
    );
  }

  if (error || !playbackData) {
    return (
      <div className="max-w-lg mx-auto p-8 text-center space-y-4">
        <AlertCircle className="w-10 h-10 text-destructive mx-auto" />
        <p className="text-destructive font-semibold">{error || 'Không tìm thấy bài học.'}</p>
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center text-primary hover:underline font-semibold text-sm"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Quay lại danh sách bài học
        </button>
      </div>
    );
  }

  // If in dedicated full-page Shadowing mode
  if (practiceMode === 'shadowing' && activeSegment) {
    return (
      <ShadowingPage
        segment={activeSegment}
        onBack={() => setPracticeMode('listen')}
        onPlayNativeSegment={() => handleReplaySegment()}
        onNextSegment={activeSegmentIndex < (playbackData.segments?.length || 0) - 1 ? handleNextSegment : null}
      />
    );
  }

  const completionPercent = playbackData.segments?.length
    ? Math.round((completedSegments.size / playbackData.segments.length) * 100)
    : 0;

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12" data-testid="lesson-player-page">
      {/* Top Bar Navigation & Progress */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <button
            type="button"
            onClick={onBack}
            className="p-2 rounded-full hover:bg-secondary text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-xl md:text-2xl font-bold font-hanzi text-foreground">
              {playbackData.title}
            </h1>
            <div className="text-xs text-muted-foreground flex items-center space-x-2">
              <span>Đoạn {activeSegmentIndex + 1} / {playbackData.segments?.length || 0}</span>
              <span>•</span>
              <span className="text-primary font-bold">{completionPercent}% hoàn thành</span>
            </div>
          </div>
        </div>

        {/* Practice Mode Switcher */}
        <div className="flex items-center bg-card border border-border p-1 rounded-pill shadow-sm">
          <button
            type="button"
            onClick={() => setPracticeMode('listen')}
            className={`px-3 py-1.5 rounded-pill text-xs font-bold transition-all flex items-center space-x-1.5 ${
              practiceMode === 'listen' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>Nghe & Đọc</span>
          </button>

          <button
            type="button"
            onClick={() => setPracticeMode('dictation')}
            className={`px-3 py-1.5 rounded-pill text-xs font-bold transition-all flex items-center space-x-1.5 ${
              practiceMode === 'dictation' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <Edit3 className="w-3.5 h-3.5" />
            <span>Nghe chép (F07)</span>
          </button>

          <button
            type="button"
            onClick={() => setPracticeMode('shadowing')}
            className={`px-3 py-1.5 rounded-pill text-xs font-bold transition-all flex items-center space-x-1.5 ${
              practiceMode === 'shadowing' ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <Mic className="w-3.5 h-3.5" />
            <span>Nói Shadowing (F08)</span>
          </button>
        </div>
      </div>

      {/* Main Grid: Player Stage (Left) & Segment List (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Player & Active Segment */}
        <div className="lg:col-span-2 space-y-6">
          {/* Media Player Box with Anti-Piracy Watermark */}
          <div className="relative bg-black rounded-2xl overflow-hidden shadow-lg border border-border aspect-video flex flex-col justify-end">
            {/* Native HTML5 Audio / Video or Media element */}
            <video
              ref={mediaRef}
              src={playbackData.mediaUrl}
              onTimeUpdate={handleTimeUpdate}
              onPlay={() => setIsPlaying(true)}
              onPause={() => setIsPlaying(false)}
              className="w-full h-full object-contain"
            />

            {/* Forensic Watermark Overlay (Anti-piracy per F06) */}
            {playbackData.forensicWatermark && (
              <div className="absolute top-3 right-3 text-[10px] font-mono text-white/30 select-none pointer-events-none tracking-widest uppercase">
                UID: {playbackData.forensicWatermark.userId?.slice(0, 8)} • {playbackData.forensicWatermark.sessionToken?.slice(0, 8)}
              </div>
            )}

            {/* On-screen Subtitle Overlay */}
            {activeSegment && (
              <div className="absolute inset-x-0 bottom-16 p-4 text-center bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none">
                <div className="text-2xl md:text-3xl font-bold font-hanzi text-white drop-shadow-md">
                  {activeSegment.hanziText}
                </div>
                {showPinyin && activeSegment.pinyinText && (
                  <div className="text-sm font-mono text-pink-300 drop-shadow">
                    {activeSegment.pinyinText}
                  </div>
                )}
                {showVietnamese && activeSegment.vietnameseText && (
                  <div className="text-xs text-white/80 drop-shadow mt-1">
                    {activeSegment.vietnameseText}
                  </div>
                )}
              </div>
            )}

            {/* Floating Custom Controls Bar */}
            <div className="absolute inset-x-0 bottom-0 bg-black/70 backdrop-blur-sm p-3 flex items-center justify-between text-white text-xs border-t border-white/10">
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={togglePlayPause}
                  className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
                >
                  {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
                </button>

                <button
                  type="button"
                  onClick={handleReplaySegment}
                  title="Nghe lại đoạn này"
                  className="p-2 hover:bg-white/10 rounded-full transition-colors"
                >
                  <RotateCcw className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={handlePrevSegment}
                  disabled={activeSegmentIndex === 0}
                  className="p-2 hover:bg-white/10 disabled:opacity-30 rounded-full transition-colors"
                >
                  <SkipBack className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={handleNextSegment}
                  disabled={activeSegmentIndex >= (playbackData.segments?.length || 0) - 1}
                  className="p-2 hover:bg-white/10 disabled:opacity-30 rounded-full transition-colors"
                >
                  <SkipForward className="w-4 h-4" />
                </button>
              </div>

              {/* Subtitle & Speed Controls */}
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={() => setIsLoopingSegment(!isLoopingSegment)}
                  className={`p-1.5 rounded-full transition-colors ${
                    isLoopingSegment ? 'bg-primary text-white' : 'hover:bg-white/10 text-white/70'
                  }`}
                  title="Lặp lại câu này"
                >
                  <Repeat className="w-3.5 h-3.5" />
                </button>

                <button
                  type="button"
                  onClick={() => setShowPinyin(!showPinyin)}
                  className={`px-2 py-0.5 rounded text-[11px] font-bold transition-colors ${
                    showPinyin ? 'bg-primary text-white' : 'bg-white/10 text-white/50'
                  }`}
                >
                  Pinyin
                </button>

                <button
                  type="button"
                  onClick={() => setShowVietnamese(!showVietnamese)}
                  className={`px-2 py-0.5 rounded text-[11px] font-bold transition-colors ${
                    showVietnamese ? 'bg-primary text-white' : 'bg-white/10 text-white/50'
                  }`}
                >
                  Nghĩa
                </button>

                {/* Speed Selector */}
                <div className="flex items-center bg-white/10 rounded-full p-0.5">
                  {[0.75, 1.0, 1.25].map((rate) => (
                    <button
                      key={rate}
                      type="button"
                      onClick={() => changePlaybackRate(rate)}
                      className={`px-1.5 py-0.5 rounded-full font-bold text-[10px] transition-colors ${
                        playbackRate === rate ? 'bg-primary text-white' : 'text-white/60 hover:text-white'
                      }`}
                    >
                      {rate}x
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Interactive Mode Content Area */}
          {practiceMode === 'listen' && activeSegment && (
            <div className="bg-card border border-border rounded-2xl p-6 space-y-4 shadow-sm">
              <div className="flex items-center justify-between text-xs text-muted-foreground pb-2 border-b border-border">
                <span className="font-bold uppercase tracking-wider text-primary">Phân đoạn hiện tại</span>
                <span>
                  {activeSegment.startTimeSeconds}s - {activeSegment.endTimeSeconds}s
                </span>
              </div>

              <div className="space-y-2">
                <div className="text-2xl md:text-3xl font-bold font-hanzi text-foreground">
                  {activeSegment.hanziText}
                </div>
                {activeSegment.pinyinText && (
                  <div className="text-base font-mono text-primary font-medium">
                    {activeSegment.pinyinText}
                  </div>
                )}
                {activeSegment.vietnameseText && (
                  <div className="text-sm text-muted-foreground">
                    {activeSegment.vietnameseText}
                  </div>
                )}
              </div>

              {/* Quick Actions */}
              <div className="flex flex-wrap items-center gap-3 pt-4 border-t border-border">
                <button
                  type="button"
                  onClick={() => setPracticeMode('dictation')}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 bg-secondary hover:bg-secondary/80 text-foreground text-xs font-bold rounded-pill transition-colors"
                >
                  <Edit3 className="w-3.5 h-3.5 text-primary" />
                  <span>Nghe chép đoạn này</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPracticeMode('shadowing')}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 bg-secondary hover:bg-secondary/80 text-foreground text-xs font-bold rounded-pill transition-colors"
                >
                  <Mic className="w-3.5 h-3.5 text-primary" />
                  <span>Luyện nói AI đoạn này</span>
                </button>
              </div>
            </div>
          )}

          {practiceMode === 'dictation' && activeSegment && (
            <DictationPanel
              segment={activeSegment}
              onPlaySegment={handleReplaySegment}
              onCompleted={handleSegmentComplete}
              onNextSegment={activeSegmentIndex < (playbackData.segments?.length || 0) - 1 ? handleNextSegment : null}
            />
          )}
        </div>

        {/* Right Column: Segment Transcript & Progression */}
        <div className="bg-card border border-border rounded-2xl p-5 space-y-4 shadow-sm flex flex-col h-full max-h-[600px]">
          <div className="flex items-center justify-between pb-3 border-b border-border">
            <div className="flex items-center space-x-2">
              <Layers className="w-4 h-4 text-primary" />
              <h3 className="text-sm font-bold text-foreground">Mục lục phân đoạn</h3>
            </div>
            <span className="text-xs text-muted-foreground">
              {completedSegments.size}/{playbackData.segments?.length || 0} Đã học
            </span>
          </div>

          {/* Segment Items List */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1 divide-y divide-border/40">
            {(playbackData.segments || []).map((seg, index) => {
              const isActive = index === activeSegmentIndex;
              const isDone = completedSegments.has(seg.segmentId);

              return (
                <button
                  key={seg.segmentId || index}
                  type="button"
                  onClick={() => handleSelectSegment(index)}
                  className={`w-full text-left p-3 rounded-xl transition-all flex items-start space-x-3 pt-3 ${
                    isActive
                      ? 'bg-primary/10 border border-primary/30 text-foreground'
                      : 'hover:bg-secondary/60 text-muted-foreground'
                  }`}
                >
                  <div className="mt-0.5">
                    {isDone ? (
                      <CheckCircle2 className="w-4 h-4 text-success" />
                    ) : (
                      <span className="w-4 h-4 rounded-full border border-border flex items-center justify-center text-[10px] font-bold">
                        {index + 1}
                      </span>
                    )}
                  </div>

                  <div className="flex-1 min-w-0 space-y-0.5">
                    <div className={`font-hanzi text-sm truncate font-medium ${isActive ? 'text-primary font-bold' : 'text-foreground'}`}>
                      {seg.hanziText}
                    </div>
                    {seg.pinyinText && (
                      <div className="font-mono text-xs text-muted-foreground truncate">
                        {seg.pinyinText}
                      </div>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
