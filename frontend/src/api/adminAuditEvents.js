import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

/**
 * Reads the server-authorized, paginated system audit projection for ADMIN only.
 * The API intentionally exposes no audit metadata, identifiers or profile values.
 */
export async function listAdminAuditEvents({ page = 0, size = 20 } = {}) {
  if (!Number.isInteger(page) || page < 0) {
    throw new Error('page must be a non-negative integer');
  }
  if (!Number.isInteger(size) || size < 1 || size > 50) {
    throw new Error('size must be an integer from 1 to 50');
  }
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return httpClient(apiUrl(`/admin/audit-events?${query.toString()}`), { method: 'GET' });
}
