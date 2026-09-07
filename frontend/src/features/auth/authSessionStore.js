/**
 * Memory-only Auth Session Store
 * CONSTITUTION Article 3 / F01 Spec: Access token lives strictly in browser memory.
 * NEVER store tokens in localStorage, sessionStorage, or indexedDB.
 */

let state = {
  accessToken: null,
  expiresAt: null,
  isAuthenticated: false,
};

const listeners = new Set();

export const authSessionStore = {
  getAccessToken() {
    return state.accessToken;
  },

  getExpiresAt() {
    return state.expiresAt;
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

  setSession({ accessToken, expiresAt }) {
    state = {
      accessToken,
      expiresAt,
      isAuthenticated: Boolean(accessToken),
    };
    this.notify();
  },

  clearSession() {
    state = {
      accessToken: null,
      expiresAt: null,
      isAuthenticated: false,
    };
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
