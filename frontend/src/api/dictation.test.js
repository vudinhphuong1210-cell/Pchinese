import { startDictationAttempt, submitDictationAttempt, fetchDictationAttempts } from './dictation.js';

describe('dictation API', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  test('startDictationAttempt calls POST /api/v1/dictation-attempts', async () => {
    const mockData = {
      dictationAttemptId: '33333333-3333-3333-3333-333333333333',
      segmentId: '44444444-4444-4444-4444-444444444444',
      status: 'IN_PROGRESS',
      pinyinHint: 'nǐ hǎo',
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockData }),
    });

    const result = await startDictationAttempt('44444444-4444-4444-4444-444444444444');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/dictation-attempts',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ segmentId: '44444444-4444-4444-4444-444444444444' }),
      }),
    );
    expect(result).toEqual(mockData);
  });

  test('submitDictationAttempt calls POST /api/v1/dictation-attempts/{attemptId}/submit', async () => {
    const mockResult = {
      dictationAttemptId: '33333333-3333-3333-3333-333333333333',
      correct: true,
      score: 100,
      correctAnswer: '你好',
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockResult }),
    });

    const result = await submitDictationAttempt('33333333-3333-3333-3333-333333333333', '你好');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/dictation-attempts/33333333-3333-3333-3333-333333333333/submit',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ userAnswer: '你好' }),
      }),
    );
    expect(result).toEqual(mockResult);
  });
});
