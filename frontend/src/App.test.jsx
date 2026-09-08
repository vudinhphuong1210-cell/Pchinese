import React from 'react';
import { act, render, screen, fireEvent, waitFor } from '@testing-library/react';
import App from './App.jsx';
import { authApi } from './api/auth.js';
import { authSessionStore } from './features/auth/authSessionStore.js';

jest.mock('./api/auth.js', () => ({
  authApi: {
    refresh: jest.fn(),
    logout: jest.fn(),
  },
}));

describe('App session restoration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    authSessionStore.clearSession();
    localStorage.clear();
    sessionStorage.clear();
  });

  test('restores the authenticated UI after a page reload without browser token storage', async () => {
    authApi.refresh.mockImplementation(async () => {
      authSessionStore.setSession({
        accessToken: 'restored.access.jwt',
        expiresAt: '2030-01-01T00:00:00.000Z',
      });
      return { success: true, data: {} };
    });

    render(<App />);

    await waitFor(() => expect(authApi.refresh).toHaveBeenCalledTimes(1));

    expect(authSessionStore.isAuthenticated()).toBe(true);
    expect(screen.getByTestId('sidebar-logout-btn')).toBeInTheDocument();
    expect(screen.queryByTestId('nav-admin')).not.toBeInTheDocument();
    expect(screen.queryByTestId('session-restoring')).not.toBeInTheDocument();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
  });

  test('does not display redundant add-account button in sidebar', async () => {
    authApi.refresh.mockResolvedValue({ success: false });
    render(<App />);

    await waitFor(() => expect(authApi.refresh).toHaveBeenCalledTimes(1));
    await act(async () => {
      authSessionStore.setSession({
        accessToken: 'current.account.access.jwt',
        expiresAt: '2030-01-01T00:00:00.000Z',
        browserSessionId: '550e8400-e29b-41d4-a716-446655440000',
      });
    });

    expect(screen.queryByTestId('add-account-btn')).not.toBeInTheDocument();
  });
});
