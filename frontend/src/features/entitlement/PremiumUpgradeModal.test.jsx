import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { PremiumUpgradeModal } from './PremiumUpgradeModal.jsx';
import { getProfile } from '../../api/profile.js';

jest.mock('../../api/profile.js', () => ({
  getProfile: jest.fn(),
}));

describe('PremiumUpgradeModal Component', () => {
  beforeEach(() => {
    getProfile.mockResolvedValue({
      success: true,
      data: {
        entitlement: {
          planCode: 'FREE',
          allowanceLimit: 30,
          usedUnits: 0,
          remainingUnits: 30,
          cycleStartAt: '2026-09-01T00:00:00Z',
          cycleEndAt: '2026-10-01T00:00:00Z',
        },
      },
    });
  });

  test('returns null when isOpen is false', () => {
    const { container } = render(
      <PremiumUpgradeModal isOpen={false} onClose={jest.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  test('renders modal title, free plan scope, coming soon premium status and MVP notice when isOpen is true', async () => {
    render(<PremiumUpgradeModal isOpen={true} onClose={jest.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('premium-upgrade-modal')).toBeInTheDocument();
    });
    expect(screen.getByText('Quản lý Gói & Nâng cấp Premium')).toBeInTheDocument();
    expect(screen.getByText('Gói Miễn Phí (Free)')).toBeInTheDocument();
    expect(screen.getByText('Sắp ra mắt')).toBeInTheDocument();
    expect(screen.getByTestId('mvp-notice-banner')).toBeInTheDocument();
  });

  test('calls onClose when close button or confirm button is clicked', async () => {
    const onCloseMock = jest.fn();
    render(<PremiumUpgradeModal isOpen={true} onClose={onCloseMock} />);

    await waitFor(() => {
      expect(screen.getByTestId('premium-upgrade-modal')).toBeInTheDocument();
    });

    const closeBtn = screen.getByTestId('close-premium-modal-btn');
    fireEvent.click(closeBtn);
    expect(onCloseMock).toHaveBeenCalledTimes(1);

    const confirmBtn = screen.getByTestId('confirm-premium-modal-btn');
    fireEvent.click(confirmBtn);
    expect(onCloseMock).toHaveBeenCalledTimes(2);
  });

  test('calls onClose when Escape key is pressed', async () => {
    const onCloseMock = jest.fn();
    render(<PremiumUpgradeModal isOpen={true} onClose={onCloseMock} />);

    await waitFor(() => {
      expect(screen.getByTestId('premium-upgrade-modal')).toBeInTheDocument();
    });

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });
});
