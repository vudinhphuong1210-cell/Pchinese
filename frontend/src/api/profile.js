import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

/**
 * Reads the canonical F02 current-user profile projection.
 * GET /api/v1/me
 * @returns {Promise<{ success: boolean, data: { profile: Object }, meta: Object }>}
 */
export async function getProfile() {
  return httpClient(apiUrl('/me'), {
    method: 'GET',
  });
}

/**
 * Updates approved caller preferences and returns canonical current-user profile projection.
 * PATCH /api/v1/me
 * @param {Object} profileData - Profile fields including required expectedProfileVersion.
 * @returns {Promise<{ success: boolean, data: { profile: Object }, meta: Object }>}
 */
export async function updateProfile(profileData) {
  return httpClient(apiUrl('/me'), {
    method: 'PATCH',
    body: profileData,
  });
}

export const profileApi = {
  getProfile,
  updateProfile,
};
