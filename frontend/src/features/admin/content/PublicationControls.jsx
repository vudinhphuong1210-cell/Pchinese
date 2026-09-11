import React, { useState } from 'react';
import { Play, Pause, Archive, CheckCircle, XCircle, AlertTriangle, RefreshCw } from 'lucide-react';

export function PublicationControls({
  entityType, // 'TOPIC' | 'LESSON' | 'SEGMENT' | 'MEDIA'
  item,
  onPublish,
  onUnpublish,
  onArchive,
  onApproveMedia,
  onRejectMedia,
  onQuarantineMedia,
  onReload,
  staleConflict = false,
  isLoading = false,
}) {
  const [confirmModal, setConfirmModal] = useState(null); // null | { type: 'unpublish'|'archive'|'reject'|'quarantine', action: fn }

  const status = item?.publicationState || item?.approvalStatus || 'DRAFT';
  const version = item?.version ?? 0;

  const handleAction = (type, actionFn) => {
    if (type === 'unpublish' || type === 'archive' || type === 'reject' || type === 'quarantine') {
      setConfirmModal({ type, action: actionFn });
    } else {
      actionFn(item.id || item.topicId || item.lessonId || item.segmentId || item.mediaAssetId, version);
    }
  };

  const confirmAction = () => {
    if (confirmModal?.action) {
      confirmModal.action(item.id || item.topicId || item.lessonId || item.segmentId || item.mediaAssetId, version);
    }
    setConfirmModal(null);
  };

  return (
    <div className="space-y-3 font-sans">
      {/* Stale Conflict Banner */}
      {staleConflict && (
        <div
          role="alert"
          className="p-3.5 bg-warning/10 border border-warning/40 rounded-xl text-warning flex items-center justify-between text-sm shadow-sm"
          data-testid="stale-conflict-banner"
        >
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 shrink-0 text-warning" />
            <span>Phiên bản dữ liệu không đồng bộ (Server phiên bản v{version + 1}). Vui lòng tải lại dữ liệu mới nhất.</span>
          </div>
          <button
            type="button"
            onClick={onReload}
            className="px-3 py-1.5 bg-warning text-warning-foreground rounded-lg font-bold text-xs flex items-center space-x-1 hover:opacity-90 transition-opacity"
            data-testid="reload-btn"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Tải lại</span>
          </button>
        </div>
      )}

      {/* Action Toolbar */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Status Badge */}
        <div className="flex items-center space-x-1.5 px-3 py-1.5 rounded-full bg-secondary border border-border text-xs font-bold uppercase tracking-wider text-foreground">
          <span
            className={`w-2 h-2 rounded-full ${
              status === 'PUBLISHED' || status === 'APPROVED'
                ? 'bg-success'
                : status === 'ARCHIVED' || status === 'REJECTED' || status === 'QUARANTINED'
                ? 'bg-destructive'
                : 'bg-warning'
            }`}
          />
          <span data-testid="publication-status-badge">{status}</span>
          <span className="text-muted-foreground text-[10px] lowercase">(v{version})</span>
        </div>

        {/* Content Lifecycle Buttons */}
        {entityType !== 'MEDIA' && status !== 'ARCHIVED' && (
          <>
            {status !== 'PUBLISHED' && (
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleAction('publish', onPublish)}
                className="px-3.5 py-1.5 bg-primary text-primary-foreground hover:opacity-90 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
                data-testid="publish-btn"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Xuất bản (Publish)</span>
              </button>
            )}

            {status === 'PUBLISHED' && (
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleAction('unpublish', onUnpublish)}
                className="px-3.5 py-1.5 bg-secondary text-foreground hover:bg-accent border border-border rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
                data-testid="unpublish-btn"
              >
                <Pause className="w-3.5 h-3.5" />
                <span>Hủy xuất bản</span>
              </button>
            )}

            <button
              type="button"
              disabled={isLoading}
              onClick={() => handleAction('archive', onArchive)}
              className="px-3 py-1.5 bg-destructive/10 text-destructive hover:bg-destructive/20 border border-destructive/30 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
              data-testid="archive-btn"
            >
              <Archive className="w-3.5 h-3.5" />
              <span>Lưu trữ (Archive)</span>
            </button>
          </>
        )}

        {/* Media Lifecycle Buttons */}
        {entityType === 'MEDIA' && (
          <>
            {status === 'PENDING_SCAN' && item?.approvalEligible && (
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleAction('approve', onApproveMedia)}
                className="px-3.5 py-1.5 bg-success text-primary-foreground hover:opacity-90 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
                data-testid="approve-media-btn"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                <span>Phê duyệt (Approve)</span>
              </button>
            )}

            {(status === 'PENDING_SCAN' || status === 'APPROVED') && (
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleAction('reject', onRejectMedia)}
                className="px-3.5 py-1.5 bg-warning/20 text-warning hover:bg-warning/30 border border-warning/40 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
                data-testid="reject-media-btn"
              >
                <XCircle className="w-3.5 h-3.5" />
                <span>Từ chối (Reject)</span>
              </button>
            )}

            {(status === 'PENDING_SCAN' || status === 'APPROVED') && (
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleAction('quarantine', onQuarantineMedia)}
                className="px-3.5 py-1.5 bg-destructive text-destructive-foreground hover:opacity-90 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-all disabled:opacity-50"
                data-testid="quarantine-media-btn"
              >
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>Cách ly (Quarantine)</span>
              </button>
            )}
          </>
        )}
      </div>

      {/* Confirmation Modal */}
      {confirmModal && (
        <div className="fixed inset-0 bg-foreground/70 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fade-in">
          <div role="dialog" aria-modal="true" className="bg-popover border border-border rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center space-x-3 text-warning">
              <AlertTriangle className="w-6 h-6 shrink-0" />
              <h3 className="text-lg font-bold text-foreground">Xác nhận thao tác</h3>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              {confirmModal.type === 'unpublish' &&
                'Bạn có chắc chắn muốn hủy xuất bản? Hành động này sẽ làm cho nội dung tạm ngừng hiển thị trên ứng dụng của học viên.'}
              {confirmModal.type === 'archive' &&
                'Lưu trữ nội dung là thao tác VĨNH VIỄN (terminal state). Bạn sẽ không thể xuất bản lại nội dung này.'}
              {confirmModal.type === 'reject' &&
                'Từ chối tệp truyền thông này? Nếu tệp đang được sử dụng trong các bài học đã xuất bản, các bài học liên quan sẽ tự động bị hủy xuất bản.'}
              {confirmModal.type === 'quarantine' &&
                'Cách ly tệp truyền thông do nghi ngờ độc hại? Mọi bài học đã xuất bản phụ thuộc vào tệp này sẽ tự động bị rút khỏi ứng dụng.'}
            </p>
            <div className="flex justify-end space-x-3 pt-2">
              <button
                type="button"
                onClick={() => setConfirmModal(null)}
                className="px-4 py-2 bg-secondary text-foreground hover:bg-accent border border-border rounded-lg font-semibold text-sm transition-all"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={confirmAction}
                className="px-4 py-2 bg-destructive text-destructive-foreground hover:opacity-90 rounded-lg font-semibold text-sm transition-all"
                data-testid="confirm-dialog-submit"
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
