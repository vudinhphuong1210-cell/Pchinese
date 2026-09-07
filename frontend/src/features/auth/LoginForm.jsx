import React, { useState } from 'react';
import { authApi } from '../../api/auth.js';
import { Mail, Lock, AlertCircle, Loader2 } from 'lucide-react';

export function LoginForm({ onLoginSuccess, onNavigateToRegister, onNavigateToForgotPassword }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!email || !password) {
      setError({ message: 'Vui lòng nhập đầy đủ email và mật khẩu.' });
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.login({
        email,
        password,
        deviceId: 'web-browser',
        deviceLabel: typeof navigator !== 'undefined' ? navigator.userAgent.slice(0, 80) : 'Web Client',
        platform: 'WEB',
      });

      if (response && response.success) {
        if (onLoginSuccess) {
          onLoginSuccess(response.data);
        }
      }
    } catch (err) {
      const errObj = err?.error || { message: 'Đăng nhập không thành công. Vui lòng kiểm tra lại thông tin.' };
      setError(errObj);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg"
      data-testid="login-form"
      noValidate
    >
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Đăng nhập</h1>
        <p className="text-sm text-muted-foreground">Chào mừng trở lại với nền tảng học tiếng Trung P Chinese.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="login-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">{error.code || 'LỖI ĐĂNG NHẬP'}</p>
            <p className="mt-0.5">{error.message}</p>
          </div>
        </div>
      )}

      <div className="space-y-4">
        <div className="space-y-1.5">
          <label htmlFor="login-email" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Địa chỉ Email
          </label>
          <div className="relative">
            <Mail className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="login-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="hocvien@pchinese.net"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="login-email-input"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="login-password" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
              Mật khẩu
            </label>
            <button
              type="button"
              onClick={() => onNavigateToForgotPassword && onNavigateToForgotPassword()}
              className="text-xs text-primary hover:underline font-medium"
              data-testid="forgot-password-link"
            >
              Quên mật khẩu?
            </button>
          </div>
          <div className="relative">
            <Lock className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="login-password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Mật khẩu của bạn"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="login-password-input"
            />
          </div>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full h-11 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center justify-center space-x-2"
        data-testid="login-submit-btn"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Đang xác thực...</span>
          </>
        ) : (
          <span>Đăng nhập</span>
        )}
      </button>

      <div className="text-center text-sm text-muted-foreground pt-2 border-t border-border">
        Chưa có tài khoản?{' '}
        <button
          type="button"
          onClick={() => onNavigateToRegister && onNavigateToRegister()}
          className="text-primary hover:underline font-semibold"
          data-testid="register-link"
        >
          Đăng ký ngay
        </button>
      </div>
    </form>
  );
}
