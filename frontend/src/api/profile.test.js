import { profileApi } from './profile.js';
import { httpClient } from './http.js';

jest.mock('./http.js', () => ({
  httpClient: jest.fn(),
  generateUUID: jest.fn(() => 'mock-uuid'),
}));

describe('profileApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('getProfile calls GET /api/v1/me', async () => {
    const mockEnvelope = {
      success: true,
      data: {
        profile: { displayName: 'Học viên Test', profileVersion: 1 },
      },
      meta: { correlationId: 'mock-uuid' },
    };
    httpClient.mockResolvedValueOnce(mockEnvelope);

    const result = await profileApi.getProfile();
    expect(httpClient).toHaveBeenCalledWith('/api/v1/me', {
      method: 'GET',
    });
    expect(result).toEqual(mockEnvelope);
  });

  test('updateProfile sends PATCH with payload including expectedProfileVersion', async () => {
    const payload = {
      displayName: 'Tên Mới',
      expectedProfileVersion: 1,
    };
    const mockEnvelope = {
      success: true,
      data: {
        profile: { displayName: 'Tên Mới', profileVersion: 2 },
      },
      meta: { correlationId: 'mock-uuid' },
    };
    httpClient.mockResolvedValueOnce(mockEnvelope);

    const result = await profileApi.updateProfile(payload);
    expect(httpClient).toHaveBeenCalledWith('/api/v1/me', {
      method: 'PATCH',
      body: payload,
    });
    expect(result).toEqual(mockEnvelope);
  });

  test('does not expose a learner activity-history client', () => {
    expect(profileApi.getActivity).toBeUndefined();
  });
});
