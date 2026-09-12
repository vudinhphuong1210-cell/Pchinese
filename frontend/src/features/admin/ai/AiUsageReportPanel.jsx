import React, { useCallback, useEffect, useState } from 'react';
import { BarChart3, RefreshCw, ShieldCheck } from 'lucide-react';
import { adminAiApi } from '../../../api/adminAi.js';
import { PanelState } from './PlanPoliciesPanel.jsx';

function toInput(instant) { return new Date(instant).toISOString().slice(0, 16); }
function defaultFilters() {
  const now = new Date();
  return { fromInclusive: toInput(now.getTime() - 7 * 24 * 60 * 60 * 1000), toExclusive: toInput(now), planCode: '', capability: '' };
}
function message(error) { return error?.error?.message || error?.message || 'Không thể tải báo cáo AI.'; }

export function AiUsageReportPanel() {
  const [filters, setFilters] = useState(defaultFilters);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async (nextFilters) => {
    setLoading(true); setError('');
    try {
      const start = new Date(nextFilters.fromInclusive);
      const end = new Date(nextFilters.toExclusive);
      if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start || end - start > 50 * 86400000) {
        throw new Error('Khoảng thời gian UTC phải hợp lệ và không quá 50 ngày.');
      }
      const response = await adminAiApi.getUsageReport({
        ...nextFilters,
        fromInclusive: start.toISOString(),
        toExclusive: end.toISOString(),
      });
      setReport(response.data);
    } catch (requestError) { setError(message(requestError)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { void load(defaultFilters()); }, [load]);
  const buckets = report?.buckets || [];
  const hasData = buckets.length > 0;
  const totals = buckets.reduce((value, bucket) => ({ requests: value.requests + bucket.requestCount, used: value.used + bucket.usedUnits, refunded: value.refunded + bucket.refundedUnits }), { requests: 0, used: 0, refunded: 0 });
  const update = (event) => setFilters((current) => ({ ...current, [event.target.name]: event.target.value }));

  return (
    <section className="space-y-5" data-testid="ai-usage-report-panel">
      <header className="border-b border-border pb-4"><h2 className="flex items-center gap-2 text-xl font-bold"><BarChart3 className="h-5 w-5 text-primary" />Báo cáo AI tổng hợp</h2><p className="mt-1 text-sm text-muted-foreground">Không hiển thị learner, nội dung học hay payload của nhà cung cấp.</p></header>
      <form className="grid gap-3 rounded-xl border border-border bg-card p-4 md:grid-cols-5" onSubmit={(event) => { event.preventDefault(); void load(filters); }}>
        <label className="text-sm font-medium">Từ (UTC)<input aria-label="Từ UTC" className="input mt-1 block w-full" type="datetime-local" name="fromInclusive" value={filters.fromInclusive} onChange={update} /></label>
        <label className="text-sm font-medium">Đến (UTC)<input aria-label="Đến UTC" className="input mt-1 block w-full" type="datetime-local" name="toExclusive" value={filters.toExclusive} onChange={update} /></label>
        <label className="text-sm font-medium">Gói<select className="input mt-1 block w-full" name="planCode" value={filters.planCode} onChange={update}><option value="">Tất cả</option><option value="FREE">Free</option><option value="PREMIUM">Premium</option></select></label>
        <label className="text-sm font-medium">Khả năng<select className="input mt-1 block w-full" name="capability" value={filters.capability} onChange={update}><option value="">Tất cả</option><option value="AI_BUDDY">AI Buddy</option><option value="SHADOWING_ASSESSMENT">Shadowing</option></select></label>
        <button type="submit" className="mt-auto min-h-11 rounded-lg bg-primary px-4 text-sm font-bold text-primary-foreground focus:outline-none focus:ring-2 focus:ring-ring"><RefreshCw className="mr-2 inline h-4 w-4" />Lọc báo cáo</button>
      </form>
      {loading && <PanelState type="loading" />}
      {!loading && error && <PanelState type="error" message={error} onRetry={() => load(filters)} />}
      {!loading && !error && report?.partialData && <div role="status" className="rounded-lg border border-warning/50 bg-warning/10 p-3 text-sm text-foreground">Dữ liệu đang một phần hoặc provider chưa gửi đủ metering. Giá trị không có không được hiểu là 0.</div>}
      {!loading && !error && !hasData && <div className="rounded-xl border border-dashed border-border p-10 text-center text-sm text-muted-foreground"><ShieldCheck className="mx-auto mb-2 h-8 w-8" />Chưa có hoạt động AI đã đo trong phạm vi đã chọn.</div>}
      {!loading && !error && hasData && <>
        <div className="grid gap-3 sm:grid-cols-3"><Metric label="Yêu cầu" value={totals.requests} /><Metric label="Lượt đã dùng" value={totals.used} /><Metric label="Lượt hoàn" value={totals.refunded} /></div>
        <div className="overflow-x-auto rounded-xl border border-border"><table className="w-full text-left text-sm"><thead className="bg-secondary text-muted-foreground"><tr><th className="p-3">Scope</th><th className="p-3">Kết quả</th><th className="p-3">Allowance</th><th className="p-3">Tokens / chi phí</th><th className="p-3">Độ trễ</th></tr></thead><tbody>{buckets.map((bucket) => <tr key={`${bucket.policyVersionId}-${bucket.capability}-${bucket.outcome}`} className="border-t border-border align-top"><td className="p-3"><strong>{bucket.planCode}</strong><br /><span className="text-xs text-muted-foreground">{bucket.capability}</span></td><td className="p-3">{bucket.outcome}<br /><span className="text-xs text-muted-foreground">{bucket.requestCount} yêu cầu</span></td><td className="p-3">Giữ {bucket.reservedUnits}<br />Dùng {bucket.usedUnits} · Hoàn {bucket.refundedUnits}</td><td className="p-3"><MetricValue label="Input" value={bucket.providerMetering.inputTokens} /><MetricValue label="Output" value={bucket.providerMetering.outputTokens} /><MetricValue label="Total" value={bucket.providerMetering.totalTokens} />{bucket.providerMetering.costByCurrency.map((cost) => <div key={cost.currency}>{cost.amount} {cost.currency}</div>)}<span className="block text-xs text-muted-foreground">{bucket.providerMetering.reportedSampleCount} có metering · {bucket.providerMetering.unavailableSampleCount} không có</span></td><td className="p-3">{bucket.responseTime.averageMilliseconds == null ? 'Không khả dụng' : `${bucket.responseTime.averageMilliseconds} ms`}<br /><span className="text-xs text-muted-foreground">{bucket.responseTime.sampleCount} mẫu</span></td></tr>)}</tbody></table></div>
      </>}
    </section>
  );
}
function Metric({ label, value }) { return <div className="rounded-xl border border-border bg-card p-4"><span className="text-xs font-bold uppercase text-muted-foreground">{label}</span><strong className="mt-1 block text-2xl">{value}</strong></div>; }
function MetricValue({ label, value }) { return <div>{label}: {value == null ? 'Không khả dụng' : value}</div>; }
