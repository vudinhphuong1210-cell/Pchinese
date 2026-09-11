import { uploadRecording, createShadowingAttempt, fetchShadowingAttempts } from './shadowing.js';

describe('shadowing API', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  test('uploadRecording calls POST /api/v1/recordings', async () => {
    const mockRecording = {
      recordingId: '55555555-5555-5555-5555-555555555555',
      segmentId: '44444444-4444-4444-4444-444444444444',
      status: 'AVAILABLE',
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockRecording }),
    });

    const result = await uploadRecording({
      segmentId: '44444444-4444-4444-4444-444444444444',
      format: 'audio/webm',
      durationSeconds: 3,
      audioBase64: 'dGVzdA==',
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/recordings',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          segmentId: '44444444-4444-4444-4444-444444444444',
          format: 'audio/webm',
          durationSeconds: 3,
          audioBase64: 'dGVzdA==',
        }),
      }),
    );
    expect(result).toEqual(mockRecording);
  });

  test('createShadowingAttempt calls POST /api/v1/shadowing-attempts', async () => {
    const mockAttempt = {
      shadowingAttemptId: '66666666-6666-6666-6666-666666666666',
      segmentId: '44444444-4444-4444-4444-444444444444',
      overallScore: 85,
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockAttempt }),
    });

    const result = await createShadowingAttempt('44444444-4444-4444-4444-444444444444', '55555555-5555-5555-5555-555555555555');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/shadowing-attempts',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          segmentId: '44444444-4444-4444-4444-444444444444',
          recordingId: '55555555-5555-5555-5555-555555555555',
        }),
      }),
    );
    expect(result).toEqual(mockAttempt);
  });
});
