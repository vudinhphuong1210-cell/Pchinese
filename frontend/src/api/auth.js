import { httpClient, refreshAccessSession } from './http.js';
import { apiUrl } from './apiUrl.js';
import { authSessionStore } from '../features/auth/authSessionStore.js';

export const authApi = {
  /**
   * Neutral registration request.
   * Returns 202 Accepted without exposing account enumeration.
   */
  async register({ email, password }) {
    return httpClient(apiUrl('/auth/register'), {
      method: 'POST',
      body: { email, password },
      skipAuth: true,
    });
  },

  /**
   * Request/resend verification email. Neutral accepted.
   */
  async requestEmailVerification({ email }) {
    return httpClient(apiUrl('/auth/email-verifications'), {
      method: 'POST',
      body: { email },
      skipAuth: true,
    });
  },

  /**
   * Confirm email verification token.
   */
  async confirmEmailVerification({ verificationToken }) {
    return httpClient(apiUrl('/auth/email-verifications/confirm'), {
      method: 'POST',
      body: { verificationToken },
      skipAuth: true,
    });
  },

  /**
   * Login with ACTIVE verified credentials.
   * On success, updates in-memory authSessionStore.
   */
  async login({ email, password, deviceId, deviceLabel, platform }) {
    const response = await httpClient(apiUrl('/auth/login'), {
      method: 'POST',
      body: {
        email,
        password,
        deviceId: deviceId || 'web-browser',
        deviceLabel: deviceLabel || (typeof navigator !== 'undefined' ? navigator.userAgent.slice(0, 100) : 'Web Client'),
        platform: platform || 'WEB',
      },
      skipAuth: true,
    });

    if (response && response.success && response.data) {
      authSessionStore.setSession({
        accessToken: response.data.accessToken,
        expiresAt: response.data.expiresAt,
        roles: response.data.roles,
        browserSessionId: response.data.browserSessionId,
      });
    }

    return response;
  },

  /**
   * Restore a memory-only access session from the browser's HttpOnly refresh
   * cookie. The HTTP layer makes this single-flight with automatic refreshes.
   */
  async refresh() {
    return refreshAccessSession();
  },

  /**
   * Revoke current session. Clears memory session.
   */
  async logout() {
    try {
      await httpClient(apiUrl('/auth/logout'), {
        method: 'POST',
        skipAuth: false,
      });
    } finally {
      authSessionStore.clearSession();
    }
  },

  /**
   * Neutral password reset request.
   */
  async requestPasswordReset({ email }) {
    return httpClient(apiUrl('/auth/password-resets'), {
      method: 'POST',
      body: { email },
      skipAuth: true,
    });
  },

  /**
   * Confirm password reset token.
   */
  async confirmPasswordReset({ resetToken, newPassword }) {
    return httpClient(apiUrl('/auth/password-resets/confirm'), {
      method: 'POST',
      body: { resetToken, newPassword },
      skipAuth: true,
    });
  },
};
