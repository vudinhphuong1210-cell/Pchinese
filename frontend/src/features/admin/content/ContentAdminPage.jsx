import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { adminContentApi } from '../../../api/adminContent.js';
import { PublicationControls } from './PublicationControls.jsx';
import { ContentEditor } from './ContentEditor.jsx';
import { Shield, Plus, Search, Filter, RefreshCw, BookOpen, Layers, FileText, Film, AlertCircle } from 'lucide-react';

function toSafeErrorNotice(error, fallbackMessage) {
  return {
    code: error?.error?.code || error?.code || null,
    message: error?.error?.message || error?.message || fallbackMessage,
    correlationId: error?.meta?.correlationId || null,
  };
}

function YouTubeThumbnail({ mediaAssetId, videoId, title }) {
  const [isUnavailable, setIsUnavailable] = useState(false);
  const label = title || 'this YouTube video';

  if (!videoId || isUnavailable) {
    return (
      <div className="w-full sm:w-56 aspect-video rounded-lg border border-border bg-secondary flex items-center justify-center text-xs text-muted-foreground" data-testid={`youtube-thumbnail-unavailable-${mediaAssetId}`}>
        Thumbnail unavailable
      </div>
    );
  }

  return (
    <div className="w-full sm:w-56 aspect-video rounded-lg overflow-hidden border border-border bg-secondary shrink-0">
      <img
        src={`https://i.ytimg.com/vi/${encodeURIComponent(videoId)}/hqdefault.jpg`}
        alt={`YouTube thumbnail for ${label}`}
        loading="lazy"
        decoding="async"
        referrerPolicy="no-referrer"
        onError={() => setIsUnavailable(true)}
        className="h-full w-full object-cover"
        data-testid={`youtube-thumbnail-${mediaAssetId}`}
      />
    </div>
  );
}

