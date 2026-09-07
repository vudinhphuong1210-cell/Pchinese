import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { LoginForm } from './LoginForm.jsx';
import { RegisterForm } from './RegisterForm.jsx';
import { authSessionStore } from './authSessionStore.js';
import { authApi } from '../../api/auth.js';

jest.mock('../../api/auth.js', () => ({
  authApi: {
    login: jest.fn(),
    register: jest.fn(),
    refresh: jest.fn(),
    logout: jest.fn(),
  },
}));

describe('F01 Auth Flows and Storage Security (CONSTITUTION Article 3)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    authSessionStore.clearSession();
    localStorage.clear();
    sessionStorage.clear();
  });

  test('LoginForm validates required input fields', async () => {
    render(<LoginForm />);

    const submitBtn = screen.getByTestId('login-submit-btn');
    fireEvent.click(submitBtn);

    expect(await screen.findByTestId('login-error')).toHaveTextContent(
      'Vui lòng nhập đầy đủ email và mật khẩu.'
    );
  });

  test('LoginForm handles login failure and does not leak raw tokens to browser storage', async () => {
    authApi.login.mockRejectedValueOnce({
      error: {
        code: 'UNAUTHENTICATED',
        message: 'Email hoặc mật khẩu không chính xác.',
      },
    });

    render(<LoginForm />);

    fireEvent.change(screen.getByTestId('login-email-input'), {
      target: { value: 'wrong@pchinese.net' },
    });
    fireEvent.change(screen.getByTestId('login-password-input'), {
      target: { value: 'wrongpass123' },
    });

    fireEvent.click(screen.getByTestId('login-submit-btn'));

    expect(await screen.findByTestId('login-error')).toHaveTextContent('UNAUTHENTICATED');

    // CONSTITUTION Rule: No tokens in browser storage
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(authSessionStore.getAccessToken()).toBeNull();
  });

  test('LoginForm handles successful login and sets memory-only session store', async () => {
    authApi.login.mockImplementation(async () => {
      authSessionStore.setSession({
        accessToken: 'mock.access.jwt',
        expiresAt: new Date(Date.now() + 600000).toISOString(),
      });
      return { success: true, data: {} };
    });

    const onLoginSuccess = jest.fn();
    render(<LoginForm onLoginSuccess={onLoginSuccess} />);

    fireEvent.change(screen.getByTestId('login-email-input'), {
      target: { value: 'user@pchinese.net' },
    });
    fireEvent.change(screen.getByTestId('login-password-input'), {
      target: { value: 'validpass123' },
    });

    fireEvent.click(screen.getByTestId('login-submit-btn'));

    await waitFor(() => expect(onLoginSuccess).toHaveBeenCalled());

    // Access token exists ONLY in memory store
    expect(authSessionStore.getAccessToken()).toBe('mock.access.jwt');
    expect(authSessionStore.getState()).not.toHaveProperty('sessionId');
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('accessToken')).toBeNull();
  });

  test('RegisterForm displays neutral success response without account enumeration', async () => {
    authApi.register.mockResolvedValueOnce({
      success: true,
      data: { accepted: true },
      error: null,
      meta: { correlationId: 'test-uuid' },
    });

    render(<RegisterForm />);

    fireEvent.change(screen.getByTestId('register-email-input'), {
      target: { value: 'newstudent@pchinese.net' },
    });
    fireEvent.change(screen.getByTestId('register-password-input'), {
      target: { value: 'securePass123' },
    });

    fireEvent.click(screen.getByTestId('register-submit-btn'));

    expect(await screen.findByTestId('register-success')).toBeInTheDocument();
    expect(screen.getByTestId('register-success')).toHaveTextContent(
      'Nếu địa chỉ email newstudent@pchinese.net hợp lệ'
    );
  });
});
