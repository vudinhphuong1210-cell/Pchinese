import React from 'react';
import { ThemePicker } from './ThemePicker.jsx';
import {
  Home,
  Mic,
  Headphones,
  Volume2,
  BookOpen,
  Shield,
  Crown,
  ChevronDown,
  UserCheck,
  Languages,
  LogOut,
} from 'lucide-react';

export function Sidebar({
  activeTab,
  onSelectTab,
  currentTheme,
  onSelectTheme,
  authState,
  onLogout,
}) {
  return (
    <aside
      className="w-full lg:w-64 h-auto lg:h-screen lg:sticky top-0 bg-background border-r border-border p-4 flex flex-col justify-between shrink-0 z-30 font-sans"
      data-testid="app-sidebar"
    >
      <div className="space-y-5">
        {/* Brand Row */}
        <div className="h-14 flex items-center justify-between border-b border-border/60 pb-3">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onSelectTab('dashboard')}>
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-primary-foreground font-black text-xl shadow-lg shadow-primary/20">
              P
            </div>
            <div>
              <span className="text-xl font-bold tracking-wider text-foreground font-sans">PCHINESE</span>
              <span className="block text-[10px] tracking-widest uppercase text-muted-foreground font-semibold">
                Shadowing Hub
              </span>
            </div>
          </div>
        </div>

        {/* Group 1: TỔNG QUAN */}
        <div className="space-y-1.5">
          <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
            <span>TỔNG QUAN</span>
            <div className="flex-1 border-b border-dashed border-border/60" />
          </div>

          <nav className="space-y-1">
            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              className={`w-full h-11 px-3.5 rounded-xl text-sm font-semibold flex items-center space-x-3 transition-all ${
                activeTab === 'dashboard'
                  ? 'bg-secondary text-foreground shadow-sm border border-border/80'
                  : 'text-muted-foreground hover:bg-accent hover:text-foreground'
              }`}
              data-testid="nav-dashboard"
            >
              <Home className={`w-5 h-5 ${activeTab === 'dashboard' ? 'text-primary' : ''}`} />
              <span>Trang chủ</span>
            </button>
          </nav>
        </div>

        {/* Group 2: LUYỆN TẬP */}
        <div className="space-y-1.5 pt-1">
          <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
            <span>LUYỆN TẬP</span>
            <div className="flex-1 border-b border-dashed border-border/60" />
          </div>

          <nav className="space-y-1">
            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              className="w-full h-11 px-3.5 rounded-xl text-sm font-semibold flex items-center space-x-3 text-muted-foreground hover:bg-accent hover:text-foreground transition-all"
            >
              <Headphones className="w-5 h-5 text-primary" />
              <span>Dictation</span>
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              className="w-full h-11 px-3.5 rounded-md text-sm font-semibold flex items-center space-x-3 text-muted-foreground hover:bg-accent hover:text-foreground transition-all"
            >
              <Mic className="w-5 h-5" />
              <span>Shadowing</span>
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              className="w-full h-11 px-3.5 rounded-md text-sm font-semibold flex items-center space-x-3 text-muted-foreground hover:bg-accent hover:text-foreground transition-all"
            >
              <Volume2 className="w-5 h-5" />
              <span>Luyện nói</span>
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('dashboard')}
              className="w-full h-11 px-3.5 rounded-md text-sm font-semibold flex items-center space-x-3 text-muted-foreground hover:bg-accent hover:text-foreground transition-all"
            >
              <BookOpen className="w-5 h-5" />
              <span>Luyện từ vựng</span>
            </button>
          </nav>
        </div>

        {/* Group 3: QUẢN TRỊ (Only when authenticated) */}
        {authState.isAuthenticated && (
          <div className="space-y-1.5 pt-1">
            <div className="flex items-center px-2 space-x-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              <span>QUẢN TRỊ</span>
              <div className="flex-1 border-b border-dashed border-border/60" />
            </div>

            <nav className="space-y-1">
              <button
                type="button"
                onClick={() => onSelectTab('admin')}
                className={`w-full h-11 px-3.5 rounded-md text-sm font-semibold flex items-center space-x-3 transition-all ${
                  activeTab === 'admin'
                    ? 'bg-secondary text-foreground shadow-sm border border-border/80'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                }`}
                data-testid="nav-admin"
              >
                <Shield className={`w-5 h-5 ${activeTab === 'admin' ? 'text-primary' : ''}`} />
                <span>Admin Account / Roles</span>
              </button>
            </nav>
          </div>
        )}
      </div>

      {/* Footer Area - Exactly matching reference design */}
      <div className="space-y-3 pt-3 border-t border-border/60">
        {/* Upgrade Premium Card */}
        <button
          type="button"
          onClick={() => alert('Chức năng Nâng cấp Premium')}
          className="w-full h-12 px-4 bg-card hover:bg-secondary/40 border border-primary/40 hover:border-primary rounded-xl flex items-center space-x-3 text-primary font-bold text-sm transition-all shadow-sm active:scale-[0.98]"
        >
          <Crown className="w-5 h-5 text-primary shrink-0" />
          <span>Nâng cấp Premium</span>
        </button>

        {/* Profile Card */}
        <div
          onClick={() => {
            if (!authState.isAuthenticated) {
              onSelectTab('auth');
            }
          }}
          className={`p-3 bg-card border border-border rounded-xl flex items-center justify-between gap-2 shadow-sm transition-all ${
            !authState.isAuthenticated
              ? 'cursor-pointer hover:border-primary/60 hover:bg-secondary/40'
              : ''
          } ${activeTab === 'auth' && !authState.isAuthenticated ? 'border-primary ring-2 ring-primary/30' : ''}`}
          data-testid="profile-card"
        >
          {/* Avatar + User details */}
          <div className="flex items-center space-x-3 min-w-0">
            <div className="w-10 h-10 rounded-full bg-amber-500 text-white font-bold text-lg flex items-center justify-center shrink-0 shadow-sm">
              {authState.isAuthenticated ? 'P' : 'G'}
            </div>
            <div className="min-w-0">
              <div className="flex items-center space-x-1">
                <span className="text-sm font-bold text-foreground truncate max-w-[90px]">
                  {authState.isAuthenticated ? 'Phương...' : 'Khách'}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-muted-foreground shrink-0" />
              </div>
              <div className="flex items-center space-x-1 text-xs text-muted-foreground mt-0.5">
                <UserCheck className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate">
                  {authState.isAuthenticated ? 'Miễn phí' : 'Đăng nhập'}
                </span>
              </div>
            </div>
          </div>

          {/* Quick Action Icons: Theme Switcher & Language */}
          <div className="flex items-center space-x-1 shrink-0">
            <ThemePicker currentTheme={currentTheme} onSelectTheme={onSelectTheme} variant="icon" />

            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                alert('Chuyển đổi ngôn ngữ Tiếng Việt / Tiếng Trung');
              }}
              className="w-8 h-8 rounded-full hover:bg-secondary flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
              title="Đổi ngôn ngữ"
            >
              <Languages className="w-4 h-4" />
            </button>

            {authState.isAuthenticated && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onLogout();
                }}
                className="w-8 h-8 rounded-full hover:bg-destructive/10 text-muted-foreground hover:text-destructive flex items-center justify-center transition-colors"
                title="Đăng xuất"
                data-testid="sidebar-logout-btn"
              >
                <LogOut className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </aside>
  );
}
