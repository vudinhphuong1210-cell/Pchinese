import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ProfilePreferencesForm } from './ProfilePreferencesForm.jsx';
import { getProfile, updateProfile } from '../../api/profile.js';

jest.mock('../../api/profile.js', () => ({
  getProfile: jest.fn(),
  updateProfile: jest.fn(),
}));

describe('ProfilePreferencesForm Component', () => {
  const mockInitialProfile = {
    displayName: 'Nguyễn Văn A',
    nativeLanguageCode: 'vi',
    interfaceLocale: 'vi-VN',
    timeZone: 'Asia/Ho_Chi_Minh',
    targetHskLevel: 3,
    dailyGoalMinutes: 45,
    profileVersion: 1,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading indicator initially then displays profile data', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: mockInitialProfile },
      meta: { correlationId: 'test-uuid' },
    });

    render(<ProfilePreferencesForm />);

    expect(screen.getByText('Đang tải tùy chọn hồ sơ...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('profile-preferences-form')).toBeInTheDocument();
    });

    expect(screen.getByTestId('profile-display-name-input')).toHaveValue('Nguyễn Văn A');
    expect(screen.getByTestId('profile-native-language-select')).toHaveValue('vi');
    expect(screen.getByTestId('profile-hsk-select')).toHaveValue('3');
    expect(screen.getByTestId('profile-daily-goal-input')).toHaveValue(45);
    expect(screen.queryByTestId('entitlement-summary-card')).not.toBeInTheDocument();
  });

  test('handles successful profile update with expectedProfileVersion', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: mockInitialProfile },
      meta: { correlationId: 'test-uuid' },
    });

    const updatedProfile = {
      ...mockInitialProfile,
      displayName: 'Nguyễn Văn B',
      profileVersion: 2,
    };

    updateProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: updatedProfile },
      meta: { correlationId: 'test-uuid' },
    });

    render(<ProfilePreferencesForm />);

    await waitFor(() => {
      expect(screen.getByTestId('profile-preferences-form')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('profile-display-name-input'), {
      target: { value: 'Nguyễn Văn B' },
    });

    fireEvent.click(screen.getByTestId('profile-submit-btn'));

    await waitFor(() => {
      expect(updateProfile).toHaveBeenCalledWith({
        displayName: 'Nguyễn Văn B',
        nativeLanguageCode: 'vi',
        interfaceLocale: 'vi-VN',
        timeZone: 'Asia/Ho_Chi_Minh',
        targetHskLevel: 3,
        dailyGoalMinutes: 45,
        expectedProfileVersion: 1,
      });
    });

    expect(screen.getByTestId('profile-success-alert')).toBeInTheDocument();
    expect(screen.getByText('Phiên bản: v2')).toBeInTheDocument();
  });

  test('displays conflict alert on 409 STATE_CONFLICT and supports profile reload', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: mockInitialProfile },
      meta: { correlationId: 'test-uuid' },
    });

    updateProfile.mockRejectedValueOnce({
      status: 409,
      error: { code: 'STATE_CONFLICT', message: 'Profile version mismatch' },
    });

    render(<ProfilePreferencesForm />);

    await waitFor(() => {
      expect(screen.getByTestId('profile-preferences-form')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('profile-submit-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('profile-conflict-alert')).toBeInTheDocument();
    });

    // Next getProfile call for reload
    const freshProfile = { ...mockInitialProfile, profileVersion: 2 };
    getProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: freshProfile },
      meta: { correlationId: 'test-uuid' },
    });

    fireEvent.click(screen.getByTestId('profile-reload-btn'));

    await waitFor(() => {
      expect(screen.queryByTestId('profile-conflict-alert')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Phiên bản: v2')).toBeInTheDocument();
  });

  test('shows field error when display name is empty', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: { profile: mockInitialProfile },
      meta: { correlationId: 'test-uuid' },
    });

    render(<ProfilePreferencesForm />);

    await waitFor(() => {
      expect(screen.getByTestId('profile-preferences-form')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('profile-display-name-input'), {
      target: { value: '' },
    });

    fireEvent.click(screen.getByTestId('profile-submit-btn'));

    expect(screen.getByText('Tên hiển thị không được để trống')).toBeInTheDocument();
    expect(updateProfile).not.toHaveBeenCalled();
  });
});
