import React, { useState } from 'react';
import { Activity, BarChart3, SlidersHorizontal } from 'lucide-react';
import { PlanPoliciesPanel } from './PlanPoliciesPanel.jsx';
import { AiUsageReportPanel } from './AiUsageReportPanel.jsx';
import { MonitoringPanel } from './MonitoringPanel.jsx';

const sections = [
  { id: 'policies', label: 'Chính sách', icon: SlidersHorizontal },
  { id: 'reports', label: 'Báo cáo', icon: BarChart3 },
  { id: 'monitoring', label: 'Monitoring', icon: Activity },
];

export function AiAdministrationWorkspace() {
  const [section, setSection] = useState('policies');
  return <section className="mx-auto w-full max-w-7xl rounded-2xl border border-border bg-background p-4 shadow-xl sm:p-6" data-testid="ai-administration-workspace">
    <header className="border-b border-border pb-5"><h1 className="text-2xl font-bold text-foreground">AI Operations</h1><p className="mt-1 text-sm text-muted-foreground">Quản trị catalogue tương lai, số liệu tổng hợp và cảnh báo nội bộ.</p></header>
    <nav className="mt-4 flex flex-wrap gap-2" aria-label="Các phần AI Operations">{sections.map((item) => { const Icon = item.icon; return <button key={item.id} type="button" onClick={() => setSection(item.id)} aria-current={section === item.id ? 'page' : undefined} className={`min-h-11 rounded-lg px-4 text-sm font-bold focus:outline-none focus:ring-2 focus:ring-ring ${section === item.id ? 'bg-primary text-primary-foreground' : 'border border-border bg-secondary text-foreground hover:bg-accent'}`}><Icon className="mr-2 inline h-4 w-4" />{item.label}</button>; })}</nav>
    <div className="mt-6">{section === 'policies' && <PlanPoliciesPanel />}{section === 'reports' && <AiUsageReportPanel />}{section === 'monitoring' && <MonitoringPanel />}</div>
  </section>;
}
