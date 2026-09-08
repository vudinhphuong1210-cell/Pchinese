import React, { useCallback, useEffect, useState, useMemo } from 'react';
import {
  History,
  RefreshCw,
  ShieldCheck,
  LogIn,
  CheckCircle2,
  KeyRound,
  UserCheck,
  UserPlus,
  ShieldX,
  Lock,
  Unlock,
  Search,
  Clock,
  Filter,
  Activity,
} from 'lucide-react';
import { listAdminAuditEvents } from '../../api/adminAuditEvents.js';

const PAGE_SIZE = 20;

const EVENT_CONFIG = {
  LOGIN: {
    label: 'Đăng nhập thành công',
    icon: LogIn,
    badgeClass: 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30',
    category: 'auth',
  },
  EMAIL_VERIFIED: {
    label: 'Đã xác minh email',
    icon: CheckCircle2,
    badgeClass: 'bg-blue-500/15 text-blue-600 dark:text-blue-400 border-blue-500/30',
    category: 'auth',
  },
  PASSWORD_RESET: {
    label: 'Đã đặt lại mật khẩu',
    icon: KeyRound,
    badgeClass: 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30',
    category: 'security',
  },
  PROFILE_PREFERENCES_UPDATED: {
    label: 'Cập nhật tùy chọn hồ sơ',
    icon: UserCheck,
    badgeClass: 'bg-purple-500/15 text-purple-600 dark:text-purple-400 border-purple-500/30',
    category: 'profile',
  },
  ROLE_GRANTED: {
    label: 'Đã cấp vai trò ADMIN',
    icon: UserPlus,
    badgeClass: 'bg-primary/15 text-primary border-primary/30',
    category: 'admin',
  },
  ROLE_REVOKED: {
    label: 'Đã thu hồi vai trò ADMIN',
    icon: ShieldX,
    badgeClass: 'bg-destructive/15 text-destructive border-destructive/30',
    category: 'admin',
  },
  ACCOUNT_LOCKED: {
    label: 'Tài khoản đã bị khóa',
    icon: Lock,
    badgeClass: 'bg-rose-500/15 text-rose-600 dark:text-rose-400 border-rose-500/30',
    category: 'security',
  },
  ACCOUNT_UNLOCKED: {
    label: 'Tài khoản đã được mở khóa',
    icon: Unlock,
    badgeClass: 'bg-teal-500/15 text-teal-600 dark:text-teal-400 border-teal-500/30',
    category: 'security',
  },
  SECURITY_EVENT: {
    label: 'Sự kiện bảo mật đã được ghi nhận',
    icon: ShieldCheck,
    badgeClass: 'bg-secondary text-foreground border-border',
    category: 'other',
  },
};

function getEventMeta(eventType) {
  return EVENT_CONFIG[eventType] || EVENT_CONFIG.SECURITY_EVENT;
}

