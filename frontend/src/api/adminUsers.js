import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const adminUsersApi = {
  /**
   * Reads exact-ID minimal role and access projection.
   * Returns { userId, roles, accessState }.
   */
  async getAccountRoleProjection(userId) {
    if (!userId || typeof userId !== 'string') {
      throw new Error('userId exact UUID required');
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
