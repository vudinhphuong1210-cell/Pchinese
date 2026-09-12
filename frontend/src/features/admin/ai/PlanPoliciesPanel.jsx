import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { History, RefreshCw, Save, ShieldCheck } from 'lucide-react';
import { adminAiApi } from '../../../api/adminAi.js';

const initialForm = (policy) => ({
  expectedCurrentVersion: policy?.version ?? 0,
  reason: '',
  displayName: policy?.displayName ?? '',
  description: policy?.description ?? '',
  benefits: (policy?.benefits ?? []).join('\n'),
  availabilityState: policy?.availabilityState ?? 'ACTIVE',
  priceAmount: String(policy?.priceAmount ?? 0),
  currency: policy?.currency ?? 'USD',
  priceInterval: policy?.priceInterval ?? 'MONTH',
  displayLabel: policy?.displayLabel ?? '',
  allowanceUnits: String(policy?.allowanceUnits ?? 0),
  allowancePeriod: policy?.allowancePeriod ?? 'MONTH',
});

function errorMessage(error) {
  return error?.error?.message || error?.message || 'Không thể hoàn thành yêu cầu. Vui lòng thử lại.';
}

export function PlanPoliciesPanel() {
  const [plans, setPlans] = useState([]);
  const [selectedCode, setSelectedCode] = useState('FREE');
  const [history, setHistory] = useState([]);
  const [form, setForm] = useState(initialForm());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const reasonRef = useRef(null);
  const selected = useMemo(() => plans.find((plan) => plan.planCode === selectedCode) ?? null, [plans, selectedCode]);

  const load = useCallback(async (code) => {
    setLoading(true);
    setError('');
    try {
      const [plansResponse, historyResponse] = await Promise.all([
        adminAiApi.listPlans(),
        adminAiApi.listPolicyRevisions(code, { page: 0, size: 20 }),
      ]);
      setPlans(plansResponse.data || []);
      setHistory(historyResponse.data || []);
      const current = (plansResponse.data || []).find((plan) => plan.planCode === code);
      setForm(initialForm(current));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load('FREE'); }, [load]);

  const choosePlan = (code) => {
    setSelectedCode(code);
    setNotice('');
    void load(code);
  };

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const publish = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');
    const benefits = form.benefits.split('\n').map((value) => value.trim()).filter(Boolean);
    if (!form.reason.trim() || !form.displayName.trim() || benefits.length === 0) {
      setError('Hãy nhập lý do, tên hiển thị và ít nhất một quyền lợi.');
      reasonRef.current?.focus();
      return;
    }
    setSaving(true);
    try {
      const response = await adminAiApi.publishPolicy(selectedCode, {
        ...form,
        expectedCurrentVersion: Number(form.expectedCurrentVersion),
        benefits,
        priceAmount: Number(form.priceAmount),
        allowanceUnits: Number(form.allowanceUnits),
        currency: form.currency.toUpperCase(),
      });
      setNotice(`Đã công bố revision ${response.data.revisionNumber}. Thay đổi chỉ áp dụng theo chu kỳ do máy chủ quản lý.`);
      await load(selectedCode);
    } catch (requestError) {
      setError(errorMessage(requestError));
      if (requestError?.error?.code === 'STATE_CONFLICT') await load(selectedCode);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <PanelState type="loading" onRetry={() => load(selectedCode)} />;
  if (error && plans.length === 0) return <PanelState type="error" message={error} onRetry={() => load(selectedCode)} />;

  return (
    <section className="space-y-5" data-testid="ai-plan-policies-panel">
      <header className="flex flex-col gap-3 border-b border-border pb-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-foreground">Chính sách gói AI</h2>
          <p className="mt-1 text-sm text-muted-foreground">Revision bất biến cho catalogue tương lai; không có điều khiển learner hoặc thanh toán.</p>
        </div>
        <button type="button" onClick={() => load(selectedCode)} className="h-11 rounded-lg border border-border px-4 text-sm font-semibold hover:bg-accent focus:outline-none focus:ring-2 focus:ring-ring">
          <RefreshCw className="mr-2 inline h-4 w-4" aria-hidden="true" />Tải lại
        </button>
      </header>

      <div className="flex gap-2" role="tablist" aria-label="Chọn gói chính sách">
        {['FREE', 'PREMIUM'].map((code) => (
          <button key={code} type="button" role="tab" aria-selected={selectedCode === code} onClick={() => choosePlan(code)}
            className={`min-h-11 rounded-lg px-4 text-sm font-bold focus:outline-none focus:ring-2 focus:ring-ring ${selectedCode === code ? 'bg-primary text-primary-foreground' : 'border border-border bg-secondary text-foreground hover:bg-accent'}`}>
            {code === 'FREE' ? 'Free' : 'Premium'}
          </button>
        ))}
      </div>

      {error && <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}
      {notice && <div role="status" className="rounded-lg border border-success/40 bg-success/10 p-3 text-sm text-foreground">{notice}</div>}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(280px,.7fr)]">
        <form onSubmit={publish} className="space-y-4 rounded-xl border border-border bg-card p-4 sm:p-5" noValidate>
          <div className="flex items-center gap-2 text-sm font-bold text-foreground"><ShieldCheck className="h-5 w-5 text-primary" aria-hidden="true" />Công bố revision {selected?.revisionNumber ? selected.revisionNumber + 1 : 1}</div>
          <Field label="Lý do thay đổi" required><input ref={reasonRef} name="reason" value={form.reason} onChange={update} maxLength="500" className="input" /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Tên hiển thị" required><input name="displayName" value={form.displayName} onChange={update} maxLength="120" className="input" /></Field>
            <Field label="Trạng thái catalogue"><select name="availabilityState" value={form.availabilityState} onChange={update} className="input"><option value="ACTIVE">Đang hiển thị</option><option value="HIDDEN">Ẩn</option></select></Field>
          </div>
          <Field label="Mô tả"><textarea name="description" value={form.description} onChange={update} maxLength="2000" rows="3" className="input" /></Field>
          <Field label="Quyền lợi (mỗi dòng một mục)" required><textarea name="benefits" value={form.benefits} onChange={update} rows="4" className="input" /></Field>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Giá"><input name="priceAmount" type="number" min="0" step="0.0001" value={form.priceAmount} onChange={update} className="input" /></Field>
            <Field label="Tiền tệ"><input name="currency" value={form.currency} onChange={update} maxLength="3" className="input uppercase" /></Field>
            <Field label="Chu kỳ giá"><select name="priceInterval" value={form.priceInterval} onChange={update} className="input"><option value="MONTH">Tháng</option><option value="YEAR">Năm</option></select></Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="AI allowance"><input name="allowanceUnits" type="number" min="0" step="1" value={form.allowanceUnits} onChange={update} className="input" /></Field>
            <Field label="Chu kỳ allowance"><select name="allowancePeriod" value={form.allowancePeriod} onChange={update} className="input"><option value="DAY">Ngày</option><option value="MONTH">Tháng</option></select></Field>
            <Field label="Nhãn hiển thị"><input name="displayLabel" value={form.displayLabel} onChange={update} maxLength="120" className="input" /></Field>
          </div>
          <button disabled={saving} type="submit" className="min-h-11 rounded-lg bg-primary px-4 text-sm font-bold text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-60">
            <Save className="mr-2 inline h-4 w-4" aria-hidden="true" />{saving ? 'Đang lưu…' : 'Công bố revision'}
          </button>
        </form>

        <aside className="rounded-xl border border-border bg-card p-4 sm:p-5">
          <h3 className="flex items-center gap-2 font-bold text-foreground"><History className="h-5 w-5 text-primary" aria-hidden="true" />Lịch sử bất biến</h3>
          {history.length === 0 ? <p className="mt-4 text-sm text-muted-foreground">Chưa có revision nào.</p> : <ol className="mt-4 space-y-3">{history.map((policy) => <li key={policy.id} className="rounded-lg border border-border p-3 text-sm"><div className="flex justify-between gap-2"><strong>Revision {policy.revisionNumber}</strong><span className="text-muted-foreground">{policy.status}</span></div><p className="mt-1 text-muted-foreground">{policy.allowanceUnits} lượt/{policy.allowancePeriod.toLowerCase()} · {policy.priceAmount} {policy.currency}</p><time className="mt-1 block text-xs text-muted-foreground" dateTime={policy.createdAt}>{new Date(policy.createdAt).toLocaleString('vi-VN')}</time></li>)}</ol>}
        </aside>
      </div>
    </section>
  );
}

function Field({ label, required, children }) { return <label className="block text-sm font-medium text-foreground"><span>{label}{required ? ' *' : ''}</span><span className="mt-1.5 block">{children}</span></label>; }
export function PanelState({ type, message, onRetry }) {
  if (type === 'loading') return <div role="status" className="py-10 text-center text-sm text-muted-foreground">Đang tải chính sách AI…</div>;
  return <div role="alert" className="rounded-xl border border-destructive/40 bg-destructive/10 p-5 text-center text-sm text-destructive"><p>{message}</p><button type="button" onClick={onRetry} className="mt-3 min-h-11 rounded-lg border border-border px-4 font-bold text-foreground">Thử lại</button></div>;
}
