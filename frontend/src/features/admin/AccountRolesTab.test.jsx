import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { AccountRolesTab } from './AccountRolesTab.jsx';
import { adminUsersApi } from '../../api/adminUsers.js';

jest.mock('../../api/adminUsers.js', () => ({
  adminUsersApi: {
    listManagedUsers: jest.fn(),
    getAccountRoleProjection: jest.fn(),
    grantAdminRole: jest.fn(),
    revokeAdminRole: jest.fn(),
    lockAccount: jest.fn(),
    unlockAccount: jest.fn(),
  },
}));

describe('F01 User Management UI security and contract tests', () => {
  const targetUUID = '550e8400-e29b-41d4-a716-446655440000';

  const directoryResponse = {
    success: true,
    data: {
      items: [{ userId: targetUUID, accountName: 'Học viên Demo', accountState: 'ACTIVE', roles: ['ADMIN'] }],
      page: 0,
      size: 20,
      totalItems: 21,
      totalPages: 2,
    },
  };

  const projection = {
    success: true,
    data: { userId: targetUUID, accountName: 'Học viên Demo', roles: ['ADMIN'], accessState: 'ACTIVE' },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    adminUsersApi.listManagedUsers.mockResolvedValue(directoryResponse);
  });

  async function selectTarget() {
    adminUsersApi.getAccountRoleProjection.mockResolvedValue(projection);
    fireEvent.click(await screen.findByTestId(`manage-user-${targetUUID}`));
    await screen.findByTestId('projection-card');
  }

  test('loads a paginated safe user list without an Exact-ID input or private fields', async () => {
    render(<AccountRolesTab />);

    expect(await screen.findByTestId('user-directory-list')).toBeInTheDocument();
    expect(adminUsersApi.listManagedUsers).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(screen.getByTestId(`user-row-${targetUUID}`)).toHaveTextContent(targetUUID);
    expect(screen.getByTestId(`user-row-${targetUUID}`)).toHaveTextContent('Học viên Demo');
    expect(screen.getByTestId(`user-row-${targetUUID}`)).toHaveTextContent('ACTIVE');
    expect(screen.getByTestId(`user-row-${targetUUID}`)).toHaveTextContent('ADMIN');
    expect(screen.queryByTestId('user-id-input')).not.toBeInTheDocument();
    expect(screen.queryByPlaceholderText(/email/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/hồ sơ|profile/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('next-page-btn'));
    await waitFor(() => expect(adminUsersApi.listManagedUsers).toHaveBeenLastCalledWith({ page: 1, size: 20 }));
  });

  test('enforces reason and note constraints on the selected user lock dialog', async () => {
    render(<AccountRolesTab />);
    await selectTarget();

    expect(screen.getByTestId('selected-account-name')).toHaveTextContent('Học viên Demo');

    fireEvent.click(screen.getByTestId('lock-account-btn'));
    fireEvent.change(screen.getByTestId('reason-select'), { target: { value: 'OTHER' } });
    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    expect(await screen.findByTestId('admin-error')).toHaveTextContent('OTHER');

    adminUsersApi.lockAccount.mockResolvedValueOnce({
      success: true,
      data: { userId: targetUUID, roles: [], accessState: 'LOCKED' },
    });
    fireEvent.change(screen.getByTestId('note-input'), { target: { value: 'Security review' } });
    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    await waitFor(() => expect(adminUsersApi.lockAccount).toHaveBeenCalledWith(targetUUID, {
      reason: 'OTHER',
      note: 'Security review',
    }));
    expect(screen.getByTestId('access-state-badge')).toHaveTextContent('LOCKED');
  });

  test('handles a selected-user 409 conflict and reloads only that projection', async () => {
    render(<AccountRolesTab />);
    await selectTarget();

    adminUsersApi.revokeAdminRole.mockRejectedValueOnce({
      error: { code: 'STATE_CONFLICT', message: 'The protected transition is not allowed.' },
    });
    fireEvent.click(screen.getByTestId('revoke-admin-btn'));
    fireEvent.click(screen.getByTestId('confirm-dialog-btn'));

    expect(await screen.findByTestId('admin-error')).toHaveTextContent('STATE_CONFLICT');
    expect(adminUsersApi.getAccountRoleProjection).toHaveBeenCalledWith(targetUUID);
  });
});