function formatOccurredAt(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Thời điểm không khả dụng';
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function SystemActivityTab() {
  const [activity, setActivity] = useState({ items: [], page: 0, size: PAGE_SIZE, totalItems: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');

  const loadPage = useCallback(async (page) => {
    setLoading(true);
    setError(null);
    try {
      const response = await listAdminAuditEvents({ page, size: PAGE_SIZE });
      if (!response?.success || !response?.data || !Array.isArray(response.data.items)) {
        throw new Error('Không thể tải nhật ký hoạt động hệ thống.');
      }
      setActivity(response.data);
    } catch (requestError) {
      setError(requestError?.error?.message || requestError?.message || 'Không thể tải nhật ký hoạt động hệ thống.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  // Client-side search and category filtering
  const filteredItems = useMemo(() => {
    if (!activity.items) return [];
    return activity.items.filter((item) => {
      const meta = getEventMeta(item.eventType);
      const matchesCategory = activeCategory === 'all' || meta.category === activeCategory;
      const query = searchQuery.trim().toLowerCase();
      const matchesQuery =
        !query ||
        meta.label.toLowerCase().includes(query) ||
        (item.actorAccountName && item.actorAccountName.toLowerCase().includes(query)) ||
        (item.targetAccountName && item.targetAccountName.toLowerCase().includes(query));
      return matchesCategory && matchesQuery;
    });
  }, [activity.items, activeCategory, searchQuery]);

  // Stat summary counters based on current loaded items
  const stats = useMemo(() => {
    const total = activity.totalItems || activity.items.length;
    let logins = 0;
    let security = 0;
    let profile = 0;
    activity.items.forEach((item) => {
      const meta = getEventMeta(item.eventType);
      if (meta.category === 'auth') logins++;
      if (meta.category === 'security' || meta.category === 'admin') security++;
      if (meta.category === 'profile') profile++;
    });
    return { total, logins, security, profile };
  }, [activity]);

  const hasPrevious = activity.page > 0;
  const hasNext = activity.page + 1 < activity.totalPages;

  return (
    <section className="w-full max-w-6xl rounded-2xl border border-border bg-card p-4 sm:p-6 shadow-xl space-y-6" data-testid="system-activity-tab">
      {/* Header */}
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/60 pb-5">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl border border-primary/30 bg-primary/10 flex items-center justify-center text-primary shadow-inner">
            <History className="w-6 h-6" aria-hidden="true" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-extrabold text-foreground tracking-tight">Nhật ký hoạt động hệ thống</h1>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-primary/10 text-primary border border-primary/20">
                ADMIN
              </span>
            </div>
            <p className="mt-0.5 text-xs sm:text-sm text-muted-foreground">
              Theo dõi và giám sát thời gian thực toàn bộ sự kiện tài khoản & bảo mật.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => loadPage(activity.page)}
          disabled={loading}
          className="h-11 px-4 rounded-xl border border-border bg-secondary text-foreground flex items-center justify-center gap-2 hover:bg-accent focus:outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-50 font-semibold text-sm transition-all shadow-sm active:scale-[0.98]"
          aria-label="Tải lại nhật ký hoạt động"
          title="Tải lại nhật ký hoạt động"
          data-testid="system-activity-reload"
        >
          <RefreshCw className={`w-4 h-4 text-primary ${loading ? 'animate-spin' : ''}`} aria-hidden="true" />
          <span className="hidden sm:inline">Làm mới</span>
        </button>
      </header>

      {/* Stat Cards Overview */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3.5 rounded-xl border border-border/70 bg-secondary/30 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
            <Activity className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">Tổng sự kiện</span>
            <span className="text-lg font-black text-foreground">{stats.total}</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-border/70 bg-secondary/30 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
            <LogIn className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">Đăng nhập</span>
            <span className="text-lg font-black text-foreground">{stats.logins}</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-border/70 bg-secondary/30 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">Bảo mật</span>
            <span className="text-lg font-black text-foreground">{stats.security}</span>
          </div>
        </div>

        <div className="p-3.5 rounded-xl border border-border/70 bg-secondary/30 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-purple-500/10 text-purple-500 flex items-center justify-center shrink-0">
            <UserCheck className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-muted-foreground uppercase tracking-wider block">Tùy chọn hồ sơ</span>
            <span className="text-lg font-black text-foreground">{stats.profile}</span>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Category Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0 no-scrollbar">
          {[
            { id: 'all', label: 'Tất cả' },
            { id: 'auth', label: 'Đăng nhập' },
            { id: 'security', label: 'Bảo mật' },
            { id: 'admin', label: 'Quyền Admin' },
            { id: 'profile', label: 'Hồ sơ' },
          ].map((cat) => (
            <button
              key={cat.id}
              type="button"
              onClick={() => setActiveCategory(cat.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap border ${
                activeCategory === cat.id
                  ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                  : 'bg-secondary/50 text-muted-foreground border-border hover:bg-secondary hover:text-foreground'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="relative min-w-[240px]">
          <Search className="w-4 h-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo người thực hiện..."
            className="w-full h-9 pl-9 pr-3 text-xs bg-secondary/50 border border-border rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
          />
        </div>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="py-12 flex flex-col items-center justify-center gap-3 text-sm text-muted-foreground" role="status" data-testid="system-activity-loading">
          <RefreshCw className="w-7 h-7 animate-spin text-primary" aria-hidden="true" />
          <span className="font-semibold">Đang tải nhật ký hoạt động hệ thống...</span>
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="p-5 rounded-2xl border border-destructive/30 bg-destructive/10 text-sm text-destructive flex flex-col items-center gap-3 text-center" role="alert" data-testid="system-activity-error">
          <p className="font-medium">{error}</p>
          <button
            type="button"
            onClick={() => loadPage(activity.page)}
            className="h-10 px-5 rounded-xl bg-primary text-primary-foreground font-bold hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-primary/50"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && filteredItems.length === 0 && (
        <div className="py-12 text-center text-sm text-muted-foreground border border-dashed border-border rounded-2xl" data-testid="system-activity-empty">
          <Filter className="w-8 h-8 text-muted-foreground/40 mx-auto mb-2" />
          <p className="font-semibold">Chưa có hoạt động nào để hiển thị.</p>
          {searchQuery && <p className="text-xs text-muted-foreground mt-1">Thử xóa bộ lọc tìm kiếm để xem tất cả nhật ký.</p>}
        </div>
      )}

      {/* Data Table with Explicit Columns */}
      {!loading && !error && filteredItems.length > 0 && (
        <div className="overflow-x-auto border border-border/80 rounded-2xl bg-card shadow-sm" data-testid="system-activity-list">
          <table className="w-full text-left border-collapse">
            <thead className="bg-secondary/70 border-b border-border/80 text-muted-foreground font-bold uppercase tracking-wider text-xs">
              <tr>
                <th scope="col" className="py-3.5 px-4 border-r border-border/40">Trạng thái</th>
                <th scope="col" className="py-3.5 px-4 border-r border-border/40">Người thực hiện</th>
                <th scope="col" className="py-3.5 px-4 border-r border-border/40">Tài khoản liên quan</th>
                <th scope="col" className="py-3.5 px-4 text-right">Thời gian</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/60 text-xs">
              {filteredItems.map((item, index) => {
                const meta = getEventMeta(item.eventType);
                const Icon = meta.icon;
                return (
                  <tr key={`${item.eventType}-${item.occurredAt}-${index}`} className="hover:bg-secondary/40 transition-colors">
                    <td className="py-3.5 px-4 border-r border-border/40 font-semibold whitespace-nowrap">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border ${meta.badgeClass}`}>
                        <Icon className="w-3.5 h-3.5 shrink-0" />
                        <span>{meta.label}</span>
                      </span>
                    </td>
                    <td className="py-3.5 px-4 border-r border-border/40 font-medium text-foreground whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-primary/20 text-primary flex items-center justify-center text-[10px] font-bold shrink-0">
                          {item.actorAccountName?.[0]?.toUpperCase() || 'U'}
                        </div>
                        <span>{item.actorAccountName}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 border-r border-border/40 font-medium text-muted-foreground whitespace-nowrap">
                      <span>{item.targetAccountName}</span>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-[11px] text-muted-foreground whitespace-nowrap">
                      <div className="inline-flex items-center justify-end gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-muted-foreground shrink-0" />
                        <time dateTime={item.occurredAt}>{formatOccurredAt(item.occurredAt)}</time>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Controls */}
      {!loading && !error && activity.totalPages > 1 && (
        <nav className="flex items-center justify-between gap-3 border-t border-border/60 pt-4" aria-label="Phân trang nhật ký hoạt động">
          <button
            type="button"
            onClick={() => loadPage(activity.page - 1)}
            disabled={!hasPrevious}
            className="h-10 px-4 rounded-xl border border-border bg-secondary text-xs font-bold text-foreground hover:bg-accent focus:outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-50 transition-all active:scale-[0.98]"
            data-testid="system-activity-previous-page"
          >
            Trang trước
          </button>
          <span className="text-xs font-medium text-muted-foreground" aria-live="polite">
            Trang <strong className="text-foreground">{activity.page + 1}</strong> / {activity.totalPages}
          </span>
          <button
            type="button"
            onClick={() => loadPage(activity.page + 1)}
            disabled={!hasNext}
            className="h-10 px-4 rounded-xl border border-border bg-secondary text-xs font-bold text-foreground hover:bg-accent focus:outline-none focus:ring-2 focus:ring-primary/50 disabled:opacity-50 transition-all active:scale-[0.98]"
            data-testid="system-activity-next-page"
          >
            Trang sau
          </button>
        </nav>
      )}
    </section>
  );
}
