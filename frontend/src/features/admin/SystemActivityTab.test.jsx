import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SystemActivityTab } from './SystemActivityTab.jsx';
import { listAdminAuditEvents } from '../../api/adminAuditEvents.js';

jest.mock('../../api/adminAuditEvents.js', () => ({
  listAdminAuditEvents: jest.fn(),
}));

describe('SystemActivityTab', () => {
  test('shows ADMIN the safe system event labels, account names and time only', async () => {
    listAdminAuditEvents.mockResolvedValueOnce({
      success: true,
      data: {
        items: [{
          eventType: 'PROFILE_PREFERENCES_UPDATED',
          actorAccountName: 'Học viên A',
          targetAccountName: 'Học viên A',
          occurredAt: '2026-09-09T08:00:00Z',
          category: 'PROFILE_CHANGE',
          outcomeCode: 'SUCCESS',
          reasonCode: 'SECURITY',
          changedFieldCodes: ['DISPLAY_NAME'],
          sessionId: 'session-private',
          correlationId: 'correlation-private',
        }],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
      },
    });

    render(<SystemActivityTab />);

    expect(await screen.findByTestId('system-activity-list')).toBeInTheDocument();
    expect(screen.getByText('Cập nhật tùy chọn hồ sơ')).toBeInTheDocument();
    expect(screen.getAllByText('Học viên A')).toHaveLength(2);
    expect(screen.queryByText(/PROFILE_CHANGE|SUCCESS|SECURITY|DISPLAY_NAME|session-private|correlation-private|actorUserId/i)).not.toBeInTheDocument();
  });

  test('uses server pagination when ADMIN requests the next page', async () => {
    listAdminAuditEvents.mockResolvedValueOnce({
      success: true,
      data: { items: [], page: 0, size: 20, totalItems: 21, totalPages: 2 },
    }).mockResolvedValueOnce({
      success: true,
      data: { items: [], page: 1, size: 20, totalItems: 21, totalPages: 2 },
    });

    render(<SystemActivityTab />);

    await screen.findByTestId('system-activity-empty');
    fireEvent.click(screen.getByTestId('system-activity-next-page'));
    await waitFor(() => expect(listAdminAuditEvents).toHaveBeenLastCalledWith({ page: 1, size: 20 }));
  });
});
