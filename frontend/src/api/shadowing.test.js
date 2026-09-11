import { uploadRecording, createShadowingAttempt, fetchShadowingAttempts } from './shadowing.js';

describe('shadowing API', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  test('uploadRecording calls POST /api/v1/recordings with FormData', async () => {
    const mockRecording = {
      recordingId: '55555555-5555-5555-5555-555555555555',
      segmentId: '44444444-4444-4444-4444-444444444444',
      status: 'AVAILABLE',
    };
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ success: true, data: mockRecording }),
    });

    const fakeBlob = new Blob(['fake audio'], { type: 'audio/webm' });
    const result = await uploadRecording({
      segmentId: '44444444-4444-4444-4444-4444-444444444444',
      audioBlob: fakeBlob,
      format: 'audio/webm',
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/recordings',
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData),
      }),
    );
    expect(result).toEqual(mockRecording);
  });

  test('createShadowingAttempt calls POST /api/v1/shadowing-attempts with segmentId query param', async () => {
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
      '/api/v1/shadowing-attempts?segmentId=44444444-4444-4444-4444-444444444444',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          recordingId: '55555555-5555-5555-5555-555555555555',
        }),
      }),
    );
    expect(result).toEqual(mockAttempt);
  });
});
