import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const uploadRecording = async ({ segmentId, audioBlob, file, format = 'audio/webm' }) => {
  const formData = new FormData();
  const audioFile = file || (audioBlob instanceof File ? audioBlob : new File([audioBlob], 'recording.webm', { type: format }));
  formData.append('file', audioFile);
  formData.append('segmentId', segmentId);

  const response = await httpClient(apiUrl('/recordings'), {
    method: 'POST',
    body: formData,
  });
  return response.data;
};

export const createShadowingAttempt = async (segmentId, recordingId) => {
  const response = await httpClient(apiUrl(`/shadowing-attempts?segmentId=${encodeURIComponent(segmentId)}`), {
    method: 'POST',
    body: {
      recordingId,
    },
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
