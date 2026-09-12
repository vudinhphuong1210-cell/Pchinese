import React, { useState, useEffect, useRef } from 'react';
import {
  Mic,
  Square,
  Play,
  Pause,
  RotateCcw,
  Sparkles,
  ArrowLeft,
  Volume2,
  CheckCircle,
  Award,
  BarChart3,
  Loader2,
} from 'lucide-react';
import { uploadRecording, createShadowingAttempt, fetchShadowingAttempts } from '../../api/shadowing.js';

export function ShadowingPage({ segment, onBack, onPlayNativeSegment, onNextSegment }) {
  const [recordingState, setRecordingState] = useState('idle'); // 'idle' | 'recording' | 'recorded' | 'evaluating'
  const [recordingDuration, setRecordingDuration] = useState(0);
  const [audioUrl, setAudioUrl] = useState(null);
  const [audioBlob, setAudioBlob] = useState(null);
  const [assessmentResult, setAssessmentResult] = useState(null);
  const [pastAttempts, setPastAttempts] = useState([]);
  const [error, setError] = useState(null);
  const [playbackRate, setPlaybackRate] = useState(1.0);

  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const timerIntervalRef = useRef(null);
  const previewAudioRef = useRef(null);
  const [isPreviewPlaying, setIsPreviewPlaying] = useState(false);

  // Load previous attempts on segment change
  useEffect(() => {
    if (!segment?.segmentId) return;

    resetState();
    fetchShadowingAttempts(segment.segmentId)
      .then((data) => {
        setPastAttempts(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        // Non-fatal
      });

    return () => {
      cleanupMedia();
    };
  }, [segment?.segmentId]);

  const resetState = () => {
    cleanupMedia();
    setRecordingState('idle');
    setRecordingDuration(0);
    setAudioUrl(null);
    setAudioBlob(null);
    setAssessmentResult(null);
    setError(null);
  };

  const cleanupMedia = () => {
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
    }
  };

  const startRecording = async () => {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioChunksRef.current = [];
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = () => {
        const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const url = URL.createObjectURL(blob);
        setAudioBlob(blob);
        setAudioUrl(url);
        setRecordingState('recorded');
        // Stop all audio tracks to release microphone
        stream.getTracks().forEach((track) => track.stop());
      };

      mediaRecorder.start();
      setRecordingState('recording');
      setRecordingDuration(0);

      timerIntervalRef.current = setInterval(() => {
        setRecordingDuration((prev) => {
          if (prev >= 60) {
            // Auto stop at 60s max
            stopRecording();
            return prev;
          }
          return prev + 1;
        });
      }, 1000);
    } catch (err) {
      setError('Không thể truy cập Microphone. Vui lòng cấp quyền truy cập ghi âm trong trình duyệt.');
    }
  };

  const stopRecording = () => {
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
    }
  };

  const togglePreviewAudio = () => {
    if (!previewAudioRef.current || !audioUrl) return;
    if (isPreviewPlaying) {
      previewAudioRef.current.pause();
      setIsPreviewPlaying(false);
    } else {
      previewAudioRef.current.currentTime = 0;
      previewAudioRef.current.play();
      setIsPreviewPlaying(true);
    }
  };

  const handleEvaluate = async () => {
    if (!segment?.segmentId || !audioBlob) return;

    setRecordingState('evaluating');
    setError(null);

    try {
      // 1. Upload recording file via FormData
      const recording = await uploadRecording({
        segmentId: segment.segmentId,
        audioBlob,
        format: 'audio/webm',
      });

      // 2. Submit for AI assessment
      const result = await createShadowingAttempt(segment.segmentId, recording.recordingId);
      setAssessmentResult(result);
      setRecordingState('recorded');

      // Refresh past attempts list
      setPastAttempts((prev) => [result, ...(prev || [])]);
    } catch (err) {
      setError(err.message || 'Không thể đánh giá phát âm. Vui lòng thử lại.');
      setRecordingState('recorded');
    }
  };

  const formatTimer = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  if (!segment) {
    return (
      <div className="p-8 text-center text-muted-foreground">
        Vui lòng chọn một phân đoạn để luyện nói Shadowing.
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12" data-testid="shadowing-page">
      {/* Header with back button */}
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center text-muted-foreground hover:text-foreground font-semibold text-sm transition-colors"
        >
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Quay lại bài học
        </button>

        <div className="flex items-center space-x-2 text-xs font-semibold px-3 py-1 bg-primary/10 text-primary rounded-full">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Luyện nói Shadowing AI</span>
        </div>
      </div>

      {/* Target Sentence Card */}
      <div className="bg-card border border-border rounded-2xl p-6 md:p-8 space-y-6 shadow-sm">
        <div className="space-y-3 text-center">
          <div className="text-xs uppercase font-bold tracking-widest text-muted-foreground">
            Mẫu câu luyện tập
          </div>

          <div className="text-3xl md:text-4xl font-bold font-hanzi text-foreground leading-relaxed">
            {segment.hanziText}
          </div>

          {segment.pinyinText && (
            <div className="text-base md:text-lg font-mono text-primary font-medium tracking-wide">
              {segment.pinyinText}
            </div>
          )}

          {segment.vietnameseText && (
            <div className="text-sm text-muted-foreground italic">
              &ldquo;{segment.vietnameseText}&rdquo;
            </div>
          )}
        </div>

        {/* Native Audio Reference Trigger */}
        <div className="pt-4 border-t border-border flex items-center justify-center space-x-4">
          <button
            type="button"
            onClick={() => onPlayNativeSegment && onPlayNativeSegment(segment, playbackRate)}
            className="inline-flex items-center space-x-2 px-5 py-2.5 bg-secondary hover:bg-secondary/80 text-foreground text-sm font-bold rounded-pill transition-colors shadow-sm"
          >
            <Volume2 className="w-4 h-4 text-primary" />
            <span>Nghe mẫu phát âm chuẩn</span>
          </button>

          <div className="flex items-center space-x-1 bg-secondary/50 rounded-pill p-1 text-xs">
            {[0.8, 1.0].map((rate) => (
              <button
                key={rate}
                type="button"
                onClick={() => setPlaybackRate(rate)}
                className={`px-2.5 py-1 rounded-pill font-bold transition-colors ${
                  playbackRate === rate ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                {rate}x
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-xl text-sm text-destructive font-medium">
          {error}
        </div>
      )}

      {/* Recording Stage */}
      <div className="bg-card border border-border rounded-2xl p-6 md:p-8 flex flex-col items-center justify-center space-y-6 shadow-sm text-center">
        {recordingState === 'idle' && (
          <div className="space-y-4">
            <button
              type="button"
              onClick={startRecording}
              className="w-20 h-20 rounded-full bg-primary hover:bg-primary/90 text-primary-foreground flex items-center justify-center shadow-lg transition-transform active:scale-95 mx-auto group"
            >
              <Mic className="w-9 h-9 group-hover:scale-110 transition-transform" />
            </button>
            <div className="space-y-1">
              <p className="text-base font-bold text-foreground">Bấm để bắt đầu thu âm</p>
              <p className="text-xs text-muted-foreground">
                Hãy đọc to, rõ ràng và bắt chước ngữ điệu mẫu câu trên.
              </p>
            </div>
          </div>
        )}

        {recordingState === 'recording' && (
          <div className="space-y-5 animate-pulse">
            <div className="relative">
              <div className="w-24 h-24 rounded-full bg-destructive/20 flex items-center justify-center animate-ping absolute inset-0" />
              <button
                type="button"
                onClick={stopRecording}
                className="w-20 h-20 rounded-full bg-destructive text-destructive-foreground flex items-center justify-center shadow-lg relative z-10 mx-auto active:scale-95"
              >
                <Square className="w-8 h-8 fill-current" />
              </button>
            </div>

            <div className="space-y-1">
              <div className="text-2xl font-mono font-bold text-destructive">
                {formatTimer(recordingDuration)}
              </div>
              <p className="text-xs text-muted-foreground">Đang ghi âm giọng của bạn... Bấm nút vuông để hoàn thành.</p>
            </div>
          </div>
        )}

        {recordingState === 'recorded' && (
          <div className="w-full space-y-6">
            <div className="flex flex-wrap items-center justify-center gap-4">
              {audioUrl && (
                <>
                  <audio
                    ref={previewAudioRef}
                    src={audioUrl}
                    onEnded={() => setIsPreviewPlaying(false)}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={togglePreviewAudio}
                    className="inline-flex items-center space-x-2 px-4 py-2 bg-secondary hover:bg-secondary/80 text-foreground text-xs font-bold rounded-pill transition-colors"
                  >
                    {isPreviewPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                    <span>{isPreviewPlaying ? 'Tạm dừng giọng bạn' : 'Nghe lại giọng bạn'}</span>
                  </button>
                </>
              )}

              <button
                type="button"
                onClick={resetState}
                className="inline-flex items-center space-x-1.5 px-4 py-2 bg-secondary/60 hover:bg-secondary text-muted-foreground hover:text-foreground text-xs font-semibold rounded-pill transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Thu âm lại</span>
              </button>
            </div>

            {!assessmentResult && (
              <button
                type="button"
                onClick={handleEvaluate}
                className="h-12 px-8 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-pill shadow-md inline-flex items-center space-x-2 transition-transform active:scale-95"
              >
                <Sparkles className="w-5 h-5" />
                <span>Chấm điểm phát âm bằng AI</span>
              </button>
            )}
          </div>
        )}

        {recordingState === 'evaluating' && (
          <div className="p-8 space-y-4 flex flex-col items-center">
            <Loader2 className="w-10 h-10 text-primary animate-spin" />
            <p className="text-sm font-bold text-foreground">AI đang phân tích ngữ điệu và phát âm của bạn...</p>
            <p className="text-xs text-muted-foreground">Đang so sánh với chuẩn ngữ âm tiếng Trung bản địa.</p>
          </div>
        )}
      </div>

      {/* AI Assessment Result Scorecard */}
      {assessmentResult && (
        <div className="bg-card border border-border rounded-2xl p-6 md:p-8 space-y-6 shadow-sm animate-in fade-in duration-300">
          <div className="flex items-center justify-between pb-4 border-b border-border">
            <div className="flex items-center space-x-2.5">
              <Award className="w-6 h-6 text-primary" />
              <h3 className="text-lg font-bold text-foreground">Kết quả đánh giá AI</h3>
            </div>
            <span
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                Number(assessmentResult.overallScore) >= 80
                  ? 'bg-success/20 text-success'
                  : Number(assessmentResult.overallScore) >= 60
                  ? 'bg-warning/20 text-warning'
                  : 'bg-destructive/20 text-destructive'
              }`}
            >
              {Number(assessmentResult.overallScore) >= 80
                ? 'Xuất sắc'
                : Number(assessmentResult.overallScore) >= 60
                ? 'Đạt yêu cầu'
                : 'Cần luyện thêm'}
            </span>
          </div>

          {/* Metric scores breakdown */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-4 bg-background border border-border rounded-xl text-center space-y-1">
              <div className="text-xs text-muted-foreground font-semibold">Tổng quan</div>
              <div className="text-2xl md:text-3xl font-bold text-primary">
                {Number(assessmentResult.overallScore || 0).toFixed(0)}
              </div>
            </div>

            <div className="p-4 bg-background border border-border rounded-xl text-center space-y-1">
              <div className="text-xs text-muted-foreground font-semibold">Phát âm</div>
              <div className="text-2xl md:text-3xl font-bold text-foreground">
                {Number(assessmentResult.pronunciationScore || 0).toFixed(0)}
              </div>
            </div>

            <div className="p-4 bg-background border border-border rounded-xl text-center space-y-1">
              <div className="text-xs text-muted-foreground font-semibold">Thanh điệu</div>
              <div className="text-2xl md:text-3xl font-bold text-foreground">
                {Number(assessmentResult.toneScore || 0).toFixed(0)}
              </div>
            </div>

            <div className="p-4 bg-background border border-border rounded-xl text-center space-y-1">
              <div className="text-xs text-muted-foreground font-semibold">Nhịp điệu</div>
              <div className="text-2xl md:text-3xl font-bold text-foreground">
                {Number(assessmentResult.rhythmScore || 0).toFixed(0)}
              </div>
            </div>
          </div>

          {/* AI Feedback notes */}
          {assessmentResult.feedback && (
            <div className="p-4 bg-secondary/40 border border-border/80 rounded-xl space-y-2">
              <div className="text-xs font-bold text-foreground flex items-center space-x-1.5">
                <Sparkles className="w-3.5 h-3.5 text-primary" />
                <span>Nhận xét từ AI:</span>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                {assessmentResult.feedback}
              </p>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={resetState}
              className="inline-flex items-center space-x-1.5 px-4 py-2 bg-secondary hover:bg-secondary/80 text-foreground text-xs font-semibold rounded-pill transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Thử lại câu này</span>
            </button>

            {onNextSegment && (
              <button
                type="button"
                onClick={onNextSegment}
                className="inline-flex items-center space-x-1.5 px-6 py-2 bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-bold rounded-pill shadow-sm transition-transform active:scale-95"
              >
                <span>Sang câu tiếp theo</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* History of Attempts */}
      {pastAttempts.length > 0 && (
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4 shadow-sm">
          <div className="flex items-center space-x-2 text-sm font-bold text-foreground">
            <BarChart3 className="w-4 h-4 text-primary" />
            <h4>Lịch sử thu âm phân đoạn này ({pastAttempts.length})</h4>
          </div>

          <div className="divide-y divide-border">
            {pastAttempts.map((attempt, index) => (
              <div key={attempt.shadowingAttemptId || index} className="py-3 flex items-center justify-between text-xs">
                <div className="flex items-center space-x-2">
                  <CheckCircle className="w-4 h-4 text-primary" />
                  <span className="text-muted-foreground">
                    Lần {pastAttempts.length - index}:
                  </span>
                  <span className="font-bold text-foreground">
                    {Number(attempt.overallScore || 0).toFixed(0)} điểm
                  </span>
                </div>
                <div className="text-muted-foreground">
                  {attempt.createdAt ? new Date(attempt.createdAt).toLocaleTimeString() : 'Vừa xong'}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
