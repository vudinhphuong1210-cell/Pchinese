import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

/**
 * Reads the canonical F02 current-user projection containing F03's entitlement fragment.
 * GET /api/v1/me
 * @returns {Promise<{ planCode: string, allowanceLimit: number, usedUnits: number, remainingUnits: number, cycleStartAt: string, cycleEndAt: string }>}
 */
export async function getEntitlementSummary() {
  const res = await httpClient(apiUrl('/me'), {
    method: 'GET',
  });
  if (res && res.success && res.data) {
    return res.data.entitlement || {
      planCode: 'FREE',
      allowanceLimit: 30,
      usedUnits: 0,
      remainingUnits: 30,
      cycleStartAt: new Date().toISOString(),
      cycleEndAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
    };
  }
  throw new Error(res?.error?.message || 'Không thể tải thông tin quyền sử dụng.');
}

/**
 * Evaluates the AI quota state based on entitlement object.
 * @param {Object} entitlement
 * @returns {{ state: 'NORMAL' | 'LOW' | 'EXHAUSTED', percentage: number, remainingUnits: number, allowanceLimit: number, usedUnits: number }}
 */
export function calculateQuotaStatus(entitlement) {
  const allowanceLimit = entitlement?.allowanceLimit ?? 30;
  const usedUnits = entitlement?.usedUnits ?? 0;
  const remainingUnits = entitlement?.remainingUnits ?? Math.max(0, allowanceLimit - usedUnits);

  const percentage = allowanceLimit > 0 ? Math.round((usedUnits / allowanceLimit) * 100) : 0;

  let state = 'NORMAL';
  if (remainingUnits <= 0) {
    state = 'EXHAUSTED';
  } else if (remainingUnits <= 5) {
    state = 'LOW';
  }

  return {
    state,
    percentage,
    remainingUnits,
    allowanceLimit,
    usedUnits,
  };
}

/**
 * Formats ISO date-time string to Vietnamese date representation.
 * @param {string} isoString
 * @returns {string}
 */
export function formatCycleDate(isoString) {
  if (!isoString) return 'Không xác định';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return 'Không xác định';
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  } catch (_e) {
    return 'Không xác định';
  }
}

export const entitlementApi = {
  getEntitlementSummary,
  calculateQuotaStatus,
  formatCycleDate,
};
