import React, { useState, useEffect, useCallback } from 'react';
import {
  User,
  Globe,
  Clock,
  Target,
  Flame,
  AlertTriangle,
  RefreshCw,
  CheckCircle2,
  ShieldCheck,
} from 'lucide-react';
import { getProfile, updateProfile } from '../../api/profile.js';

export function ProfilePreferencesForm() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fetchError, setFetchError] = useState(null);
  const [serverError, setServerError] = useState(null);
  const [conflictError, setConflictError] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  const [profileVersion, setProfileVersion] = useState(0);
  const [entitlement, setEntitlement] = useState(null);

  const [formData, setFormData] = useState({
    displayName: '',
    nativeLanguageCode: 'vi',
    interfaceLocale: 'vi-VN',
    timeZone: 'Asia/Ho_Chi_Minh',
    targetHskLevel: 1,
    dailyGoalMinutes: 30,
  });

  const [fieldErrors, setFieldErrors] = useState({});

  const loadData = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    setServerError(null);
    setConflictError(false);
    setSuccessMessage(null);

    try {
      const res = await getProfile();
      if (res && res.success && res.data) {
        const { profile, entitlement: entData } = res.data;
        setProfileVersion(profile.profileVersion ?? 0);
        setEntitlement(entData || null);
        setFormData({
          displayName: profile.displayName || '',
          nativeLanguageCode: profile.nativeLanguageCode || 'vi',
          interfaceLocale: profile.interfaceLocale || 'vi-VN',
          timeZone: profile.timeZone || 'Asia/Ho_Chi_Minh',
          targetHskLevel: profile.targetHskLevel ?? 1,
          dailyGoalMinutes: profile.dailyGoalMinutes ?? 30,
        });
      } else {
        setFetchError('Không thể tải thông tin hồ sơ.');
      }
    } catch (err) {
      setFetchError(err?.error?.message || 'Có lỗi xảy ra khi lấy dữ liệu hồ sơ.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const validate = () => {
    const errors = {};

    if (!formData.displayName || formData.displayName.trim().length < 1) {
      errors.displayName = 'Tên hiển thị không được để trống';
    } else if (formData.displayName.length > 120) {
      errors.displayName = 'Tên hiển thị không được vượt quá 120 ký tự';
    }

    const hsk = Number(formData.targetHskLevel);
    if (isNaN(hsk) || hsk < 1 || hsk > 6) {
      errors.targetHskLevel = 'Mục tiêu HSK phải từ 1 đến 6';
    }

    const goal = Number(formData.dailyGoalMinutes);
    if (isNaN(goal) || goal < 1 || goal > 240) {
      errors.dailyGoalMinutes = 'Mục tiêu hàng ngày phải từ 1 đến 240 phút';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSuccessMessage(null);
    setServerError(null);
    setConflictError(false);

    if (!validate()) return;

    setSubmitting(true);
    try {
      const payload = {
        displayName: formData.displayName.trim(),
        nativeLanguageCode: formData.nativeLanguageCode,
        interfaceLocale: formData.interfaceLocale,
        timeZone: formData.timeZone,
        targetHskLevel: Number(formData.targetHskLevel),
        dailyGoalMinutes: Number(formData.dailyGoalMinutes),
        expectedProfileVersion: profileVersion,
      };

      const res = await updateProfile(payload);
      if (res && res.success && res.data) {
        const { profile, entitlement: entData } = res.data;
        setProfileVersion(profile.profileVersion);
        if (entData) setEntitlement(entData);
        setFormData({
          displayName: profile.displayName || '',
          nativeLanguageCode: profile.nativeLanguageCode || 'vi',
          interfaceLocale: profile.interfaceLocale || 'vi-VN',
          timeZone: profile.timeZone || 'Asia/Ho_Chi_Minh',
          targetHskLevel: profile.targetHskLevel ?? 1,
          dailyGoalMinutes: profile.dailyGoalMinutes ?? 30,
        });
        setSuccessMessage('Cập nhật tùy chọn hồ sơ thành công!');
      }
    } catch (err) {
      const errCode = err?.error?.code;
      const status = err?.status;

      if (errCode === 'STATE_CONFLICT' || status === 409) {
        setConflictError(true);
      } else if (errCode === 'VALIDATION_ERROR') {
        setServerError(err?.error?.message || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.');
      } else {
        setServerError(
          err?.error?.message || 'Không thể lưu cài đặt. Vui lòng thử lại sau.'
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 bg-card border border-border rounded-2xl flex flex-col items-center justify-center space-y-4">
        <RefreshCw className="w-8 h-8 text-primary animate-spin" />
        <p className="text-sm font-medium text-muted-foreground">Đang tải tùy chọn hồ sơ...</p>
      </div>
    );
  }

  if (fetchError) {
    return (
      <div
        className="p-6 bg-destructive/10 border border-destructive/30 rounded-2xl space-y-4"
        data-testid="profile-error-alert"
      >
        <div className="flex items-center space-x-3 text-destructive font-bold">
          <AlertTriangle className="w-6 h-6 shrink-0" />
          <span>Lỗi tải dữ liệu</span>
        </div>
        <p className="text-sm text-foreground/90">{fetchError}</p>
        <button
          type="button"
          onClick={loadData}
          className="h-11 px-4 bg-primary text-primary-foreground font-bold text-sm rounded-xl flex items-center space-x-2 transition-all hover:opacity-90 active:scale-95"
          data-testid="profile-reload-btn"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Tải lại dữ liệu</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* State Conflict Alert (FR-003 / Scenario 3) */}
      {conflictError && (
        <div
          className="p-5 bg-amber-500/10 border border-amber-500/40 rounded-2xl space-y-3"
          data-testid="profile-conflict-alert"
          role="alert"
        >
          <div className="flex items-start space-x-3">
            <AlertTriangle className="w-6 h-6 text-amber-500 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="font-bold text-amber-600 dark:text-amber-400">
                Xung đột phiên bản hồ sơ
              </h4>
              <p className="text-sm text-foreground/90">
                Hồ sơ của bạn đã bị thay đổi trên thiết bị khác từ lúc màn hình này được tải. Vui lòng tải lại trước khi thực hiện lưu mới.
              </p>
            </div>
          </div>
          <div className="pt-1 flex justify-end">
            <button
              type="button"
              onClick={loadData}
              className="h-11 px-4 bg-amber-500 text-white font-bold text-sm rounded-xl flex items-center space-x-2 transition-all hover:bg-amber-600 active:scale-95 shadow-sm"
              data-testid="profile-reload-btn"
            >
              <RefreshCw className="w-4 h-4 animate-spin-once" />
              <span>Tải lại hồ sơ ngay</span>
            </button>
          </div>
        </div>
      )}

      {/* Success Notification */}
      {successMessage && (
        <div
          className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl flex items-center space-x-3 text-emerald-600 dark:text-emerald-400 font-semibold text-sm"
          data-testid="profile-success-alert"
        >
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Server Generic Error */}
      {serverError && (
        <div
          className="p-4 bg-destructive/10 border border-destructive/30 rounded-2xl flex items-center space-x-3 text-destructive font-semibold text-sm"
          data-testid="profile-error-alert"
        >
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span>{serverError}</span>
        </div>
      )}

      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className="p-6 bg-card border border-border rounded-2xl shadow-sm space-y-6"
        data-testid="profile-preferences-form"
      >
        <div className="flex items-center justify-between border-b border-border/60 pb-4">
          <div>
            <h3 className="text-lg font-bold text-foreground">Tùy chọn cá nhân</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              Cập nhật ngôn ngữ, timezone và mục tiêu học tập của bạn
            </p>
          </div>
          <div className="text-xs font-mono px-3 py-1 bg-secondary rounded-full border border-border text-muted-foreground">
            Phiên bản: v{profileVersion}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Display Name */}
          <div className="space-y-2 md:col-span-2">
            <label
              htmlFor="displayName"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <User className="w-4 h-4 text-primary" />
              <span>Tên hiển thị</span>
            </label>
            <input
              id="displayName"
              name="displayName"
              type="text"
              value={formData.displayName}
              onChange={handleChange}
              placeholder="Nhập tên hiển thị..."
              className={`w-full h-11 px-4 bg-background border ${
                fieldErrors.displayName ? 'border-destructive' : 'border-border'
              } rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all`}
              data-testid="profile-display-name-input"
            />
            {fieldErrors.displayName && (
              <p className="text-xs text-destructive font-medium">{fieldErrors.displayName}</p>
            )}
          </div>

          {/* Native Language */}
          <div className="space-y-2">
            <label
              htmlFor="nativeLanguageCode"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <Globe className="w-4 h-4 text-primary" />
              <span>Ngôn ngữ mẹ đẻ</span>
            </label>
            <select
              id="nativeLanguageCode"
              name="nativeLanguageCode"
              value={formData.nativeLanguageCode}
              onChange={handleChange}
              className="w-full h-11 px-4 bg-background border border-border rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
              data-testid="profile-native-language-select"
            >
              <option value="vi">Tiếng Việt (vi)</option>
              <option value="en">English (en)</option>
              <option value="zh">Tiếng Trung (zh)</option>
              <option value="ja">Tiếng Nhật (ja)</option>
              <option value="ko">Tiếng Hàn (ko)</option>
            </select>
          </div>

          {/* Interface Locale */}
          <div className="space-y-2">
            <label
              htmlFor="interfaceLocale"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <Globe className="w-4 h-4 text-primary" />
              <span>Giao diện ứng dụng</span>
            </label>
            <select
              id="interfaceLocale"
              name="interfaceLocale"
              value={formData.interfaceLocale}
              onChange={handleChange}
              className="w-full h-11 px-4 bg-background border border-border rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
              data-testid="profile-interface-locale-select"
            >
              <option value="vi-VN">Tiếng Việt (vi-VN)</option>
              <option value="en-US">English (en-US)</option>
              <option value="zh-CN">Tiếng Trung (zh-CN)</option>
            </select>
          </div>

          {/* TimeZone */}
          <div className="space-y-2">
            <label
              htmlFor="timeZone"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <Clock className="w-4 h-4 text-primary" />
              <span>Múi giờ</span>
            </label>
            <select
              id="timeZone"
              name="timeZone"
              value={formData.timeZone}
              onChange={handleChange}
              className="w-full h-11 px-4 bg-background border border-border rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
              data-testid="profile-timezone-select"
            >
              <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh (GMT+7)</option>
              <option value="Asia/Shanghai">Asia/Shanghai (GMT+8)</option>
              <option value="Asia/Tokyo">Asia/Tokyo (GMT+9)</option>
              <option value="UTC">UTC (Coordinated Universal Time)</option>
              <option value="America/New_York">America/New_York (EST)</option>
            </select>
          </div>

          {/* Target HSK Level */}
          <div className="space-y-2">
            <label
              htmlFor="targetHskLevel"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <Target className="w-4 h-4 text-primary" />
              <span>Mục tiêu HSK (1 - 6)</span>
            </label>
            <select
              id="targetHskLevel"
              name="targetHskLevel"
              value={formData.targetHskLevel}
              onChange={handleChange}
              className={`w-full h-11 px-4 bg-background border ${
                fieldErrors.targetHskLevel ? 'border-destructive' : 'border-border'
              } rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all`}
              data-testid="profile-hsk-select"
            >
              {[1, 2, 3, 4, 5, 6].map((lvl) => (
                <option key={lvl} value={lvl}>
                  HSK Level {lvl}
                </option>
              ))}
            </select>
            {fieldErrors.targetHskLevel && (
              <p className="text-xs text-destructive font-medium">{fieldErrors.targetHskLevel}</p>
            )}
          </div>

          {/* Daily Goal Minutes */}
          <div className="space-y-2 md:col-span-2">
            <label
              htmlFor="dailyGoalMinutes"
              className="block text-sm font-semibold text-foreground flex items-center space-x-2"
            >
              <Flame className="w-4 h-4 text-primary" />
              <span>Mục tiêu học mỗi ngày (Phút)</span>
            </label>
            <div className="flex items-center space-x-3">
              <input
                id="dailyGoalMinutes"
                name="dailyGoalMinutes"
                type="number"
                min="1"
                max="240"
                value={formData.dailyGoalMinutes}
                onChange={handleChange}
                className={`w-full h-11 px-4 bg-background border ${
                  fieldErrors.dailyGoalMinutes ? 'border-destructive' : 'border-border'
                } rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all`}
                data-testid="profile-daily-goal-input"
              />
              <div className="flex space-x-1 shrink-0">
                {[15, 30, 45, 60].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => {
                      setFormData((prev) => ({ ...prev, dailyGoalMinutes: preset }));
                      if (fieldErrors.dailyGoalMinutes) {
                        setFieldErrors((prev) => ({ ...prev, dailyGoalMinutes: null }));
                      }
                    }}
                    className={`h-11 px-3 text-xs font-bold rounded-xl border transition-all ${
                      Number(formData.dailyGoalMinutes) === preset
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'bg-secondary text-muted-foreground border-border hover:text-foreground'
                    }`}
                  >
                    {preset} phút
                  </button>
                ))}
              </div>
            </div>
            {fieldErrors.dailyGoalMinutes && (
              <p className="text-xs text-destructive font-medium">{fieldErrors.dailyGoalMinutes}</p>
            )}
          </div>
        </div>

        {/* Submit Actions */}
        <div className="pt-4 border-t border-border/60 flex items-center justify-end space-x-3">
          <button
            type="submit"
            disabled={submitting}
            className="h-11 px-6 bg-primary text-primary-foreground font-bold text-sm rounded-xl flex items-center space-x-2 transition-all hover:opacity-90 active:scale-95 disabled:opacity-50 shadow-md shadow-primary/20"
            data-testid="profile-submit-btn"
          >
            {submitting ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Đang lưu...</span>
              </>
            ) : (
              <>
                <CheckCircle2 className="w-4 h-4" />
                <span>Lưu thay đổi</span>
              </>
            )}
          </button>
        </div>
      </form>

      {/* Read-Only Entitlement Summary Fragment (F03 Safe Fragment) */}
      {entitlement && (
        <div
          className="p-6 bg-card border border-border rounded-2xl shadow-sm space-y-3"
          data-testid="entitlement-summary-card"
        >
          <div className="flex items-center space-x-2 text-foreground font-bold">
            <ShieldCheck className="w-5 h-5 text-primary" />
            <span>Gói đăng ký & Quyền sử dụng</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
            <div className="p-3 bg-secondary/50 rounded-xl border border-border/60">
              <span className="text-xs text-muted-foreground font-medium block">Gói hiện tại</span>
              <span className="text-sm font-bold text-primary tracking-wide">
                {entitlement.planCode || 'FREE'}
              </span>
            </div>
            <div className="p-3 bg-secondary/50 rounded-xl border border-border/60">
              <span className="text-xs text-muted-foreground font-medium block">Lượt học còn lại</span>
              <span className="text-sm font-bold text-foreground">
                {entitlement.remainingUnits ?? 30} / {entitlement.allowanceLimit ?? 30}
              </span>
            </div>
            <div className="p-3 bg-secondary/50 rounded-xl border border-border/60">
              <span className="text-xs text-muted-foreground font-medium block">Chu kỳ đến</span>
              <span className="text-sm font-semibold text-foreground">
                {entitlement.cycleEndAt
                  ? new Date(entitlement.cycleEndAt).toLocaleDateString('vi-VN')
                  : 'Không giới hạn'}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
