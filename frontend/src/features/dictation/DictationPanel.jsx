import React, { useState, useEffect } from 'react';
import { Volume2, CheckCircle2, XCircle, RotateCcw, HelpCircle, ArrowRight, Sparkles } from 'lucide-react';
import { startDictationAttempt, submitDictationAttempt } from '../../api/dictation.js';

export function DictationPanel({ segment, onPlaySegment, onCompleted, onNextSegment }) {
  const [attemptId, setAttemptId] = useState(null);
  const [pinyinHint, setPinyinHint] = useState('');
  const [showHint, setShowHint] = useState(false);
  const [userAnswer, setUserAnswer] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Initialize dictation attempt whenever segmentId changes
  useEffect(() => {
    if (!segment?.segmentId) return;

    let isMounted = true;
    setLoading(true);
    setError(null);
    setResult(null);
    setUserAnswer('');
    setShowHint(false);

    startDictationAttempt(segment.segmentId)
      .then((data) => {
        if (!isMounted) return;
        setAttemptId(data.dictationAttemptId);
        setPinyinHint(data.pinyinHint || segment.pinyinText || '');
      })
      .catch((err) => {
        if (!isMounted) return;
        setError(err.message || 'Không thể khởi tạo bài nghe chép chính tả.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [segment?.segmentId]);

  const handleSubmit = async (e) => {
    e?.preventDefault();
    if (!attemptId || !userAnswer.trim() || submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      const submission = await submitDictationAttempt(attemptId, userAnswer.trim());
      setResult(submission);
      if (submission.correct && onCompleted) {
        onCompleted(segment.segmentId);
      }
    } catch (err) {
      setError(err.message || 'Gặp lỗi khi nộp bài.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRetry = async () => {
    setLoading(true);
    setResult(null);
    setUserAnswer('');
    setError(null);
    try {
      const data = await startDictationAttempt(segment.segmentId);
      setAttemptId(data.dictationAttemptId);
      setPinyinHint(data.pinyinHint || segment.pinyinText || '');
    } catch (err) {
      setError(err.message || 'Không thể bắt đầu lại lượt mới.');
    } finally {
      setLoading(false);
    }
  };

  if (!segment) {
    return (
      <div className="p-6 text-center text-muted-foreground">
        Vui lòng chọn một phân đoạn để bắt đầu nghe chép chính tả.
      </div>
    );
  }

  if (loading) {
    return (
      <div className="p-8 text-center text-muted-foreground animate-pulse">
        Đang chuẩn bị câu hỏi nghe chép...
      </div>
    );
  }

  return (
    <div className="bg-card border border-border rounded-xl p-5 md:p-6 space-y-5 shadow-sm" data-testid="dictation-panel">
      {/* Header & Audio Trigger */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-border">
        <div className="flex items-center space-x-2">
          <span className="w-2.5 h-2.5 rounded-full bg-primary animate-ping" />
          <h3 className="text-base font-bold text-foreground">
            Luyện nghe chép chính tả (Dictation)
          </h3>
        </div>

        <button
          type="button"
          onClick={() => onPlaySegment && onPlaySegment(segment)}
          className="inline-flex items-center space-x-2 px-3.5 py-1.5 bg-primary/10 hover:bg-primary/20 text-primary rounded-pill text-xs font-semibold transition-colors"
        >
          <Volume2 className="w-4 h-4" />
          <span>Nghe lại audio đoạn này</span>
        </button>
      </div>

      {error && (
        <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-lg text-xs text-destructive font-medium">
          {error}
        </div>
      )}

      {/* Hint toggle */}
      <div className="flex items-center justify-between text-xs text-muted-foreground">
        <span>Gõ lại chữ Hán chính xác bạn vừa nghe được:</span>
        {pinyinHint && (
          <button
            type="button"
            onClick={() => setShowHint(!showHint)}
            className="inline-flex items-center space-x-1 text-primary hover:underline font-medium"
          >
            <HelpCircle className="w-3.5 h-3.5" />
            <span>{showHint ? 'Ẩn gợi ý Pinyin' : 'Xem gợi ý Pinyin'}</span>
          </button>
        )}
      </div>

      {showHint && pinyinHint && (
        <div className="p-3 bg-secondary/50 rounded-lg border border-border/50 text-sm font-mono text-primary font-medium tracking-wide">
          Gợi ý Pinyin: {pinyinHint}
        </div>
      )}

      {/* Main Submission Form */}
      {!result ? (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <input
              type="text"
              value={userAnswer}
              onChange={(e) => setUserAnswer(e.target.value)}
              placeholder="Nhập chữ Hán (ví dụ: 你好, 谢谢)..."
              disabled={submitting}
              autoFocus
              className="w-full px-4 py-3 bg-background border border-border focus:border-primary focus:ring-1 focus:ring-primary rounded-lg text-lg font-hanzi text-foreground placeholder:text-muted-foreground focus:outline-none transition-all"
            />
          </div>

          <div className="flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={() => onPlaySegment && onPlaySegment(segment)}
              className="px-4 py-2 bg-secondary hover:bg-secondary/80 text-secondary-foreground text-sm font-semibold rounded-pill transition-colors"
            >
              Nghe lại (Space)
            </button>
            <button
              type="submit"
              disabled={!userAnswer.trim() || submitting}
              className="px-6 py-2 bg-primary hover:bg-primary/90 disabled:opacity-50 text-primary-foreground text-sm font-bold rounded-pill shadow-sm transition-transform active:scale-95 flex items-center space-x-1.5"
            >
              {submitting ? (
                <span>Đang chấm...</span>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Kiểm tra đáp án</span>
                </>
              )}
            </button>
          </div>
        </form>
      ) : (
        /* Evaluation Result Card */
        <div className="space-y-4 pt-1">
          <div
            className={`p-4 rounded-xl border flex items-start space-x-3 ${
              result.correct
                ? 'bg-success/10 border-success/30 text-success'
                : 'bg-destructive/10 border-destructive/30 text-destructive'
            }`}
          >
            {result.correct ? (
              <CheckCircle2 className="w-6 h-6 flex-shrink-0 mt-0.5" />
            ) : (
              <XCircle className="w-6 h-6 flex-shrink-0 mt-0.5" />
            )}
            <div className="flex-1 space-y-1">
              <div className="flex items-center justify-between">
                <h4 className="font-bold text-sm md:text-base">
                  {result.correct ? 'Chính xác 100%! Rất xuất sắc!' : 'Chưa chính xác, hãy thử lại!'}
                </h4>
                <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-background/50">
                  Điểm: {result.score}%
                </span>
              </div>
              <p className="text-xs opacity-90">
                {result.correct
                  ? 'Bạn đã nghe và gõ đúng chuẩn toàn bộ chữ Hán trong phân đoạn này.'
                  : 'Hãy so sánh câu trả lời của bạn với đáp án chuẩn bên dưới.'}
              </p>
            </div>
          </div>

          <div className="p-4 bg-background border border-border rounded-xl space-y-3">
            <div className="text-xs text-muted-foreground">Đáp án chuẩn:</div>
            <div className="text-xl font-bold font-hanzi text-foreground">
              {result.correctAnswer}
            </div>
            {result.pinyin && (
              <div className="text-xs font-mono text-primary font-medium">
                {result.pinyin}
              </div>
            )}
            {result.vietnameseMeaning && (
              <div className="text-xs text-muted-foreground">
                Ý nghĩa: {result.vietnameseMeaning}
              </div>
            )}

            {!result.correct && result.userAnswer && (
              <div className="pt-2 border-t border-border/60 text-xs">
                <span className="text-muted-foreground">Bạn đã nhập: </span>
                <span className="font-hanzi font-medium line-through text-destructive">
                  {result.userAnswer}
                </span>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={handleRetry}
              className="inline-flex items-center space-x-1.5 px-4 py-2 bg-secondary hover:bg-secondary/80 text-secondary-foreground text-xs font-semibold rounded-pill transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Luyện lại câu này</span>
            </button>

            {onNextSegment && (
              <button
                type="button"
                onClick={onNextSegment}
                className="inline-flex items-center space-x-1.5 px-5 py-2 bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-bold rounded-pill shadow-sm transition-transform active:scale-95"
              >
                <span>Đoạn tiếp theo</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
