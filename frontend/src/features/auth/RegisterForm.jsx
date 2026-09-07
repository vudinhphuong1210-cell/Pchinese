import React, { useState } from 'react';
import { authApi } from '../../api/auth.js';
import { Mail, Lock, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export function RegisterForm({ onNavigateToLogin, onNavigateToVerify }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!email || !password) {
      setError({ message: 'Vui lòng điền đầy đủ email và mật khẩu.' });
      return;
    }

    if (password.length < 8) {
      setError({ message: 'Mật khẩu phải có ít nhất 8 ký tự.' });
      return;
    }

    setLoading(true);

    try {
      await authApi.register({ email, password });
      setSubmitted(true);
    } catch (err) {
      setError(err?.error || { message: 'Đã có lỗi xảy ra. Vui lòng thử lại.' });
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="bg-card border border-border rounded-xl p-6 sm:p-8 space-y-6 max-w-md w-full shadow-lg" data-testid="register-success">
        <div className="flex items-center space-x-3 text-success">
          <CheckCircle2 className="w-8 h-8 shrink-0" />
          <h2 className="text-xl font-bold">Yêu cầu đã được tiếp nhận</h2>
        </div>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Nếu địa chỉ email <strong className="text-foreground">{email}</strong> hợp lệ và chưa được liên kết, chúng tôi đã gửi email hướng dẫn xác thực tài khoản. Vui lòng kiểm tra hộp thư.
        </p>
        <div className="flex flex-col gap-3 pt-2">
          <button
            type="button"
            onClick={() => onNavigateToVerify && onNavigateToVerify(email)}
            className="w-full h-11 bg-primary text-primary-foreground font-semibold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none"
            data-testid="go-to-verify-btn"
          >
            Nhập mã xác thực
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
      data-testid="register-form"
      noValidate
    >
      <div className="space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Đăng ký tài khoản</h1>
        <p className="text-sm text-muted-foreground">Bắt đầu hành trình luyện tiếng Trung chuyên sâu với P Chinese.</p>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-md p-3.5 text-sm flex items-start space-x-2.5" role="alert" data-testid="register-error">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{error.message}</span>
        </div>
      )}

      <div className="space-y-4">
        <div className="space-y-1.5">
          <label htmlFor="register-email" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Địa chỉ Email
          </label>
          <div className="relative">
            <Mail className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="register-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="hocvien@pchinese.net"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="register-email-input"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label htmlFor="register-password" className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
            Mật khẩu
          </label>
          <div className="relative">
            <Lock className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
            <input
              id="register-password"
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Tối thiểu 8 ký tự"
              className="w-full h-11 pl-10 pr-3.5 bg-secondary/50 border border-input rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              data-testid="register-password-input"
            />
          </div>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full h-11 bg-primary text-primary-foreground font-bold rounded-md hover:opacity-90 transition-opacity focus:ring-2 focus:ring-ring focus:outline-none disabled:opacity-50 flex items-center justify-center space-x-2"
        data-testid="register-submit-btn"
      >
        {loading ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Đang xử lý...</span>
          </>
        ) : (
          <span>Tạo tài khoản</span>
        )}
      </button>

      <div className="text-center text-sm text-muted-foreground pt-2 border-t border-border">
        Đã có tài khoản?{' '}
        <button
          type="button"
          onClick={() => onNavigateToLogin && onNavigateToLogin()}
          className="text-primary hover:underline font-semibold"
          data-testid="login-link"
        >
          Đăng nhập ngay
        </button>
      </div>
    </form>
  );
}
