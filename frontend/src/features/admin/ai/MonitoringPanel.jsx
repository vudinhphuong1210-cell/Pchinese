import React, { useEffect, useState } from 'react';
import { BellRing, Check, Plus, RefreshCw } from 'lucide-react';
import { adminAiApi } from '../../../api/adminAi.js';
import { PanelState } from './PlanPoliciesPanel.jsx';

const blankRule = { metric: 'REQUEST_VOLUME', threshold: '1', evaluationWindow: 'ONE_HOUR', planCode: '', capability: '', enabled: true };
function message(error) { return error?.error?.message || error?.message || 'Không thể tải monitoring AI.'; }

export function MonitoringPanel() {
  const [rules, setRules] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [audits, setAudits] = useState([]);
  const [rule, setRule] = useState(blankRule);
  const [editingRule, setEditingRule] = useState(null);
  const [noteByAlert, setNoteByAlert] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = async () => {
    setLoading(true); setError('');
    try {
      const [rulesResponse, alertsResponse, auditResponse] = await Promise.all([
        adminAiApi.listMonitoringRules(), adminAiApi.listAlerts(), adminAiApi.listAuditEvents({ page: 0, size: 10 }),
      ]);
      setRules(rulesResponse.data || []); setAlerts(alertsResponse.data || []); setAudits(auditResponse.data || []);
    } catch (requestError) { setError(message(requestError)); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  const update = (event) => setRule((current) => ({ ...current, [event.target.name]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const resetRuleForm = () => { setRule(blankRule); setEditingRule(null); };
  const editRule = (item) => {
    setRule({ metric: item.metric, threshold: String(item.threshold), evaluationWindow: item.evaluationWindow,
      planCode: item.planCode || '', capability: item.capability || '', enabled: item.enabled });
    setEditingRule(item);
    setError(''); setNotice('');
  };
  const saveRule = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setNotice('');
    try {
      const payload = { ...rule, threshold: Number(rule.threshold), planCode: rule.planCode || null, capability: rule.capability || null };
      if (editingRule) {
        await adminAiApi.updateMonitoringRule(editingRule.id, { ...payload, expectedVersion: editingRule.version });
        setNotice('Đã cập nhật rule monitoring.');
      } else {
        await adminAiApi.createMonitoringRule(payload);
        setNotice('Đã lưu rule monitoring. Cảnh báo chỉ hiển thị trong dashboard này.');
      }
      resetRuleForm(); await load();
    } catch (requestError) { setError(message(requestError)); }
    finally { setSaving(false); }
  };
  const acknowledge = async (alert) => {
    setError('');
    try {
      await adminAiApi.acknowledgeAlert(alert.id, { expectedVersion: alert.version, note: noteByAlert[alert.id] || undefined });
      setNotice('Đã ghi nhận acknowledgement; rule vẫn tiếp tục đo lường.'); await load();
    } catch (requestError) { setError(message(requestError)); if (requestError?.error?.code === 'STATE_CONFLICT') await load(); }
  };

  if (loading) return <PanelState type="loading" />;
  if (error && rules.length === 0 && alerts.length === 0) return <PanelState type="error" message={error} onRetry={load} />;
  return <section className="space-y-5" data-testid="ai-monitoring-panel">
    <header className="flex flex-col gap-3 border-b border-border pb-4 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="flex items-center gap-2 text-xl font-bold"><BellRing className="h-5 w-5 text-primary" />Monitoring AI</h2><p className="mt-1 text-sm text-muted-foreground">Rule tổng hợp 1 giờ hoặc 24 giờ; không có email, chat hay pager.</p></div><button type="button" onClick={load} className="min-h-11 rounded-lg border border-border px-4 text-sm font-bold"><RefreshCw className="mr-2 inline h-4 w-4" />Tải lại</button></header>
    {error && <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}{notice && <div role="status" className="rounded-lg border border-success/40 bg-success/10 p-3 text-sm">{notice}</div>}
    <div className="grid gap-5 xl:grid-cols-[minmax(0,.9fr)_minmax(0,1.1fr)]">
      <form onSubmit={saveRule} className="space-y-3 rounded-xl border border-border bg-card p-4"><h3 className="font-bold">{editingRule ? 'Cập nhật rule tổng hợp' : 'Tạo rule tổng hợp'}</h3><label className="block text-sm">Metric<select className="input mt-1 block w-full" name="metric" value={rule.metric} onChange={update}>{['TOKEN_VOLUME','ESTIMATED_COST','REQUEST_VOLUME','QUOTA_DENIAL_RATE','FAILURE_RATE','RESPONSE_TIME'].map((item) => <option key={item}>{item}</option>)}</select></label><label className="block text-sm">Ngưỡng<input className="input mt-1 block w-full" name="threshold" type="number" min="0" step="0.01" value={rule.threshold} onChange={update} /></label><div className="grid gap-3 sm:grid-cols-2"><label className="text-sm">Cửa sổ<select className="input mt-1 block w-full" name="evaluationWindow" value={rule.evaluationWindow} onChange={update}><option value="ONE_HOUR">1 giờ</option><option value="TWENTY_FOUR_HOURS">24 giờ</option></select></label><label className="text-sm">Gói<select className="input mt-1 block w-full" name="planCode" value={rule.planCode} onChange={update}><option value="">Tất cả</option><option value="FREE">Free</option><option value="PREMIUM">Premium</option></select></label></div><label className="block text-sm">Khả năng<select className="input mt-1 block w-full" name="capability" value={rule.capability} onChange={update}><option value="">Tất cả</option><option value="AI_BUDDY">AI Buddy</option><option value="SHADOWING_ASSESSMENT">Shadowing</option></select></label><label className="flex min-h-11 items-center gap-2 text-sm"><input name="enabled" type="checkbox" checked={rule.enabled} onChange={update} />Bật rule</label><div className="flex gap-2"><button disabled={saving} className="min-h-11 rounded-lg bg-primary px-4 text-sm font-bold text-primary-foreground disabled:opacity-60"><Plus className="mr-2 inline h-4 w-4" />{saving ? 'Đang lưu…' : editingRule ? 'Cập nhật rule' : 'Lưu rule'}</button>{editingRule && <button type="button" onClick={resetRuleForm} className="min-h-11 rounded-lg border border-border px-4 text-sm font-bold">Hủy</button>}</div></form>
      <div className="rounded-xl border border-border bg-card p-4"><h3 className="font-bold">Rule hiện tại</h3>{rules.length === 0 ? <p className="mt-3 text-sm text-muted-foreground">Chưa có rule nào.</p> : <ul className="mt-3 space-y-2">{rules.map((item) => <li key={item.id} className="rounded-lg border border-border p-3 text-sm"><strong>{item.metric}</strong> &gt; {item.threshold} · {item.evaluationWindow}<span className="ml-2 text-muted-foreground">{item.enabled ? 'Bật' : 'Tắt'}{item.planCode ? ` · ${item.planCode}` : ''}{item.capability ? ` · ${item.capability}` : ''}</span><button type="button" onClick={() => editRule(item)} className="ml-3 min-h-11 rounded-lg border border-border px-3 text-sm font-bold">Sửa</button></li>)}</ul>}</div>
    </div>
    <div className="rounded-xl border border-border bg-card p-4"><h3 className="font-bold">Cảnh báo trong dashboard</h3>{alerts.length === 0 ? <p className="mt-3 text-sm text-muted-foreground">Không có cảnh báo tổng hợp.</p> : <ul className="mt-3 space-y-3">{alerts.map((alert) => <li key={alert.id} className="rounded-lg border border-border p-3"><div className="flex flex-wrap items-center justify-between gap-2"><div><strong>{alert.metric}</strong> · {alert.latestValue} / ngưỡng {alert.threshold}<p className="text-xs text-muted-foreground">{alert.state} · {alert.partialData ? 'Dữ liệu một phần' : 'Dữ liệu hoàn chỉnh'}</p></div>{alert.state !== 'RESOLVED' && <button type="button" onClick={() => acknowledge(alert)} className="min-h-11 rounded-lg border border-border px-3 text-sm font-bold"><Check className="mr-1 inline h-4 w-4" />Ghi nhận</button>}</div>{alert.state !== 'RESOLVED' && <label className="mt-2 block text-xs text-muted-foreground">Ghi chú vận hành (tùy chọn)<input className="input mt-1 block w-full" maxLength="500" value={noteByAlert[alert.id] || ''} onChange={(event) => setNoteByAlert((current) => ({ ...current, [alert.id]: event.target.value }))} /></label>}</li>)}</ul>}</div>
    <div className="rounded-xl border border-border bg-card p-4"><h3 className="font-bold">Audit an toàn</h3>{audits.length === 0 ? <p className="mt-3 text-sm text-muted-foreground">Chưa có sự kiện F12.</p> : <ul className="mt-3 space-y-2 text-sm">{audits.map((event) => <li key={`${event.targetId}-${event.occurredAt}`} className="border-b border-border pb-2"><strong>{event.eventType}</strong> · {event.targetType}<span className="ml-2 text-muted-foreground">{new Date(event.occurredAt).toLocaleString('vi-VN')}</span>{event.reasonOrNote && <p className="text-muted-foreground">{event.reasonOrNote}</p>}</li>)}</ul>}</div>
  </section>;
}
