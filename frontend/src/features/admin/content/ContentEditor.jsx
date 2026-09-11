import React, { useState } from 'react';
import { X, Save, AlertCircle } from 'lucide-react';

export function ContentEditor({
  mode, // 'TOPIC' | 'LESSON' | 'SEGMENT' | 'MEDIA'
  initialData = null,
  topics = [],
  lessons = [],
  mediaList = [],
  onSave,
  onCancel,
  serverError = null,
  onClearServerError = () => {},
  isLoading = false,
}) {
  const isEdit = Boolean(initialData);

  // Topic form state
  const [topicForm, setTopicForm] = useState({
    title: initialData?.title || '',
    slug: initialData?.slug || '',
    description: initialData?.description || '',
    hskLevel: initialData?.hskLevel || 1,
    sortOrder: initialData?.sortOrder || 0,
    expectedVersion: initialData?.version ?? 0,
  });

  // Lesson form state
  const [lessonForm, setLessonForm] = useState({
    topicId: initialData?.topicId || topics[0]?.topicId || '',
    title: initialData?.title || '',
    slug: initialData?.slug || '',
    summary: initialData?.summary || '',
    hskLevel: initialData?.hskLevel || 1,
    sortOrder: initialData?.sortOrder || 0,
    accessLevel: 'FREE',
    expectedVersion: initialData?.version ?? 0,
  });

  // Segment form state
  const [segmentForm, setSegmentForm] = useState({
    lessonId: initialData?.lessonId || lessons[0]?.lessonId || '',
    mediaAssetId: initialData?.mediaAssetId || mediaList[0]?.mediaAssetId || '',
    sequenceNo: initialData?.sequenceNo || 1,
    segmentType: initialData?.segmentType || 'BOTH',
    startMilliseconds: initialData?.startMilliseconds || 0,
    endMilliseconds: initialData?.endMilliseconds || 5000,
    transcriptHanzi: initialData?.transcriptHanzi || '',
    transcriptPinyin: initialData?.transcriptPinyin || '',
    translationVi: initialData?.translationVi || '',
    dictationHint: initialData?.dictationHint || '',
    expectedVersion: initialData?.version ?? 0,
  });

  // Media form state
  const [mediaForm, setMediaForm] = useState({
    youtubeVideoReference: '',
    durationMilliseconds: initialData?.durationMilliseconds || 120000,
    title: initialData?.title || '',
    altText: initialData?.altText || '',
    expectedVersion: initialData?.version ?? 0,
  });

  const [formError, setFormError] = useState('');
  const visibleError = formError ? { message: formError } : serverError;

  const handleSlugGen = (title, setFn) => {
    const slug = title
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-');
    setFn((prev) => ({ ...prev, title, slug }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    onClearServerError();

    try {
      if (mode === 'TOPIC') {
        if (!topicForm.title || !topicForm.slug) throw new Error('Vui lòng nhập đầy đủ Tiêu đề và Slug.');
        if (!/^[a-z0-9-]+$/.test(topicForm.slug)) throw new Error('Slug chỉ được chứa chữ cái thường, số và dấu gạch ngang.');
        onSave(topicForm);
      } else if (mode === 'LESSON') {
        if (!lessonForm.topicId || !lessonForm.title || !lessonForm.slug) throw new Error('Vui lòng chọn Chủ đề và nhập Tiêu đề, Slug.');
        if (!/^[a-z0-9-]+$/.test(lessonForm.slug)) throw new Error('Slug chỉ được chứa chữ cái thường, số và dấu gạch ngang.');
        onSave(lessonForm);
      } else if (mode === 'SEGMENT') {
        if (!segmentForm.lessonId || !segmentForm.mediaAssetId || !segmentForm.transcriptHanzi) {
          throw new Error('Vui lòng chọn Bài học, Tệp truyền thông và nhập Trang chính chữ Hán.');
        }
        onSave(segmentForm);
      } else if (mode === 'MEDIA') {
        if ((!isEdit && !mediaForm.youtubeVideoReference) || !mediaForm.title) {
          throw new Error('Enter a YouTube URL or 11-character Video ID and a title.');
        }
        onSave(isEdit
          ? { title: mediaForm.title, altText: mediaForm.altText, expectedVersion: mediaForm.expectedVersion }
          : {
            youtubeVideoReference: mediaForm.youtubeVideoReference,
            durationMilliseconds: mediaForm.durationMilliseconds,
            title: mediaForm.title,
            altText: mediaForm.altText,
          });
      }
    } catch (err) {
      setFormError(err.message);
    }
  };

  return (
    <div className="fixed inset-0 bg-foreground/70 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in font-sans">
      <div role="dialog" aria-modal="true" className="bg-popover border border-border rounded-xl max-w-2xl w-full p-6 space-y-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <h2 className="text-xl font-bold text-foreground">
            {isEdit ? 'Chỉnh sửa' : 'Tạo mới'}{' '}
            {mode === 'TOPIC' ? 'Chủ đề' : mode === 'LESSON' ? 'Bài học' : mode === 'SEGMENT' ? 'Phân đoạn' : 'Media Asset'}
          </h2>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Close content editor"
            className="w-8 h-8 rounded-lg hover:bg-accent text-muted-foreground hover:text-foreground flex items-center justify-center transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error Alert */}
        {visibleError && (
          <div role="alert" data-testid="content-editor-server-error" className="p-3 bg-destructive/10 border border-destructive/30 rounded-lg text-destructive flex items-center space-x-2 text-sm">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <div>
              {visibleError.code && <p className="font-semibold">{visibleError.code}</p>}
              <p>{visibleError.message}</p>
              {visibleError.correlationId && <p className="mt-1 text-xs text-muted-foreground">Request ID: {visibleError.correlationId}</p>}
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} onInput={onClearServerError} className="space-y-4" data-testid="content-editor-form">
          {/* TOPIC FORM */}
          {mode === 'TOPIC' && (
            <>
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                  Tiêu đề chủ đề *
                </label>
                <input
                  type="text"
                  required
                  value={topicForm.title}
                  onChange={(e) => handleSlugGen(e.target.value, setTopicForm)}
                  placeholder="Ví dụ: Luyện nghe HSK 1..."
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  data-testid="topic-title-input"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">
                  Slug URL * (a-z, 0-9, -)
                </label>
                <input
                  type="text"
                  required
                  pattern="^[a-z0-9-]+$"
                  value={topicForm.slug}
                  onChange={(e) => setTopicForm({ ...topicForm, slug: e.target.value })}
                  placeholder="hsk1-luyen-nghe"
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm font-mono"
                  data-testid="topic-slug-input"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Cấp độ HSK</label>
                  <select
                    value={topicForm.hskLevel}
                    onChange={(e) => setTopicForm({ ...topicForm, hskLevel: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  >
                    {[1, 2, 3, 4, 5, 6].map((lvl) => (
                      <option key={lvl} value={lvl}>
                        HSK {lvl}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Thứ tự sắp xếp</label>
                  <input
                    type="number"
                    min="0"
                    value={topicForm.sortOrder}
                    onChange={(e) => setTopicForm({ ...topicForm, sortOrder: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Mô tả chủ đề</label>
                <textarea
                  rows={3}
                  value={topicForm.description}
                  onChange={(e) => setTopicForm({ ...topicForm, description: e.target.value })}
                  placeholder="Mô tả ngắn gọn về chủ đề bài học..."
                  className="w-full p-3 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm resize-none"
                />
              </div>
            </>
          )}

          {/* LESSON FORM */}
          {mode === 'LESSON' && (
            <>
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Chủ đề cha *</label>
                <select
                  required
                  value={lessonForm.topicId}
                  onChange={(e) => setLessonForm({ ...lessonForm, topicId: e.target.value })}
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  data-testid="lesson-topic-select"
                >
                  {topics.map((t) => (
                    <option key={t.topicId} value={t.topicId}>
                      {t.title} ({t.publicationState})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Tên bài học *</label>
                <input
                  type="text"
                  required
                  value={lessonForm.title}
                  onChange={(e) => handleSlugGen(e.target.value, setLessonForm)}
                  placeholder="Ví dụ: Bài 1 - Chào hỏi cơ bản..."
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  data-testid="lesson-title-input"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Slug URL *</label>
                <input
                  type="text"
                  required
                  pattern="^[a-z0-9-]+$"
                  value={lessonForm.slug}
                  onChange={(e) => setLessonForm({ ...lessonForm, slug: e.target.value })}
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm font-mono"
                  data-testid="lesson-slug-input"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Cấp độ HSK</label>
                  <select
                    value={lessonForm.hskLevel}
                    onChange={(e) => setLessonForm({ ...lessonForm, hskLevel: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  >
                    {[1, 2, 3, 4, 5, 6].map((lvl) => (
                      <option key={lvl} value={lvl}>
                        HSK {lvl}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Quyền truy cập</label>
                  <input
                    type="text"
                    disabled
                    value="FREE (Miễn phí)"
                    className="w-full h-11 px-3.5 bg-secondary border border-border rounded-lg text-muted-foreground text-sm cursor-not-allowed font-semibold"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Tóm tắt bài học</label>
                <textarea
                  rows={2}
                  value={lessonForm.summary}
                  onChange={(e) => setLessonForm({ ...lessonForm, summary: e.target.value })}
                  placeholder="Tóm tắt nội dung chính..."
                  className="w-full p-3 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm resize-none"
                />
              </div>
            </>
          )}

          {/* SEGMENT FORM */}
          {mode === 'SEGMENT' && (
            <>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Bài học cha *</label>
                  <select
                    required
                    value={segmentForm.lessonId}
                    onChange={(e) => setSegmentForm({ ...segmentForm, lessonId: e.target.value })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                    data-testid="segment-lesson-select"
                  >
                    {lessons.map((l) => (
                      <option key={l.lessonId} value={l.lessonId}>
                        {l.title}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Tệp media *</label>
                  <select
                    required
                    value={segmentForm.mediaAssetId}
                    onChange={(e) => setSegmentForm({ ...segmentForm, mediaAssetId: e.target.value })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                    data-testid="segment-media-select"
                  >
                    {mediaList.map((m) => (
                      <option key={m.mediaAssetId} value={m.mediaAssetId}>
                        {m.title || m.providerAssetIdentifier} ({m.approvalStatus})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Số thứ tự (#) *</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={segmentForm.sequenceNo}
                    onChange={(e) => setSegmentForm({ ...segmentForm, sequenceNo: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                    data-testid="segment-seq-input"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Bắt đầu (ms)</label>
                  <input
                    type="number"
                    min="0"
                    value={segmentForm.startMilliseconds}
                    onChange={(e) => setSegmentForm({ ...segmentForm, startMilliseconds: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Kết thúc (ms)</label>
                  <input
                    type="number"
                    min="0"
                    value={segmentForm.endMilliseconds}
                    onChange={(e) => setSegmentForm({ ...segmentForm, endMilliseconds: Number(e.target.value) })}
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Nội dung chữ Hán (Transcript) *</label>
                <textarea
                  rows={2}
                  required
                  value={segmentForm.transcriptHanzi}
                  onChange={(e) => setSegmentForm({ ...segmentForm, transcriptHanzi: e.target.value })}
                  placeholder="Ví dụ: 你好，很高兴认识你。"
                  className="w-full p-3 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-base font-hanzi resize-none"
                  data-testid="segment-hanzi-input"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Pinyin (Phiên âm)</label>
                  <input
                    type="text"
                    value={segmentForm.transcriptPinyin}
                    onChange={(e) => setSegmentForm({ ...segmentForm, transcriptPinyin: e.target.value })}
                    placeholder="nǐ hǎo, hěn gāoxìng rènshi nǐ."
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Dịch nghĩa tiếng Việt</label>
                  <input
                    type="text"
                    value={segmentForm.translationVi}
                    onChange={(e) => setSegmentForm({ ...segmentForm, translationVi: e.target.value })}
                    placeholder="Xin chào, rất vui được gặp bạn."
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  />
                </div>
              </div>
            </>
          )}

          {/* MEDIA FORM */}
          {mode === 'MEDIA' && (
            <>
              {!isEdit && <div>
                <div>
                  <label htmlFor="media-youtube-reference" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">YouTube URL or Video ID *</label>
                  <input
                    id="media-youtube-reference"
                    type="text"
                    required
                    value={mediaForm.youtubeVideoReference}
                    onChange={(e) => setMediaForm({ ...mediaForm, youtubeVideoReference: e.target.value })}
                    placeholder="https://youtu.be/dQw4w9WgXcQ or dQw4w9WgXcQ"
                    aria-label="YouTube URL or Video ID"
                    aria-describedby="media-youtube-reference-help"
                    className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm font-mono"
                    data-testid="media-youtube-reference-input"
                  />
                  <p id="media-youtube-reference-help" className="mt-1 text-xs text-muted-foreground">
                    Supports HTTPS YouTube watch, share, or embed links, or an 11-character Video ID. The original URL is not stored.
                  </p>
                </div>
              </div>}

              {isEdit && <p role="status" className="rounded-lg border border-border bg-secondary/40 px-3 py-2 text-sm text-muted-foreground">
                YouTube source is immutable. Create a new media asset to use a different video.
              </p>}


              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Tiêu đề tệp truyền thông *</label>
                <input
                  type="text"
                  required
                  value={mediaForm.title}
                  onChange={(e) => setMediaForm({ ...mediaForm, title: e.target.value })}
                  placeholder="Tiêu đề audio/video..."
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                  data-testid="media-title-input"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">Mô tả Alt Text</label>
                <input
                  type="text"
                  value={mediaForm.altText}
                  onChange={(e) => setMediaForm({ ...mediaForm, altText: e.target.value })}
                  placeholder="Mô tả cho trình đọc màn hình..."
                  className="w-full h-11 px-3.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                />
              </div>
            </>
          )}

          {/* Action Buttons */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-border">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2.5 bg-secondary text-foreground hover:bg-accent border border-border rounded-lg font-bold text-sm transition-all"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-5 py-2.5 bg-primary text-primary-foreground hover:opacity-90 rounded-lg font-bold text-sm flex items-center space-x-2 transition-all shadow-md active:scale-[0.98] disabled:opacity-50"
              data-testid="content-editor-submit"
            >
              <Save className="w-4 h-4" />
              <span>{isEdit ? 'Cập nhật' : 'Tạo mới'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
