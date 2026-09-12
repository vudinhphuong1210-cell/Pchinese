import React, { useState } from 'react';
import { Palette, Moon, Check } from 'lucide-react';

const THEMES = [
  { id: 'light', label: 'Sáng' },
  { id: 'dark', label: 'Tối' },
  { id: 'light-purple', label: 'Tím Sáng' },
  { id: 'dark-purple', label: 'Tím Tối' },
  { id: 'light-pink', label: 'Hồng Sáng' },
  { id: 'dark-pink', label: 'Hồng Tối (Mặc định)' },
  { id: 'light-blue', label: 'Xanh Sáng' },
  { id: 'dark-blue', label: 'Xanh Tối' },
  { id: 'light-red', label: 'Đỏ Sáng' },
  { id: 'dark-red', label: 'Đỏ Tối' },
];

export function ThemePicker({ currentTheme, onSelectTheme, variant = 'full' }) {
  const [open, setOpen] = useState(false);

  const handleSelect = (themeId) => {
    onSelectTheme(themeId);
    setOpen(false);
  };

  return (
    <div className="relative inline-block text-left">
      {variant === 'icon' ? (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setOpen(!open);
          }}
          className="w-8 h-8 rounded-full hover:bg-secondary flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors focus:outline-none"
          title="Chọn giao diện Theme"
          aria-label="Chọn giao diện theme"
          data-testid="theme-picker-trigger"
        >
          <Moon className="w-4.5 h-4.5" />
        </button>
      ) : (
        <button
          type="button"
          onClick={() => setOpen(!open)}
          className="w-full h-10 px-3 bg-secondary/60 hover:bg-secondary text-secondary-foreground border border-border rounded-md text-xs font-semibold flex items-center justify-between gap-2 transition-colors focus:ring-2 focus:ring-ring focus:outline-none"
          aria-label="Chọn giao diện theme"
          data-testid="theme-picker-trigger"
        >
          <div className="flex items-center space-x-2 shrink-0">
            <Palette className="w-4 h-4 text-primary" />
            <span>Giao diện</span>
          </div>
          <span className="text-[11px] text-muted-foreground capitalize truncate ml-2">
            {THEMES.find((t) => t.id === currentTheme)?.label || currentTheme}
          </span>
        </button>
      )}

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={(e) => { e.stopPropagation(); setOpen(false); }} />
          <div
            className={`absolute bottom-11 z-50 p-2 bg-popover border border-border rounded-xl shadow-2xl animate-in fade-in zoom-in-95 space-y-1 ${
              variant === 'icon' ? '-left-[100px] w-52' : 'right-0 w-52'
            }`}
            data-testid="theme-picker-menu"
          >
            <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
              Chọn Theme Màu
            </div>
            {THEMES.map((theme) => (
              <button
                key={theme.id}
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSelect(theme.id);
                }}
                className={`w-full px-2.5 py-1.5 rounded-md text-xs font-medium flex items-center justify-between transition-colors ${
                  currentTheme === theme.id
                    ? 'bg-primary/20 text-primary font-bold'
                    : 'text-foreground hover:bg-accent'
                }`}
                data-testid={`theme-option-${theme.id}`}
              >
                <div className="flex items-center space-x-2">
                  <span className="flex w-4 h-4 items-center justify-center rounded-sm bg-secondary text-primary shrink-0" aria-hidden="true">
                    <Palette className="w-3 h-3" />
                  </span>
                  <span>{theme.label}</span>
                </div>
                {currentTheme === theme.id && <Check className="w-3.5 h-3.5 text-primary" />}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
