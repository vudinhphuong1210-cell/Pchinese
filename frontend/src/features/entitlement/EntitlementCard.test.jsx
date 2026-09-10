import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { EntitlementCard } from './EntitlementCard.jsx';

describe('EntitlementCard Component', () => {
  const mockNormalEntitlement = {
    planCode: 'FREE',
    allowanceLimit: 30,
    usedUnits: 5,
    remainingUnits: 25,
    cycleStartAt: '2026-09-01T00:00:00Z',
    cycleEndAt: '2026-10-01T00:00:00Z',
  };

  const mockLowEntitlement = {
    planCode: 'FREE',
    allowanceLimit: 30,
    usedUnits: 27,
    remainingUnits: 3,
    cycleStartAt: '2026-09-01T00:00:00Z',
    cycleEndAt: '2026-10-01T00:00:00Z',
  };

  const mockExhaustedEntitlement = {
    planCode: 'FREE',
    allowanceLimit: 30,
    usedUnits: 30,
    remainingUnits: 0,
    cycleStartAt: '2026-09-01T00:00:00Z',
    cycleEndAt: '2026-10-01T00:00:00Z',
  };

  test('renders plan details, progress bar, and remaining quota count', () => {
    render(<EntitlementCard entitlement={mockNormalEntitlement} />);

    expect(screen.getByTestId('entitlement-card')).toBeInTheDocument();
    expect(screen.getByText('Gói hiện tại: FREE')).toBeInTheDocument();
    expect(screen.getByTestId('entitlement-status-pill')).toHaveTextContent('Đang hoạt động');
    expect(screen.getByTestId('quota-counter-text')).toHaveTextContent('25 / 30 lượt còn lại');
    expect(screen.getByText('Đã sử dụng: 5 lượt')).toBeInTheDocument();
  });

  test('displays low quota warning when remainingUnits <= 5', () => {
    render(<EntitlementCard entitlement={mockLowEntitlement} />);

    expect(screen.getByTestId('quota-low-alert')).toBeInTheDocument();
    expect(screen.getByTestId('quota-low-alert')).toHaveTextContent('Lượt AI của bạn sắp hết (còn 3 lượt)');
  });

  test('displays exhausted quota alert when remainingUnits === 0', () => {
    render(<EntitlementCard entitlement={mockExhaustedEntitlement} />);

    expect(screen.getByTestId('quota-exhausted-alert')).toBeInTheDocument();
    expect(screen.getByTestId('quota-exhausted-alert')).toHaveTextContent('Bạn đã sử dụng hết 30 lượt AI cho chu kỳ này');
  });

  test('triggers onOpenUpgradeModal callback when upgrade button is clicked', () => {
    const handleUpgradeMock = jest.fn();
    render(
      <EntitlementCard
        entitlement={mockNormalEntitlement}
        onOpenUpgradeModal={handleUpgradeMock}
      />
    );

    const upgradeBtn = screen.getByTestId('upgrade-premium-btn');
    expect(upgradeBtn).toBeInTheDocument();
    fireEvent.click(upgradeBtn);
    expect(handleUpgradeMock).toHaveBeenCalledTimes(1);
  });
});
