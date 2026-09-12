import { adminAiApi } from './adminAi.js';
import { httpClient } from './http.js';

jest.mock('./http.js', () => ({ httpClient: jest.fn() }));

describe('adminAiApi', () => {
  beforeEach(() => jest.clearAllMocks());

  test('uses the bounded aggregate report contract', async () => {
    httpClient.mockResolvedValue({ success: true, data: { buckets: [] } });
    await adminAiApi.getUsageReport({ fromInclusive: '2026-09-01T00:00:00.000Z', toExclusive: '2026-09-02T00:00:00.000Z', planCode: 'FREE' });
    expect(httpClient).toHaveBeenCalledWith(expect.stringContaining('/admin/ai/usage-report?'));
    expect(httpClient.mock.calls[0][0]).toContain('planCode=FREE');
  });

  test('sends acknowledgement only to the aggregate alert endpoint', async () => {
    httpClient.mockResolvedValue({ success: true, data: {} });
    await adminAiApi.acknowledgeAlert('alert-id', { expectedVersion: 2, note: 'Investigating' });
    expect(httpClient).toHaveBeenCalledWith(expect.stringContaining('/admin/ai/alerts/alert-id/acknowledgements'), expect.objectContaining({ method: 'POST' }));
  });

  test('updates a monitoring rule through the versioned aggregate endpoint', async () => {
    httpClient.mockResolvedValue({ success: true, data: {} });
    await adminAiApi.updateMonitoringRule('rule-id', { expectedVersion: 3, metric: 'REQUEST_VOLUME' });
    expect(httpClient).toHaveBeenCalledWith(expect.stringContaining('/admin/ai/monitoring-rules/rule-id'),
      expect.objectContaining({ method: 'PATCH', body: expect.objectContaining({ expectedVersion: 3 }) }));
  });
});
