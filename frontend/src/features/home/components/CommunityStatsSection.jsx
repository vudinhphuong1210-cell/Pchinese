import React from 'react';
import { Layers3, Palette, Route, Sparkles } from 'lucide-react';

const DEFAULT_CAPABILITIES = [
  { value: '6', label: 'cấp độ HSK', icon: Route },
  { value: '4', label: 'phương pháp chủ động', icon: Layers3 },
  { value: '30', label: 'lượt AI mỗi chu kỳ Free', icon: Sparkles },
  { value: '10', label: 'theme dễ đọc', icon: Palette },
];

export function CommunityStatsSection({ stats = DEFAULT_CAPABILITIES }) {
  return (
    <section className="rounded-xl border border-border bg-card px-5 py-6" aria-label="Khả năng hiện có của P Chinese" data-testid="community-stats-section">
      <p className="mb-5 text-center text-xs font-bold uppercase tracking-widest text-muted-foreground">Những gì bạn có thể dùng ngay</p>
      <dl className="grid grid-cols-2 gap-5 lg:grid-cols-4">
        {stats.map(({ value, label, icon: Icon }) => (
          <div key={label} className="text-center">
            <Icon className="mx-auto h-5 w-5 text-primary" aria-hidden="true" />
            <dt className="mt-2 text-2xl font-bold text-foreground">{value}</dt>
            <dd className="mt-1 text-xs font-medium text-muted-foreground">{label}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
