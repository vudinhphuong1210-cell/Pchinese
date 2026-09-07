import React, { useState } from 'react';
import { authApi } from '../../api/auth.js';
import { Mail, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export function ForgotPasswordForm({ onNavigateToLogin, onNavigateToReset }) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!email) return;

    setLoading(true);

    try {
      await authApi.requestPasswordReset({ email });
      setSubmitted(true);
    } catch (err) {
      setError(err?.error || { message: 'Đã có lỗi xảy ra. Vui lòng thử lại.' });
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg" data-testid="forgot-success">
        <div className="flex items-center space-x-3 text-success">
          <CheckCircle2 className="w-8 h-8 shrink-0" />
          <h2 className="text-xl font-bold">Yêu cầu đã được nhận</h2>
        </div>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Nếu địa chỉ email <strong className="text-foreground">{email}</strong> hợp lệ, chúng tôi đã gửi mã đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.
        </p>
        <div className="flex flex-col gap-3 pt-2">
          <button
            type="button"
            onClick={() => onNavigateToReset && onNavigateToReset()}
            className="w-full h-11 bg-primary text-primary-foreground font-semibold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none"
            data-testid="go-to-reset-btn"
          >
            Nhập mã đặt lại mật khẩu
          </button>
          <button
            type="button"
            onClick={() => onNavigateToLogin && onNavigateToLogin()}
            className="w-full h-11 bg-secondary text-secondary-foreground font-medium rounded-md hover:bg-accent transition-colors"
          >
            Quay lại Đăng nhập
          </button>
        </div>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg"
      data-testid="forgot-form"
    >
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Quên mật khẩu</h1>
        <p className="text-sm text-muted-foreground">Nhập email đã đăng ký để nhận liên kết/mã khôi phục mật khẩu.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="forgot-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{error.message}</span>
        </div>
      )}

      <div className="space-y-1.5">
        <label htmlFor="forgot-email" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Địa chỉ Email
        </label>
        <div className="relative">
          <Mail className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
          <input
            id="forgot-email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="hocvien@pchinese.net"
            className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            data-testid="forgot-email-input"
          />
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full h-11 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center justify-center space-x-2"
        data-testid="forgot-submit-btn"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Đang gửi yêu cầu...</span>
          </>
        ) : (
          <span>Gửi yêu cầu khôi phục</span>
        )}
      </button>

      <div className="text-center text-sm text-muted-foreground pt-2 border-t border-border">
        <button
          type="button"
          onClick={() => onNavigateToLogin && onNavigateToLogin()}
          className="text-primary hover:underline font-semibold"
        >
          Quay lại Đăng nhập
        </button>
      </div>
    </form>
  );
}
