import React, { useState } from 'react';
import { authApi } from '../../api/auth.js';
import { KeyRound, Lock, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export function ResetPasswordForm({ initialToken = '', onNavigateToLogin }) {
  const [token, setToken] = useState(initialToken);
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!token.trim() || !newPassword) {
      setError({ message: 'Vui lòng nhập mã khôi phục và mật khẩu mới.' });
      return;
    }

    if (newPassword.length < 8) {
      setError({ message: 'Mật khẩu mới phải có ít nhất 8 ký tự.' });
      return;
    }

    setLoading(true);

    try {
      await authApi.confirmPasswordReset({
        resetToken: token.trim(),
        newPassword,
      });
      setSuccess(true);
    } catch (err) {
      setError(err?.error || { message: 'Mã khôi phục không hợp lệ hoặc đã hết hạn.' });
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg" data-testid="reset-success">
        <div className="flex items-center space-x-3 text-success">
          <CheckCircle2 className="w-8 h-8 shrink-0" />
          <h2 className="text-xl font-bold">Đặt lại mật khẩu thành công!</h2>
        </div>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Mật khẩu của bạn đã được thay đổi. Tất cả phiên đăng nhập cũ đã được đăng xuất an toàn. Vui lòng đăng nhập lại với mật khẩu mới.
        </p>
        <button
          type="button"
          onClick={() => onNavigateToLogin && onNavigateToLogin()}
          className="w-full h-11 bg-primary text-primary-foreground font-semibold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none"
          data-testid="reset-login-btn"
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
      data-testid="reset-form"
    >
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Đặt lại Mật khẩu</h1>
        <p className="text-sm text-muted-foreground">Nhập mã khôi phục và thiết lập mật khẩu mới.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="reset-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{error.message}</span>
        </div>
      )}

      <div className="space-y-4">
        <div className="space-y-1.5">
          <label htmlFor="reset-token" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Mã khôi phục (Reset Token)
          </label>
          <div className="relative">
            <KeyRound className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="reset-token"
              type="text"
              required
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Dán mã khôi phục tại đây"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring font-mono"
              data-testid="reset-token-input"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label htmlFor="reset-new-password" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Mật khẩu mới
          </label>
          <div className="relative">
            <Lock className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="reset-new-password"
              type="password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Tối thiểu 8 ký tự"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="reset-new-password-input"
            />
          </div>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading || !token.trim() || !newPassword}
        className="w-full h-11 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center justify-center space-x-2"
        data-testid="reset-submit-btn"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Đang đổi mật khẩu...</span>
          </>
        ) : (
          <span>Cập nhật mật khẩu</span>
        )}
      </button>
    </form>
  );
}
