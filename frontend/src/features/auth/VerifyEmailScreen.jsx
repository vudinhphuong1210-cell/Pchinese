import React, { useState } from 'react';
import { authApi } from '../../api/auth.js';
import { KeyRound, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export function VerifyEmailScreen({ initialToken = '', onNavigateToLogin }) {
  const [token, setToken] = useState(initialToken);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token.trim()) return;

    setError(null);
    setLoading(true);

    try {
      await authApi.confirmEmailVerification({ verificationToken: token.trim() });
      setSuccess(true);
    } catch (err) {
      setError(err?.error || { message: 'Mã xác thực không hợp lệ hoặc đã hết hạn.' });
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg" data-testid="verify-success">
        <div className="flex items-center space-x-3 text-success">
          <CheckCircle2 className="w-8 h-8 shrink-0" />
          <h2 className="text-xl font-bold">Xác thực thành công!</h2>
        </div>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay bây giờ để sử dụng dịch vụ.
        </p>
        <button
          type="button"
          onClick={() => onNavigateToLogin && onNavigateToLogin()}
          className="w-full h-11 bg-primary text-primary-foreground font-semibold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none"
          data-testid="verify-login-btn"
        >
          Đăng nhập ngay
        </button>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg"
      data-testid="verify-form"
    >
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Xác thực Email</h1>
        <p className="text-sm text-muted-foreground">Nhập mã xác thực đã được gửi tới email của bạn.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="verify-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{error.message}</span>
        </div>
      )}

      <div className="space-y-1.5">
        <label htmlFor="verify-token" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Mã xác thực
        </label>
        <div className="relative">
          <KeyRound className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
          <input
            id="verify-token"
            type="text"
            required
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="Dán mã xác thực tại đây"
            className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring font-mono"
            data-testid="verify-token-input"
          />
        </div>
      </div>

      <button
        type="submit"
        disabled={loading || !token.trim()}
        className="w-full h-11 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center justify-center space-x-2"
        data-testid="verify-submit-btn"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Đang xác thực...</span>
          </>
        ) : (
          <span>Kích hoạt tài khoản</span>
        )}
      </button>
    </form>
  );
}
