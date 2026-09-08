import { refreshAccessSession } from './http.js';
import { authSessionStore } from '../features/auth/authSessionStore.js';

describe('refreshAccessSession', () => {
  const originalFetch = global.fetch;
  const browserSessionId = '550e8400-e29b-41d4-a716-446655440000';

  beforeEach(() => {
    authSessionStore.clearSession();
    document.cookie = 'XSRF-TOKEN=test-csrf-token; path=/';
    global.fetch = jest.fn();
  });

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/';
    document.cookie = `XSRF-TOKEN-${browserSessionId}=; Max-Age=0; path=/`;
    global.fetch = originalFetch;
  });

  test('shares concurrent startup refresh requests and restores the memory-only access token', async () => {
    const responseBody = {
      success: true,
      data: {
        accessToken: 'fresh.access.jwt',
        expiresAt: '2030-01-01T00:00:00.000Z',
        roles: ['ADMIN'],
        browserSessionId,
      },
      error: null,
      meta: { correlationId: '47b9f704-fb5b-4898-932f-f4d78a28780d' },
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue(responseBody),
    });
    // This selector is not a credential: it is per-tab routing information for
    // the matching HttpOnly refresh cookie. It survives an F5 in this tab.
    authSessionStore.setSession({ accessToken: null, expiresAt: null, browserSessionId });
    document.cookie = `XSRF-TOKEN-${browserSessionId}=test-csrf-token; path=/`;

    const [first, second] = await Promise.all([
      refreshAccessSession(),
      refreshAccessSession(),
    ]);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith('/api/v1/auth/refresh', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      headers: expect.objectContaining({
        'X-CSRF-Token': 'test-csrf-token',
        'X-Refresh-Request-Id': expect.any(String),
        'X-Browser-Session-Id': browserSessionId,
      }),
    }));
    expect(first).toEqual(responseBody);
    expect(second).toEqual(responseBody);
    expect(authSessionStore.getAccessToken()).toBe('fresh.access.jwt');
    expect(authSessionStore.getState().roles).toEqual(['ADMIN']);
    expect(authSessionStore.getState().isAdmin).toBe(true);
    expect(authSessionStore.getState().browserSessionId).toBe(browserSessionId);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('pchinese.browser-session-id')).toBe(browserSessionId);
  });
});
