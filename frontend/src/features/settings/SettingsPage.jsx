import React from 'react';
import { Settings, UserCheck } from 'lucide-react';
import { ProfilePreferencesForm } from './ProfilePreferencesForm.jsx';

export function SettingsPage({ onOpenUpgradeModal }) {
  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12 font-sans" data-testid="settings-page">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/80 pb-6">
        <div className="space-y-1">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
              <Settings className="w-5 h-5" />
            </div>
            <h1 className="text-2xl font-extrabold text-foreground tracking-tight">
              Cài đặt & Tùy chọn học tập
            </h1>
          </div>
          <p className="text-sm text-muted-foreground pl-13">
            Quản lý tùy chọn cá nhân, ngôn ngữ hiển thị và mục tiêu học tập HSK của bạn.
          </p>
        </div>

        <div className="flex items-center space-x-2 px-3 py-1.5 bg-secondary border border-border rounded-xl text-xs text-muted-foreground font-medium self-start sm:self-auto">
          <UserCheck className="w-4 h-4 text-emerald-500" />
          <span>Tài khoản chính chủ</span>
        </div>
      </div>

      {/* Main Settings Content */}
      <main>
        <ProfilePreferencesForm onOpenUpgradeModal={onOpenUpgradeModal} />
      </main>
    </div>
  );
}
