import { apiUrl } from './apiUrl.js';
import { httpClient } from './http.js';

function pageQuery({ page = 0, size = 20 } = {}) {
  if (!Number.isInteger(page) || page < 0 || !Number.isInteger(size) || size < 1 || size > 50) {
    throw new Error('Trang phải hợp lệ và kích thước từ 1 đến 50.');
  }
  return new URLSearchParams({ page: String(page), size: String(size) }).toString();
}

function optionalQuery(values) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') params.set(key, String(value));
  });
  return params.toString();
}

/** Contract-bound F12 client. It never sends learner identity, quota state or provider data. */
export const adminAiApi = {
  listPlans: () => httpClient(apiUrl('/admin/ai/plans')),
  listPolicyRevisions: (planCode, pagination) => httpClient(apiUrl(`/admin/ai/plans/${encodeURIComponent(planCode)}/revisions?${pageQuery(pagination)}`)),
  publishPolicy: (planCode, payload) => httpClient(apiUrl(`/admin/ai/plans/${encodeURIComponent(planCode)}/revisions`), {
    method: 'POST', body: payload,
  }),
  retirePlan: (planCode, payload) => httpClient(apiUrl(`/admin/ai/plans/${encodeURIComponent(planCode)}/retire`), {
    method: 'POST', body: payload,
  }),
  getUsageReport: ({ fromInclusive, toExclusive, planCode, policyVersionId, capability }) => {
    const query = optionalQuery({ fromInclusive, toExclusive, planCode, policyVersionId, capability });
    return httpClient(apiUrl(`/admin/ai/usage-report?${query}`));
  },
  listMonitoringRules: () => httpClient(apiUrl('/admin/ai/monitoring-rules')),
  createMonitoringRule: (payload) => httpClient(apiUrl('/admin/ai/monitoring-rules'), { method: 'POST', body: payload }),
  updateMonitoringRule: (ruleId, payload) => httpClient(apiUrl(`/admin/ai/monitoring-rules/${encodeURIComponent(ruleId)}`), {
    method: 'PATCH', body: payload,
  }),
  listAlerts: ({ page = 0, size = 20, state } = {}) => {
    const query = optionalQuery({ page, size, state });
    return httpClient(apiUrl(`/admin/ai/alerts?${query}`));
  },
  acknowledgeAlert: (alertId, payload) => httpClient(apiUrl(`/admin/ai/alerts/${encodeURIComponent(alertId)}/acknowledgements`), {
    method: 'POST', body: payload,
  }),
  listAuditEvents: (pagination) => httpClient(apiUrl(`/admin/ai/audit-events?${pageQuery(pagination)}`)),
};
