import { authSessionStore } from '../features/auth/authSessionStore.js';
import { apiUrl } from './apiUrl.js';

/**
 * Generates a standard UUID v4 for request correlation and idempotency headers.
 */
export function generateUUID() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * Gets CSRF token from document.cookie if set by backend.
 */
function cookieValue(name) {
  if (typeof document === 'undefined') return null;
  const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp(`(^| )${escapedName}=([^;]+)`));
  return match ? decodeURIComponent(match[2]) : null;
}

function getCsrfToken() {
  const browserSessionId = authSessionStore.getBrowserSessionId();
  if (browserSessionId) {
    return cookieValue(`XSRF-TOKEN-${browserSessionId}`);
  }
  if (typeof document === 'undefined') return null;
  const namedTokens = document.cookie.split(';').map((value) => value.trim())
    .filter((value) => value.startsWith('XSRF-TOKEN-'));
  if (namedTokens.length === 1) {
    return decodeURIComponent(namedTokens[0].substring(namedTokens[0].indexOf('=') + 1));
  }
  return cookieValue('XSRF-TOKEN');
}

function applyBrowserSessionHeader(headers) {
  const browserSessionId = authSessionStore.getBrowserSessionId();
  if (browserSessionId) {
    headers['X-Browser-Session-Id'] = browserSessionId;
  }
}

let refreshPromise = null;

/**
 * Performs single-flight refresh call to /api/v1/auth/refresh
 */
async function performSingleFlightRefresh() {
  if (refreshPromise) {
    return refreshPromise;
  }

  const refreshRequestId = generateUUID();

  refreshPromise = (async () => {
    try {
      const headers = {
        'Content-Type': 'application/json',
        'X-Refresh-Request-Id': refreshRequestId,
      };
      applyBrowserSessionHeader(headers);

      const csrfToken = getCsrfToken();
      if (csrfToken) {
        headers['X-CSRF-Token'] = csrfToken;
      }

      const response = await fetch(apiUrl('/auth/refresh'), {
        method: 'POST',
        headers,
        credentials: 'include',
      });

      const body = await response.json().catch(() => null);

      if (response.ok && body && body.success && body.data) {
        authSessionStore.setSession({
          accessToken: body.data.accessToken,
          expiresAt: body.data.expiresAt,
          roles: body.data.roles,
          browserSessionId: body.data.browserSessionId,
        });
        return body;
      } else {
        authSessionStore.clearSession();
        const err = new Error(body?.error?.message || 'Refresh failed');
        err.code = body?.error?.code || 'REFRESH_TOKEN_INVALID';
        err.meta = body?.meta;
        throw err;
      }
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

/**
 * Restores the memory-only access session from the HttpOnly refresh cookie.
 *
 * This is deliberately shared with the 401 interceptor: React Strict Mode can
 * run startup effects twice in development, and concurrent refreshes with
 * different request IDs would otherwise be treated as refresh-token reuse.
 */
export function refreshAccessSession() {
  return performSingleFlightRefresh();
}

/**
 * Core HTTP Client with envelope unwrap and 401 refresh interceptor
 */
export async function httpClient(url, options = {}) {
  const {
    method = 'GET',
    body,
    headers: customHeaders = {},
    skipAuth = false,
    _isRetry = false,
    ...rest
  } = options;

  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
  const headers = {
    ...(!isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...customHeaders,
  };
  applyBrowserSessionHeader(headers);

  const token = authSessionStore.getAccessToken();
  if (!skipAuth && token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const csrfToken = getCsrfToken();
  if (csrfToken) {
    headers['X-CSRF-Token'] = csrfToken;
  }

  const config = {
    method,
    headers,
    credentials: 'include',
    ...rest,
  };

  if (body !== undefined) {
    config.body = isFormData || typeof body === 'string' ? body : JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, config);
  } catch (netErr) {
    throw {
      success: false,
      data: null,
      error: {
        code: 'NETWORK_ERROR',
        message: netErr.message || 'Không thể kết nối đến máy chủ',
      },
      meta: { correlationId: generateUUID() },
    };
  }

  let responseBody;
  try {
    responseBody = await response.json();
  } catch (_e) {
    responseBody = null;
  }

  // Intercept 401 ACCESS_TOKEN_EXPIRED for automatic refresh retry
  if (
    response.status === 401 &&
    responseBody?.error?.code === 'ACCESS_TOKEN_EXPIRED' &&
    !_isRetry &&
    !skipAuth
  ) {
    try {
      await refreshAccessSession();
      // Retry original request once with new token
      return httpClient(url, { ...options, _isRetry: true });
    } catch (refreshErr) {
      throw {
        success: false,
        data: null,
        error: {
          code: refreshErr.code || 'UNAUTHENTICATED',
          message: refreshErr.message || 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.',
        },
        meta: refreshErr.meta || { correlationId: generateUUID() },
      };
    }
  }

  if (!response.ok || (responseBody && responseBody.success === false)) {
    const errorObj = responseBody?.error || {
      code: `HTTP_${response.status}`,
      message: response.statusText || 'Yêu cầu không thành công',
    };
    const metaObj = responseBody?.meta || { correlationId: generateUUID() };

    throw {
      success: false,
      data: null,
      error: errorObj,
      meta: metaObj,
      status: response.status,
    };
  }

  return responseBody;
}
