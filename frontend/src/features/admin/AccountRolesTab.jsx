import React, { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { adminUsersApi } from '../../api/adminUsers.js';
import {
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Lock,
  Shield,
  ShieldAlert,
  Unlock,
  UserCheck,
  Users,
  X,
  User,
  AlertTriangle,
} from 'lucide-react';

const PAGE_SIZE = 20;
const MISSING_ACCOUNT_NAME = 'Chưa đặt tên';
const REASON_OPTIONS = [
  { value: 'SECURITY', label: 'Bảo mật (SECURITY)' },
  { value: 'POLICY', label: 'Vi phạm chính sách (POLICY)' },
  { value: 'USER_REQUEST', label: 'Yêu cầu của người dùng (USER_REQUEST)' },
  { value: 'OTHER', label: 'Lý do khác (OTHER - yêu cầu ghi chú)' },
];

const REASON_LABELS = {
  SECURITY: 'Bảo mật',
  POLICY: 'Vi phạm chính sách',
  USER_REQUEST: 'Yêu cầu người dùng',
  OTHER: 'Lý do khác',
};

const manageableStates = new Set(['ACTIVE', 'LOCKED']);

function DirectoryRoles({ roles }) {
  if (!roles?.length) {
    return <span className="text-xs text-muted-foreground">Learner</span>;
  }
  return roles.map((role) => (
    <span
      key={role}
      className="px-2 py-1 bg-primary/10 text-primary border border-primary/30 rounded-md text-xs font-bold"
      data-testid={`directory-role-${role}`}
    >
      {role}
    </span>
  ));
}

export function AccountRolesTab() {
  const [directory, setDirectory] = useState(null);
  const [page, setPage] = useState(0);
  const [directoryLoading, setDirectoryLoading] = useState(true);
  const [projection, setProjection] = useState(null);
  const [loadingUserId, setLoadingUserId] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [dialogState, setDialogState] = useState(null);
  const [reason, setReason] = useState('SECURITY');
  const [note, setNote] = useState('');

  const loadDirectory = useCallback(async (requestedPage) => {
    setDirectoryLoading(true);
    setError(null);
    try {
      const res = await adminUsersApi.listManagedUsers({ page: requestedPage, size: PAGE_SIZE });
      if (res?.success && res.data) {
        setDirectory(res.data);
      }
    } catch (err) {
      setError(err?.error || { message: 'Không thể tải danh sách người dùng.' });
    } finally {
      setDirectoryLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDirectory(page);
  }, [loadDirectory, page]);

  const selectUser = async (userId, preserveError = false) => {
    if (!preserveError) {
      setError(null);
    }
    setSuccessMsg(null);
    setDialogState(null);
    setLoadingUserId(userId);
    try {
      const res = await adminUsersApi.getAccountRoleProjection(userId);
      if (res?.success && res.data) {
        const matchedDirUser = directory?.items?.find((item) => item.userId === userId);
        const accountName =
          res.data.accountName ||
          matchedDirUser?.accountName ||
          MISSING_ACCOUNT_NAME;

        const mergedData = {
          ...res.data,
          accountName: accountName,
          lockReason: res.data.lockReason || res.data.note || matchedDirUser?.lockReason || matchedDirUser?.note || null,
        };
        setProjection(mergedData);
      }
    } catch (err) {
      if (!preserveError) {
        setError(err?.error || { message: 'Không thể tải trạng thái tài khoản đã chọn.' });
      }
    } finally {
      setLoadingUserId(null);
    }
  };

  const closeModal = () => {
    setProjection(null);
    setDialogState(null);
    setError(null);
    setSuccessMsg(null);
  };

  const openActionDialog = (type) => {
    setError(null);
    setSuccessMsg(null);
    setReason('SECURITY');
    setNote('');
    setDialogState({ type });
  };

  const updateDirectoryFromProjection = (nextProjection) => {
    setDirectory((current) => {
      if (!current) return current;
      return {
        ...current,
        items: current.items.map((item) =>
          item.userId === nextProjection.userId
            ? {
                ...item,
                roles: nextProjection.roles,
                accountState: nextProjection.accessState,
                lockReason: nextProjection.lockReason || nextProjection.note || item.lockReason,
              }
            : item
        ),
      };
    });
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

      if (res?.success && res.data) {
        const updatedProjection = {
          ...res.data,
          accountName: projection.accountName,
          lockReason: type === 'LOCK' ? (REASON_LABELS[reason] || reason) + (note ? `: ${note}` : '') : null,
        };
        setProjection(updatedProjection);
        updateDirectoryFromProjection(updatedProjection);
        setSuccessMsg(`Thực thi lệnh ${type} thành công.`);
        setDialogState(null);
      }
    } catch (err) {
      const errObj = err?.error || { message: 'Không thể thực thi lệnh.' };
      setError(errObj);
      setDialogState(null);
      if (errObj.code === 'STATE_CONFLICT') {
        void selectUser(projection.userId, true);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const totalPages = directory?.totalPages ?? 0;
  const currentPage = directory?.page ?? page;

  return (
    <div
      className="bg-card border border-border rounded-2xl p-6 sm:p-8 space-y-6 max-w-5xl w-full shadow-lg font-sans"
      data-testid="account-roles-tab"
    >
      {/* Page Header */}
      <div className="border-b border-border pb-4 space-y-1">
        <div className="flex items-center space-x-3 text-primary">
          <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center">
            <Users className="w-5 h-5 text-primary" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Quản lý người dùng</h2>
        </div>
        <p className="text-sm text-muted-foreground leading-relaxed pl-13">
          Chọn một người dùng từ danh sách để quản lý vai trò ADMIN hoặc khóa/mở khóa tài khoản.
          Danh sách chỉ hiển thị mã tài khoản, tên tài khoản, trạng thái và vai trò.
        </p>
      </div>

      {/* Global Alerts */}
      {error && !dialogState && !projection && (
        <div
          className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 text-sm flex items-start space-x-3"
          role="alert"
          data-testid="admin-error"
        >
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">{error.code || 'LỖI THỰC THI'}</p>
            <p className="mt-0.5">{error.message}</p>
          </div>
        </div>
      )}

      {successMsg && !projection && (
        <div
          className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 rounded-xl p-4 text-sm flex items-start space-x-3"
          aria-live="polite"
          data-testid="admin-success"
        >
          <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* User Directory Table Section */}
      <section aria-labelledby="user-directory-heading" className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div>
            <h3 id="user-directory-heading" className="font-bold text-foreground">
              Danh sách người dùng
            </h3>
            <p className="text-sm text-muted-foreground">Chọn tài khoản cần quản lý.</p>
          </div>
          {directory && (
            <span className="text-xs font-semibold px-3 py-1 bg-secondary rounded-full border border-border text-muted-foreground">
              {directory.totalItems} tài khoản
            </span>
          )}
        </div>

        {directoryLoading && (
          <div className="min-h-24 flex items-center justify-center text-sm text-muted-foreground" aria-live="polite">
            <Loader2 className="w-5 h-5 animate-spin mr-2 text-primary" /> Đang tải danh sách người dùng...
          </div>
        )}

        {!directoryLoading && directory && (
          <div className="border border-border rounded-2xl overflow-hidden shadow-sm bg-card" data-testid="user-directory-list">
            {directory.items.length === 0 ? (
              <p className="p-6 text-sm text-muted-foreground text-center">Không có tài khoản nào trên trang này.</p>
            ) : (
              <div className="divide-y divide-border/60">
                {directory.items.map((user) => {
                  const canManage = manageableStates.has(user.accountState);
                  const isLocked = user.accountState === 'LOCKED';
                  const userAccountName = user.accountName || MISSING_ACCOUNT_NAME;
                  const displayLockReason = isLocked ? (user.lockReason || user.note || 'Đang bị khóa') : '-';

                  return (
                    <div
                      key={user.userId}
                      className="p-4 sm:p-5 flex flex-col lg:flex-row lg:items-center gap-4 hover:bg-secondary/20 transition-colors"
                      data-testid={`user-row-${user.userId}`}
                    >
                      {/* Column 1: User ID & Account Name */}
                      <div className="min-w-0 flex-1 space-y-1">
                        <div className="flex items-center space-x-2">
                          <User className="w-4 h-4 text-primary shrink-0" />
                          <span className="font-bold text-sm text-foreground truncate">{userAccountName}</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-[11px] font-bold uppercase tracking-wider text-muted-foreground">USER ID:</span>
                          <span className="font-mono text-xs text-muted-foreground truncate">{user.userId}</span>
                        </div>
                      </div>

                      {/* Column 2: State */}
                      <div className="min-w-28 space-y-1">
                        <span className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">Trạng thái</span>
                        <span
                          className={`inline-flex px-2.5 py-1 rounded-full text-xs font-bold ${
                            user.accountState === 'ACTIVE'
                              ? 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/30'
                              : 'bg-destructive/10 text-destructive border border-destructive/30'
                          }`}
                        >
                          {user.accountState}
                        </span>
                      </div>

                      {/* Column 3: Lock Reason */}
                      <div className="min-w-36 space-y-1">
                        <span className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">Lý do khóa</span>
                        <span className={`text-xs block truncate ${isLocked ? 'text-destructive font-medium' : 'text-muted-foreground'}`}>
                          {displayLockReason}
                        </span>
                      </div>

                      {/* Column 4: Roles */}
                      <div className="min-w-28 space-y-1">
                        <span className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">Vai trò</span>
                        <div className="flex gap-1 flex-wrap">
                          <DirectoryRoles roles={user.roles} />
                        </div>
                      </div>

                      {/* Column 5: Manage Action Button */}
                      <div className="shrink-0 pt-2 lg:pt-0">
                        <button
                          type="button"
                          onClick={() => void selectUser(user.userId)}
                          disabled={!canManage || loadingUserId === user.userId}
                          title={canManage ? 'Quản lý tài khoản này' : 'Trạng thái tài khoản này chưa thể quản lý'}
                          className="h-10 px-5 bg-primary text-primary-foreground font-bold text-sm rounded-xl hover:opacity-90 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                          data-testid={`manage-user-${user.userId}`}
                        >
                          {loadingUserId === user.userId ? (
                            <div className="flex items-center space-x-1">
                              <Loader2 className="w-4 h-4 animate-spin" />
                              <span>Đang tải...</span>
                            </div>
                          ) : (
                            'Quản lý'
                          )}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination Controls */}
            <div className="p-4 border-t border-border flex items-center justify-between gap-3 bg-secondary/30">
              <span className="text-xs text-muted-foreground font-medium">
                Trang {totalPages ? currentPage + 1 : 0}/{totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPage((value) => Math.max(0, value - 1))}
                  disabled={directoryLoading || currentPage <= 0}
                  className="h-9 px-3 bg-card border border-border rounded-xl disabled:opacity-50 hover:bg-secondary transition-colors"
                  aria-label="Trang trước"
                  data-testid="previous-page-btn"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setPage((value) => value + 1)}
                  disabled={directoryLoading || totalPages === 0 || currentPage >= totalPages - 1}
                  className="h-9 px-3 bg-card border border-border rounded-xl disabled:opacity-50 hover:bg-secondary transition-colors"
                  aria-label="Trang sau"
                  data-testid="next-page-btn"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* ULTRA-COMPACT SINGLE MODAL DIALOG: Mounted via createPortal to document.body */}
      {projection &&
        createPortal(
          <div
            className="fixed inset-0 z-[9999] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
            role="presentation"
          >
            <div
              className="bg-card border border-border rounded-2xl p-5 sm:p-6 max-w-lg w-full space-y-4 shadow-2xl animate-in fade-in zoom-in-95 duration-150"
              data-testid="projection-card"
              role="dialog"
              aria-modal="true"
              aria-labelledby="selected-user-heading"
            >
              {/* Modal Header */}
              <div className="flex items-center justify-between border-b border-border pb-3">
                <div className="flex items-center space-x-3 min-w-0">
                  <div className="w-9 h-9 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center text-primary font-bold shrink-0">
                    <User className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <h3 id="selected-user-heading" className="text-base font-bold text-foreground">
                      Quản lý người dùng
                    </h3>
                    <p className="text-xs text-muted-foreground truncate font-medium" data-testid="selected-account-name">
                      {projection.accountName || MISSING_ACCOUNT_NAME}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={closeModal}
                  className="w-8 h-8 rounded-full hover:bg-secondary text-muted-foreground hover:text-foreground flex items-center justify-center transition-colors shrink-0"
                  title="Đóng"
                  data-testid="close-modal-btn"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* In-Modal Errors / Success messages */}
              {error && (
                <div
                  className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-3 text-xs flex items-start space-x-2"
                  role="alert"
                  data-testid="admin-error"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold">{error.code || 'LỖI THỰC THI'}</p>
                    <p className="mt-0.5">{error.message}</p>
                  </div>
                </div>
              )}

              {successMsg && (
                <div
                  className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 rounded-xl p-3 text-xs flex items-start space-x-2"
                  aria-live="polite"
                  data-testid="admin-success"
                >
                  <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>{successMsg}</span>
                </div>
              )}

              {/* Selected User Details Card */}
              <div className="bg-secondary/20 border border-border rounded-xl p-4 space-y-3.5">
                {/* Account Name & Access State */}
                <div className="flex items-center justify-between gap-3 border-b border-border/60 pb-3">
                  <div className="min-w-0">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-muted-foreground block">
                      TÊN TÀI KHOẢN
                    </span>
                    <p className="text-sm font-bold text-foreground truncate mt-0.5">
                      {projection.accountName || MISSING_ACCOUNT_NAME}
                    </p>
                    <span className="text-[11px] font-mono text-muted-foreground block truncate">
                      ID: {projection.userId}
                    </span>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground block mb-1">
                      TRẠNG THÁI
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider ${
                        projection.accessState === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/30'
                          : 'bg-destructive/10 text-destructive border border-destructive/30'
                      }`}
                      data-testid="access-state-badge"
                    >
                      {projection.accessState}
                    </span>
                  </div>
                </div>

                {/* Current Lock Reason if account is LOCKED */}
                {projection.accessState === 'LOCKED' && !dialogState && (
                  <div className="p-2.5 bg-destructive/10 border border-destructive/20 rounded-lg flex items-start space-x-2 text-xs">
                    <AlertTriangle className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold text-destructive">Lý do khóa: </span>
                      <span className="text-foreground">{projection.lockReason || 'Đang bị khóa bởi quản trị viên.'}</span>
                    </div>
                  </div>
                )}

                {/* Roles */}
                <div className="flex items-center justify-between gap-2 text-xs">
                  <span className="font-bold text-muted-foreground uppercase text-[11px]">VAI TRÒ:</span>
                  <div className="flex gap-1 flex-wrap">
                    {projection.roles?.length ? (
                      projection.roles.map((role) => (
                        <span
                          key={role}
                          className="px-2 py-0.5 bg-primary/10 text-primary border border-primary/30 rounded-md font-bold text-[11px] flex items-center space-x-1"
                          data-testid={`role-badge-${role}`}
                        >
                          <UserCheck className="w-3 h-3" />
                          <span>{role}</span>
                        </span>
                      ))
                    ) : (
                      <span className="text-muted-foreground italic text-[11px]">Learner</span>
                    )}
                  </div>
                </div>

                {/* Main Action Buttons */}
                <div className="pt-2 border-t border-border/60 grid grid-cols-2 gap-2">
                  {projection.roles?.includes('ADMIN') ? (
                    <button
                      type="button"
                      onClick={() => openActionDialog('REVOKE_ROLE')}
                      className={`h-9 px-3 border font-semibold rounded-lg transition-all flex items-center justify-center space-x-1.5 text-xs shadow-sm ${
                        dialogState?.type === 'REVOKE_ROLE'
                          ? 'bg-destructive/20 text-destructive border-destructive'
                          : 'bg-secondary border-border text-foreground hover:bg-destructive/10 hover:text-destructive'
                      }`}
                      data-testid="revoke-admin-btn"
                    >
                      <ShieldAlert className="w-3.5 h-3.5 text-destructive" />
                      <span>Thu hồi ADMIN</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => openActionDialog('GRANT_ROLE')}
                      className={`h-9 px-3 border font-bold rounded-lg transition-all flex items-center justify-center space-x-1.5 text-xs shadow-sm ${
                        dialogState?.type === 'GRANT_ROLE'
                          ? 'bg-primary/30 text-primary border-primary'
                          : 'bg-primary/10 border-primary/30 text-primary hover:bg-primary/20'
                      }`}
                      data-testid="grant-admin-btn"
                    >
                      <Shield className="w-3.5 h-3.5" />
                      <span>Gán ADMIN</span>
                    </button>
                  )}

                  {projection.accessState === 'ACTIVE' ? (
                    <button
                      type="button"
                      onClick={() => openActionDialog('LOCK')}
                      className={`h-9 px-3 border font-bold rounded-lg transition-all flex items-center justify-center space-x-1.5 text-xs shadow-sm ${
                        dialogState?.type === 'LOCK'
                          ? 'bg-destructive/30 text-destructive border-destructive'
                          : 'bg-destructive/10 border-destructive/30 text-destructive hover:bg-destructive/20'
                      }`}
                      data-testid="lock-account-btn"
                    >
                      <Lock className="w-3.5 h-3.5" />
                      <span>Khóa tài khoản</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => openActionDialog('UNLOCK')}
                      className={`h-9 px-3 border font-bold rounded-lg transition-all flex items-center justify-center space-x-1.5 text-xs shadow-sm ${
                        dialogState?.type === 'UNLOCK'
                          ? 'bg-emerald-500/30 text-emerald-600 dark:text-emerald-400 border-emerald-500'
                          : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/20'
                      }`}
                      data-testid="unlock-account-btn"
                    >
                      <Unlock className="w-3.5 h-3.5" />
                      <span>Mở khóa</span>
                    </button>
                  )}
                </div>

                {/* Compact Action Execution Form */}
                {dialogState && (
                  <div className="p-3 bg-card border border-primary/30 rounded-lg space-y-3 animate-in fade-in duration-150">
                    <div className="flex items-center space-x-1.5 text-xs font-bold text-primary border-b border-border/60 pb-2">
                      <ShieldAlert className="w-4 h-4 shrink-0" />
                      <span>Lệnh: <span className="uppercase text-foreground font-mono">{dialogState.type}</span></span>
                    </div>

                    {(dialogState.type === 'LOCK' || dialogState.type === 'UNLOCK') && (
                      <div className="space-y-2.5">
                        <div className="space-y-1">
                          <label htmlFor="reason-select" className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">LÝ DO THỰC THI</label>
                          <select
                            id="reason-select"
                            value={reason}
                            onChange={(event) => setReason(event.target.value)}
                            className="w-full h-9 px-2.5 bg-background border border-input rounded-lg text-foreground focus:outline-none focus:ring-1 focus:ring-primary text-xs"
                            data-testid="reason-select"
                          >
                            {REASON_OPTIONS.map((option) => (
                              <option key={option.value} value={option.value}>{option.label}</option>
                            ))}
                          </select>
                        </div>

                        <div className="space-y-1">
                          <label htmlFor="note-input" className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
                            GHI CHÚ {reason === 'OTHER' ? '(BẮT BUỘC)' : '(TÙY CHỌN)'}
                          </label>
                          <textarea
                            id="note-input"
                            rows={2}
                            maxLength={280}
                            value={note}
                            onChange={(event) => setNote(event.target.value)}
                            placeholder="Ghi rõ lý do..."
                            className="w-full p-2 bg-background border border-input rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary text-xs resize-none"
                            data-testid="note-input"
                          />
                        </div>
                      </div>
                    )}

                    {dialogState.type === 'REVOKE_ROLE' && (
                      <p className="text-xs text-amber-500 bg-amber-500/10 border border-amber-500/20 p-2.5 rounded-lg">
                        Lệnh sẽ bị từ chối nếu đây là ADMIN cuối cùng hoặc tài khoản của bạn.
                      </p>
                    )}

                    <div className="flex items-center justify-end space-x-2 pt-1">
                      <button
                        type="button"
                        onClick={() => setDialogState(null)}
                        disabled={actionLoading}
                        className="h-8 px-3 text-xs font-semibold bg-secondary text-secondary-foreground rounded-lg hover:bg-accent"
                        data-testid="cancel-dialog-btn"
                      >
                        Hủy
                      </button>
                      <button
                        type="button"
                        onClick={executeAction}
                        disabled={actionLoading}
                        className="h-8 px-4 text-xs font-bold bg-primary text-primary-foreground rounded-lg hover:opacity-90 flex items-center space-x-1.5 shadow-sm"
                        data-testid="confirm-dialog-btn"
                      >
                        {actionLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Xác nhận</span>}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Modal Footer */}
              <div className="flex justify-end pt-1 border-t border-border">
                <button
                  type="button"
                  onClick={closeModal}
                  className="h-9 px-5 bg-secondary text-foreground font-bold text-xs rounded-lg hover:bg-accent transition-colors"
                >
                  Đóng
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
}
