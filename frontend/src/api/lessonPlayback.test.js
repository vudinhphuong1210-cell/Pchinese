import { fetchLessonPlayback, submitPlaybackEvent, fetchLessonProgress } from './lessonPlayback.js';

describe('lessonPlayback API', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  test('fetchLessonPlayback calls GET /api/v1/lessons/{lessonId}/playback', async () => {
    const mockData = {
      lessonId: '11111111-1111-1111-1111-111111111111',
      title: 'Bài 1: Chào hỏi',
      segments: [],
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockData }),
    });

    const result = await fetchLessonPlayback('11111111-1111-1111-1111-111111111111');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/lessons/11111111-1111-1111-1111-111111111111/playback',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result).toEqual(mockData);
  });

  test('submitPlaybackEvent calls POST /api/v1/lesson-progress/{lessonId}/playback-events', async () => {
    const mockProgress = {
      lessonProgressId: '22222222-2222-2222-2222-222222222222',
      status: 'IN_PROGRESS',
      completionPercentage: 50,
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockProgress }),
    });

    const result = await submitPlaybackEvent('11111111-1111-1111-1111-111111111111', {
      sessionToken: 'test-token',
      eventType: 'PLAYBACK_HEARTBEAT',
      currentPositionSeconds: 15,
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/lesson-progress/11111111-1111-1111-1111-111111111111/playback-events',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          sessionToken: 'test-token',
          eventType: 'PLAYBACK_HEARTBEAT',
          currentPositionSeconds: 15,
        }),
      }),
    );
    expect(result).toEqual(mockProgress);
  });
});
