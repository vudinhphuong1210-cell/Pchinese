import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const uploadRecording = async ({ segmentId, format = 'audio/webm', durationSeconds = 5, audioBase64 }) => {
  const response = await httpClient(apiUrl('/recordings'), {
    method: 'POST',
    body: JSON.stringify({
      segmentId,
      format,
      durationSeconds,
      audioBase64,
    }),
  });
  return response.data;
};

export const createShadowingAttempt = async (segmentId, recordingId) => {
  const response = await httpClient(apiUrl('/shadowing-attempts'), {
    method: 'POST',
    body: JSON.stringify({
      segmentId,
      recordingId,
    }),
  });
  return response.data;
};

export const fetchShadowingAttempts = async (segmentId) => {
  const params = new URLSearchParams();
  if (segmentId) params.append('segmentId', segmentId);
  const response = await httpClient(apiUrl(`/shadowing-attempts?${params.toString()}`), {
    method: 'GET',
  });
  return response.data;
};

export const fetchShadowingAttemptDetail = async (attemptId) => {
  const response = await httpClient(apiUrl(`/shadowing-attempts/${attemptId}`), {
    method: 'GET',
  });
  return response.data;
};
