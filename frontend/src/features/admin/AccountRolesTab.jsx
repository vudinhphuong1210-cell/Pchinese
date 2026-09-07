import React, { useState } from 'react';
import { adminUsersApi } from '../../api/adminUsers.js';
import { Search, Shield, ShieldAlert, Lock, Unlock, CheckCircle2, AlertCircle, Loader2, UserCheck } from 'lucide-react';

const REASON_OPTIONS = [
  { value: 'SECURITY', label: 'Bảo mật (SECURITY)' },
  { value: 'POLICY', label: 'Vi phạm chính sách (POLICY)' },
  { value: 'USER_REQUEST', label: 'Yêu cầu của người dùng (USER_REQUEST)' },
  { value: 'OTHER', label: 'Lý do khác (OTHER - Yêu cầu ghi chú)' },
];

export function AccountRolesTab() {
  const [searchUserId, setSearchUserId] = useState('');
  const [projection, setProjection] = useState(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Command Dialog States
  const [dialogState, setDialogState] = useState(null);
  const [reason, setReason] = useState('SECURITY');
  const [note, setNote] = useState('');

  const handleSearch = async (e, preserveError = false) => {
    if (e) e.preventDefault();
    const targetId = searchUserId.trim();
    if (!targetId) {
      setError({ message: 'Vui lòng nhập exact User UUID.' });
      return;
    }

    if (!preserveError) {
      setError(null);
    }
    setSuccessMsg(null);
    setLoading(true);

    try {
      const res = await adminUsersApi.getAccountRoleProjection(targetId);
      if (res && res.success && res.data) {
        setProjection(res.data);
      }
    } catch (err) {
      if (!preserveError) {
        setError(err?.error || { message: 'Không tìm thấy tài khoản hợp lệ với UUID đã cho.' });
      }
    } finally {
      setLoading(false);
    }
  };

  const openActionDialog = (type) => {
    setError(null);
    setSuccessMsg(null);
    setReason('SECURITY');
    setNote('');
    setDialogState({ type });
  };

  const executeAction = async () => {
    if (!projection || !dialogState) return;

    const { type } = dialogState;
    if ((type === 'LOCK' || type === 'UNLOCK') && reason === 'OTHER' && !note.trim()) {
      setError({ message: 'Lý do OTHER yêu cầu phải có ghi chú chi tiết.' });
      return;
    }

    if ((type === 'LOCK' || type === 'UNLOCK') && note.length > 280) {
      setError({ message: 'Ghi chú tối đa 280 ký tự.' });
      return;
    }

    setActionLoading(true);
    setError(null);

    try {
      let res;
      if (type === 'GRANT_ROLE') {
        res = await adminUsersApi.grantAdminRole(projection.userId);
      } else if (type === 'REVOKE_ROLE') {
        res = await adminUsersApi.revokeAdminRole(projection.userId);
      } else if (type === 'LOCK') {
        res = await adminUsersApi.lockAccount(projection.userId, { reason, note });
      } else if (type === 'UNLOCK') {
        res = await adminUsersApi.unlockAccount(projection.userId, { reason, note });
      }

      if (res && res.success && res.data) {
        setProjection(res.data);
        setSuccessMsg(`Thực thi lệnh ${type} thành công.`);
        setDialogState(null);
      }
    } catch (err) {
      const errObj = err?.error || { message: 'Không thể thực thi lệnh.' };
      setError(errObj);
      setDialogState(null);
      // Reload projection on conflict or state error while preserving conflict error
      if (errObj.code === 'STATE_CONFLICT') {
        handleSearch(null, true);
      }
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-4xl w-full shadow-lg" data-testid="account-roles-tab">
      <div className="border-b border-border pb-4 space-y-1">
        <div className="flex items-center space-x-2 text-primary">
          <Shield className="w-6 h-6" />
          <h2 className="text-xl font-bold text-foreground">Quản trị Vai trò & Quyền truy cập (Exact-ID Admin)</h2>
        </div>
        <p className="text-sm text-muted-foreground leading-relaxed">
          Tra cứu chính xác tài khoản theo UUID để kiểm tra vai trò ADMIN hoặc thực hiện khóa/mở khóa tài khoản.
        </p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="admin-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">{error.code || 'LỖI THỰC THI'}</p>
            <p className="mt-0.5">{error.message}</p>
          </div>
        </div>
      )}

      {successMsg && (
        <div className="bg-success/10 border border-success/30 text-success rounded-md p-3.5 text-sm flex items-start space-x-2.5" data-testid="admin-success">
          <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Lookup Form */}
      <form onSubmit={(e) => handleSearch(e, false)} className="flex gap-3 items-end" data-testid="lookup-form">
        <div className="flex-1 space-y-1.5">
          <label htmlFor="user-id-input" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            User ID (UUID v4)
          </label>
          <div className="relative">
            <Search className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="user-id-input"
              type="text"
              required
              value={searchUserId}
              onChange={(e) => setSearchUserId(e.target.value)}
              placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring font-mono text-sm"
              data-testid="user-id-input"
            />
          </div>
        </div>
        <button
          type="submit"
          disabled={loading || !searchUserId.trim()}
          className="h-11 px-5 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center space-x-2 shrink-0"
          data-testid="lookup-btn"
        >
          {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Search className="w-5 h-5" />}
          <span>Tra cứu</span>
        </button>
      </form>

      {/* Projection Result Card */}
      {projection && (
        <div className="bg-secondary/20 border border-border rounded-lg p-5 space-y-6 animate-in fade-in" data-testid="projection-card">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/80 pb-4">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Target User ID</span>
              <p className="font-mono text-base font-semibold text-foreground mt-0.5">{projection.userId}</p>
            </div>
            <div className="flex items-center space-x-3">
              <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Trạng thái truy cập:</span>
              <span
                className={`px-3 py-1 rounded-pill text-xs font-bold uppercase tracking-wider ${
                  projection.accessState === 'ACTIVE'
                    ? 'bg-success/20 text-success border border-success/40'
                    : 'bg-destructive/20 text-destructive border border-destructive/40'
                }`}
                data-testid="access-state-badge"
              >
                {projection.accessState}
              </span>
            </div>
          </div>

          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Vai trò hệ thống</h3>
            <div className="flex flex-wrap gap-2 items-center">
              {projection.roles && projection.roles.length > 0 ? (
                projection.roles.map((r) => (
                  <span
                    key={r}
                    className="px-3 py-1 bg-primary/20 text-primary border border-primary/30 rounded-md text-xs font-bold flex items-center space-x-1.5"
                    data-testid={`role-badge-${r}`}
                  >
                    <UserCheck className="w-4 h-4" />
                    <span>{r}</span>
                  </span>
                ))
              ) : (
                <span className="text-sm text-muted-foreground italic">Không có vai trò đặc biệt (Learner chuẩn)</span>
              )}
            </div>
          </div>

          {/* Action Commands Grid */}
          <div className="pt-2 border-t border-border/80 grid grid-cols-1 sm:grid-cols-2 gap-3">
            {projection.roles?.includes('ADMIN') ? (
              <button
                type="button"
                onClick={() => openActionDialog('REVOKE_ROLE')}
                className="h-10 px-4 bg-secondary border border-border text-foreground font-semibold rounded-md hover:bg-accent hover:text-destructive transition-colors flex items-center justify-center space-x-2 text-sm"
                data-testid="revoke-admin-btn"
              >
                <ShieldAlert className="w-4 h-4 text-destructive" />
                <span>Thu hồi quyền ADMIN</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => openActionDialog('GRANT_ROLE')}
                className="h-10 px-4 bg-primary/10 border border-primary/30 text-primary font-semibold rounded-md hover:bg-primary/20 transition-colors flex items-center justify-center space-x-2 text-sm"
                data-testid="grant-admin-btn"
              >
                <Shield className="w-4 h-4" />
                <span>Gán quyền ADMIN</span>
              </button>
            )}

            {projection.accessState === 'ACTIVE' ? (
              <button
                type="button"
                onClick={() => openActionDialog('LOCK')}
                className="h-10 px-4 bg-destructive/10 border border-destructive/30 text-destructive font-semibold rounded-md hover:bg-destructive/20 transition-colors flex items-center justify-center space-x-2 text-sm"
                data-testid="lock-account-btn"
              >
                <Lock className="w-4 h-4" />
                <span>Khóa tài khoản</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => openActionDialog('UNLOCK')}
                className="h-10 px-4 bg-success/10 border border-success/30 text-success font-semibold rounded-md hover:bg-success/20 transition-colors flex items-center justify-center space-x-2 text-sm"
                data-testid="unlock-account-btn"
              >
                <Unlock className="w-4 h-4" />
                <span>Mở khóa tài khoản</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* Confirmation & Parameters Dialog */}
      {dialogState && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-popover border border-border rounded-xl p-6 max-w-lg w-full space-y-5 shadow-2xl animate-in fade-in zoom-in-95">
            <div className="flex items-start space-x-3">
              <ShieldAlert className="w-6 h-6 text-primary shrink-0 mt-0.5" />
              <div>
                <h3 className="font-bold text-lg text-foreground">
                  Xác nhận lệnh: <span className="text-primary">{dialogState.type}</span>
                </h3>
                <p className="text-sm text-muted-foreground mt-1">
                  Thao tác trên User ID <code className="font-mono text-foreground font-bold">{projection?.userId}</code>.
                </p>
              </div>
            </div>

            {(dialogState.type === 'LOCK' || dialogState.type === 'UNLOCK') && (
              <div className="space-y-4 pt-2 border-t border-border">
                <div className="space-y-1.5">
                  <label htmlFor="reason-select" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Lý do thực thi (Reason)
                  </label>
                  <select
                    id="reason-select"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    className="w-full h-11 px-3.5 bg-secondary/50 border border-input rounded-md text-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                    data-testid="reason-select"
                  >
                    {REASON_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value} className="bg-popover text-foreground">
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="note-input" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Ghi chú an toàn {reason === 'OTHER' ? '(Bắt buộc)' : '(Tùy chọn, tối đa 280 ký tự)'}
                  </label>
                  <textarea
                    id="note-input"
                    rows={3}
                    maxLength={280}
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Không chứa mật khẩu hay thông tin cá nhân riêng tư..."
                    className="w-full p-3 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring text-sm"
                    data-testid="note-input"
                  />
                  <div className="text-right text-[11px] text-muted-foreground">{note.length}/280</div>
                </div>
              </div>
            )}

            {dialogState.type === 'REVOKE_ROLE' && (
              <p className="text-sm text-amber-400 bg-amber-950/40 border border-amber-800/50 p-3 rounded-md">
                Chú ý: Lệnh này sẽ thu hồi quyền ADMIN của tài khoản. Lệnh sẽ bị từ chối với 409 Conflict nếu đây là ADMIN cuối cùng hoặc tự thu hồi chính mình.
              </p>
            )}

            <div className="flex items-center justify-end space-x-3 pt-3 border-t border-border">
              <button
                type="button"
                onClick={() => setDialogState(null)}
                disabled={actionLoading}
                className="h-10 px-4 text-sm font-semibold bg-secondary text-secondary-foreground rounded-md hover:bg-accent"
                data-testid="cancel-dialog-btn"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={executeAction}
                disabled={actionLoading}
                className="h-10 px-5 text-sm font-bold bg-primary text-primary-foreground rounded-md hover:opacity-90 flex items-center space-x-2"
                data-testid="confirm-dialog-btn"
              >
                {actionLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang thực thi...</span>
                  </>
                ) : (
                  <span>Xác nhận thực thi</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
