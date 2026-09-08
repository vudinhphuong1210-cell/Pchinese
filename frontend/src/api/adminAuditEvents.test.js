import { listAdminAuditEvents } from './adminAuditEvents.js';
import { httpClient } from './http.js';

jest.mock('./http.js', () => ({
  httpClient: jest.fn(),
}));

describe('admin audit activity API', () => {
  beforeEach(() => jest.clearAllMocks());

  test('uses the ADMIN-only paginated system-audit endpoint', async () => {
    const response = { success: true, data: { items: [], page: 1, size: 10, totalItems: 0, totalPages: 0 } };
    httpClient.mockResolvedValueOnce(response);

    await expect(listAdminAuditEvents({ page: 1, size: 10 })).resolves.toEqual(response);
    expect(httpClient).toHaveBeenCalledWith('/api/v1/admin/audit-events?page=1&size=10', { method: 'GET' });
  });
});
