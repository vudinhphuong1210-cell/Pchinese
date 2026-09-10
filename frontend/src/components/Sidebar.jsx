import React, { useState } from 'react';
import { ThemePicker } from './ThemePicker.jsx';
import {
  Home,
  History,
  Mic,
  Headphones,
  Volume2,
  BookOpen,
  Shield,
  Crown,
  ChevronDown,
  Languages,
  LogOut,
  PanelLeft,
} from 'lucide-react';

export function Sidebar({
  activeTab,
  onSelectTab,
  currentTheme,
  onSelectTheme,
  authState,
  onLogout,
  onAddAccount,
  isCollapsed: propIsCollapsed,
  onToggleCollapse,
  onOpenUpgradeModal,
}) {
  const [internalIsCollapsed, setInternalIsCollapsed] = useState(() => {
    return localStorage.getItem('pchinese_sidebar_collapsed') === 'true';
  });

  const isCollapsed = propIsCollapsed !== undefined ? propIsCollapsed : internalIsCollapsed;

  const handleToggleCollapse = () => {
    const nextState = !isCollapsed;
    if (propIsCollapsed === undefined) {
      setInternalIsCollapsed(nextState);
      localStorage.setItem('pchinese_sidebar_collapsed', String(nextState));
    }
    if (onToggleCollapse) {
      onToggleCollapse(nextState);
    }
  };

  const handleUpgradeClick = () => {
    if (onOpenUpgradeModal) {
      onOpenUpgradeModal();
    } else {
      onSelectTab('entitlement');
    }
  };

  return (
    <aside
      className={`w-full ${
        isCollapsed ? 'lg:w-20' : 'lg:w-64'
      } h-auto lg:h-screen lg:sticky top-0 bg-background border-r border-border ${
        isCollapsed ? 'p-2 sm:p-3' : 'p-4'
      } flex flex-col justify-between shrink-0 z-30 font-sans transition-all duration-300`}
      data-testid="app-sidebar"
    >
      <div className="space-y-5">
        {/* Brand Row */}
        <div
          className={`h-14 flex items-center ${
            isCollapsed ? 'justify-center' : 'justify-between'
          } border-b border-border/60 pb-3`}
        >
          <div
            className="flex items-center space-x-3 cursor-pointer"
            onClick={() => onSelectTab('dashboard')}
            title="PCHINESE Shadowing Hub"
          >
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-primary-foreground font-black text-xl shadow-lg shadow-primary/20 shrink-0">
              P
            </div>
            {!isCollapsed && (
              <div>
                <span className="text-xl font-bold tracking-wider text-foreground font-sans">PCHINESE</span>
                <span className="block text-[10px] tracking-widest uppercase text-muted-foreground font-semibold">
                  Shadowing Hub
                </span>
              </div>
            )}
          </div>

          {!isCollapsed && (
            <button
              type="button"
              onClick={handleToggleCollapse}
              className="w-8 h-8 rounded-lg text-muted-foreground hover:text-foreground hover:bg-accent flex items-center justify-center transition-colors"
              title="Thu gọn thanh bên"
              data-testid="sidebar-toggle-btn"
            >
              <PanelLeft className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Group 1: TỔNG QUAN */}
        <div className="space-y-1.5">
          {!isCollapsed ? (
            <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              <span>TỔNG QUAN</span>
              <div className="flex-1 border-b border-dashed border-border/60" />
            </div>
          ) : (
            <div className="w-8 border-b border-dashed border-border/60 mx-auto my-1" />
          )}

          <nav className="space-y-1">
            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              title="Trang chủ"
              className={`${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                  : 'w-full h-11 px-3.5 rounded-xl space-x-3'
              } text-sm font-semibold flex items-center transition-all ${
                activeTab === 'dashboard'
                  ? 'bg-secondary text-foreground shadow-sm border border-border/80'
                  : 'text-muted-foreground hover:bg-accent hover:text-foreground'
              }`}
              data-testid="nav-dashboard"
            >
              <Home className={`w-5 h-5 shrink-0 ${activeTab === 'dashboard' ? 'text-primary' : ''}`} />
              {!isCollapsed && <span>Trang chủ</span>}
            </button>
          </nav>
        </div>

        {/* Group 2: LUYỆN TẬP */}
        <div className="space-y-1.5 pt-1">
          {!isCollapsed ? (
            <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              <span>LUYỆN TẬP</span>
              <div className="flex-1 border-b border-dashed border-border/60" />
            </div>
          ) : (
            <div className="w-8 border-b border-dashed border-border/60 mx-auto my-1" />
          )}

          <nav className="space-y-1">
            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              title="Dictation"
              className={`${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                  : 'w-full h-11 px-3.5 rounded-xl space-x-3'
              } text-sm font-semibold flex items-center text-muted-foreground hover:bg-accent hover:text-foreground transition-all`}
            >
              <Headphones className="w-5 h-5 text-primary shrink-0" />
              {!isCollapsed && <span>Dictation</span>}
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              title="Shadowing"
              className={`${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                  : 'w-full h-11 px-3.5 rounded-md space-x-3'
              } text-sm font-semibold flex items-center text-muted-foreground hover:bg-accent hover:text-foreground transition-all`}
            >
              <Mic className="w-5 h-5 shrink-0" />
              {!isCollapsed && <span>Shadowing</span>}
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              title="Luyện nói"
              className={`${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                  : 'w-full h-11 px-3.5 rounded-md space-x-3'
              } text-sm font-semibold flex items-center text-muted-foreground hover:bg-accent hover:text-foreground transition-all`}
            >
              <Volume2 className="w-5 h-5 shrink-0" />
              {!isCollapsed && <span>Luyện nói</span>}
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              title="Luyện từ vựng"
              className={`${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                  : 'w-full h-11 px-3.5 rounded-md space-x-3'
              } text-sm font-semibold flex items-center text-muted-foreground hover:bg-accent hover:text-foreground transition-all`}
            >
              <BookOpen className="w-5 h-5 shrink-0" />
              {!isCollapsed && <span>Luyện từ vựng</span>}
            </button>
          </nav>
        </div>

        {/* Group 3: QUẢN TRỊ (Only when authenticated) */}
        {authState.isAuthenticated && authState.isAdmin && (
          <div className="space-y-1.5 pt-1">
            {!isCollapsed ? (
              <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
                <span>QUẢN TRỊ</span>
                <div className="flex-1 border-b border-dashed border-border/60" />
              </div>
            ) : (
              <div className="w-8 border-b border-dashed border-border/60 mx-auto my-1" />
            )}

            <nav className="space-y-1">
              <button
                type="button"
                onClick={() => onSelectTab('admin')}
                title="Quản lý người dùng"
                className={`${
                  isCollapsed
                    ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                    : 'w-full h-11 px-3.5 rounded-md space-x-3'
                } text-sm font-semibold flex items-center transition-all ${
                  activeTab === 'admin'
                    ? 'bg-secondary text-foreground shadow-sm border border-border/80'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                }`}
                data-testid="nav-admin"
              >
                <Shield className={`w-5 h-5 shrink-0 ${activeTab === 'admin' ? 'text-primary' : ''}`} />
                {!isCollapsed && <span>Quản lý người dùng</span>}
              </button>
              <button
                type="button"
                onClick={() => onSelectTab('admin-audit')}
                title="Nhật ký hoạt động"
                className={`${
                  isCollapsed
                    ? 'w-11 h-11 mx-auto justify-center rounded-xl'
                    : 'w-full h-11 px-3.5 rounded-md space-x-3'
                } text-sm font-semibold flex items-center transition-all ${
                  activeTab === 'admin-audit'
                    ? 'bg-secondary text-foreground shadow-sm border border-border/80'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                }`}
                data-testid="nav-admin-audit"
              >
                <History className={`w-5 h-5 shrink-0 ${activeTab === 'admin-audit' ? 'text-primary' : ''}`} />
                {!isCollapsed && <span>Nhật ký hoạt động</span>}
              </button>
            </nav>
          </div>
        )}
      </div>

      {/* Footer Area */}
      <div className="space-y-3 pt-3 border-t border-border/60">
        {/* Upgrade Premium Card */}
        <button
          type="button"
          onClick={handleUpgradeClick}
          title="Nâng cấp Premium"
          className={`${
            isCollapsed
              ? 'w-11 h-11 mx-auto justify-center rounded-xl'
              : 'w-full h-12 px-4 space-x-3 rounded-xl'
          } bg-card hover:bg-secondary/40 border border-primary/40 hover:border-primary flex items-center text-primary font-bold text-sm transition-all shadow-sm active:scale-[0.98]`}
          data-testid="sidebar-upgrade-btn"
        >
          <Crown className="w-5 h-5 text-primary shrink-0" />
          {!isCollapsed && <span>Nâng cấp Premium</span>}
        </button>

        {/* Profile Card & Action Icons */}
        {!isCollapsed ? (
          <div
            onClick={() => {
              if (!authState.isAuthenticated) {
                onSelectTab('auth');
              } else {
                onSelectTab('settings');
              }
            }}
            className={`p-3 bg-card border border-border rounded-xl flex items-center justify-between gap-2 shadow-sm transition-all cursor-pointer hover:border-primary/60 hover:bg-secondary/40 ${
              (activeTab === 'auth' && !authState.isAuthenticated) || activeTab === 'settings'
                ? 'border-primary ring-2 ring-primary/30 bg-primary/5'
                : ''
            }`}
            data-testid="profile-card"
            title={authState.isAuthenticated ? 'Bấm để xem & tùy chọn hồ sơ' : 'Bấm để đăng nhập'}
          >
            {/* Avatar + User details */}
            <div className="flex items-center space-x-2.5 min-w-0 flex-1" data-testid="nav-settings">
              <div className="w-9 h-9 rounded-full bg-amber-500 text-white font-bold text-base flex items-center justify-center shrink-0 shadow-sm">
                {authState.isAuthenticated ? 'P' : 'G'}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center space-x-1">
                  <span className="text-sm font-bold text-foreground truncate max-w-[100px]">
                    {authState.isAuthenticated ? 'Phương...' : 'Khách'}
                  </span>
                  <ChevronDown className="w-3.5 h-3.5 text-muted-foreground shrink-0" />
                </div>
                <div className="flex items-center space-x-1 text-xs text-muted-foreground mt-0.5 truncate">
                  <span className="truncate">
                    {authState.isAuthenticated ? 'Hồ sơ' : 'Đăng nhập'}
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Action Icons */}
            <div className="flex items-center space-x-0.5 shrink-0">
              <ThemePicker currentTheme={currentTheme} onSelectTheme={onSelectTheme} variant="icon" />

              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  alert('Chuyển đổi ngôn ngữ Tiếng Việt / Tiếng Trung');
                }}
                className="w-7 h-7 rounded-lg hover:bg-secondary flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
                title="Đổi ngôn ngữ"
              >
                <Languages className="w-3.5 h-3.5" />
              </button>

              {authState.isAuthenticated && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onLogout();
                  }}
                  className="w-7 h-7 rounded-lg hover:bg-destructive/10 text-muted-foreground hover:text-destructive flex items-center justify-center transition-colors"
                  title="Đăng xuất"
                  data-testid="sidebar-logout-btn"
                >
                  <LogOut className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>
        ) : (
          /* Collapsed Bottom Stack (Matching Reference Image 2) */
          <div className="flex flex-col items-center space-y-2 pt-1 border-t border-border/60">
            {/* Toggle Sidebar Button */}
            <button
              type="button"
              onClick={handleToggleCollapse}
              className="w-10 h-10 rounded-xl hover:bg-accent flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
              title="Mở rộng thanh bên"
              data-testid="sidebar-toggle-btn"
            >
              <PanelLeft className="w-5 h-5" />
            </button>

            {/* Language Switcher */}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                alert('Chuyển đổi ngôn ngữ Tiếng Việt / Tiếng Trung');
              }}
              className="w-10 h-10 rounded-xl hover:bg-accent flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
              title="Đổi ngôn ngữ"
            >
              <Languages className="w-4 h-4" />
            </button>

            {/* Theme Picker */}
            <div className="w-10 h-10 rounded-xl hover:bg-accent flex items-center justify-center transition-colors">
              <ThemePicker currentTheme={currentTheme} onSelectTheme={onSelectTheme} variant="icon" />
            </div>

            {/* Profile Avatar */}
            <div
              onClick={() => {
                if (!authState.isAuthenticated) {
                  onSelectTab('auth');
                } else {
                  onSelectTab('settings');
                }
              }}
              className={`w-10 h-10 rounded-full bg-amber-500 text-white font-bold text-base flex items-center justify-center cursor-pointer shadow-sm hover:ring-2 hover:ring-primary/40 transition-all ${
                (activeTab === 'auth' && !authState.isAuthenticated) || activeTab === 'settings'
                  ? 'ring-2 ring-primary'
                  : ''
              }`}
              data-testid="profile-card"
              title={authState.isAuthenticated ? 'Hồ sơ' : 'Đăng nhập'}
            >
              {authState.isAuthenticated ? 'P' : 'G'}
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}
