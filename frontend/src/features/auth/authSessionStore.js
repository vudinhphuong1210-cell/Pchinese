/**
 * Memory-only Auth Session Store
 * CONSTITUTION Article 3 / F01 Spec: Access token lives strictly in browser memory.
 * NEVER store tokens in localStorage, sessionStorage, or indexedDB.
 */

const BROWSER_SESSION_STORAGE_KEY = 'pchinese.browser-session-id';
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function storedBrowserSessionId() {
  if (typeof sessionStorage === 'undefined') return null;
  const value = sessionStorage.getItem(BROWSER_SESSION_STORAGE_KEY);
  return value && UUID_PATTERN.test(value) ? value : null;
}

function persistBrowserSessionId(value) {
  if (typeof sessionStorage === 'undefined') return;
  if (value) sessionStorage.setItem(BROWSER_SESSION_STORAGE_KEY, value);
  else sessionStorage.removeItem(BROWSER_SESSION_STORAGE_KEY);
}

let state = {
  accessToken: null,
  expiresAt: null,
  roles: [],
  isAdmin: false,
  isAuthenticated: false,
  browserSessionId: storedBrowserSessionId(),
};

const listeners = new Set();

export const authSessionStore = {
  getAccessToken() {
    return state.accessToken;
  },

  getExpiresAt() {
    return state.expiresAt;
  },

  getBrowserSessionId() {
    return state.browserSessionId;
  },

  isAuthenticated() {
    if (!state.accessToken) return false;
    if (state.expiresAt && new Date(state.expiresAt).getTime() <= Date.now()) {
      return false;
    }
    return true;
  },

  getState() {
    return { ...state };
  },

  setSession({ accessToken, expiresAt, roles = [], browserSessionId }) {
    const currentRoles = Array.isArray(roles) ? roles.filter((role) => role === 'ADMIN') : [];
    const selectedBrowserSessionId = UUID_PATTERN.test(browserSessionId || '')
      ? browserSessionId
      : state.browserSessionId;
    persistBrowserSessionId(selectedBrowserSessionId);
    state = {
      accessToken,
      expiresAt,
      roles: currentRoles,
      isAdmin: currentRoles.includes('ADMIN'),
      isAuthenticated: Boolean(accessToken),
      browserSessionId: selectedBrowserSessionId,
    };
    this.notify();
  },

  clearSession() {
    state = {
      accessToken: null,
      expiresAt: null,
      roles: [],
      isAdmin: false,
      isAuthenticated: false,
      browserSessionId: null,
    };
    persistBrowserSessionId(null);
    this.notify();
  },

  subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  notify() {
    for (const listener of listeners) {
      try {
        listener({ ...state });
      } catch (err) {
        console.error('Error notifying auth session listener:', err);
      }
    }
  },
};
