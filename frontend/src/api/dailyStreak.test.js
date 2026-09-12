import { checkInToday, getDailyStreak } from './dailyStreak.js';
import { httpClient } from './http.js';

jest.mock('./http.js', () => ({ httpClient: jest.fn() }));

describe('daily streak API', () => {
  beforeEach(() => jest.clearAllMocks());

  test('uses the protected summary and idempotent check-in endpoints', async () => {
    httpClient.mockResolvedValueOnce({ success: true, data: { currentStreak: 2 } });
    httpClient.mockResolvedValueOnce({ success: true, data: { currentStreak: 3 } });

    await expect(getDailyStreak()).resolves.toEqual({ currentStreak: 2 });
    await expect(checkInToday()).resolves.toEqual({ currentStreak: 3 });
    expect(httpClient).toHaveBeenNthCalledWith(1, '/api/v1/daily-streak', { method: 'GET' });
    expect(httpClient).toHaveBeenNthCalledWith(2, '/api/v1/daily-streak/check-ins', { method: 'POST' });
  });
});
