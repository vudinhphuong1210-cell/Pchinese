import { getDueQueue, submitReview } from './review.js';
import { httpClient } from './http.js';

jest.mock('./http.js', () => ({
  httpClient: jest.fn(),
}));

describe('review API client', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('getDueQueue calls GET /api/v1/srs/due with limit', async () => {
    httpClient.mockResolvedValueOnce({
      success: true,
      data: { items: [], totalDueCount: 0 },
    });

    const result = await getDueQueue(15);
    expect(httpClient).toHaveBeenCalledWith(
      expect.stringContaining('/srs/due?limit=15')
    );
    expect(result.data.items).toEqual([]);
  });

  test('submitReview calls POST /api/v1/srs/review', async () => {
    httpClient.mockResolvedValueOnce({
      success: true,
      data: { nextStatus: 'REVIEW', idempotentReplay: false },
    });

    const result = await submitReview('sched-1', 'client-1', 'GOOD', 0);
    expect(httpClient).toHaveBeenCalledWith(
      expect.stringContaining('/srs/review'),
      expect.objectContaining({
        method: 'POST',
        body: {
          srsScheduleId: 'sched-1',
          clientReviewId: 'client-1',
          rating: 'GOOD',
          expectedScheduleVersion: 0,
        },
      })
    );
    expect(result.data.nextStatus).toBe('REVIEW');
  });
});
