import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { AccountRolesTab } from './AccountRolesTab.jsx';
import { adminUsersApi } from '../../api/adminUsers.js';

jest.mock('../../api/adminUsers.js', () => ({
  adminUsersApi: {
    getAccountRoleProjection: jest.fn(),
    grantAdminRole: jest.fn(),
    revokeAdminRole: jest.fn(),
    lockAccount: jest.fn(),
    unlockAccount: jest.fn(),
  },
}));

describe('F01 Admin Account/Roles UI Security & Contract Tests (T021)', () => {
  const targetUUID = '550e8400-e29b-41d4-a716-446655440000';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('Guards against directory/private-learning-data paths by requiring exact UUID input', async () => {
    adminUsersApi.getAccountRoleProjection.mockResolvedValueOnce({
      success: true,
      data: {
        userId: targetUUID,
        roles: ['ADMIN'],
        accessState: 'ACTIVE',
      },
    });

    render(<AccountRolesTab />);

    // Verify there is NO email search input, NO student name field, NO directory autocomplete
    expect(screen.queryByPlaceholderText(/email/i)).toBeNull();
    expect(screen.queryByPlaceholderText(/tên học viên/i)).toBeNull();
    expect(screen.queryByTestId('user-directory-list')).toBeNull();

    // Fill in exact UUID
    fireEvent.change(screen.getByTestId('user-id-input'), {
      target: { value: targetUUID },
    });

    fireEvent.click(screen.getByTestId('lookup-btn'));

    await waitFor(() => expect(adminUsersApi.getAccountRoleProjection).toHaveBeenCalledWith(targetUUID));

    expect(screen.getByTestId('projection-card')).toBeInTheDocument();
    expect(screen.getByTestId('access-state-badge')).toHaveTextContent('ACTIVE');
    expect(screen.getByTestId('role-badge-ADMIN')).toBeInTheDocument();
  });

  test('Enforces reason and note constraints on Lock account dialog', async () => {
    adminUsersApi.getAccountRoleProjection.mockResolvedValueOnce({
      success: true,
      data: {
        userId: targetUUID,
        roles: [],
        accessState: 'ACTIVE',
      },
    });

    render(<AccountRolesTab />);

    fireEvent.change(screen.getByTestId('user-id-input'), {
      target: { value: targetUUID },
    });
    fireEvent.click(screen.getByTestId('lookup-btn'));

    await screen.findByTestId('projection-card');

    // Click Lock Account
    fireEvent.click(screen.getByTestId('lock-account-btn'));

    // Select OTHER reason
    fireEvent.change(screen.getByTestId('reason-select'), {
      target: { value: 'OTHER' },
    });

    // Try to confirm without note
    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    expect(await screen.findByTestId('admin-error')).toHaveTextContent(
      'Lý do OTHER yêu cầu phải có ghi chú chi tiết.'
    );

    // Provide note and submit
    adminUsersApi.lockAccount.mockResolvedValueOnce({
      success: true,
      data: {
        userId: targetUUID,
        roles: [],
        accessState: 'LOCKED',
      },
    });

    fireEvent.change(screen.getByTestId('note-input'), {
      target: { value: 'Khóa tài khoản do nghi ngờ vi phạm an toàn.' },
    });

    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    await waitFor(() => {
      expect(adminUsersApi.lockAccount).toHaveBeenCalledWith(targetUUID, {
        reason: 'OTHER',
        note: 'Khóa tài khoản do nghi ngờ vi phạm an toàn.',
      });
    });

    expect(screen.getByTestId('access-state-badge')).toHaveTextContent('LOCKED');
  });

  test('Handles 409 STATE_CONFLICT cleanly and reloads projection', async () => {
    adminUsersApi.getAccountRoleProjection.mockResolvedValue({
      success: true,
      data: {
        userId: targetUUID,
        roles: ['ADMIN'],
        accessState: 'ACTIVE',
      },
    });

    adminUsersApi.revokeAdminRole.mockRejectedValueOnce({
      error: {
        code: 'STATE_CONFLICT',
        message: 'Không thể thu hồi quyền ADMIN từ ADMIN cuối cùng trong hệ thống.',
      },
    });

    render(<AccountRolesTab />);

    fireEvent.change(screen.getByTestId('user-id-input'), {
      target: { value: targetUUID },
    });
    fireEvent.click(screen.getByTestId('lookup-btn'));

    await screen.findByTestId('projection-card');

    // Attempt revoke admin
    fireEvent.click(screen.getByTestId('revoke-admin-btn'));
    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    expect(await screen.findByTestId('admin-error')).toHaveTextContent('STATE_CONFLICT');
  });
});
