import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const adminUsersApi = {
  /**
   * Lists the server-paginated, privacy-safe User Management summaries.
   * Each item contains only userId, permitted accountName, accountState and active ADMIN roles.
   */
  async listManagedUsers({ page = 0, size = 20 } = {}) {
    if (!Number.isInteger(page) || page < 0) {
      throw new Error('page must be a non-negative integer');
    }
    if (!Number.isInteger(size) || size < 1 || size > 50) {
      throw new Error('size must be an integer from 1 to 50');
    }
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    return httpClient(apiUrl(`/users?${query.toString()}`), { method: 'GET' });
  },

  /**
   * Reads the selected user's minimal role and access projection.
   * Returns { userId, accountName, roles, accessState }.
   */
  async getAccountRoleProjection(userId) {
    if (!userId || typeof userId !== 'string') {
      throw new Error('selected userId required');
    }
    return httpClient(apiUrl(`/users/${encodeURIComponent(userId)}/roles`), {
      method: 'GET',
    });
  },

  /**
   * Grants ADMIN role to an exact target UUID.
   */
  async grantAdminRole(userId) {
    return httpClient(apiUrl(`/users/${encodeURIComponent(userId)}/roles`), {
      method: 'POST',
      body: { role: 'ADMIN' },
    });
  },

  /**
   * Revokes ADMIN role from an exact target UUID.
   */
  async revokeAdminRole(userId) {
    return httpClient(apiUrl(`/users/${encodeURIComponent(userId)}/roles/ADMIN`), {
      method: 'DELETE',
    });
  },

  /**
   * Locks target account with required reason and optional/required note.
   */
  async lockAccount(userId, { reason, note }) {
    const payload = { reason };
    if (note) {
      payload.note = note.trim();
    }

    return httpClient(apiUrl(`/users/${encodeURIComponent(userId)}/lock`), {
      method: 'POST',
      body: payload,
    });
  },

  /**
   * Unlocks target account with required reason and optional/required note.
   */
  async unlockAccount(userId, { reason, note }) {
    const payload = { reason };
    if (note) {
      payload.note = note.trim();
    }

    return httpClient(apiUrl(`/users/${encodeURIComponent(userId)}/unlock`), {
      method: 'POST',
      body: payload,
    });
  },
};
