import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { EntitlementPage } from './EntitlementPage.jsx';
import { getProfile } from '../../api/profile.js';

jest.mock('../../api/profile.js', () => ({
  getProfile: jest.fn(),
}));

describe('EntitlementPage Component', () => {
  const mockEntitlementData = {
    planCode: 'FREE',
    allowanceLimit: 30,
    usedUnits: 10,
    remainingUnits: 20,
    cycleStartAt: '2026-09-01T00:00:00Z',
    cycleEndAt: '2026-10-01T00:00:00Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading spinner then displays entitlement page content and comparison table', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: {
        profile: { displayName: 'Học viên PChinese' },
        entitlement: mockEntitlementData,
      },
    });

    render(<EntitlementPage />);

    expect(screen.getByTestId('entitlement-loading')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('entitlement-page')).toBeInTheDocument();
    });

    expect(screen.getByText('Quản lý Gói & Hạn mức AI (Entitlement)')).toBeInTheDocument();
    expect(screen.getByTestId('entitlement-card')).toBeInTheDocument();
    expect(screen.getByText('So sánh tính năng các Gói')).toBeInTheDocument();
    expect(screen.getByText('Câu hỏi thường gặp về AI Allowance')).toBeInTheDocument();
  });

  test('opens upgrade modal when clicking upgrade button', async () => {
    getProfile.mockResolvedValueOnce({
      success: true,
      data: {
        profile: { displayName: 'Học viên PChinese' },
        entitlement: mockEntitlementData,
      },
    });

    render(<EntitlementPage />);

    await waitFor(() => {
      expect(screen.getByTestId('entitlement-page')).toBeInTheDocument();
    });

    const upgradeBtns = screen.getAllByRole('button', { name: /Nâng cấp Premium/i });
    fireEvent.click(upgradeBtns[0]);

    await waitFor(() => {
      expect(screen.getByTestId('premium-upgrade-modal')).toBeInTheDocument();
    });
  });

  test('renders error state on getProfile failure', async () => {
    getProfile.mockRejectedValueOnce({
      error: { message: 'Lỗi tải thông tin entitlement' },
    });

    render(<EntitlementPage />);

    await waitFor(() => {
      expect(screen.getByTestId('entitlement-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Lỗi tải thông tin entitlement')).toBeInTheDocument();
  });
});