export function ContentAdminPage() {
  const [activeSubTab, setActiveSubTab] = useState('topics'); // 'topics' | 'lessons' | 'segments' | 'media'
  const [topics, setTopics] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [segments, setSegments] = useState([]);
  const [mediaList, setMediaList] = useState([]);

  const [isLoading, setIsLoading] = useState(false);
  const [errorNotice, setErrorNotice] = useState(null);
  const [successMsg, setSuccessMsg] = useState('');
  const [staleConflictId, setStaleConflictId] = useState(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [hskFilter, setHskFilter] = useState('ALL');

  const [editorState, setEditorState] = useState(null); // null | { mode: 'TOPIC'|'LESSON'|'SEGMENT'|'MEDIA', item?: object }

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorNotice(null);
    try {
      const [tRes, lRes, sRes, mRes] = await Promise.all([
        adminContentApi.listTopics().catch(() => ({ data: [] })),
        adminContentApi.listLessons().catch(() => ({ data: [] })),
        adminContentApi.listSegments().catch(() => ({ data: [] })),
        adminContentApi.listMedia().catch(() => ({ data: [] })),
      ]);
      setTopics(tRes.data || []);
      setLessons(lRes.data || []);
      setSegments(sRes.data || []);
      setMediaList(mRes.data || []);
      setStaleConflictId(null);
    } catch (err) {
      setErrorNotice(toSafeErrorNotice(err, 'Không thể tải dữ liệu quản trị nội dung.'));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleCreate = (mode) => {
    setEditorState({ mode, item: null });
  };

  const handleEdit = (mode, item) => {
    setEditorState({ mode, item });
  };

  const handleSaveEditor = async (formData) => {
    setIsLoading(true);
    setErrorNotice(null);
    setSuccessMsg('');
    try {
      if (editorState.mode === 'TOPIC') {
        if (editorState.item) {
          await adminContentApi.updateTopic(editorState.item.topicId, formData);
        } else {
          await adminContentApi.createTopic(formData);
        }
      } else if (editorState.mode === 'LESSON') {
        if (editorState.item) {
          await adminContentApi.updateLesson(editorState.item.lessonId, formData);
        } else {
          await adminContentApi.createLesson(formData);
        }
      } else if (editorState.mode === 'SEGMENT') {
        if (editorState.item) {
          await adminContentApi.updateSegment(editorState.item.segmentId, formData);
        } else {
          await adminContentApi.createSegment(formData);
        }
      } else if (editorState.mode === 'MEDIA') {
        if (editorState.item) {
          await adminContentApi.updateMedia(editorState.item.mediaAssetId, formData);
        } else {
          const created = await adminContentApi.createMedia(formData);
          if (created?.data?.providerAssetIdentifier) {
            setSuccessMsg(`YouTube video registered as ID: ${created.data.providerAssetIdentifier}`);
          }
        }
      }
      setEditorState(null);
      await loadData();
    } catch (err) {
      if (err?.error?.code === 'STATE_CONFLICT' || err?.code === 'STATE_CONFLICT' || err?.status === 409) {
        setStaleConflictId(editorState.item?.topicId || editorState.item?.lessonId || editorState.item?.segmentId || editorState.item?.mediaAssetId);
      }
      setErrorNotice(toSafeErrorNotice(err, 'Lỗi khi lưu dữ liệu.'));
    } finally {
      setIsLoading(false);
    }
  };

  // --- Lifecycle Action Handlers ---
  const executeLifecycleAction = async (actionFn, id, expectedVersion) => {
    setIsLoading(true);
    setErrorNotice(null);
    try {
      await actionFn(id, expectedVersion);
      await loadData();
    } catch (err) {
      if (err?.error?.code === 'STATE_CONFLICT' || err?.code === 'STATE_CONFLICT' || err?.status === 409) {
        setStaleConflictId(id);
      }
      setErrorNotice(toSafeErrorNotice(err, 'Không thể thực hiện thao tác.'));
    } finally {
      setIsLoading(false);
    }
  };

  // Topic lifecycle
  const handlePublishTopic = (id, ver) => executeLifecycleAction(adminContentApi.publishTopic, id, ver);
  const handleUnpublishTopic = (id, ver) => executeLifecycleAction(adminContentApi.unpublishTopic, id, ver);
  const handleArchiveTopic = (id, ver) => executeLifecycleAction(adminContentApi.archiveTopic, id, ver);

  // Lesson lifecycle
  const handlePublishLesson = (id, ver) => executeLifecycleAction(adminContentApi.publishLesson, id, ver);
  const handleUnpublishLesson = (id, ver) => executeLifecycleAction(adminContentApi.unpublishLesson, id, ver);
  const handleArchiveLesson = (id, ver) => executeLifecycleAction(adminContentApi.archiveLesson, id, ver);

  // Segment lifecycle
  const handlePublishSegment = (id, ver) => executeLifecycleAction(adminContentApi.publishSegment, id, ver);
  const handleUnpublishSegment = (id, ver) => executeLifecycleAction(adminContentApi.unpublishSegment, id, ver);
  const handleArchiveSegment = (id, ver) => executeLifecycleAction(adminContentApi.archiveSegment, id, ver);

  // Media lifecycle
  const handleApproveMedia = (id, ver) => executeLifecycleAction(adminContentApi.approveMedia, id, ver);
  const handleRejectMedia = (id, ver) => executeLifecycleAction(adminContentApi.rejectMedia, id, ver);
  const handleQuarantineMedia = (id, ver) => executeLifecycleAction(adminContentApi.quarantineMedia, id, ver);

  // Filtered lists
  const filteredTopics = topics.filter((t) => {
    const matchSearch = (t.title || '').toLowerCase().includes(searchQuery.toLowerCase()) || (t.slug || '').includes(searchQuery);
    const matchHsk = hskFilter === 'ALL' || t.hskLevel === Number(hskFilter);
    return matchSearch && matchHsk;
  });

  const filteredLessons = lessons.filter((l) => {
    const matchSearch = (l.title || '').toLowerCase().includes(searchQuery.toLowerCase()) || (l.slug || '').includes(searchQuery);
    const matchHsk = hskFilter === 'ALL' || l.hskLevel === Number(hskFilter);
    return matchSearch && matchHsk;
  });

  const filteredSegments = segments.filter((s) => {
    const matchSearch = (s.transcriptHanzi || '').includes(searchQuery) || (s.transcriptPinyin || '').toLowerCase().includes(searchQuery.toLowerCase());
    return matchSearch;
  });

  const filteredMedia = mediaList.filter((m) => {
    const matchSearch = (m.title || '').toLowerCase().includes(searchQuery.toLowerCase()) || (m.providerAssetIdentifier || '').includes(searchQuery);
    return matchSearch;
  });

  const publishedLessonIds = useMemo(
    () => new Set(lessons.filter((lesson) => lesson.publicationState === 'PUBLISHED').map((lesson) => lesson.lessonId)),
    [lessons],
  );
  const mediaUsedByPublishedLessonIds = useMemo(
    () => new Set(segments.filter((segment) => publishedLessonIds.has(segment.lessonId)).map((segment) => segment.mediaAssetId)),
    [segments, publishedLessonIds],
  );

  return (
    <div className="space-y-6 font-sans" data-testid="content-admin-page">
      {/* Workspace Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border/80 pb-6">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center text-primary shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground font-sans">Quản lý nội dung & Media</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Soạn thảo, kiểm soát phiên bản và quản lý vòng đời bài học theo chuẩn ADMIN.
            </p>
          </div>
        </div>

        {/* Action Button */}
        <div className="flex items-center space-x-3">
          <button
            type="button"
            onClick={loadData}
            disabled={isLoading}
            className="p-2.5 bg-secondary hover:bg-accent text-foreground border border-border rounded-xl font-semibold text-sm transition-all"
            title="Tải lại dữ liệu"
            data-testid="refresh-content-btn"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => handleCreate(activeSubTab.toUpperCase().replace(/S$/, ''))}
            className="px-4 py-2.5 bg-primary text-primary-foreground hover:opacity-90 rounded-xl font-bold text-sm flex items-center space-x-2 transition-all shadow-md active:scale-[0.98]"
            data-testid="create-content-btn"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo mới {activeSubTab === 'topics' ? 'Chủ đề' : activeSubTab === 'lessons' ? 'Bài học' : activeSubTab === 'segments' ? 'Phân đoạn' : 'Media'}</span>
          </button>
        </div>
      </div>

      {/* General Error Banner */}
      {errorNotice && !editorState && (
        <div role="alert" data-testid="content-admin-error-banner" className="p-4 bg-destructive/10 border border-destructive/30 rounded-xl text-destructive flex items-center space-x-3 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <div>
            {errorNotice.code && <p className="font-semibold">{errorNotice.code}</p>}
            <p>{errorNotice.message}</p>
            {errorNotice.correlationId && <p className="mt-1 text-xs text-muted-foreground">Request ID: {errorNotice.correlationId}</p>}
          </div>
        </div>
      )}

      {successMsg && (
        <div role="status" className="p-4 bg-success/10 border border-success/30 rounded-xl text-success flex items-center space-x-3 text-sm">
          <span>{successMsg}</span>
        </div>
      )}

      {/* SubTab Switcher & Filter Dock */}
      <div className="space-y-4">
        <div className="flex items-center space-x-2 border-b border-border overflow-x-auto pb-1">
          <button
            type="button"
            onClick={() => setActiveSubTab('topics')}
            className={`px-4 py-2.5 font-bold text-sm flex items-center space-x-2 border-b-2 transition-all ${
              activeSubTab === 'topics' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
            data-testid="subtab-topics"
          >
            <BookOpen className="w-4 h-4" />
            <span>Chủ đề ({topics.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('lessons')}
            className={`px-4 py-2.5 font-bold text-sm flex items-center space-x-2 border-b-2 transition-all ${
              activeSubTab === 'lessons' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
            data-testid="subtab-lessons"
          >
            <Layers className="w-4 h-4" />
            <span>Bài học ({lessons.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('segments')}
            className={`px-4 py-2.5 font-bold text-sm flex items-center space-x-2 border-b-2 transition-all ${
              activeSubTab === 'segments' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
            data-testid="subtab-segments"
          >
            <FileText className="w-4 h-4" />
            <span>Phân đoạn ({segments.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveSubTab('media')}
            className={`px-4 py-2.5 font-bold text-sm flex items-center space-x-2 border-b-2 transition-all ${
              activeSubTab === 'media' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
            data-testid="subtab-media"
          >
            <Film className="w-4 h-4" />
            <span>Media Assets ({mediaList.length})</span>
          </button>
        </div>

        {/* Search & HSK Dock */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-card p-3 rounded-xl border border-border">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-muted-foreground" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tiêu đề, slug, chữ Hán..."
              className="w-full h-10 pl-9 pr-3 bg-background border border-input rounded-lg text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="content-search-input"
            />
          </div>

          {(activeSubTab === 'topics' || activeSubTab === 'lessons') && (
            <div className="flex items-center space-x-1.5 overflow-x-auto w-full sm:w-auto">
              <span className="text-xs font-bold uppercase text-muted-foreground px-2 flex items-center gap-1">
                <Filter className="w-3 h-3" /> HSK:
              </span>
              {['ALL', '1', '2', '3', '4', '5', '6'].map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setHskFilter(lvl)}
                  className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all ${
                    hskFilter === lvl ? 'bg-primary text-primary-foreground' : 'bg-secondary text-muted-foreground hover:text-foreground'
                  }`}
                >
                  {lvl === 'ALL' ? 'Tất cả' : `HSK ${lvl}`}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Main Content Feeds */}

      {/* 1. TOPICS */}
      {activeSubTab === 'topics' && (
        <div className="space-y-4" data-testid="topics-feed">
          {filteredTopics.length === 0 ? (
            <div className="p-8 text-center bg-card border border-border rounded-xl space-y-2">
              <BookOpen className="w-10 h-10 text-muted-foreground mx-auto" />
              <p className="text-base font-bold text-foreground">Chưa có chủ đề nào</p>
              <p className="text-xs text-muted-foreground">Bấm nút &quot;Tạo mới Chủ đề&quot; để bắt đầu soạn nội dung.</p>
            </div>
          ) : (
            filteredTopics.map((topic) => (
              <div
                key={topic.topicId}
                className="p-5 bg-card border border-border hover:border-primary/40 rounded-xl transition-all space-y-4 shadow-sm"
                data-testid={`topic-card-${topic.topicId}`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs font-extrabold rounded-md">
                        HSK {topic.hskLevel || 1}
                      </span>
                      <h3 className="text-lg font-bold text-foreground font-sans">{topic.title}</h3>
                    </div>
                    <p className="text-xs font-mono text-muted-foreground">slug: /{topic.slug}</p>
                    {topic.description && <p className="text-sm text-muted-foreground">{topic.description}</p>}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleEdit('TOPIC', topic)}
                    className="px-3 py-1.5 bg-secondary hover:bg-accent border border-border rounded-lg text-xs font-bold text-foreground shrink-0 self-start"
                    data-testid={`edit-topic-${topic.topicId}`}
                  >
                    Chỉnh sửa
                  </button>
                </div>

                <PublicationControls
                  entityType="TOPIC"
                  item={topic}
                  onPublish={handlePublishTopic}
                  onUnpublish={handleUnpublishTopic}
                  onArchive={handleArchiveTopic}
                  onReload={loadData}
                  staleConflict={staleConflictId === topic.topicId}
                  isLoading={isLoading}
                />
              </div>
            ))
          )}
        </div>
      )}

      {/* 2. LESSONS */}
      {activeSubTab === 'lessons' && (
        <div className="space-y-4" data-testid="lessons-feed">
          {filteredLessons.length === 0 ? (
            <div className="p-8 text-center bg-card border border-border rounded-xl space-y-2">
              <Layers className="w-10 h-10 text-muted-foreground mx-auto" />
              <p className="text-base font-bold text-foreground">Chưa có bài học nào</p>
              <p className="text-xs text-muted-foreground">Tạo bài học mới gắn liền với chủ đề để cho học viên luyện tập.</p>
            </div>
          ) : (
            filteredLessons.map((lesson) => (
              <div
                key={lesson.lessonId}
                className="p-5 bg-card border border-border hover:border-primary/40 rounded-xl transition-all space-y-4 shadow-sm"
                data-testid={`lesson-card-${lesson.lessonId}`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 bg-success/10 text-success text-xs font-extrabold rounded-md">
                        FREE
                      </span>
                      <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs font-extrabold rounded-md">
                        HSK {lesson.hskLevel || 1}
                      </span>
                      <h3 className="text-lg font-bold text-foreground font-sans">{lesson.title}</h3>
                    </div>
                    <p className="text-xs font-mono text-muted-foreground">slug: /{lesson.slug}</p>
                    {lesson.summary && <p className="text-sm text-muted-foreground">{lesson.summary}</p>}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleEdit('LESSON', lesson)}
                    className="px-3 py-1.5 bg-secondary hover:bg-accent border border-border rounded-lg text-xs font-bold text-foreground shrink-0 self-start"
                    data-testid={`edit-lesson-${lesson.lessonId}`}
                  >
                    Chỉnh sửa
                  </button>
                </div>

                <PublicationControls
                  entityType="LESSON"
                  item={lesson}
                  onPublish={handlePublishLesson}
                  onUnpublish={handleUnpublishLesson}
                  onArchive={handleArchiveLesson}
                  onReload={loadData}
                  staleConflict={staleConflictId === lesson.lessonId}
                  isLoading={isLoading}
                />
              </div>
            ))
          )}
        </div>
      )}

      {/* 3. SEGMENTS */}
      {activeSubTab === 'segments' && (
        <div className="space-y-4" data-testid="segments-feed">
          {filteredSegments.length === 0 ? (
            <div className="p-8 text-center bg-card border border-border rounded-xl space-y-2">
              <FileText className="w-10 h-10 text-muted-foreground mx-auto" />
              <p className="text-base font-bold text-foreground">Chưa có phân đoạn nào</p>
              <p className="text-xs text-muted-foreground">Tạo phân đoạn câu chữ Hán và liên kết với media để bắt đầu chép chính tả/shadowing.</p>
            </div>
          ) : (
            filteredSegments.map((segment) => (
              <div
                key={segment.segmentId}
                className="p-5 bg-card border border-border hover:border-primary/40 rounded-xl transition-all space-y-4 shadow-sm"
                data-testid={`segment-card-${segment.segmentId}`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="space-y-1.5">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 bg-secondary text-foreground text-xs font-mono font-bold rounded-md">
                        Seq #{segment.sequenceNo}
                      </span>
                      <span className="text-xs text-muted-foreground">({segment.segmentType})</span>
                    </div>
                    <p className="text-xl font-bold text-foreground font-hanzi tracking-wide">{segment.transcriptHanzi}</p>
                    {segment.transcriptPinyin && <p className="text-sm font-sans text-primary/90">{segment.transcriptPinyin}</p>}
                    {segment.translationVi && <p className="text-xs text-muted-foreground">{segment.translationVi}</p>}
                  </div>

                  {!publishedLessonIds.has(segment.lessonId) && <button
                    type="button"
                    onClick={() => handleEdit('SEGMENT', segment)}
                    className="px-3 py-1.5 bg-secondary hover:bg-accent border border-border rounded-lg text-xs font-bold text-foreground shrink-0 self-start"
                    data-testid={`edit-segment-${segment.segmentId}`}
                  >
                    Chỉnh sửa
                  </button>}
                </div>

                {!publishedLessonIds.has(segment.lessonId) ? <PublicationControls
                  entityType="SEGMENT"
                  item={segment}
                  onPublish={handlePublishSegment}
                  onUnpublish={handleUnpublishSegment}
                  onArchive={handleArchiveSegment}
                  onReload={loadData}
                  staleConflict={staleConflictId === segment.segmentId}
                  isLoading={isLoading}
                /> : <p role="status" className="text-sm text-muted-foreground">Unpublish the parent lesson before changing this learner-visible segment.</p>}
              </div>
            ))
          )}
        </div>
      )}

      {/* 4. MEDIA ASSETS */}
      {activeSubTab === 'media' && (
        <div className="space-y-4" data-testid="media-feed">
          {filteredMedia.length === 0 ? (
            <div className="p-8 text-center bg-card border border-border rounded-xl space-y-2">
              <Film className="w-10 h-10 text-muted-foreground mx-auto" />
              <p className="text-base font-bold text-foreground">Chưa có tệp media nào</p>
              <p className="text-xs text-muted-foreground">Khai báo thông tin video YouTube hoặc audio để dùng cho phân đoạn bài học.</p>
            </div>
          ) : (
            filteredMedia.map((media) => (
              <div
                key={media.mediaAssetId}
                className="p-5 bg-card border border-border hover:border-primary/40 rounded-xl transition-all space-y-4 shadow-sm"
                data-testid={`media-card-${media.mediaAssetId}`}
              >
                <YouTubeThumbnail
                  mediaAssetId={media.mediaAssetId}
                  videoId={media.providerAssetIdentifier}
                  title={media.title}
                />
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 bg-secondary text-foreground text-xs font-mono font-bold rounded-md">
                        {media.providerName}
                      </span>
                      <h3 className="text-base font-bold text-foreground font-sans">{media.title || media.providerAssetIdentifier}</h3>
                    </div>
                    <p className="text-xs font-mono text-muted-foreground">ID: {media.providerAssetIdentifier}</p>
                  </div>

                  {!mediaUsedByPublishedLessonIds.has(media.mediaAssetId) && <button
                    type="button"
                    onClick={() => handleEdit('MEDIA', media)}
                    className="px-3 py-1.5 bg-secondary hover:bg-accent border border-border rounded-lg text-xs font-bold text-foreground shrink-0 self-start"
                    data-testid={`edit-media-${media.mediaAssetId}`}
                  >
                    Chỉnh sửa
                  </button>}
                </div>

                {!mediaUsedByPublishedLessonIds.has(media.mediaAssetId) ? <PublicationControls
                  entityType="MEDIA"
                  item={media}
                  onApproveMedia={handleApproveMedia}
                  onRejectMedia={handleRejectMedia}
                  onQuarantineMedia={handleQuarantineMedia}
                  onReload={loadData}
                  staleConflict={staleConflictId === media.mediaAssetId}
                  isLoading={isLoading}
                /> : <p role="status" className="text-sm text-muted-foreground">Unpublish dependent lessons before changing this learner-visible media.</p>}
              </div>
            ))
          )}
        </div>
      )}

      {/* Content Editor Modal */}
      {editorState && (
        <ContentEditor
          mode={editorState.mode}
          initialData={editorState.item}
          topics={topics}
          lessons={lessons}
          mediaList={mediaList}
          onSave={handleSaveEditor}
          onCancel={() => setEditorState(null)}
          serverError={errorNotice}
          onClearServerError={() => setErrorNotice(null)}
          isLoading={isLoading}
        />
      )}
    </div>
  );
}
